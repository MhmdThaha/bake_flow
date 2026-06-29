package com.bakeflow.app.data.remote

object ProductionFirestoreConstants {
    const val COLLECTION = "production_batches"

    const val FIELD_BATCH_ID = "batchId"
    const val FIELD_OWNER_ID = "ownerId"
    const val FIELD_PRODUCT_ID = "productId"
    const val FIELD_PRODUCT_NAME = "productName"
    const val FIELD_RECIPE_ID = "recipeId"
    const val FIELD_QUANTITY_PRODUCED = "quantityProduced"
    const val FIELD_STATUS = "status"
    const val FIELD_ESTIMATED_COST = "estimatedCost"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_COMPLETED_AT = "completedAt"
    const val FIELD_CREATED_BY = "createdBy"
    const val FIELD_INGREDIENT_USAGE = "ingredientUsage"

    const val FIELD_USAGE_INGREDIENT_ID = "ingredientId"
    const val FIELD_USAGE_INGREDIENT_NAME = "ingredientName"
    const val FIELD_USAGE_REQUIRED_QUANTITY = "requiredQuantity"
    const val FIELD_USAGE_UNIT = "unit"
    const val FIELD_USAGE_COST_PER_UNIT = "costPerUnit"
    const val FIELD_USAGE_TOTAL_COST = "totalCost"
}
