package com.bakeflow.app.stockadjustment

import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.AdjustmentItemType
import com.bakeflow.app.domain.model.AdjustmentReason
import com.bakeflow.app.domain.model.Ingredient
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.model.StockAdjustment
import com.bakeflow.app.domain.model.StockAdjustmentSummary

data class StockAdjustmentListUiState(
    val adjustmentsState: UiState<List<StockAdjustment>> = UiState.Loading,
    val summary: StockAdjustmentSummary = StockAdjustmentSummary(),
    val searchQuery: String = "",
    val filteredAdjustments: List<StockAdjustment> = emptyList(),
    val snackbarMessage: String? = null
)

data class StockAdjustmentFormUiState(
    val itemType: AdjustmentItemType = AdjustmentItemType.INGREDIENT,
    val ingredients: List<Ingredient> = emptyList(),
    val products: List<Product> = emptyList(),
    val selectedItemId: String? = null,
    val actualStockText: String = "",
    val reason: AdjustmentReason = AdjustmentReason.PHYSICAL_COUNT,
    val notes: String = "",
    val itemError: String? = null,
    val quantityError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSucceeded: Boolean = false
) {
    val selectedIngredient: Ingredient?
        get() = if (itemType == AdjustmentItemType.INGREDIENT) {
            ingredients.find { it.id == selectedItemId }
        } else {
            null
        }

    val selectedProduct: Product?
        get() = if (itemType == AdjustmentItemType.PRODUCT) {
            products.find { it.id == selectedItemId }
        } else {
            null
        }

    val currentStock: Double
        get() = when (itemType) {
            AdjustmentItemType.INGREDIENT -> selectedIngredient?.currentStock ?: 0.0
            AdjustmentItemType.PRODUCT -> selectedProduct?.finishedStock ?: 0.0
        }

    val actualStock: Double
        get() = actualStockText.toDoubleOrNull() ?: 0.0

    val difference: Double
        get() = actualStock - currentStock

    val hasNegativeStock: Boolean
        get() = actualStockText.isNotBlank() && actualStock < 0
}

data class StockAdjustmentDetailUiState(
    val isLoading: Boolean = true,
    val adjustment: StockAdjustment? = null,
    val errorMessage: String? = null
)
