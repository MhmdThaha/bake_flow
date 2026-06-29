package com.bakeflow.app.domain.model

data class Recipe(
    val id: String,
    val ownerId: String,
    val productId: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

data class RecipeWithItems(
    val recipe: Recipe,
    val items: List<RecipeItem>
)
