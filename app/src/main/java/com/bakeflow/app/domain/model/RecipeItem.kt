package com.bakeflow.app.domain.model

data class RecipeItem(
    val id: String,
    val recipeId: String,
    val ingredientId: String,
    val quantity: Double,
    val unit: String
)
