package com.bakeflow.app.products.validation

object ProductValidator {

    fun validateName(name: String): String? {
        return if (name.trim().isBlank()) "Product name is required" else null
    }

    fun validateSellingPrice(priceText: String): Pair<Double?, String?> {
        val trimmed = priceText.trim()
        if (trimmed.isBlank()) {
            return null to "Selling price is required"
        }
        val price = trimmed.toDoubleOrNull()
        return when {
            price == null -> null to "Enter a valid price"
            price <= 0 -> null to "Selling price must be greater than 0"
            else -> price to null
        }
    }
}
