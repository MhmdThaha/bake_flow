package com.bakeflow.app.data.repository

import com.bakeflow.app.data.model.IngredientDocument
import com.bakeflow.app.data.model.ProductDocument
import com.bakeflow.app.data.model.StockAdjustmentDocument
import com.bakeflow.app.data.network.NetworkMonitor
import com.bakeflow.app.data.remote.FirestoreListenerHelper
import com.bakeflow.app.data.remote.IngredientFirestoreConstants
import com.bakeflow.app.data.remote.ProductFirestoreConstants
import com.bakeflow.app.data.remote.StockAdjustmentFirestoreConstants
import com.bakeflow.app.domain.model.AdjustmentItemType
import com.bakeflow.app.domain.model.StockAdjustment
import com.bakeflow.app.domain.model.StockAdjustmentSummary
import com.bakeflow.app.domain.repository.RecordStockAdjustmentRequest
import com.bakeflow.app.domain.repository.StockAdjustmentRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class StockAdjustmentRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor
) : StockAdjustmentRepository {

    override fun observeAdjustments(): Flow<List<StockAdjustment>> = callbackFlow {
        val ownerId = firebaseAuth.currentUser?.uid
        if (ownerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(StockAdjustmentFirestoreConstants.COLLECTION)
            .whereEqualTo(StockAdjustmentFirestoreConstants.FIELD_OWNER_ID, ownerId)
            .orderBy(StockAdjustmentFirestoreConstants.FIELD_ADJUSTMENT_DATE, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (FirestoreListenerHelper.isMissingCompositeIndex(error)) {
                        FirestoreListenerHelper.logMissingIndex(
                            error,
                            StockAdjustmentFirestoreConstants.COLLECTION,
                            "ownerId ASC + adjustmentDate DESC"
                        )
                        trySend(emptyList())
                    } else {
                        close(mapFirestoreException(error))
                    }
                    return@addSnapshotListener
                }
                val adjustments = snapshot?.documents
                    ?.mapNotNull { StockAdjustmentDocument.fromSnapshot(it)?.toDomain() }
                    .orEmpty()
                trySend(adjustments)
            }

        awaitClose { listener.remove() }
    }

    override fun observeAdjustmentSummary(): Flow<StockAdjustmentSummary> =
        observeAdjustments()
            .map { adjustments -> computeSummary(adjustments) }
            .flowOn(Dispatchers.Default)

    override suspend fun getAdjustment(adjustmentId: String): Result<StockAdjustment> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(
                StockAdjustmentException("No internet connection. Check your network and try again.")
            )
        }
        return runCatching {
            val snapshot = firestore.collection(StockAdjustmentFirestoreConstants.COLLECTION)
                .document(adjustmentId)
                .get()
                .await()
            val document = StockAdjustmentDocument.fromSnapshot(snapshot)
                ?: throw StockAdjustmentException("Adjustment not found.")
            verifyOwnership(document.ownerId)
            document.toDomain()
        }.mapAdjustmentError()
    }

    override suspend fun recordAdjustment(
        request: RecordStockAdjustmentRequest
    ): Result<StockAdjustment> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(
                StockAdjustmentException("No internet connection. Check your network and try again.")
            )
        }
        if (request.adjustedQuantity < 0) {
            return Result.failure(StockAdjustmentException("Actual stock cannot be negative."))
        }
        if (request.itemId.isBlank()) {
            return Result.failure(StockAdjustmentException("Select an item to adjust."))
        }

        val ownerId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(
                StockAdjustmentException("You must be signed in to record an adjustment.")
            )

        return runCatching {
            val now = Timestamp.now()
            val adjustmentDate = Timestamp(Date(request.adjustmentDateMillis))
            val adjustmentRef = firestore.collection(StockAdjustmentFirestoreConstants.COLLECTION).document()
            val adjustmentId = adjustmentRef.id

            firestore.runTransaction { transaction ->
                val (itemName, previousQuantity) = when (request.itemType) {
                    AdjustmentItemType.INGREDIENT -> {
                        val ref = firestore
                            .collection(IngredientFirestoreConstants.INGREDIENTS_COLLECTION)
                            .document(request.itemId)
                        val doc = IngredientDocument.fromSnapshot(transaction.get(ref))
                            ?: throw StockAdjustmentException("Ingredient not found.")
                        if (doc.ownerId != ownerId) {
                            throw StockAdjustmentException("You do not have access to this ingredient.")
                        }
                        transaction.update(
                            ref,
                            mapOf(
                                IngredientFirestoreConstants.FIELD_CURRENT_STOCK to request.adjustedQuantity,
                                IngredientFirestoreConstants.FIELD_UPDATED_AT to now
                            )
                        )
                        doc.name to doc.currentStock
                    }
                    AdjustmentItemType.PRODUCT -> {
                        val ref = firestore
                            .collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
                            .document(request.itemId)
                        val doc = ProductDocument.fromSnapshot(transaction.get(ref))
                            ?: throw StockAdjustmentException("Product not found.")
                        if (doc.ownerId != ownerId) {
                            throw StockAdjustmentException("You do not have access to this product.")
                        }
                        transaction.update(
                            ref,
                            mapOf(
                                ProductFirestoreConstants.FIELD_FINISHED_STOCK to request.adjustedQuantity,
                                ProductFirestoreConstants.FIELD_UPDATED_AT to now
                            )
                        )
                        doc.name to doc.finishedStock
                    }
                }

                val difference = request.adjustedQuantity - previousQuantity
                val adjustmentDocument = StockAdjustmentDocument(
                    adjustmentId = adjustmentId,
                    ownerId = ownerId,
                    itemType = request.itemType.name,
                    itemId = request.itemId,
                    itemName = itemName,
                    previousQuantity = previousQuantity,
                    adjustedQuantity = request.adjustedQuantity,
                    difference = difference,
                    adjustmentReason = request.reason.name,
                    notes = request.notes.trim(),
                    adjustmentDate = adjustmentDate,
                    createdAt = now,
                    createdBy = request.createdBy
                )
                transaction.set(adjustmentRef, adjustmentDocument.toFirestoreMap())
            }.await()

            getAdjustment(adjustmentId).getOrThrow()
        }.mapAdjustmentError()
    }

    private fun computeSummary(adjustments: List<StockAdjustment>): StockAdjustmentSummary {
        val startOfDay = startOfTodayMillis()
        val todayAdjustments = adjustments.filter { it.adjustmentDate >= startOfDay }
        return StockAdjustmentSummary(
            todayCount = todayAdjustments.size,
            todayDifferenceTotal = todayAdjustments.sumOf { it.difference },
            recentAdjustments = adjustments.take(5)
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
            throw StockAdjustmentException("You do not have access to this adjustment.")
        }
    }

    private fun <T> Result<T>.mapAdjustmentError(): Result<T> = this
        .recoverCatching { throw mapException(it) }

    private fun mapException(throwable: Throwable): StockAdjustmentException = when (throwable) {
        is StockAdjustmentException -> throwable
        is FirebaseFirestoreException -> StockAdjustmentException(
            message = when (throwable.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Unable to save adjustment. Check Firestore permissions."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No internet connection. Check your network and try again."
                else -> throwable.localizedMessage ?: "Adjustment failed. Please try again."
            },
            cause = throwable
        )
        else -> StockAdjustmentException(
            message = throwable.localizedMessage ?: "Adjustment failed. Please try again.",
            cause = throwable
        )
    }

    private fun mapFirestoreException(exception: FirebaseFirestoreException): StockAdjustmentException =
        StockAdjustmentException(
            message = exception.localizedMessage ?: "Failed to load adjustments.",
            cause = exception
        )
}

class StockAdjustmentException(message: String, cause: Throwable? = null) : Exception(message, cause)
