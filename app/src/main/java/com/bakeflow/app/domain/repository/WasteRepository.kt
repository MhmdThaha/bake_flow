package com.bakeflow.app.domain.repository

import com.bakeflow.app.domain.model.Waste
import com.bakeflow.app.domain.model.WasteReason
import com.bakeflow.app.domain.model.WasteSummary
import kotlinx.coroutines.flow.Flow

data class RecordWasteRequest(
    val productId: String,
    val quantity: Double,
    val reason: WasteReason,
    val notes: String,
    val wasteDateMillis: Long,
    val createdBy: String
)

interface WasteRepository {
    fun observeWaste(): Flow<List<Waste>>

    fun observeWasteSummary(): Flow<WasteSummary>

    suspend fun getWaste(wasteId: String): Result<Waste>

    suspend fun recordWaste(request: RecordWasteRequest): Result<Waste>
}
