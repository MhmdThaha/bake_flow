package com.bakeflow.app.recipes

import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.Ingredient
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.model.Recipe
import com.bakeflow.app.domain.model.RecipeItem

data class RecipeListEntry(
    val recipe: Recipe,
    val productName: String,
    val itemCount: Int
)

data class RecipeListUiState(
    val recipesState: UiState<List<RecipeListEntry>> = UiState.Loading,
    val searchQuery: String = "",
    val filteredRecipes: List<RecipeListEntry> = emptyList(),
    val deleteInProgressId: String? = null,
    val snackbarMessage: String? = null
)

data class RecipeDetailItem(
    val item: RecipeItem,
    val ingredientName: String,
    val unitDisplay: String
)

data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val productName: String = "",
    val recipe: Recipe? = null,
    val items: List<RecipeDetailItem> = emptyList()
)

enum class RecipeWizardStep(val title: String, val stepNumber: Int) {
    CHOOSE_PRODUCT("Choose Product", 1),
    ADD_INGREDIENTS("Add Ingredients", 2),
    ENTER_QUANTITIES("Enter Quantities", 3),
    SAVE_RECIPE("Review", 4)
}

data class DraftRecipeItem(
    val localId: String,
    val ingredientId: String,
    val quantity: String = "",
    val unit: String,
    val quantityError: String? = null
)

data class RecipeWizardUiState(
    val recipeId: String? = null,
    val currentStep: RecipeWizardStep = RecipeWizardStep.CHOOSE_PRODUCT,
    val productSearchQuery: String = "",
    val selectedProductId: String? = null,
    val productError: String? = null,
    val draftItems: List<DraftRecipeItem> = emptyList(),
    val ingredientsError: String? = null,
    val products: List<Product> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val usedProductIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val errorMessage: String? = null,
    val saveSucceeded: Boolean = false
)
