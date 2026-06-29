package com.bakeflow.app.data.model

import com.bakeflow.app.data.remote.WasteFirestoreConstants
import com.bakeflow.app.domain.model.Waste
import com.bakeflow.app.domain.model.WasteReason
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class WasteDocument(
    val wasteId: String = "",
    val ownerId: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Double = 0.0,
    val reason: String = WasteReason.OTHER.name,
    val notes: String = "",
    val estimatedLoss: Double = 0.0,
    val wasteDate: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val createdBy: String = ""
) {
    fun toDomain(): Waste = Waste(
        wasteId = wasteId,
        ownerId = ownerId,
        productId = productId,
        productName = productName,
        quantity = quantity,
        reason = WasteReason.fromString(reason),
        notes = notes,
        estimatedLoss = estimatedLoss,
        wasteDate = wasteDate?.toDate()?.time ?: 0L,
        createdAt = createdAt?.toDate()?.time ?: 0L,
        createdBy = createdBy
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        WasteFirestoreConstants.FIELD_WASTE_ID to wasteId,
        WasteFirestoreConstants.FIELD_OWNER_ID to ownerId,
        WasteFirestoreConstants.FIELD_PRODUCT_ID to productId,
        WasteFirestoreConstants.FIELD_PRODUCT_NAME to productName,
        WasteFirestoreConstants.FIELD_QUANTITY to quantity,
        WasteFirestoreConstants.FIELD_REASON to reason,
        WasteFirestoreConstants.FIELD_NOTES to notes,
        WasteFirestoreConstants.FIELD_ESTIMATED_LOSS to estimatedLoss,
        WasteFirestoreConstants.FIELD_WASTE_DATE to wasteDate,
        WasteFirestoreConstants.FIELD_CREATED_AT to createdAt,
        WasteFirestoreConstants.FIELD_CREATED_BY to createdBy
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): WasteDocument? {
            if (!snapshot.exists()) return null
            return WasteDocument(
                wasteId = snapshot.getString(WasteFirestoreConstants.FIELD_WASTE_ID) ?: snapshot.id,
                ownerId = snapshot.getString(WasteFirestoreConstants.FIELD_OWNER_ID).orEmpty(),
                productId = snapshot.getString(WasteFirestoreConstants.FIELD_PRODUCT_ID).orEmpty(),
                productName = snapshot.getString(WasteFirestoreConstants.FIELD_PRODUCT_NAME).orEmpty(),
                quantity = snapshot.getDouble(WasteFirestoreConstants.FIELD_QUANTITY) ?: 0.0,
                reason = snapshot.getString(WasteFirestoreConstants.FIELD_REASON)
                    ?: WasteReason.OTHER.name,
                notes = snapshot.getString(WasteFirestoreConstants.FIELD_NOTES).orEmpty(),
                estimatedLoss = snapshot.getDouble(WasteFirestoreConstants.FIELD_ESTIMATED_LOSS) ?: 0.0,
                wasteDate = snapshot.getTimestamp(WasteFirestoreConstants.FIELD_WASTE_DATE),
                createdAt = snapshot.getTimestamp(WasteFirestoreConstants.FIELD_CREATED_AT),
                createdBy = snapshot.getString(WasteFirestoreConstants.FIELD_CREATED_BY).orEmpty()
            )
        }
    }
}
