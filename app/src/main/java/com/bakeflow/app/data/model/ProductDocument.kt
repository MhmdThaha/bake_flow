package com.bakeflow.app.data.model

import com.bakeflow.app.data.remote.ProductFirestoreConstants
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.model.ProductStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class ProductDocument(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val category: String = "",
    val sellingPrice: Double = 0.0,
    val finishedStock: Double = 0.0,
    val status: String = ProductStatus.ACTIVE.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): Product = Product(
        id = id,
        ownerId = ownerId,
        name = name,
        category = category,
        sellingPrice = sellingPrice,
        finishedStock = finishedStock,
        status = ProductStatus.fromString(status)
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        ProductFirestoreConstants.FIELD_ID to id,
        ProductFirestoreConstants.FIELD_OWNER_ID to ownerId,
        ProductFirestoreConstants.FIELD_NAME to name,
        ProductFirestoreConstants.FIELD_CATEGORY to category,
        ProductFirestoreConstants.FIELD_SELLING_PRICE to sellingPrice,
        ProductFirestoreConstants.FIELD_FINISHED_STOCK to finishedStock,
        ProductFirestoreConstants.FIELD_STATUS to status,
        ProductFirestoreConstants.FIELD_CREATED_AT to createdAt,
        ProductFirestoreConstants.FIELD_UPDATED_AT to updatedAt
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): ProductDocument? {
            if (!snapshot.exists()) return null
            return ProductDocument(
                id = snapshot.getString(ProductFirestoreConstants.FIELD_ID)
                    ?: snapshot.id,
                ownerId = snapshot.getString(ProductFirestoreConstants.FIELD_OWNER_ID).orEmpty(),
                name = snapshot.getString(ProductFirestoreConstants.FIELD_NAME).orEmpty(),
                category = snapshot.getString(ProductFirestoreConstants.FIELD_CATEGORY).orEmpty(),
                sellingPrice = snapshot.getDouble(ProductFirestoreConstants.FIELD_SELLING_PRICE) ?: 0.0,
                finishedStock = snapshot.getDouble(ProductFirestoreConstants.FIELD_FINISHED_STOCK) ?: 0.0,
                status = snapshot.getString(ProductFirestoreConstants.FIELD_STATUS)
                    ?: ProductStatus.ACTIVE.name,
                createdAt = snapshot.getTimestamp(ProductFirestoreConstants.FIELD_CREATED_AT),
                updatedAt = snapshot.getTimestamp(ProductFirestoreConstants.FIELD_UPDATED_AT)
            )
        }
    }
}
