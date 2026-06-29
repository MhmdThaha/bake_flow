package com.bakeflow.app.domain.repository

import com.bakeflow.app.domain.model.PaymentMethod
import com.bakeflow.app.domain.model.Sale
import com.bakeflow.app.domain.model.SalesSummary
import kotlinx.coroutines.flow.Flow

data class RecordSaleRequest(
    val productId: String,
    val quantity: Double,
    val unitPrice: Double,
    val paymentMethod: PaymentMethod,
    val customerName: String,
    val customerPhone: String,
    val notes: String,
    val saleDateMillis: Long,
    val createdBy: String
)

interface SaleRepository {
    fun observeSales(): Flow<List<Sale>>

    fun observeSalesSummary(): Flow<SalesSummary>

    suspend fun getSale(saleId: String): Result<Sale>

    suspend fun recordSale(request: RecordSaleRequest): Result<Sale>
}
