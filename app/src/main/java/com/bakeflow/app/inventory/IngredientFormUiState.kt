package com.bakeflow.app.inventory

import com.bakeflow.app.domain.model.IngredientCategory
import com.bakeflow.app.domain.model.IngredientStatus
import com.bakeflow.app.domain.model.IngredientUnit

data class IngredientFormUiState(
    val ingredientId: String? = null,
    val ownerId: String = "",
    val name: String = "",
    val category: IngredientCategory = IngredientCategory.OTHER,
    val unit: IngredientUnit = IngredientUnit.KG,
    val costPerUnit: String = "",
    val currentStock: String = "",
    val reorderLevel: String = "",
    val status: IngredientStatus = IngredientStatus.ACTIVE,
    val nameError: String? = null,
    val unitError: String? = null,
    val costPerUnitError: String? = null,
    val currentStockError: String? = null,
    val reorderLevelError: String? = null,
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val errorMessage: String? = null,
    val saveSucceeded: Boolean = false,
    val deleteSucceeded: Boolean = false
)
