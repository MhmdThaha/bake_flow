package com.bakeflow.app.navigation

/**
 * Central route definitions for Navigation Compose.
 */
object Routes {
    const val AUTH_GRAPH = "auth"
    const val AUTH_LOGIN = "auth/login"
    const val AUTH_REGISTER = "auth/register"
    const val AUTH_FORGOT_PASSWORD = "auth/forgot_password"

    const val DASHBOARD = "dashboard"
    const val PRODUCTS = "products"
    const val PRODUCTS_ADD = "products/add"
    const val PRODUCTS_EDIT = "products/edit/{productId}"

    const val ARG_PRODUCT_ID = "productId"

    fun productEditRoute(productId: String): String = "products/edit/$productId"

    val productFormDestinations = listOf(PRODUCTS_ADD, PRODUCTS_EDIT)
    const val INVENTORY = "inventory"
    const val INVENTORY_ADD = "inventory/add"
    const val INVENTORY_EDIT = "inventory/edit/{ingredientId}"

    const val ARG_INGREDIENT_ID = "ingredientId"

    fun ingredientEditRoute(ingredientId: String): String = "inventory/edit/$ingredientId"

    const val PURCHASE_RECEIVE = "inventory/purchases/receive"
    const val PURCHASE_DETAIL = "inventory/purchases/{purchaseId}"

    const val ARG_PURCHASE_ID = "purchaseId"

    fun purchaseDetailRoute(purchaseId: String): String = "inventory/purchases/$purchaseId"

    const val STOCK_ADJUSTMENTS = "inventory/adjustments"
    const val STOCK_ADJUSTMENT_DETAIL = "inventory/adjustments/{adjustmentId}"

    const val ARG_ADJUSTMENT_ID = "adjustmentId"

    fun stockAdjustmentDetailRoute(adjustmentId: String): String = "inventory/adjustments/$adjustmentId"

    const val PRODUCTION = "production"
    const val RECIPE_CREATE = "production/recipe/create"
    const val RECIPE_EDIT = "production/recipe/edit/{recipeId}"
    const val RECIPE_VIEW = "production/recipe/view/{recipeId}"

    const val ARG_RECIPE_ID = "recipeId"

    fun recipeEditRoute(recipeId: String): String = "production/recipe/edit/$recipeId"
    fun recipeViewRoute(recipeId: String): String = "production/recipe/view/$recipeId"

    const val PRODUCTION_WIZARD = "production/wizard"
    const val PRODUCTION_HISTORY = "production/history"
    const val PRODUCTION_RECIPES = "production/recipes"
    const val PRODUCTION_BATCH = "production/batch/{batchId}"

    const val ARG_BATCH_ID = "batchId"

    fun productionBatchRoute(batchId: String): String = "production/batch/$batchId"

    const val SALES = "sales"
    const val WASTE = "waste"
    const val WASTE_DETAIL = "waste/detail/{wasteId}"

    const val ARG_WASTE_ID = "wasteId"

    fun wasteDetailRoute(wasteId: String): String = "waste/detail/$wasteId"

    const val REPORTS = "reports"

    val authDestinations = listOf(
        AUTH_LOGIN,
        AUTH_REGISTER,
        AUTH_FORGOT_PASSWORD
    )

    val mainDestinations = listOf(
        DASHBOARD,
        PRODUCTS,
        INVENTORY,
        PRODUCTION,
        SALES,
        WASTE,
        REPORTS
    )

    fun isMainTabRoute(route: String?): Boolean =
        route != null && route in mainDestinations
}
