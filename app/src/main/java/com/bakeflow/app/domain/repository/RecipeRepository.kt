package com.bakeflow.app.domain.repository

import com.bakeflow.app.domain.model.Recipe
import com.bakeflow.app.domain.model.RecipeItem
import com.bakeflow.app.domain.model.RecipeWithItems
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeRecipes(): Flow<List<Recipe>>

    fun observeRecipeItems(): Flow<List<RecipeItem>>

    suspend fun getRecipeWithItems(recipeId: String): Result<RecipeWithItems>

    suspend fun saveRecipe(recipe: Recipe, items: List<RecipeItem>): Result<RecipeWithItems>

    suspend fun deleteRecipe(recipeId: String): Result<Unit>
}
