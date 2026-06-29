package com.bakeflow.app.data.model

import com.bakeflow.app.data.remote.RecipeFirestoreConstants
import com.bakeflow.app.domain.model.Recipe
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class RecipeDocument(
    val id: String = "",
    val ownerId: String = "",
    val productId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): Recipe = Recipe(
        id = id,
        ownerId = ownerId,
        productId = productId,
        createdAt = createdAt?.toDate()?.time,
        updatedAt = updatedAt?.toDate()?.time
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        RecipeFirestoreConstants.FIELD_ID to id,
        RecipeFirestoreConstants.FIELD_OWNER_ID to ownerId,
        RecipeFirestoreConstants.FIELD_PRODUCT_ID to productId,
        RecipeFirestoreConstants.FIELD_CREATED_AT to createdAt,
        RecipeFirestoreConstants.FIELD_UPDATED_AT to updatedAt
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): RecipeDocument? {
            if (!snapshot.exists()) return null
            return RecipeDocument(
                id = snapshot.getString(RecipeFirestoreConstants.FIELD_ID) ?: snapshot.id,
                ownerId = snapshot.getString(RecipeFirestoreConstants.FIELD_OWNER_ID).orEmpty(),
                productId = snapshot.getString(RecipeFirestoreConstants.FIELD_PRODUCT_ID).orEmpty(),
                createdAt = snapshot.getTimestamp(RecipeFirestoreConstants.FIELD_CREATED_AT),
                updatedAt = snapshot.getTimestamp(RecipeFirestoreConstants.FIELD_UPDATED_AT)
            )
        }
    }
}
