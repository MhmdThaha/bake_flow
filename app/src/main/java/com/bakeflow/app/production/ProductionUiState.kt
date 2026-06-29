package com.bakeflow.app.production

import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.Ingredient
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.model.ProductionBatch
import com.bakeflow.app.domain.model.ProductionRequirementLine
import com.bakeflow.app.domain.model.Recipe
import com.bakeflow.app.domain.model.RecipeItem

enum class ProductionWizardStep(val title: String, val stepNumber: Int) {
    CHOOSE_PRODUCT("Choose Product", 1),
    ENTER_QUANTITY("Enter Quantity", 2),
    REVIEW_REQUIREMENTS("Required Ingredients", 3),
    CONFIRM("Review & Confirm", 4)
}

data class ProductWithRecipe(
    val product: Product,
    val recipe: Recipe
)

data class ProductionListUiState(
    val stats: ProductionDashboardStatsUi = ProductionDashboardStatsUi(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class ProductionDashboardStatsUi(
    val todayBatchCount: Int = 0,
    val todayCompleted: Int = 0,
    val todayPending: Int = 0
)

data class ProductionHistoryUiState(
    val batchesState: UiState<List<ProductionBatch>> = UiState.Loading,
    val searchQuery: String = "",
    val filteredBatches: List<ProductionBatch> = emptyList(),
    val snackbarMessage: String? = null
)

data class ProductionWizardUiState(
    val currentStep: ProductionWizardStep = ProductionWizardStep.CHOOSE_PRODUCT,
    val productSearchQuery: String = "",
    val productsWithRecipes: List<ProductWithRecipe> = emptyList(),
    val selectedProductId: String? = null,
    val selectedRecipeId: String? = null,
    val quantityText: String = "1",
    val quantityError: String? = null,
    val productError: String? = null,
    val requirementLines: List<ProductionRequirementLine> = emptyList(),
    val isCalculating: Boolean = false,
    val isExecuting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val saveSucceeded: Boolean = false
)

data class ProductionDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val batch: ProductionBatch? = null
)

object ProductionCalculator {

    fun calculateRequirements(
        recipeItems: List<RecipeItem>,
        ingredients: List<Ingredient>,
        quantityProduced: Double
    ): List<ProductionRequirementLine> {
        val ingredientMap = ingredients.associateBy { it.id }
        return recipeItems.map { item ->
            val ingredient = ingredientMap[item.ingredientId]
            val required = item.quantity * quantityProduced
            val available = ingredient?.currentStock ?: 0.0
            val costPerUnit = ingredient?.costPerUnit ?: 0.0
            ProductionRequirementLine(
                ingredientId = item.ingredientId,
                ingredientName = ingredient?.name ?: "Unknown",
                unit = item.unit.ifBlank { ingredient?.unit?.displayName.orEmpty() },
                requiredQuantity = required,
                availableQuantity = available,
                costPerUnit = costPerUnit,
                totalCost = required * costPerUnit,
                hasShortage = available < required
            )
        }
    }

    fun totalCost(lines: List<ProductionRequirementLine>): Double =
        lines.sumOf { it.totalCost }

    fun costPerProduct(totalCost: Double, quantity: Double): Double =
        if (quantity <= 0) 0.0 else totalCost / quantity
}
