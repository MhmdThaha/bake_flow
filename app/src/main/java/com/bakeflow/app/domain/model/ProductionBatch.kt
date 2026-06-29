package com.bakeflow.app.domain.model

data class ProductionBatch(
    val batchId: String,
    val ownerId: String,
    val productId: String,
    val productName: String,
    val recipeId: String,
    val quantityProduced: Double,
    val status: ProductionStatus,
    val estimatedCost: Double,
    val createdAt: Long,
    val completedAt: Long?,
    val createdBy: String,
    val ingredientUsage: List<IngredientUsage>
)

data class ProductionDashboardStats(
    val todayCompleted: Int = 0,
    val todayPending: Int = 0,
    val todayBatchCount: Int = 0
)

data class ProductionRequirementLine(
    val ingredientId: String,
    val ingredientName: String,
    val unit: String,
    val requiredQuantity: Double,
    val availableQuantity: Double,
    val costPerUnit: Double,
    val totalCost: Double,
    val hasShortage: Boolean
) {
    val shortageAmount: Double =
        (requiredQuantity - availableQuantity).coerceAtLeast(0.0)
}
