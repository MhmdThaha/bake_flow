package com.bakeflow.app.data.model

import com.bakeflow.app.data.remote.RecipeFirestoreConstants
import com.bakeflow.app.domain.model.RecipeItem
import com.google.firebase.firestore.DocumentSnapshot

data class RecipeItemDocument(
    val id: String = "",
    val recipeId: String = "",
    val ownerId: String = "",
    val ingredientId: String = "",
    val quantity: Double = 0.0,
    val unit: String = ""
) {
    fun toDomain(): RecipeItem = RecipeItem(
        id = id,
        recipeId = recipeId,
        ingredientId = ingredientId,
        quantity = quantity,
        unit = unit
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        RecipeFirestoreConstants.FIELD_ID to id,
        RecipeFirestoreConstants.FIELD_RECIPE_ID to recipeId,
        RecipeFirestoreConstants.FIELD_OWNER_ID to ownerId,
        RecipeFirestoreConstants.FIELD_INGREDIENT_ID to ingredientId,
        RecipeFirestoreConstants.FIELD_QUANTITY to quantity,
        RecipeFirestoreConstants.FIELD_UNIT to unit
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): RecipeItemDocument? {
            if (!snapshot.exists()) return null
            return RecipeItemDocument(
                id = snapshot.getString(RecipeFirestoreConstants.FIELD_ID) ?: snapshot.id,
                recipeId = snapshot.getString(RecipeFirestoreConstants.FIELD_RECIPE_ID).orEmpty(),
                ownerId = snapshot.getString(RecipeFirestoreConstants.FIELD_OWNER_ID).orEmpty(),
                ingredientId = snapshot.getString(RecipeFirestoreConstants.FIELD_INGREDIENT_ID).orEmpty(),
                quantity = snapshot.getDouble(RecipeFirestoreConstants.FIELD_QUANTITY) ?: 0.0,
                unit = snapshot.getString(RecipeFirestoreConstants.FIELD_UNIT).orEmpty()
            )
        }
    }
}
