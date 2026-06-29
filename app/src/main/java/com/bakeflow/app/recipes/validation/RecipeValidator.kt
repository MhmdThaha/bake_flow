package com.bakeflow.app.recipes.validation

import com.bakeflow.app.recipes.DraftRecipeItem

object RecipeValidator {

    fun validateProduct(productId: String?): String? =
        if (productId.isNullOrBlank()) "Please select a product" else null

    fun validateHasIngredients(items: List<DraftRecipeItem>): String? =
        if (items.isEmpty()) "Add at least one ingredient" else null

    fun validateNoDuplicateIngredients(items: List<DraftRecipeItem>): String? {
        val duplicates = items.groupBy { it.ingredientId }.filter { it.value.size > 1 }.keys
        return if (duplicates.isNotEmpty()) {
            "Each ingredient can only appear once in a recipe"
        } else {
            null
        }
    }

    fun validateQuantity(quantityText: String): Pair<Double?, String?> {
        val trimmed = quantityText.trim()
        if (trimmed.isBlank()) return null to "Quantity is required"
        val quantity = trimmed.toDoubleOrNull()
        return when {
            quantity == null -> null to "Enter a valid quantity"
            quantity <= 0 -> null to "Quantity must be greater than 0"
            else -> quantity to null
        }
    }

    fun validateAllQuantities(items: List<DraftRecipeItem>): List<DraftRecipeItem> =
        items.map { item ->
            val (_, error) = validateQuantity(item.quantity)
            item.copy(quantityError = error)
        }

    fun hasQuantityErrors(items: List<DraftRecipeItem>): Boolean =
        items.any { it.quantityError != null || it.quantity.isBlank() }
}
