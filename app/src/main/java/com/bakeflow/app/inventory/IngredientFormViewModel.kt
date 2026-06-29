package com.bakeflow.app.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.data.repository.IngredientException
import com.bakeflow.app.domain.model.Ingredient
import com.bakeflow.app.domain.model.IngredientCategory
import com.bakeflow.app.domain.model.IngredientStatus
import com.bakeflow.app.domain.model.IngredientUnit
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.inventory.validation.IngredientValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IngredientFormViewModel(
    private val ingredientRepository: IngredientRepository,
    private val ingredientId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngredientFormUiState(ingredientId = ingredientId))
    val uiState: StateFlow<IngredientFormUiState> = _uiState.asStateFlow()

    init {
        if (ingredientId != null) {
            loadIngredient(ingredientId)
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null, errorMessage = null) }
    }

    fun onCategoryChange(category: IngredientCategory) {
        _uiState.update { it.copy(category = category, errorMessage = null) }
    }

    fun onUnitChange(unit: IngredientUnit) {
        _uiState.update { it.copy(unit = unit, unitError = null, errorMessage = null) }
    }

    fun onCostPerUnitChange(value: String) {
        _uiState.update { it.copy(costPerUnit = value, costPerUnitError = null, errorMessage = null) }
    }

    fun onCurrentStockChange(value: String) {
        _uiState.update { it.copy(currentStock = value, currentStockError = null, errorMessage = null) }
    }

    fun onReorderLevelChange(value: String) {
        _uiState.update { it.copy(reorderLevel = value, reorderLevelError = null, errorMessage = null) }
    }

    fun onStatusChange(status: IngredientStatus) {
        _uiState.update { it.copy(status = status, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveIngredient() {
        val state = _uiState.value
        val nameError = IngredientValidator.validateName(state.name)
        val unitError = IngredientValidator.validateUnit(state.unit)
        val (costPerUnit, costError) = IngredientValidator.validateNonNegativeNumber(
            state.costPerUnit,
            "Cost per unit"
        )
        val (currentStock, stockError) = IngredientValidator.validateNonNegativeNumber(
            state.currentStock,
            "Current stock"
        )
        val (reorderLevel, reorderError) = IngredientValidator.validateNonNegativeNumber(
            state.reorderLevel,
            "Reorder level"
        )

        if (nameError != null || unitError != null || costError != null ||
            stockError != null || reorderError != null
        ) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    unitError = unitError,
                    costPerUnitError = costError,
                    currentStockError = stockError,
                    reorderLevelError = reorderError
                )
            }
            return
        }

        val ownerId = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerId == null) {
            _uiState.update { it.copy(errorMessage = "You must be signed in to save ingredients.") }
            return
        }

        val ingredient = Ingredient(
            id = state.ingredientId.orEmpty(),
            ownerId = state.ownerId.ifBlank { ownerId },
            name = state.name.trim(),
            category = state.category,
            unit = state.unit,
            costPerUnit = costPerUnit!!,
            currentStock = currentStock!!,
            reorderLevel = reorderLevel!!,
            status = state.status
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (state.ingredientId == null) {
                ingredientRepository.addIngredient(ingredient)
            } else {
                ingredientRepository.updateIngredient(ingredient)
            }
            result
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, saveSucceeded = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    fun deleteIngredient() {
        val id = _uiState.value.ingredientId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            ingredientRepository.deleteIngredient(id)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, deleteSucceeded = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    private fun loadIngredient(ingredientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitialLoading = true, errorMessage = null) }
            ingredientRepository.getIngredient(ingredientId)
                .onSuccess { ingredient ->
                    _uiState.update {
                        it.copy(
                            ingredientId = ingredient.id,
                            ownerId = ingredient.ownerId,
                            name = ingredient.name,
                            category = ingredient.category,
                            unit = ingredient.unit,
                            costPerUnit = formatNumber(ingredient.costPerUnit),
                            currentStock = formatNumber(ingredient.currentStock),
                            reorderLevel = formatNumber(ingredient.reorderLevel),
                            status = ingredient.status,
                            isInitialLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isInitialLoading = false, errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private fun Throwable.toUserMessage(): String =
        (this as? IngredientException)?.message ?: message ?: "Something went wrong. Please try again."
}

class IngredientFormViewModelFactory(
    private val ingredientRepository: IngredientRepository,
    private val ingredientId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IngredientFormViewModel::class.java)) {
            return IngredientFormViewModel(ingredientRepository, ingredientId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
