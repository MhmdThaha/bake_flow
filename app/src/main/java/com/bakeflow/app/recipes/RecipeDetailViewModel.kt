package com.bakeflow.app.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.data.repository.RecipeException
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository,
    private val recipeId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    init {
        loadRecipe()
    }

    private fun loadRecipe() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            recipeRepository.getRecipeWithItems(recipeId)
                .onSuccess { recipeWithItems ->
                    val products = productRepository.observeProducts().first()
                    val ingredients = ingredientRepository.observeIngredients().first()
                    val productName = products.find { it.id == recipeWithItems.recipe.productId }?.name
                        ?: "Unknown Product"
                    val ingredientMap = ingredients.associateBy { it.id }
                    val detailItems = recipeWithItems.items.map { item ->
                        val ingredient = ingredientMap[item.ingredientId]
                        RecipeDetailItem(
                            item = item,
                            ingredientName = ingredient?.name ?: "Unknown Ingredient",
                            unitDisplay = item.unit.ifBlank { ingredient?.unit?.displayName ?: "" }
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recipe = recipeWithItems.recipe,
                            productName = productName,
                            items = detailItems
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toUserMessage()
                        )
                    }
                }
        }
    }

    private fun Throwable.toUserMessage(): String =
        (this as? RecipeException)?.message ?: message ?: "Something went wrong. Please try again."
}

class RecipeDetailViewModelFactory(
    private val recipeRepository: RecipeRepository,
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository,
    private val recipeId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeDetailViewModel::class.java)) {
            return RecipeDetailViewModel(
                recipeRepository,
                productRepository,
                ingredientRepository,
                recipeId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
