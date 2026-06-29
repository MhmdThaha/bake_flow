package com.bakeflow.app.data.model

import com.bakeflow.app.data.remote.SaleFirestoreConstants
import com.bakeflow.app.domain.model.PaymentMethod
import com.bakeflow.app.domain.model.Sale
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class SaleDocument(
    val saleId: String = "",
    val ownerId: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val customerName: String = "",
    val customerPhone: String = "",
    val notes: String = "",
    val saleDate: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val createdBy: String = ""
) {
    fun toDomain(): Sale = Sale(
        saleId = saleId,
        ownerId = ownerId,
        productId = productId,
        productName = productName,
        quantity = quantity,
        unitPrice = unitPrice,
        totalAmount = totalAmount,
        paymentMethod = PaymentMethod.fromString(paymentMethod),
        customerName = customerName,
        customerPhone = customerPhone,
        notes = notes,
        saleDate = saleDate?.toDate()?.time ?: 0L,
        createdAt = createdAt?.toDate()?.time ?: 0L,
        createdBy = createdBy
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        SaleFirestoreConstants.FIELD_SALE_ID to saleId,
        SaleFirestoreConstants.FIELD_OWNER_ID to ownerId,
        SaleFirestoreConstants.FIELD_PRODUCT_ID to productId,
        SaleFirestoreConstants.FIELD_PRODUCT_NAME to productName,
        SaleFirestoreConstants.FIELD_QUANTITY to quantity,
        SaleFirestoreConstants.FIELD_UNIT_PRICE to unitPrice,
        SaleFirestoreConstants.FIELD_TOTAL_AMOUNT to totalAmount,
        SaleFirestoreConstants.FIELD_PAYMENT_METHOD to paymentMethod,
        SaleFirestoreConstants.FIELD_CUSTOMER_NAME to customerName,
        SaleFirestoreConstants.FIELD_CUSTOMER_PHONE to customerPhone,
        SaleFirestoreConstants.FIELD_NOTES to notes,
        SaleFirestoreConstants.FIELD_SALE_DATE to saleDate,
        SaleFirestoreConstants.FIELD_CREATED_AT to createdAt,
        SaleFirestoreConstants.FIELD_CREATED_BY to createdBy
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): SaleDocument? {
            if (!snapshot.exists()) return null
            return SaleDocument(
                saleId = snapshot.getString(SaleFirestoreConstants.FIELD_SALE_ID) ?: snapshot.id,
                ownerId = snapshot.getString(SaleFirestoreConstants.FIELD_OWNER_ID).orEmpty(),
                productId = snapshot.getString(SaleFirestoreConstants.FIELD_PRODUCT_ID).orEmpty(),
                productName = snapshot.getString(SaleFirestoreConstants.FIELD_PRODUCT_NAME).orEmpty(),
                quantity = snapshot.getDouble(SaleFirestoreConstants.FIELD_QUANTITY) ?: 0.0,
                unitPrice = snapshot.getDouble(SaleFirestoreConstants.FIELD_UNIT_PRICE) ?: 0.0,
                totalAmount = snapshot.getDouble(SaleFirestoreConstants.FIELD_TOTAL_AMOUNT) ?: 0.0,
                paymentMethod = snapshot.getString(SaleFirestoreConstants.FIELD_PAYMENT_METHOD)
                    ?: PaymentMethod.CASH.name,
                customerName = snapshot.getString(SaleFirestoreConstants.FIELD_CUSTOMER_NAME).orEmpty(),
                customerPhone = snapshot.getString(SaleFirestoreConstants.FIELD_CUSTOMER_PHONE).orEmpty(),
                notes = snapshot.getString(SaleFirestoreConstants.FIELD_NOTES).orEmpty(),
                saleDate = snapshot.getTimestamp(SaleFirestoreConstants.FIELD_SALE_DATE),
                createdAt = snapshot.getTimestamp(SaleFirestoreConstants.FIELD_CREATED_AT),
                createdBy = snapshot.getString(SaleFirestoreConstants.FIELD_CREATED_BY).orEmpty()
            )
        }
    }
}
