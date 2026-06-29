package com.bakeflow.app.purchase.validation

object PurchaseValidator {

    fun validateIngredient(ingredientId: String?): String? =
        if (ingredientId.isNullOrBlank()) "Select an ingredient" else null

    fun validateQuantity(quantityText: String): String? {
        if (quantityText.isBlank()) return "Quantity is required"
        val value = quantityText.toDoubleOrNull()
            ?: return "Enter a valid quantity"
        if (value <= 0) return "Quantity must be greater than zero"
        return null
    }

    fun validateCostPerUnit(costText: String): String? {
        if (costText.isBlank()) return "Cost per unit is required"
        val value = costText.toDoubleOrNull()
            ?: return "Enter a valid cost"
        if (value <= 0) return "Cost must be greater than zero"
        return null
    }
}
