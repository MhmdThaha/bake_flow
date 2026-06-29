package com.bakeflow.app.waste

import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.model.Waste
import com.bakeflow.app.domain.model.WasteReason
import com.bakeflow.app.domain.model.WasteSummary

data class WasteListUiState(
    val wasteState: UiState<List<Waste>> = UiState.Loading,
    val summary: WasteSummary = WasteSummary(),
    val searchQuery: String = "",
    val filteredWaste: List<Waste> = emptyList(),
    val snackbarMessage: String? = null
)

data class WasteFormUiState(
    val products: List<Product> = emptyList(),
    val selectedProductId: String? = null,
    val quantityText: String = "1",
    val reason: WasteReason = WasteReason.DAMAGED,
    val notes: String = "",
    val productError: String? = null,
    val quantityError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSucceeded: Boolean = false
) {
    val selectedProduct: Product?
        get() = products.find { it.id == selectedProductId }

    val availableStock: Double
        get() = selectedProduct?.finishedStock ?: 0.0

    val wasteQuantity: Double
        get() = quantityText.toDoubleOrNull() ?: 0.0

    val remainingStock: Double
        get() = (availableStock - wasteQuantity).coerceAtLeast(0.0)

    val estimatedLoss: Double
        get() {
            val qty = quantityText.toDoubleOrNull() ?: return 0.0
            val price = selectedProduct?.sellingPrice ?: return 0.0
            return qty * price
        }

    val hasInsufficientStock: Boolean
        get() = wasteQuantity > availableStock
}

data class WasteDetailUiState(
    val isLoading: Boolean = true,
    val waste: Waste? = null,
    val errorMessage: String? = null
)
