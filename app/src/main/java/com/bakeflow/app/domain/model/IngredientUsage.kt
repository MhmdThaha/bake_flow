package com.bakeflow.app.domain.model

data class IngredientUsage(
    val ingredientId: String,
    val ingredientName: String,
    val requiredQuantity: Double,
    val unit: String,
    val costPerUnit: Double,
    val totalCost: Double
)
