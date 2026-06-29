package com.bakeflow.app.inventory.validation

import com.bakeflow.app.domain.model.IngredientUnit

object IngredientValidator {

    fun validateName(name: String): String? =
        if (name.trim().isBlank()) "Ingredient name is required" else null

    fun validateUnit(unit: IngredientUnit?): String? =
        if (unit == null) "Unit is required" else null

    fun validateNonNegativeNumber(valueText: String, fieldLabel: String): Pair<Double?, String?> {
        val trimmed = valueText.trim()
        if (trimmed.isBlank()) {
            return null to "$fieldLabel is required"
        }
        val value = trimmed.toDoubleOrNull()
        return when {
            value == null -> null to "Enter a valid $fieldLabel"
            value < 0 -> null to "$fieldLabel must be 0 or greater"
            else -> value to null
        }
    }
}
