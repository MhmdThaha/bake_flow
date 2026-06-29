package com.bakeflow.app.domain.repository

import com.bakeflow.app.domain.model.AdjustmentItemType
import com.bakeflow.app.domain.model.AdjustmentReason
import com.bakeflow.app.domain.model.StockAdjustment
import com.bakeflow.app.domain.model.StockAdjustmentSummary
import kotlinx.coroutines.flow.Flow

data class RecordStockAdjustmentRequest(
    val itemType: AdjustmentItemType,
    val itemId: String,
    val adjustedQuantity: Double,
    val reason: AdjustmentReason,
    val notes: String,
    val adjustmentDateMillis: Long,
    val createdBy: String
)

interface StockAdjustmentRepository {
    fun observeAdjustments(): Flow<List<StockAdjustment>>

    fun observeAdjustmentSummary(): Flow<StockAdjustmentSummary>

    suspend fun getAdjustment(adjustmentId: String): Result<StockAdjustment>

    suspend fun recordAdjustment(request: RecordStockAdjustmentRequest): Result<StockAdjustment>
}
