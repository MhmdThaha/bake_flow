package com.bakeflow.app.data.remote

object StockAdjustmentFirestoreConstants {
    const val COLLECTION = "stock_adjustments"

    const val FIELD_ADJUSTMENT_ID = "adjustmentId"
    const val FIELD_OWNER_ID = "ownerId"
    const val FIELD_ITEM_TYPE = "itemType"
    const val FIELD_ITEM_ID = "itemId"
    const val FIELD_ITEM_NAME = "itemName"
    const val FIELD_PREVIOUS_QUANTITY = "previousQuantity"
    const val FIELD_ADJUSTED_QUANTITY = "adjustedQuantity"
    const val FIELD_DIFFERENCE = "difference"
    const val FIELD_ADJUSTMENT_REASON = "adjustmentReason"
    const val FIELD_NOTES = "notes"
    const val FIELD_ADJUSTMENT_DATE = "adjustmentDate"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_CREATED_BY = "createdBy"
}
