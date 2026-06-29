package com.bakeflow.app.data.model

import com.bakeflow.app.data.remote.FirestoreConstants
import com.bakeflow.app.domain.model.User
import com.bakeflow.app.domain.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class UserDocument(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = UserRole.STAFF.name,
    val createdAt: Timestamp? = null,
    val lastLogin: Timestamp? = null
) {
    fun toDomain(): User = User(
        uid = uid,
        name = name,
        email = email,
        role = UserRole.fromString(role),
        createdAt = createdAt?.toDate()?.time,
        lastLogin = lastLogin?.toDate()?.time
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        FirestoreConstants.FIELD_UID to uid,
        FirestoreConstants.FIELD_NAME to name,
        FirestoreConstants.FIELD_EMAIL to email,
        FirestoreConstants.FIELD_ROLE to role,
        FirestoreConstants.FIELD_CREATED_AT to createdAt,
        FirestoreConstants.FIELD_LAST_LOGIN to lastLogin
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): UserDocument? {
            if (!snapshot.exists()) return null
            return UserDocument(
                uid = snapshot.getString(FirestoreConstants.FIELD_UID).orEmpty(),
                name = snapshot.getString(FirestoreConstants.FIELD_NAME).orEmpty(),
                email = snapshot.getString(FirestoreConstants.FIELD_EMAIL).orEmpty(),
                role = snapshot.getString(FirestoreConstants.FIELD_ROLE) ?: UserRole.STAFF.name,
                createdAt = snapshot.getTimestamp(FirestoreConstants.FIELD_CREATED_AT),
                lastLogin = snapshot.getTimestamp(FirestoreConstants.FIELD_LAST_LOGIN)
            )
        }
    }
}
