package com.bakeflow.app.purchase

import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.Ingredient
import com.bakeflow.app.domain.model.Purchase
import com.bakeflow.app.domain.model.PurchaseSummary

data class PurchaseListUiState(
    val purchasesState: UiState<List<Purchase>> = UiState.Loading,
    val summary: PurchaseSummary = PurchaseSummary(),
    val searchQuery: String = "",
    val ingredientFilter: String? = null,
    val supplierFilter: String? = null,
    val filteredPurchases: List<Purchase> = emptyList(),
    val availableIngredientFilters: List<String> = emptyList(),
    val availableSupplierFilters: List<String> = emptyList(),
    val snackbarMessage: String? = null
)

data class PurchaseFormUiState(
    val ingredients: List<Ingredient> = emptyList(),
    val selectedIngredientId: String? = null,
    val quantityText: String = "",
    val costPerUnitText: String = "",
    val unit: String = "",
    val supplierName: String = "",
    val invoiceNumber: String = "",
    val notes: String = "",
    val ingredientError: String? = null,
    val quantityError: String? = null,
    val costError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSucceeded: Boolean = false,
    val recentSuppliers: List<String> = emptyList()
) {
    val totalCost: Double
        get() {
            val qty = quantityText.toDoubleOrNull() ?: return 0.0
            val cost = costPerUnitText.toDoubleOrNull() ?: return 0.0
            return qty * cost
        }
}

data class PurchaseDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val purchase: Purchase? = null
)
