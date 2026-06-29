package com.bakeflow.app.data.repository

import com.bakeflow.app.data.model.ProductDocument
import com.bakeflow.app.data.network.NetworkMonitor
import com.bakeflow.app.data.remote.FirestoreListenerHelper
import com.bakeflow.app.data.remote.ProductFirestoreConstants
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.repository.ProductRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor
) : ProductRepository {

    override fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val ownerId = firebaseAuth.currentUser?.uid
        if (ownerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
            .whereEqualTo(ProductFirestoreConstants.FIELD_OWNER_ID, ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (FirestoreListenerHelper.isMissingCompositeIndex(error)) {
                        FirestoreListenerHelper.logMissingIndex(
                            error,
                            ProductFirestoreConstants.PRODUCTS_COLLECTION,
                            "ownerId filter"
                        )
                        trySend(emptyList())
                    } else {
                        close(mapFirestoreException(error))
                    }
                    return@addSnapshotListener
                }
                val products = snapshot?.documents
                    ?.mapNotNull { ProductDocument.fromSnapshot(it)?.toDomain() }
                    ?.sortedBy { it.name.lowercase() }
                    .orEmpty()
                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getProduct(productId: String): Result<Product> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(ProductException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val snapshot = firestore.collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
                .document(productId)
                .get()
                .await()
            val document = ProductDocument.fromSnapshot(snapshot)
                ?: throw ProductException("Product not found.")
            verifyOwnership(document.ownerId)
            document.toDomain()
        }.mapProductError()
    }

    override suspend fun addProduct(product: Product): Result<Product> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(ProductException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val ownerId = requireOwnerId()
            val documentId = firestore.collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
                .document()
                .id
            val now = Timestamp.now()
            val document = ProductDocument(
                id = documentId,
                ownerId = ownerId,
                name = product.name.trim(),
                category = product.category.trim(),
                sellingPrice = product.sellingPrice,
                finishedStock = 0.0,
                status = product.status.name,
                createdAt = now,
                updatedAt = now
            )
            firestore.collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
                .document(documentId)
                .set(document.toFirestoreMap())
                .await()
            document.toDomain()
        }.mapProductError()
    }

    override suspend fun updateProduct(product: Product): Result<Product> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(ProductException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val ownerId = requireOwnerId()
            verifyOwnership(product.ownerId)
            val now = Timestamp.now()
            val existing = firestore.collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
                .document(product.id)
                .get()
                .await()
            val createdAt = existing.getTimestamp(ProductFirestoreConstants.FIELD_CREATED_AT) ?: now
            val existingFinishedStock = existing.getDouble(ProductFirestoreConstants.FIELD_FINISHED_STOCK) ?: 0.0
            val document = ProductDocument(
                id = product.id,
                ownerId = ownerId,
                name = product.name.trim(),
                category = product.category.trim(),
                sellingPrice = product.sellingPrice,
                finishedStock = existingFinishedStock,
                status = product.status.name,
                createdAt = createdAt,
                updatedAt = now
            )
            firestore.collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
                .document(product.id)
                .set(document.toFirestoreMap())
                .await()
            document.toDomain()
        }.mapProductError()
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(ProductException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val snapshot = firestore.collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
                .document(productId)
                .get()
                .await()
            val document = ProductDocument.fromSnapshot(snapshot)
                ?: throw ProductException("Product not found.")
            verifyOwnership(document.ownerId)
            firestore.collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
                .document(productId)
                .delete()
                .await()
            Unit
        }.mapProductError()
    }

    private fun requireOwnerId(): String =
        firebaseAuth.currentUser?.uid
            ?: throw ProductException("You must be signed in to manage products.")

    private fun verifyOwnership(ownerId: String) {
        val currentOwnerId = firebaseAuth.currentUser?.uid
        if (currentOwnerId == null || currentOwnerId != ownerId) {
            throw ProductException("You do not have permission to modify this product.")
        }
    }

    private fun mapFirestoreException(exception: FirebaseFirestoreException): ProductException =
        ProductException(
            when (exception.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No internet connection. Check your network and try again."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied. Check your Firestore security rules."
                else -> "Failed to load products. Please try again."
            },
            exception
        )

    private fun <T> Result<T>.mapProductError(): Result<T> = recoverCatching { throw mapException(it) }

    private fun mapException(throwable: Throwable): ProductException =
        when (throwable) {
            is ProductException -> throwable
            is FirebaseFirestoreException -> mapFirestoreException(throwable)
            else -> ProductException(
                throwable.localizedMessage ?: "Something went wrong. Please try again.",
                throwable
            )
        }
}

class ProductException(message: String, cause: Throwable? = null) : Exception(message, cause)
