package com.bakeflow.app.data.repository

import com.bakeflow.app.data.model.IngredientDocument
import com.bakeflow.app.data.network.NetworkMonitor
import com.bakeflow.app.data.remote.FirestoreListenerHelper
import com.bakeflow.app.data.remote.IngredientFirestoreConstants
import com.bakeflow.app.domain.model.Ingredient
import com.bakeflow.app.domain.repository.IngredientRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class IngredientRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor
) : IngredientRepository {

    override fun observeIngredients(): Flow<List<Ingredient>> = callbackFlow {
        val ownerId = firebaseAuth.currentUser?.uid
        if (ownerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(IngredientFirestoreConstants.INGREDIENTS_COLLECTION)
            .whereEqualTo(IngredientFirestoreConstants.FIELD_OWNER_ID, ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (FirestoreListenerHelper.isMissingCompositeIndex(error)) {
                        FirestoreListenerHelper.logMissingIndex(
                            error,
                            IngredientFirestoreConstants.INGREDIENTS_COLLECTION,
                            "ownerId filter"
                        )
                        trySend(emptyList())
                    } else {
                        close(mapFirestoreException(error))
                    }
                    return@addSnapshotListener
                }
                val ingredients = snapshot?.documents
                    ?.mapNotNull { IngredientDocument.fromSnapshot(it)?.toDomain() }
                    ?.sortedBy { it.name.lowercase() }
                    .orEmpty()
                trySend(ingredients)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getIngredient(ingredientId: String): Result<Ingredient> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(IngredientException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val snapshot = firestore.collection(IngredientFirestoreConstants.INGREDIENTS_COLLECTION)
                .document(ingredientId)
                .get()
                .await()
            val document = IngredientDocument.fromSnapshot(snapshot)
                ?: throw IngredientException("Ingredient not found.")
            verifyOwnership(document.ownerId)
            document.toDomain()
        }.mapIngredientError()
    }

    override suspend fun addIngredient(ingredient: Ingredient): Result<Ingredient> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(IngredientException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val ownerId = requireOwnerId()
            val documentId = firestore.collection(IngredientFirestoreConstants.INGREDIENTS_COLLECTION)
                .document()
                .id
            val now = Timestamp.now()
            val document = IngredientDocument(
                id = documentId,
                ownerId = ownerId,
                name = ingredient.name.trim(),
                category = ingredient.category.name,
                unit = ingredient.unit.name,
                costPerUnit = ingredient.costPerUnit,
                currentStock = ingredient.currentStock,
                reorderLevel = ingredient.reorderLevel,
                status = ingredient.status.name,
                createdAt = now,
                updatedAt = now
            )
            firestore.collection(IngredientFirestoreConstants.INGREDIENTS_COLLECTION)
                .document(documentId)
                .set(document.toFirestoreMap())
                .await()
            document.toDomain()
        }.mapIngredientError()
    }

    override suspend fun updateIngredient(ingredient: Ingredient): Result<Ingredient> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(IngredientException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val ownerId = requireOwnerId()
            verifyOwnership(ingredient.ownerId)
            val now = Timestamp.now()
            val existing = firestore.collection(IngredientFirestoreConstants.INGREDIENTS_COLLECTION)
                .document(ingredient.id)
                .get()
                .await()
            val createdAt = existing.getTimestamp(IngredientFirestoreConstants.FIELD_CREATED_AT) ?: now
            val document = IngredientDocument(
                id = ingredient.id,
                ownerId = ownerId,
                name = ingredient.name.trim(),
                category = ingredient.category.name,
                unit = ingredient.unit.name,
                costPerUnit = ingredient.costPerUnit,
                currentStock = ingredient.currentStock,
                reorderLevel = ingredient.reorderLevel,
                status = ingredient.status.name,
                createdAt = createdAt,
                updatedAt = now
            )
            firestore.collection(IngredientFirestoreConstants.INGREDIENTS_COLLECTION)
                .document(ingredient.id)
                .set(document.toFirestoreMap())
                .await()
            document.toDomain()
        }.mapIngredientError()
    }

    override suspend fun deleteIngredient(ingredientId: String): Result<Unit> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(IngredientException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val snapshot = firestore.collection(IngredientFirestoreConstants.INGREDIENTS_COLLECTION)
                .document(ingredientId)
                .get()
                .await()
            val document = IngredientDocument.fromSnapshot(snapshot)
                ?: throw IngredientException("Ingredient not found.")
            verifyOwnership(document.ownerId)
            firestore.collection(IngredientFirestoreConstants.INGREDIENTS_COLLECTION)
                .document(ingredientId)
                .delete()
                .await()
            Unit
        }.mapIngredientError()
    }

    private fun requireOwnerId(): String =
        firebaseAuth.currentUser?.uid
            ?: throw IngredientException("You must be signed in to manage ingredients.")

    private fun verifyOwnership(ownerId: String) {
        val currentOwnerId = firebaseAuth.currentUser?.uid
        if (currentOwnerId == null || currentOwnerId != ownerId) {
            throw IngredientException("You do not have permission to modify this ingredient.")
        }
    }

    private fun mapFirestoreException(exception: FirebaseFirestoreException): IngredientException =
        IngredientException(
            when (exception.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No internet connection. Check your network and try again."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied. Check your Firestore security rules."
                else -> "Failed to load ingredients. Please try again."
            },
            exception
        )

    private fun <T> Result<T>.mapIngredientError(): Result<T> =
        recoverCatching { throw mapException(it) }

    private fun mapException(throwable: Throwable): IngredientException =
        when (throwable) {
            is IngredientException -> throwable
            is FirebaseFirestoreException -> mapFirestoreException(throwable)
            else -> IngredientException(
                throwable.localizedMessage ?: "Something went wrong. Please try again.",
                throwable
            )
        }
}

class IngredientException(message: String, cause: Throwable? = null) : Exception(message, cause)
