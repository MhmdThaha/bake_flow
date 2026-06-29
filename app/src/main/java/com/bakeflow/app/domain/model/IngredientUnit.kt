package com.bakeflow.app.domain.model

enum class IngredientUnit(val displayName: String) {
    KG("kg"),
    GRAM("gram"),
    LITRE("litre"),
    ML("ml"),
    PIECE("piece"),
    PACKET("packet"),
    BOX("box");

    companion object {
        fun fromString(value: String): IngredientUnit =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: entries.find { it.displayName.equals(value, ignoreCase = true) }
                ?: KG
    }
}
