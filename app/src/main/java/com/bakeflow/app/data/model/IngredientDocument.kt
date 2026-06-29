package com.bakeflow.app.data.model

import com.bakeflow.app.data.remote.IngredientFirestoreConstants
import com.bakeflow.app.domain.model.Ingredient
import com.bakeflow.app.domain.model.IngredientCategory
import com.bakeflow.app.domain.model.IngredientStatus
import com.bakeflow.app.domain.model.IngredientUnit
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class IngredientDocument(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val category: String = IngredientCategory.OTHER.name,
    val unit: String = IngredientUnit.KG.name,
    val costPerUnit: Double = 0.0,
    val currentStock: Double = 0.0,
    val reorderLevel: Double = 0.0,
    val status: String = IngredientStatus.ACTIVE.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): Ingredient = Ingredient(
        id = id,
        ownerId = ownerId,
        name = name,
        category = IngredientCategory.fromString(category),
        unit = IngredientUnit.fromString(unit),
        costPerUnit = costPerUnit,
        currentStock = currentStock,
        reorderLevel = reorderLevel,
        status = IngredientStatus.fromString(status)
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        IngredientFirestoreConstants.FIELD_ID to id,
        IngredientFirestoreConstants.FIELD_OWNER_ID to ownerId,
        IngredientFirestoreConstants.FIELD_NAME to name,
        IngredientFirestoreConstants.FIELD_CATEGORY to category,
        IngredientFirestoreConstants.FIELD_UNIT to unit,
        IngredientFirestoreConstants.FIELD_COST_PER_UNIT to costPerUnit,
        IngredientFirestoreConstants.FIELD_CURRENT_STOCK to currentStock,
        IngredientFirestoreConstants.FIELD_REORDER_LEVEL to reorderLevel,
        IngredientFirestoreConstants.FIELD_STATUS to status,
        IngredientFirestoreConstants.FIELD_CREATED_AT to createdAt,
        IngredientFirestoreConstants.FIELD_UPDATED_AT to updatedAt
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): IngredientDocument? {
            if (!snapshot.exists()) return null
            return IngredientDocument(
                id = snapshot.getString(IngredientFirestoreConstants.FIELD_ID) ?: snapshot.id,
                ownerId = snapshot.getString(IngredientFirestoreConstants.FIELD_OWNER_ID).orEmpty(),
                name = snapshot.getString(IngredientFirestoreConstants.FIELD_NAME).orEmpty(),
                category = snapshot.getString(IngredientFirestoreConstants.FIELD_CATEGORY)
                    ?: IngredientCategory.OTHER.name,
                unit = snapshot.getString(IngredientFirestoreConstants.FIELD_UNIT)
                    ?: IngredientUnit.KG.name,
                costPerUnit = snapshot.getDouble(IngredientFirestoreConstants.FIELD_COST_PER_UNIT) ?: 0.0,
                currentStock = snapshot.getDouble(IngredientFirestoreConstants.FIELD_CURRENT_STOCK) ?: 0.0,
                reorderLevel = snapshot.getDouble(IngredientFirestoreConstants.FIELD_REORDER_LEVEL) ?: 0.0,
                status = snapshot.getString(IngredientFirestoreConstants.FIELD_STATUS)
                    ?: IngredientStatus.ACTIVE.name,
                createdAt = snapshot.getTimestamp(IngredientFirestoreConstants.FIELD_CREATED_AT),
                updatedAt = snapshot.getTimestamp(IngredientFirestoreConstants.FIELD_UPDATED_AT)
            )
        }
    }
}
