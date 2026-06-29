package com.bakeflow.app.common

import android.content.Context
import com.bakeflow.app.domain.model.PaymentMethod

/**
 * Local UX preferences — smart defaults, recent selections, onboarding state.
 * Does not touch Firestore or business repositories.
 */
class BakeFlowPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isSetupComplete(userId: String): Boolean =
        prefs.getBoolean(keySetupComplete(userId), false)

    fun markSetupComplete(userId: String) {
        prefs.edit().putBoolean(keySetupComplete(userId), true).apply()
    }

    fun getBakeryName(): String =
        prefs.getString(KEY_BAKERY_NAME, "")?.trim().orEmpty()

    fun setBakeryName(name: String) {
        prefs.edit().putString(KEY_BAKERY_NAME, name.trim()).apply()
    }

    fun getLastSupplier(): String =
        prefs.getString(KEY_LAST_SUPPLIER, "").orEmpty()

    fun setLastSupplier(supplier: String) {
        prefs.edit().putString(KEY_LAST_SUPPLIER, supplier.trim()).apply()
    }

    fun getLastPurchaseCost(): String =
        prefs.getString(KEY_LAST_PURCHASE_COST, "").orEmpty()

    fun setLastPurchaseCost(cost: String) {
        prefs.edit().putString(KEY_LAST_PURCHASE_COST, cost.trim()).apply()
    }

    fun getLastPaymentMethod(): PaymentMethod =
        PaymentMethod.fromString(prefs.getString(KEY_LAST_PAYMENT_METHOD, PaymentMethod.CASH.name))

    fun setLastPaymentMethod(method: PaymentMethod) {
        prefs.edit().putString(KEY_LAST_PAYMENT_METHOD, method.name).apply()
    }

    fun recordRecentProduct(productId: String) = recordRecent(KEY_RECENT_PRODUCTS, productId)

    fun getRecentProductIds(): List<String> = readRecent(KEY_RECENT_PRODUCTS)

    fun recordRecentIngredient(ingredientId: String) = recordRecent(KEY_RECENT_INGREDIENTS, ingredientId)

    fun getRecentIngredientIds(): List<String> = readRecent(KEY_RECENT_INGREDIENTS)

    fun recordRecentSupplier(supplier: String) {
        val name = supplier.trim()
        if (name.isBlank()) return
        recordRecent(KEY_RECENT_SUPPLIERS, name)
    }

    fun getRecentSuppliers(): List<String> = readRecent(KEY_RECENT_SUPPLIERS)

    fun recordRecentProductionProduct(productId: String) =
        recordRecent(KEY_RECENT_PRODUCTION_PRODUCTS, productId)

    fun getRecentProductionProductIds(): List<String> = readRecent(KEY_RECENT_PRODUCTION_PRODUCTS)

    fun <T> sortByRecent(items: List<T>, idSelector: (T) -> String, recentIds: List<String>): List<T> {
        if (recentIds.isEmpty()) return items
        val order = recentIds.withIndex().associate { it.value to it.index }
        return items.sortedWith { a, b ->
            val aIndex = order[idSelector(a)] ?: Int.MAX_VALUE
            val bIndex = order[idSelector(b)] ?: Int.MAX_VALUE
            if (aIndex != bIndex) aIndex.compareTo(bIndex) else 0
        }
    }

    private fun recordRecent(key: String, value: String) {
        if (value.isBlank()) return
        val current = readRecent(key).toMutableList()
        current.remove(value)
        current.add(0, value)
        while (current.size > MAX_RECENT) current.removeLast()
        prefs.edit().putString(key, current.joinToString(SEPARATOR)).apply()
    }

    private fun readRecent(key: String): List<String> =
        prefs.getString(key, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private fun keySetupComplete(userId: String) = "setup_complete_$userId"

    companion object {
        private const val PREFS_NAME = "bakeflow_prefs"
        private const val KEY_BAKERY_NAME = "bakery_name"
        private const val KEY_LAST_SUPPLIER = "last_supplier"
        private const val KEY_LAST_PURCHASE_COST = "last_purchase_cost"
        private const val KEY_LAST_PAYMENT_METHOD = "last_payment_method"
        private const val KEY_RECENT_PRODUCTS = "recent_products"
        private const val KEY_RECENT_INGREDIENTS = "recent_ingredients"
        private const val KEY_RECENT_SUPPLIERS = "recent_suppliers"
        private const val KEY_RECENT_PRODUCTION_PRODUCTS = "recent_production_products"
        private const val SEPARATOR = "|"
        private const val MAX_RECENT = 5
    }
}
