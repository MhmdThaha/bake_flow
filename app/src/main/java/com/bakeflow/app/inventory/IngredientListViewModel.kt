package com.bakeflow.app.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.UiState
import com.bakeflow.app.data.repository.IngredientException
import com.bakeflow.app.domain.model.Ingredient
import com.bakeflow.app.domain.repository.IngredientRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IngredientListViewModel(
    private val ingredientRepository: IngredientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngredientListUiState())
    val uiState: StateFlow<IngredientListUiState> = _uiState.asStateFlow()

    private var allIngredients: List<Ingredient> = emptyList()
    private var observeJob: Job? = null

    init {
        startObservingIngredients()
    }

    private fun startObservingIngredients() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            ingredientRepository.observeIngredients()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            ingredientsState = UiState.Error(
                                (error as? IngredientException)?.message
                                    ?: "Failed to load ingredients. Please try again."
                            )
                        )
                    }
                }
                .collect { ingredients ->
                    allIngredients = ingredients
                    applySearch(_uiState.value.searchQuery, ingredients)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearch(query, allIngredients)
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun deleteIngredient(ingredientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(deleteInProgressId = ingredientId) }
            ingredientRepository.deleteIngredient(ingredientId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            deleteInProgressId = null,
                            snackbarMessage = "Ingredient deleted"
                        )
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
        _uiState.update { it.copy(ingredientsState = UiState.Loading) }
        startObservingIngredients()
    }

    private fun applySearch(query: String, ingredients: List<Ingredient>) {
        val filtered = if (query.isBlank()) {
            ingredients
        } else {
            val normalizedQuery = query.trim().lowercase()
            ingredients.filter { ingredient ->
                ingredient.name.lowercase().contains(normalizedQuery) ||
                    ingredient.category.displayName.lowercase().contains(normalizedQuery)
            }
        }
        _uiState.update {
            it.copy(
                ingredientsState = if (ingredients.isEmpty()) UiState.Empty else UiState.Success(filtered),
                filteredIngredients = filtered
            )
        }
    }

    private fun Throwable.toUserMessage(): String =
        (this as? IngredientException)?.message ?: message ?: "Something went wrong. Please try again."
}

class IngredientListViewModelFactory(
    private val ingredientRepository: IngredientRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IngredientListViewModel::class.java)) {
            return IngredientListViewModel(ingredientRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
