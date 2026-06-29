package com.bakeflow.app.sales

import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.PaymentMethod
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.model.Sale
import com.bakeflow.app.domain.model.SalesSummary

data class SalesListUiState(
    val salesState: UiState<List<Sale>> = UiState.Loading,
    val summary: SalesSummary = SalesSummary(),
    val searchQuery: String = "",
    val filteredSales: List<Sale> = emptyList(),
    val snackbarMessage: String? = null
)

data class SaleFormUiState(
    val products: List<Product> = emptyList(),
    val selectedProductId: String? = null,
    val quantityText: String = "1",
    val unitPriceText: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val customerName: String = "",
    val customerPhone: String = "",
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

    val soldQuantity: Double
        get() = quantityText.toDoubleOrNull() ?: 0.0

    val remainingStock: Double
        get() = (availableStock - soldQuantity).coerceAtLeast(0.0)

    val totalAmount: Double
        get() {
            val qty = quantityText.toDoubleOrNull() ?: return 0.0
            val price = unitPriceText.toDoubleOrNull() ?: return 0.0
            return qty * price
        }

    val hasInsufficientStock: Boolean
        get() = soldQuantity > availableStock
}
