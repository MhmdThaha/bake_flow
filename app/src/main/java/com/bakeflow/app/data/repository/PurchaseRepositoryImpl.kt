package com.bakeflow.app.data.repository

import com.bakeflow.app.data.model.IngredientDocument
import com.bakeflow.app.data.model.PurchaseDocument
import com.bakeflow.app.data.network.NetworkMonitor
import com.bakeflow.app.data.remote.IngredientFirestoreConstants
import com.bakeflow.app.data.remote.FirestoreListenerHelper
import com.bakeflow.app.data.remote.PurchaseFirestoreConstants
import com.bakeflow.app.domain.model.Purchase
import com.bakeflow.app.domain.model.PurchaseSummary
import com.bakeflow.app.domain.repository.PurchaseRepository
import com.bakeflow.app.domain.repository.RecordPurchaseRequest
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class PurchaseRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor
) : PurchaseRepository {

    override fun observePurchases(): Flow<List<Purchase>> = callbackFlow {
        val ownerId = firebaseAuth.currentUser?.uid
        if (ownerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(PurchaseFirestoreConstants.COLLECTION)
            .whereEqualTo(PurchaseFirestoreConstants.FIELD_OWNER_ID, ownerId)
            .orderBy(PurchaseFirestoreConstants.FIELD_PURCHASE_DATE, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (FirestoreListenerHelper.isMissingCompositeIndex(error)) {
                        FirestoreListenerHelper.logMissingIndex(
                            error,
                            PurchaseFirestoreConstants.COLLECTION,
                            "ownerId ASC + purchaseDate DESC"
                        )
                        trySend(emptyList())
                    } else {
                        close(mapFirestoreException(error))
                    }
                    return@addSnapshotListener
                }
                val purchases = snapshot?.documents
                    ?.mapNotNull { PurchaseDocument.fromSnapshot(it)?.toDomain() }
                    .orEmpty()
                trySend(purchases)
            }

        awaitClose { listener.remove() }
    }

    override fun observePurchaseSummary(): Flow<PurchaseSummary> =
        observePurchases().map { purchases -> computeSummary(purchases) }

    override suspend fun getPurchase(purchaseId: String): Result<Purchase> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(PurchaseException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val snapshot = firestore.collection(PurchaseFirestoreConstants.COLLECTION)
                .document(purchaseId)
                .get()
                .await()
            val document = PurchaseDocument.fromSnapshot(snapshot)
                ?: throw PurchaseException("Purchase not found.")
            verifyOwnership(document.ownerId)
            document.toDomain()
        }.mapPurchaseError()
    }

    override suspend fun recordPurchase(request: RecordPurchaseRequest): Result<Purchase> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(PurchaseException("No internet connection. Check your network and try again."))
        }
        if (request.quantity <= 0) {
            return Result.failure(PurchaseException("Quantity must be greater than zero."))
        }
        if (request.costPerUnit <= 0) {
            return Result.failure(PurchaseException("Cost per unit must be greater than zero."))
        }
        if (request.ingredientId.isBlank()) {
            return Result.failure(PurchaseException("Select an ingredient."))
        }

        val ownerId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(PurchaseException("You must be signed in to record a purchase."))

        return runCatching {
            val now = Timestamp.now()
            val purchaseDate = Timestamp(Date(request.purchaseDateMillis))
            val purchaseRef = firestore.collection(PurchaseFirestoreConstants.COLLECTION).document()
            val purchaseId = purchaseRef.id
            val totalCost = request.quantity * request.costPerUnit

            firestore.runTransaction { transaction ->
                val ingredientRef = firestore
                    .collection(IngredientFirestoreConstants.INGREDIENTS_COLLECTION)
                    .document(request.ingredientId)
                val ingredientSnapshot = transaction.get(ingredientRef)
                val ingredientDoc = IngredientDocument.fromSnapshot(ingredientSnapshot)
                    ?: throw PurchaseException("Ingredient not found.")
                if (ingredientDoc.ownerId != ownerId) {
                    throw PurchaseException("You do not have access to this ingredient.")
                }

                val newStock = ingredientDoc.currentStock + request.quantity
                if (newStock < 0) {
                    throw PurchaseException("Stock update would result in negative inventory.")
                }

                transaction.update(
                    ingredientRef,
                    mapOf(
                        IngredientFirestoreConstants.FIELD_CURRENT_STOCK to newStock,
                        IngredientFirestoreConstants.FIELD_UPDATED_AT to now
                    )
                )

                val purchaseDocument = PurchaseDocument(
                    purchaseId = purchaseId,
                    ownerId = ownerId,
                    ingredientId = ingredientDoc.id,
                    ingredientName = ingredientDoc.name,
                    supplierName = request.supplierName.trim(),
                    quantity = request.quantity,
                    unit = ingredientDoc.unit,
                    costPerUnit = request.costPerUnit,
                    totalCost = totalCost,
                    purchaseDate = purchaseDate,
                    invoiceNumber = request.invoiceNumber.trim(),
                    notes = request.notes.trim(),
                    createdAt = now,
                    createdBy = request.createdBy
                )
                transaction.set(purchaseRef, purchaseDocument.toFirestoreMap())
            }.await()

            getPurchase(purchaseId).getOrThrow()
        }.mapPurchaseError()
    }

    private fun computeSummary(purchases: List<Purchase>): PurchaseSummary {
        val startOfDay = startOfTodayMillis()
        val todayPurchases = purchases.filter { it.purchaseDate >= startOfDay }
        return PurchaseSummary(
            todayCount = todayPurchases.size,
            todayTotalCost = todayPurchases.sumOf { it.totalCost }
        )
    }

    private fun startOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun verifyOwnership(ownerId: String) {
        val currentOwner = firebaseAuth.currentUser?.uid
        if (currentOwner == null || currentOwner != ownerId) {
            throw PurchaseException("You do not have access to this purchase.")
        }
    }

    private fun <T> Result<T>.mapPurchaseError(): Result<T> = this
        .recoverCatching { throw mapException(it) }

    private fun mapException(throwable: Throwable): PurchaseException = when (throwable) {
        is PurchaseException -> throwable
        is FirebaseFirestoreException -> PurchaseException(
            message = when (throwable.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Unable to save purchase. Check Firestore permissions."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No internet connection. Check your network and try again."
                else -> throwable.localizedMessage ?: "Purchase failed. Please try again."
            },
            cause = throwable
        )
        else -> PurchaseException(
            message = throwable.localizedMessage ?: "Purchase failed. Please try again.",
            cause = throwable
        )
    }

    private fun mapFirestoreException(exception: FirebaseFirestoreException): PurchaseException =
        PurchaseException(
            message = exception.localizedMessage ?: "Failed to load purchases.",
            cause = exception
        )
}

class PurchaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
