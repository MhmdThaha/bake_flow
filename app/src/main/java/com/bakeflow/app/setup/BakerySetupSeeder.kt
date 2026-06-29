package com.bakeflow.app.setup

import com.bakeflow.app.domain.model.Ingredient
import com.bakeflow.app.domain.model.IngredientCategory
import com.bakeflow.app.domain.model.IngredientStatus
import com.bakeflow.app.domain.model.IngredientUnit
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.model.ProductStatus
import com.bakeflow.app.domain.model.Recipe
import com.bakeflow.app.domain.model.RecipeItem
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.domain.repository.RecipeRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first

enum class BakeryType(val label: String) {
    BREAD_BAKERY("Bread Bakery"),
    CAKE_SHOP("Cake Shop"),
    SWEET_SHOP("Sweet Shop"),
    MIXED("Mixed Bakery")
}

object BakerySetupSeeder {

    suspend fun seed(
        bakeryType: BakeryType,
        productRepository: ProductRepository,
        ingredientRepository: IngredientRepository,
        recipeRepository: RecipeRepository
    ) {
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val existingProducts = productRepository.observeProducts().first()
        if (existingProducts.isNotEmpty()) return

        commonIngredients(ownerId).forEach { ingredientRepository.addIngredient(it) }
        productsFor(bakeryType, ownerId).forEach { productRepository.addProduct(it) }

        val ingredients = ingredientRepository.observeIngredients().first()
        val products = productRepository.observeProducts().first()
        products.forEach { product ->
            val items = recipeItemsFor(product.name, ingredients)
            if (items.isEmpty()) return@forEach
            recipeRepository.saveRecipe(
                recipe = Recipe(id = "", ownerId = ownerId, productId = product.id),
                items = items.map { item ->
                    RecipeItem(
                        id = "",
                        recipeId = "",
                        ingredientId = item.first,
                        quantity = item.second,
                        unit = item.third
                    )
                }
            )
        }
    }

    private fun commonIngredients(ownerId: String): List<Ingredient> = listOf(
        ing(ownerId, "Flour", IngredientCategory.FLOUR, IngredientUnit.KG, 1.2, 25.0, 5.0),
        ing(ownerId, "Sugar", IngredientCategory.SWEETENER, IngredientUnit.KG, 1.0, 10.0, 2.0),
        ing(ownerId, "Butter", IngredientCategory.DAIRY, IngredientUnit.KG, 4.5, 5.0, 1.0),
        ing(ownerId, "Eggs", IngredientCategory.DAIRY, IngredientUnit.PIECE, 0.25, 60.0, 12.0),
        ing(ownerId, "Yeast", IngredientCategory.ADDITIVE, IngredientUnit.GRAM, 0.02, 500.0, 100.0),
        ing(ownerId, "Milk", IngredientCategory.DAIRY, IngredientUnit.LITRE, 1.1, 8.0, 2.0),
        ing(ownerId, "Cocoa", IngredientCategory.OTHER, IngredientUnit.KG, 3.0, 3.0, 0.5),
        ing(ownerId, "Vanilla", IngredientCategory.ADDITIVE, IngredientUnit.LITRE, 8.0, 1.0, 0.2)
    )

    private fun productsFor(type: BakeryType, ownerId: String): List<Product> = when (type) {
        BakeryType.BREAD_BAKERY -> listOf(
            prod(ownerId, "White Loaf", "Bread", 3.50),
            prod(ownerId, "Brown Loaf", "Bread", 4.00),
            prod(ownerId, "Dinner Roll", "Rolls", 0.80)
        )
        BakeryType.CAKE_SHOP -> listOf(
            prod(ownerId, "Vanilla Sponge", "Cakes", 18.00),
            prod(ownerId, "Chocolate Cake", "Cakes", 22.00),
            prod(ownerId, "Cupcake", "Cakes", 2.50)
        )
        BakeryType.SWEET_SHOP -> listOf(
            prod(ownerId, "Chocolate Chip Cookie", "Cookies", 1.50),
            prod(ownerId, "Cream Puff", "Pastries", 2.00),
            prod(ownerId, "Eclair", "Pastries", 2.50)
        )
        BakeryType.MIXED -> listOf(
            prod(ownerId, "White Loaf", "Bread", 3.50),
            prod(ownerId, "Vanilla Sponge", "Cakes", 18.00),
            prod(ownerId, "Chocolate Chip Cookie", "Cookies", 1.50),
            prod(ownerId, "Cinnamon Bun", "Buns", 2.20)
        )
    }

    /** Triple: ingredientId, quantity per unit, unit label */
    private fun recipeItemsFor(
        productName: String,
        ingredients: List<Ingredient>
    ): List<Triple<String, Double, String>> {
        fun id(name: String) = ingredients.find { it.name.equals(name, ignoreCase = true) }?.id
        val flour = id("Flour") ?: return emptyList()
        val sugar = id("Sugar")
        val butter = id("Butter")
        val eggs = id("Eggs")
        val yeast = id("Yeast")
        val milk = id("Milk")
        val cocoa = id("Cocoa")

        return when {
            productName.contains("Loaf", ignoreCase = true) -> buildList {
                add(Triple(flour, 0.35, "kg"))
                yeast?.let { add(Triple(it, 7.0, "g")) }
                sugar?.let { add(Triple(it, 0.02, "kg")) }
                butter?.let { add(Triple(it, 0.02, "kg")) }
            }
            productName.contains("Roll", ignoreCase = true) -> buildList {
                add(Triple(flour, 0.08, "kg"))
                yeast?.let { add(Triple(it, 2.0, "g")) }
                butter?.let { add(Triple(it, 0.01, "kg")) }
            }
            productName.contains("Cake", ignoreCase = true) || productName.contains("Sponge", ignoreCase = true) -> buildList {
                add(Triple(flour, 0.25, "kg"))
                sugar?.let { add(Triple(it, 0.2, "kg")) }
                butter?.let { add(Triple(it, 0.15, "kg")) }
                eggs?.let { add(Triple(it, 4.0, "piece")) }
                milk?.let { add(Triple(it, 0.2, "L")) }
                if (productName.contains("Chocolate", ignoreCase = true)) {
                    cocoa?.let { add(Triple(it, 0.05, "kg")) }
                }
            }
            productName.contains("Cupcake", ignoreCase = true) -> buildList {
                add(Triple(flour, 0.05, "kg"))
                sugar?.let { add(Triple(it, 0.04, "kg")) }
                eggs?.let { add(Triple(it, 1.0, "piece")) }
                butter?.let { add(Triple(it, 0.03, "kg")) }
            }
            productName.contains("Cookie", ignoreCase = true) -> buildList {
                add(Triple(flour, 0.12, "kg"))
                sugar?.let { add(Triple(it, 0.08, "kg")) }
                butter?.let { add(Triple(it, 0.08, "kg")) }
                eggs?.let { add(Triple(it, 1.0, "piece")) }
            }
            productName.contains("Bun", ignoreCase = true) -> buildList {
                add(Triple(flour, 0.1, "kg"))
                sugar?.let { add(Triple(it, 0.03, "kg")) }
                butter?.let { add(Triple(it, 0.04, "kg")) }
                yeast?.let { add(Triple(it, 3.0, "g")) }
            }
            productName.contains("Puff", ignoreCase = true) || productName.contains("Eclair", ignoreCase = true) -> buildList {
                add(Triple(flour, 0.08, "kg"))
                butter?.let { add(Triple(it, 0.06, "kg")) }
                eggs?.let { add(Triple(it, 2.0, "piece")) }
                milk?.let { add(Triple(it, 0.15, "L")) }
            }
            else -> listOf(Triple(flour, 0.2, "kg"))
        }
    }

    private fun prod(ownerId: String, name: String, category: String, price: Double) = Product(
        id = "",
        ownerId = ownerId,
        name = name,
        category = category,
        sellingPrice = price,
        status = ProductStatus.ACTIVE
    )

    private fun ing(
        ownerId: String,
        name: String,
        category: IngredientCategory,
        unit: IngredientUnit,
        cost: Double,
        stock: Double,
        reorder: Double
    ) = Ingredient(
        id = "",
        ownerId = ownerId,
        name = name,
        category = category,
        unit = unit,
        costPerUnit = cost,
        currentStock = stock,
        reorderLevel = reorder,
        status = IngredientStatus.ACTIVE
    )
}
