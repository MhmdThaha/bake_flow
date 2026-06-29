package com.bakeflow.app.domain.repository

import com.bakeflow.app.domain.model.Purchase
import com.bakeflow.app.domain.model.PurchaseSummary
import kotlinx.coroutines.flow.Flow

data class RecordPurchaseRequest(
    val ingredientId: String,
    val quantity: Double,
    val costPerUnit: Double,
    val supplierName: String,
    val invoiceNumber: String,
    val notes: String,
    val purchaseDateMillis: Long,
    val createdBy: String
)

interface PurchaseRepository {
    fun observePurchases(): Flow<List<Purchase>>

    fun observePurchaseSummary(): Flow<PurchaseSummary>

    suspend fun getPurchase(purchaseId: String): Result<Purchase>

    suspend fun recordPurchase(request: RecordPurchaseRequest): Result<Purchase>
}
