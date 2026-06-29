package com.bakeflow.app.data.model

import com.bakeflow.app.data.remote.StockAdjustmentFirestoreConstants
import com.bakeflow.app.domain.model.AdjustmentItemType
import com.bakeflow.app.domain.model.AdjustmentReason
import com.bakeflow.app.domain.model.StockAdjustment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class StockAdjustmentDocument(
    val adjustmentId: String = "",
    val ownerId: String = "",
    val itemType: String = AdjustmentItemType.INGREDIENT.name,
    val itemId: String = "",
    val itemName: String = "",
    val previousQuantity: Double = 0.0,
    val adjustedQuantity: Double = 0.0,
    val difference: Double = 0.0,
    val adjustmentReason: String = AdjustmentReason.OTHER.name,
    val notes: String = "",
    val adjustmentDate: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val createdBy: String = ""
) {
    fun toDomain(): StockAdjustment = StockAdjustment(
        adjustmentId = adjustmentId,
        ownerId = ownerId,
        itemType = AdjustmentItemType.fromString(itemType),
        itemId = itemId,
        itemName = itemName,
        previousQuantity = previousQuantity,
        adjustedQuantity = adjustedQuantity,
        difference = difference,
        adjustmentReason = AdjustmentReason.fromString(adjustmentReason),
        notes = notes,
        adjustmentDate = adjustmentDate?.toDate()?.time ?: 0L,
        createdAt = createdAt?.toDate()?.time ?: 0L,
        createdBy = createdBy
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        StockAdjustmentFirestoreConstants.FIELD_ADJUSTMENT_ID to adjustmentId,
        StockAdjustmentFirestoreConstants.FIELD_OWNER_ID to ownerId,
        StockAdjustmentFirestoreConstants.FIELD_ITEM_TYPE to itemType,
        StockAdjustmentFirestoreConstants.FIELD_ITEM_ID to itemId,
        StockAdjustmentFirestoreConstants.FIELD_ITEM_NAME to itemName,
        StockAdjustmentFirestoreConstants.FIELD_PREVIOUS_QUANTITY to previousQuantity,
        StockAdjustmentFirestoreConstants.FIELD_ADJUSTED_QUANTITY to adjustedQuantity,
        StockAdjustmentFirestoreConstants.FIELD_DIFFERENCE to difference,
        StockAdjustmentFirestoreConstants.FIELD_ADJUSTMENT_REASON to adjustmentReason,
        StockAdjustmentFirestoreConstants.FIELD_NOTES to notes,
        StockAdjustmentFirestoreConstants.FIELD_ADJUSTMENT_DATE to adjustmentDate,
        StockAdjustmentFirestoreConstants.FIELD_CREATED_AT to createdAt,
        StockAdjustmentFirestoreConstants.FIELD_CREATED_BY to createdBy
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): StockAdjustmentDocument? {
            if (!snapshot.exists()) return null
            return StockAdjustmentDocument(
                adjustmentId = snapshot.getString(StockAdjustmentFirestoreConstants.FIELD_ADJUSTMENT_ID)
                    ?: snapshot.id,
                ownerId = snapshot.getString(StockAdjustmentFirestoreConstants.FIELD_OWNER_ID).orEmpty(),
                itemType = snapshot.getString(StockAdjustmentFirestoreConstants.FIELD_ITEM_TYPE)
                    ?: AdjustmentItemType.INGREDIENT.name,
                itemId = snapshot.getString(StockAdjustmentFirestoreConstants.FIELD_ITEM_ID).orEmpty(),
                itemName = snapshot.getString(StockAdjustmentFirestoreConstants.FIELD_ITEM_NAME).orEmpty(),
                previousQuantity = snapshot.getDouble(
                    StockAdjustmentFirestoreConstants.FIELD_PREVIOUS_QUANTITY
                ) ?: 0.0,
                adjustedQuantity = snapshot.getDouble(
                    StockAdjustmentFirestoreConstants.FIELD_ADJUSTED_QUANTITY
                ) ?: 0.0,
                difference = snapshot.getDouble(StockAdjustmentFirestoreConstants.FIELD_DIFFERENCE) ?: 0.0,
                adjustmentReason = snapshot.getString(
                    StockAdjustmentFirestoreConstants.FIELD_ADJUSTMENT_REASON
                ) ?: AdjustmentReason.OTHER.name,
                notes = snapshot.getString(StockAdjustmentFirestoreConstants.FIELD_NOTES).orEmpty(),
                adjustmentDate = snapshot.getTimestamp(StockAdjustmentFirestoreConstants.FIELD_ADJUSTMENT_DATE),
                createdAt = snapshot.getTimestamp(StockAdjustmentFirestoreConstants.FIELD_CREATED_AT),
                createdBy = snapshot.getString(StockAdjustmentFirestoreConstants.FIELD_CREATED_BY).orEmpty()
            )
        }
    }
}
