package com.bakeflow.app.domain.model

data class Ingredient(
    val id: String,
    val ownerId: String,
    val name: String,
    val category: IngredientCategory,
    val unit: IngredientUnit,
    val costPerUnit: Double,
    val currentStock: Double,
    val reorderLevel: Double,
    val status: IngredientStatus
) {
    val isLowStock: Boolean = currentStock <= reorderLevel
}
