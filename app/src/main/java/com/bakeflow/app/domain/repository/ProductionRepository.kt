package com.bakeflow.app.domain.repository

import com.bakeflow.app.domain.model.ProductionBatch
import com.bakeflow.app.domain.model.ProductionDashboardStats
import kotlinx.coroutines.flow.Flow

data class ExecuteProductionRequest(
    val productId: String,
    val productName: String,
    val recipeId: String,
    val quantityProduced: Double,
    val createdBy: String
)

interface ProductionRepository {
    fun observeBatches(): Flow<List<ProductionBatch>>

    fun observeDashboardStats(): Flow<ProductionDashboardStats>

    suspend fun getBatch(batchId: String): Result<ProductionBatch>

    suspend fun executeProduction(request: ExecuteProductionRequest): Result<ProductionBatch>
}
