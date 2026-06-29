package com.bakeflow.app.waste.validation

object WasteValidator {

    fun validateProduct(productId: String?): String? =
        if (productId.isNullOrBlank()) "Select a product" else null

    fun validateQuantity(quantityText: String, availableStock: Double): String? {
        if (quantityText.isBlank()) return "Quantity is required"
        val value = quantityText.toDoubleOrNull()
            ?: return "Enter a valid quantity"
        if (value <= 0) return "Quantity must be greater than zero"
        if (value > availableStock) {
            return "Only ${formatQty(availableStock)} available in finished stock"
        }
        return null
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.2f", value)
}
