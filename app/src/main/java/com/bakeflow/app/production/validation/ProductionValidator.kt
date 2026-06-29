package com.bakeflow.app.production.validation

import com.bakeflow.app.domain.model.ProductionRequirementLine

object ProductionValidator {

    fun validateQuantity(quantityText: String): String? {
        if (quantityText.isBlank()) return "Quantity is required"
        val value = quantityText.toDoubleOrNull()
            ?: return "Enter a valid quantity"
        if (value <= 0) return "Quantity must be greater than zero"
        return null
    }

    fun validateHasRecipe(recipeId: String?): String? =
        if (recipeId.isNullOrBlank()) "This product has no recipe. Create a recipe first." else null

    fun validateNoShortages(lines: List<ProductionRequirementLine>): String? {
        val shortages = lines.filter { it.hasShortage }
        if (shortages.isEmpty()) return null
        val names = shortages.joinToString(", ") { it.ingredientName }
        return "Insufficient stock: $names. Receive stock before confirming."
    }
}
