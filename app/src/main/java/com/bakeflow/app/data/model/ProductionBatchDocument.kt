package com.bakeflow.app.data.model

import com.bakeflow.app.data.remote.ProductionFirestoreConstants
import com.bakeflow.app.domain.model.IngredientUsage
import com.bakeflow.app.domain.model.ProductionBatch
import com.bakeflow.app.domain.model.ProductionStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class ProductionBatchDocument(
    val batchId: String = "",
    val ownerId: String = "",
    val productId: String = "",
    val productName: String = "",
    val recipeId: String = "",
    val quantityProduced: Double = 0.0,
    val status: String = ProductionStatus.COMPLETED.name,
    val estimatedCost: Double = 0.0,
    val createdAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val createdBy: String = "",
    val ingredientUsage: List<IngredientUsageDocument> = emptyList()
) {
    fun toDomain(): ProductionBatch = ProductionBatch(
        batchId = batchId,
        ownerId = ownerId,
        productId = productId,
        productName = productName,
        recipeId = recipeId,
        quantityProduced = quantityProduced,
        status = ProductionStatus.fromString(status),
        estimatedCost = estimatedCost,
        createdAt = createdAt?.toDate()?.time ?: 0L,
        completedAt = completedAt?.toDate()?.time,
        createdBy = createdBy,
        ingredientUsage = ingredientUsage.map { it.toDomain() }
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        ProductionFirestoreConstants.FIELD_BATCH_ID to batchId,
        ProductionFirestoreConstants.FIELD_OWNER_ID to ownerId,
        ProductionFirestoreConstants.FIELD_PRODUCT_ID to productId,
        ProductionFirestoreConstants.FIELD_PRODUCT_NAME to productName,
        ProductionFirestoreConstants.FIELD_RECIPE_ID to recipeId,
        ProductionFirestoreConstants.FIELD_QUANTITY_PRODUCED to quantityProduced,
        ProductionFirestoreConstants.FIELD_STATUS to status,
        ProductionFirestoreConstants.FIELD_ESTIMATED_COST to estimatedCost,
        ProductionFirestoreConstants.FIELD_CREATED_AT to createdAt,
        ProductionFirestoreConstants.FIELD_COMPLETED_AT to completedAt,
        ProductionFirestoreConstants.FIELD_CREATED_BY to createdBy,
        ProductionFirestoreConstants.FIELD_INGREDIENT_USAGE to ingredientUsage.map { it.toFirestoreMap() }
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): ProductionBatchDocument? {
            if (!snapshot.exists()) return null
            @Suppress("UNCHECKED_CAST")
            val usageMaps = snapshot.get(ProductionFirestoreConstants.FIELD_INGREDIENT_USAGE)
                as? List<Map<String, Any?>>
            return ProductionBatchDocument(
                batchId = snapshot.getString(ProductionFirestoreConstants.FIELD_BATCH_ID)
                    ?: snapshot.id,
                ownerId = snapshot.getString(ProductionFirestoreConstants.FIELD_OWNER_ID).orEmpty(),
                productId = snapshot.getString(ProductionFirestoreConstants.FIELD_PRODUCT_ID).orEmpty(),
                productName = snapshot.getString(ProductionFirestoreConstants.FIELD_PRODUCT_NAME).orEmpty(),
                recipeId = snapshot.getString(ProductionFirestoreConstants.FIELD_RECIPE_ID).orEmpty(),
                quantityProduced = snapshot.getDouble(ProductionFirestoreConstants.FIELD_QUANTITY_PRODUCED)
                    ?: 0.0,
                status = snapshot.getString(ProductionFirestoreConstants.FIELD_STATUS)
                    ?: ProductionStatus.COMPLETED.name,
                estimatedCost = snapshot.getDouble(ProductionFirestoreConstants.FIELD_ESTIMATED_COST)
                    ?: 0.0,
                createdAt = snapshot.getTimestamp(ProductionFirestoreConstants.FIELD_CREATED_AT),
                completedAt = snapshot.getTimestamp(ProductionFirestoreConstants.FIELD_COMPLETED_AT),
                createdBy = snapshot.getString(ProductionFirestoreConstants.FIELD_CREATED_BY).orEmpty(),
                ingredientUsage = usageMaps?.mapNotNull { IngredientUsageDocument.fromMap(it) }.orEmpty()
            )
        }
    }
}

data class IngredientUsageDocument(
    val ingredientId: String = "",
    val ingredientName: String = "",
    val requiredQuantity: Double = 0.0,
    val unit: String = "",
    val costPerUnit: Double = 0.0,
    val totalCost: Double = 0.0
) {
    fun toDomain(): IngredientUsage = IngredientUsage(
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        requiredQuantity = requiredQuantity,
        unit = unit,
        costPerUnit = costPerUnit,
        totalCost = totalCost
    )

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        ProductionFirestoreConstants.FIELD_USAGE_INGREDIENT_ID to ingredientId,
        ProductionFirestoreConstants.FIELD_USAGE_INGREDIENT_NAME to ingredientName,
        ProductionFirestoreConstants.FIELD_USAGE_REQUIRED_QUANTITY to requiredQuantity,
        ProductionFirestoreConstants.FIELD_USAGE_UNIT to unit,
        ProductionFirestoreConstants.FIELD_USAGE_COST_PER_UNIT to costPerUnit,
        ProductionFirestoreConstants.FIELD_USAGE_TOTAL_COST to totalCost
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): IngredientUsageDocument? {
            val ingredientId = map[ProductionFirestoreConstants.FIELD_USAGE_INGREDIENT_ID] as? String
                ?: return null
            return IngredientUsageDocument(
                ingredientId = ingredientId,
                ingredientName = (map[ProductionFirestoreConstants.FIELD_USAGE_INGREDIENT_NAME] as? String)
                    .orEmpty(),
                requiredQuantity = (map[ProductionFirestoreConstants.FIELD_USAGE_REQUIRED_QUANTITY] as? Number)
                    ?.toDouble() ?: 0.0,
                unit = (map[ProductionFirestoreConstants.FIELD_USAGE_UNIT] as? String).orEmpty(),
                costPerUnit = (map[ProductionFirestoreConstants.FIELD_USAGE_COST_PER_UNIT] as? Number)
                    ?.toDouble() ?: 0.0,
                totalCost = (map[ProductionFirestoreConstants.FIELD_USAGE_TOTAL_COST] as? Number)
                    ?.toDouble() ?: 0.0
            )
        }
    }
}
