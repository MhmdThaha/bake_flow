package com.bakeflow.app.data.repository

import com.bakeflow.app.data.model.ProductDocument
import com.bakeflow.app.data.model.SaleDocument
import com.bakeflow.app.data.network.NetworkMonitor
import com.bakeflow.app.data.remote.ProductFirestoreConstants
import com.bakeflow.app.data.remote.FirestoreListenerHelper
import com.bakeflow.app.data.remote.SaleFirestoreConstants
import com.bakeflow.app.domain.model.ProductSoldToday
import com.bakeflow.app.domain.model.Sale
import com.bakeflow.app.domain.model.SalesSummary
import com.bakeflow.app.domain.repository.RecordSaleRequest
import com.bakeflow.app.domain.repository.SaleRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import java.util.Date

class SaleRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor
) : SaleRepository {

    override fun observeSales(): Flow<List<Sale>> = callbackFlow {
        val ownerId = firebaseAuth.currentUser?.uid
        if (ownerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(SaleFirestoreConstants.COLLECTION)
            .whereEqualTo(SaleFirestoreConstants.FIELD_OWNER_ID, ownerId)
            .orderBy(SaleFirestoreConstants.FIELD_SALE_DATE, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (FirestoreListenerHelper.isMissingCompositeIndex(error)) {
                        FirestoreListenerHelper.logMissingIndex(
                            error,
                            SaleFirestoreConstants.COLLECTION,
                            "ownerId ASC + saleDate DESC"
                        )
                        trySend(emptyList())
                    } else {
                        close(mapFirestoreException(error))
                    }
                    return@addSnapshotListener
                }
                val sales = snapshot?.documents
                    ?.mapNotNull { SaleDocument.fromSnapshot(it)?.toDomain() }
                    .orEmpty()
                trySend(sales)
            }

        awaitClose { listener.remove() }
    }

    override fun observeSalesSummary(): Flow<SalesSummary> =
        observeSales()
            .map { sales -> computeSummary(sales) }
            .flowOn(Dispatchers.Default)

    override suspend fun getSale(saleId: String): Result<Sale> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(SaleException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val snapshot = firestore.collection(SaleFirestoreConstants.COLLECTION)
                .document(saleId)
                .get()
                .await()
            val document = SaleDocument.fromSnapshot(snapshot)
                ?: throw SaleException("Sale not found.")
            verifyOwnership(document.ownerId)
            document.toDomain()
        }.mapSaleError()
    }

    override suspend fun recordSale(request: RecordSaleRequest): Result<Sale> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(SaleException("No internet connection. Check your network and try again."))
        }
        if (request.quantity <= 0) {
            return Result.failure(SaleException("Quantity must be greater than zero."))
        }
        if (request.unitPrice <= 0) {
            return Result.failure(SaleException("Unit price must be greater than zero."))
        }
        if (request.productId.isBlank()) {
            return Result.failure(SaleException("Select a product."))
        }

        val ownerId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(SaleException("You must be signed in to record a sale."))

        return runCatching {
            val now = Timestamp.now()
            val saleDate = Timestamp(Date(request.saleDateMillis))
            val saleRef = firestore.collection(SaleFirestoreConstants.COLLECTION).document()
            val saleId = saleRef.id
            val totalAmount = request.quantity * request.unitPrice

            firestore.runTransaction { transaction ->
                val productRef = firestore
                    .collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
                    .document(request.productId)
                val productSnapshot = transaction.get(productRef)
                val productDoc = ProductDocument.fromSnapshot(productSnapshot)
                    ?: throw SaleException("Product not found.")
                if (productDoc.ownerId != ownerId) {
                    throw SaleException("You do not have access to this product.")
                }
                if (productDoc.finishedStock < request.quantity) {
                    throw SaleException(
                        "Insufficient finished stock for ${productDoc.name}. " +
                            "Available ${formatQty(productDoc.finishedStock)}, " +
                            "requested ${formatQty(request.quantity)}."
                    )
                }

                val newStock = productDoc.finishedStock - request.quantity
                if (newStock < 0) {
                    throw SaleException("Stock update would result in negative inventory.")
                }

                transaction.update(
                    productRef,
                    mapOf(
                        ProductFirestoreConstants.FIELD_FINISHED_STOCK to newStock,
                        ProductFirestoreConstants.FIELD_UPDATED_AT to now
                    )
                )

                val saleDocument = SaleDocument(
                    saleId = saleId,
                    ownerId = ownerId,
                    productId = productDoc.id,
                    productName = productDoc.name,
                    quantity = request.quantity,
                    unitPrice = request.unitPrice,
                    totalAmount = totalAmount,
                    paymentMethod = request.paymentMethod.name,
                    customerName = request.customerName.trim(),
                    customerPhone = request.customerPhone.trim(),
                    notes = request.notes.trim(),
                    saleDate = saleDate,
                    createdAt = now,
                    createdBy = request.createdBy
                )
                transaction.set(saleRef, saleDocument.toFirestoreMap())
            }.await()

            getSale(saleId).getOrThrow()
        }.mapSaleError()
    }

    private fun computeSummary(sales: List<Sale>): SalesSummary {
        val startOfDay = startOfTodayMillis()
        val todaySales = sales.filter { it.saleDate >= startOfDay }
        val grouped = todaySales.groupBy { it.productName }
        val productsSoldToday = grouped.map { (name, items) ->
            ProductSoldToday(
                productName = name,
                quantitySold = items.sumOf { it.quantity },
                revenue = items.sumOf { it.totalAmount }
            )
        }.sortedByDescending { it.quantitySold }

        val best = productsSoldToday.firstOrNull()
        return SalesSummary(
            todayCount = todaySales.size,
            todayRevenue = todaySales.sumOf { it.totalAmount },
            bestSellingProductName = best?.productName ?: "—",
            bestSellingQuantity = best?.quantitySold ?: 0.0,
            productsSoldToday = productsSoldToday
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
            throw SaleException("You do not have access to this sale.")
        }
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.2f", value)

    private fun <T> Result<T>.mapSaleError(): Result<T> = this
        .recoverCatching { throw mapException(it) }

    private fun mapException(throwable: Throwable): SaleException = when (throwable) {
        is SaleException -> throwable
        is FirebaseFirestoreException -> SaleException(
            message = when (throwable.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Unable to save sale. Check Firestore permissions."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No internet connection. Check your network and try again."
                else -> throwable.localizedMessage ?: "Sale failed. Please try again."
            },
            cause = throwable
        )
        else -> SaleException(
            message = throwable.localizedMessage ?: "Sale failed. Please try again.",
            cause = throwable
        )
    }

    private fun mapFirestoreException(exception: FirebaseFirestoreException): SaleException =
        SaleException(
            message = exception.localizedMessage ?: "Failed to load sales.",
            cause = exception
        )
}

class SaleException(message: String, cause: Throwable? = null) : Exception(message, cause)
