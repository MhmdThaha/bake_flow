package com.bakeflow.app.domain.repository

import com.bakeflow.app.domain.model.Ingredient
import kotlinx.coroutines.flow.Flow

interface IngredientRepository {
    fun observeIngredients(): Flow<List<Ingredient>>

    suspend fun getIngredient(ingredientId: String): Result<Ingredient>

    suspend fun addIngredient(ingredient: Ingredient): Result<Ingredient>

    suspend fun updateIngredient(ingredient: Ingredient): Result<Ingredient>

    suspend fun deleteIngredient(ingredientId: String): Result<Unit>
}
