package com.bakeflow.app.data.model

import com.bakeflow.app.data.remote.PurchaseFirestoreConstants
import com.bakeflow.app.domain.model.Purchase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class PurchaseDocument(
    val purchaseId: String = "",
    val ownerId: String = "",
    val ingredientId: String = "",
    val ingredientName: String = "",
    val supplierName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val costPerUnit: Double = 0.0,
    val totalCost: Double = 0.0,
    val purchaseDate: Timestamp? = null,
    val invoiceNumber: String = "",
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val createdBy: String = ""
) {
    fun toDomain(): Purchase = Purchase(
        purchaseId = purchaseId,
        ownerId = ownerId,
        ingredientId = ingredientId,
        ingredientName = ingredientName,
        supplierName = supplierName,
        quantity = quantity,
        unit = unit,
        costPerUnit = costPerUnit,
        totalCost = totalCost,
        purchaseDate = purchaseDate?.toDate()?.time ?: 0L,
        invoiceNumber = invoiceNumber,
        notes = notes,
        createdAt = createdAt?.toDate()?.time ?: 0L,
        createdBy = createdBy
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        PurchaseFirestoreConstants.FIELD_PURCHASE_ID to purchaseId,
        PurchaseFirestoreConstants.FIELD_OWNER_ID to ownerId,
        PurchaseFirestoreConstants.FIELD_INGREDIENT_ID to ingredientId,
        PurchaseFirestoreConstants.FIELD_INGREDIENT_NAME to ingredientName,
        PurchaseFirestoreConstants.FIELD_SUPPLIER_NAME to supplierName,
        PurchaseFirestoreConstants.FIELD_QUANTITY to quantity,
        PurchaseFirestoreConstants.FIELD_UNIT to unit,
        PurchaseFirestoreConstants.FIELD_COST_PER_UNIT to costPerUnit,
        PurchaseFirestoreConstants.FIELD_TOTAL_COST to totalCost,
        PurchaseFirestoreConstants.FIELD_PURCHASE_DATE to purchaseDate,
        PurchaseFirestoreConstants.FIELD_INVOICE_NUMBER to invoiceNumber,
        PurchaseFirestoreConstants.FIELD_NOTES to notes,
        PurchaseFirestoreConstants.FIELD_CREATED_AT to createdAt,
        PurchaseFirestoreConstants.FIELD_CREATED_BY to createdBy
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): PurchaseDocument? {
            if (!snapshot.exists()) return null
            return PurchaseDocument(
                purchaseId = snapshot.getString(PurchaseFirestoreConstants.FIELD_PURCHASE_ID)
                    ?: snapshot.id,
                ownerId = snapshot.getString(PurchaseFirestoreConstants.FIELD_OWNER_ID).orEmpty(),
                ingredientId = snapshot.getString(PurchaseFirestoreConstants.FIELD_INGREDIENT_ID).orEmpty(),
                ingredientName = snapshot.getString(PurchaseFirestoreConstants.FIELD_INGREDIENT_NAME).orEmpty(),
                supplierName = snapshot.getString(PurchaseFirestoreConstants.FIELD_SUPPLIER_NAME).orEmpty(),
                quantity = snapshot.getDouble(PurchaseFirestoreConstants.FIELD_QUANTITY) ?: 0.0,
                unit = snapshot.getString(PurchaseFirestoreConstants.FIELD_UNIT).orEmpty(),
                costPerUnit = snapshot.getDouble(PurchaseFirestoreConstants.FIELD_COST_PER_UNIT) ?: 0.0,
                totalCost = snapshot.getDouble(PurchaseFirestoreConstants.FIELD_TOTAL_COST) ?: 0.0,
                purchaseDate = snapshot.getTimestamp(PurchaseFirestoreConstants.FIELD_PURCHASE_DATE),
                invoiceNumber = snapshot.getString(PurchaseFirestoreConstants.FIELD_INVOICE_NUMBER).orEmpty(),
                notes = snapshot.getString(PurchaseFirestoreConstants.FIELD_NOTES).orEmpty(),
                createdAt = snapshot.getTimestamp(PurchaseFirestoreConstants.FIELD_CREATED_AT),
                createdBy = snapshot.getString(PurchaseFirestoreConstants.FIELD_CREATED_BY).orEmpty()
            )
        }
    }
}
