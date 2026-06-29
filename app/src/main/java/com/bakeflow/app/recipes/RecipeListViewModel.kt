package com.bakeflow.app.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.UiState
import com.bakeflow.app.data.repository.RecipeException
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.domain.repository.RecipeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeListViewModel(
    private val recipeRepository: RecipeRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeListUiState())
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()

    private var allEntries: List<RecipeListEntry> = emptyList()
    private var observeJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                recipeRepository.observeRecipes(),
                recipeRepository.observeRecipeItems(),
                productRepository.observeProducts()
            ) { recipes, items, products ->
                val productMap = products.associateBy { it.id }
                recipes.map { recipe ->
                    RecipeListEntry(
                        recipe = recipe,
                        productName = productMap[recipe.productId]?.name ?: "Unknown Product",
                        itemCount = items.count { it.recipeId == recipe.id }
                    )
                }.sortedBy { it.productName.lowercase() }
            }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            recipesState = UiState.Error(
                                (error as? RecipeException)?.message
                                    ?: "Failed to load recipes. Please try again."
                            )
                        )
                    }
                }
                .collect { entries ->
                    allEntries = entries
                    applySearch(_uiState.value.searchQuery, entries)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearch(query, allEntries)
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(deleteInProgressId = recipeId) }
            recipeRepository.deleteRecipe(recipeId)
                .onSuccess {
                    _uiState.update {
                        it.copy(deleteInProgressId = null, snackbarMessage = "Recipe deleted")
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            deleteInProgressId = null,
                            snackbarMessage = error.toUserMessage()
                        )
                    }
                }
        }
    }

    fun retryLoad() {
        _uiState.update { it.copy(recipesState = UiState.Loading) }
        startObserving()
    }

    private fun applySearch(query: String, entries: List<RecipeListEntry>) {
        val filtered = if (query.isBlank()) {
            entries
        } else {
            val normalized = query.trim().lowercase()
            entries.filter { it.productName.lowercase().contains(normalized) }
        }
        _uiState.update {
            it.copy(
                recipesState = if (entries.isEmpty()) UiState.Empty else UiState.Success(filtered),
                filteredRecipes = filtered
            )
        }
    }

    private fun Throwable.toUserMessage(): String =
        (this as? RecipeException)?.message ?: message ?: "Something went wrong. Please try again."
}

class RecipeListViewModelFactory(
    private val recipeRepository: RecipeRepository,
    private val productRepository: ProductRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeListViewModel::class.java)) {
            return RecipeListViewModel(recipeRepository, productRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
