package com.bakeflow.app.inventory

import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.Ingredient

data class IngredientListUiState(
    val ingredientsState: UiState<List<Ingredient>> = UiState.Loading,
    val searchQuery: String = "",
    val filteredIngredients: List<Ingredient> = emptyList(),
    val deleteInProgressId: String? = null,
    val snackbarMessage: String? = null
)
