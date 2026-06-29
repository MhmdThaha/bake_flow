package com.bakeflow.app.stockadjustment.validation

object StockAdjustmentValidator {

    fun validateItem(itemId: String?): String? =
        if (itemId.isNullOrBlank()) "Select an item" else null

    fun validateActualStock(actualStockText: String): String? {
        if (actualStockText.isBlank()) return "Actual stock is required"
        val value = actualStockText.toDoubleOrNull()
            ?: return "Enter a valid quantity"
        if (value < 0) return "Actual stock cannot be negative"
        return null
    }
}
