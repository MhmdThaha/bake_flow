package com.bakeflow.app.data.repository

import com.bakeflow.app.data.model.ProductDocument
import com.bakeflow.app.data.model.WasteDocument
import com.bakeflow.app.data.network.NetworkMonitor
import com.bakeflow.app.data.remote.FirestoreListenerHelper
import com.bakeflow.app.data.remote.ProductFirestoreConstants
import com.bakeflow.app.data.remote.WasteFirestoreConstants
import com.bakeflow.app.domain.model.Waste
import com.bakeflow.app.domain.model.WasteSummary
import com.bakeflow.app.domain.repository.RecordWasteRequest
import com.bakeflow.app.domain.repository.WasteRepository
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

class WasteRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor
) : WasteRepository {

    override fun observeWaste(): Flow<List<Waste>> = callbackFlow {
        val ownerId = firebaseAuth.currentUser?.uid
        if (ownerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(WasteFirestoreConstants.COLLECTION)
            .whereEqualTo(WasteFirestoreConstants.FIELD_OWNER_ID, ownerId)
            .orderBy(WasteFirestoreConstants.FIELD_WASTE_DATE, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (FirestoreListenerHelper.isMissingCompositeIndex(error)) {
                        FirestoreListenerHelper.logMissingIndex(
                            error,
                            WasteFirestoreConstants.COLLECTION,
                            "ownerId ASC + wasteDate DESC"
                        )
                        trySend(emptyList())
                    } else {
                        close(mapFirestoreException(error))
                    }
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents
                    ?.mapNotNull { WasteDocument.fromSnapshot(it)?.toDomain() }
                    .orEmpty()
                trySend(entries)
            }

        awaitClose { listener.remove() }
    }

    override fun observeWasteSummary(): Flow<WasteSummary> =
        observeWaste()
            .map { waste -> computeSummary(waste) }
            .flowOn(Dispatchers.Default)

    override suspend fun getWaste(wasteId: String): Result<Waste> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(WasteException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val snapshot = firestore.collection(WasteFirestoreConstants.COLLECTION)
                .document(wasteId)
                .get()
                .await()
            val document = WasteDocument.fromSnapshot(snapshot)
                ?: throw WasteException("Waste entry not found.")
            verifyOwnership(document.ownerId)
            document.toDomain()
        }.mapWasteError()
    }

    override suspend fun recordWaste(request: RecordWasteRequest): Result<Waste> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(WasteException("No internet connection. Check your network and try again."))
        }
        if (request.quantity <= 0) {
            return Result.failure(WasteException("Quantity must be greater than zero."))
        }
        if (request.productId.isBlank()) {
            return Result.failure(WasteException("Select a product."))
        }

        val ownerId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(WasteException("You must be signed in to record waste."))

        return runCatching {
            val now = Timestamp.now()
            val wasteDate = Timestamp(Date(request.wasteDateMillis))
            val wasteRef = firestore.collection(WasteFirestoreConstants.COLLECTION).document()
            val wasteId = wasteRef.id

            firestore.runTransaction { transaction ->
                val productRef = firestore
                    .collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
                    .document(request.productId)
                val productSnapshot = transaction.get(productRef)
                val productDoc = ProductDocument.fromSnapshot(productSnapshot)
                    ?: throw WasteException("Product not found.")
                if (productDoc.ownerId != ownerId) {
                    throw WasteException("You do not have access to this product.")
                }
                if (productDoc.finishedStock < request.quantity) {
                    throw WasteException(
                        "Insufficient finished stock for ${productDoc.name}. " +
                            "Available ${formatQty(productDoc.finishedStock)}, " +
                            "requested ${formatQty(request.quantity)}."
                    )
                }

                val newStock = productDoc.finishedStock - request.quantity
                if (newStock < 0) {
                    throw WasteException("Stock update would result in negative inventory.")
                }

                val estimatedLoss = request.quantity * productDoc.sellingPrice

                transaction.update(
                    productRef,
                    mapOf(
                        ProductFirestoreConstants.FIELD_FINISHED_STOCK to newStock,
                        ProductFirestoreConstants.FIELD_UPDATED_AT to now
                    )
                )

                val wasteDocument = WasteDocument(
                    wasteId = wasteId,
                    ownerId = ownerId,
                    productId = productDoc.id,
                    productName = productDoc.name,
                    quantity = request.quantity,
                    reason = request.reason.name,
                    notes = request.notes.trim(),
                    estimatedLoss = estimatedLoss,
                    wasteDate = wasteDate,
                    createdAt = now,
                    createdBy = request.createdBy
                )
                transaction.set(wasteRef, wasteDocument.toFirestoreMap())
            }.await()

            getWaste(wasteId).getOrThrow()
        }.mapWasteError()
    }

    private fun computeSummary(waste: List<Waste>): WasteSummary {
        val startOfDay = startOfTodayMillis()
        val todayWaste = waste.filter { it.wasteDate >= startOfDay }
        return WasteSummary(
            todayCount = todayWaste.size,
            todayQuantity = todayWaste.sumOf { it.quantity },
            todayEstimatedLoss = todayWaste.sumOf { it.estimatedLoss },
            recentWaste = waste.take(5)
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
            throw WasteException("You do not have access to this waste entry.")
        }
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.2f", value)

    private fun <T> Result<T>.mapWasteError(): Result<T> = this
        .recoverCatching { throw mapException(it) }

    private fun mapException(throwable: Throwable): WasteException = when (throwable) {
        is WasteException -> throwable
        is FirebaseFirestoreException -> WasteException(
            message = when (throwable.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Unable to save waste. Check Firestore permissions."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No internet connection. Check your network and try again."
                else -> throwable.localizedMessage ?: "Waste recording failed. Please try again."
            },
            cause = throwable
        )
        else -> WasteException(
            message = throwable.localizedMessage ?: "Waste recording failed. Please try again.",
            cause = throwable
        )
    }

    private fun mapFirestoreException(exception: FirebaseFirestoreException): WasteException =
        WasteException(
            message = exception.localizedMessage ?: "Failed to load waste history.",
            cause = exception
        )
}

class WasteException(message: String, cause: Throwable? = null) : Exception(message, cause)
