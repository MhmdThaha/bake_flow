package com.bakeflow.app.data.repository

import com.bakeflow.app.data.model.UserDocument
import com.bakeflow.app.data.network.NetworkMonitor
import com.bakeflow.app.data.remote.FirestoreConstants
import com.bakeflow.app.domain.model.User
import com.bakeflow.app.domain.model.UserRole
import com.bakeflow.app.domain.repository.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor
) : AuthRepository {

    private val userRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val currentUser: Flow<User?> = callbackFlow {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun emitCurrentUser() {
            val firebaseUser = firebaseAuth.currentUser
            val user = if (firebaseUser == null) {
                null
            } else {
                loadUserProfile(firebaseUser.uid)
            }
            trySend(user)
        }

        val listener = FirebaseAuth.AuthStateListener {
            scope.launch { emitCurrentUser() }
        }
        firebaseAuth.addAuthStateListener(listener)
        scope.launch {
            userRefresh.collect { emitCurrentUser() }
        }
        scope.launch { emitCurrentUser() }
        awaitClose {
            scope.cancel()
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(OfflineException())
        }
        return runCatching {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: throw AuthException("Sign in failed. Please try again.")
            updateLastLogin(firebaseUser.uid)
            val user = loadUserProfile(firebaseUser.uid)
                ?: createFallbackProfile(firebaseUser, UserRole.STAFF)
            userRefresh.tryEmit(Unit)
            user
        }.mapAuthError()
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<User> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(OfflineException())
        }
        return runCatching {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: throw AuthException("Registration failed. Please try again.")
            val now = Timestamp.now()
            val userDocument = UserDocument(
                uid = firebaseUser.uid,
                name = name.trim(),
                email = email.trim(),
                role = role.name,
                createdAt = now,
                lastLogin = now
            )
            try {
                saveUserProfile(userDocument)
            } catch (e: Exception) {
                runCatching { firebaseUser.delete().await() }
                throw e
            }
            userRefresh.tryEmit(Unit)
            userDocument.toDomain()
        }.mapAuthError()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(OfflineException())
        }
        return runCatching {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            Unit
        }.mapAuthError()
    }

    override suspend fun signInWithGoogle(idToken: String, displayName: String?): Result<User> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(OfflineException())
        }
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: throw AuthException("Google sign in failed. Please try again.")
            val existingProfile = loadUserProfile(firebaseUser.uid)
            val user = if (existingProfile != null) {
                updateLastLogin(firebaseUser.uid)
                existingProfile.copy(lastLogin = System.currentTimeMillis())
            } else {
                val now = Timestamp.now()
                val userDocument = UserDocument(
                    uid = firebaseUser.uid,
                    name = displayName?.trim().orEmpty().ifBlank {
                        firebaseUser.displayName.orEmpty().ifBlank { "BakeFlow User" }
                    },
                    email = firebaseUser.email.orEmpty(),
                    role = UserRole.STAFF.name,
                    createdAt = now,
                    lastLogin = now
                )
                try {
                    saveUserProfile(userDocument)
                } catch (e: Exception) {
                    runCatching { firebaseUser.delete().await() }
                    throw e
                }
                userDocument.toDomain()
            }
            userRefresh.tryEmit(Unit)
            user
        }.mapAuthError()
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun refreshCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return loadUserProfile(firebaseUser.uid)
    }

    private suspend fun loadUserProfile(uid: String): User? {
        return try {
            val snapshot = firestore.collection(FirestoreConstants.USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
            UserDocument.fromSnapshot(snapshot)?.toDomain()
        } catch (e: FirebaseFirestoreException) {
            null
        }
    }

    private suspend fun saveUserProfile(userDocument: UserDocument) {
        firestore.collection(FirestoreConstants.USERS_COLLECTION)
            .document(userDocument.uid)
            .set(userDocument.toFirestoreMap())
            .await()
    }

    private suspend fun updateLastLogin(uid: String) {
        firestore.collection(FirestoreConstants.USERS_COLLECTION)
            .document(uid)
            .update(FirestoreConstants.FIELD_LAST_LOGIN, Timestamp.now())
            .await()
    }

    private suspend fun createFallbackProfile(
        firebaseUser: FirebaseUser,
        role: UserRole
    ): User {
        val now = Timestamp.now()
        val userDocument = UserDocument(
            uid = firebaseUser.uid,
            name = firebaseUser.displayName.orEmpty().ifBlank { "BakeFlow User" },
            email = firebaseUser.email.orEmpty(),
            role = role.name,
            createdAt = now,
            lastLogin = now
        )
        saveUserProfile(userDocument)
        return userDocument.toDomain()
    }

    private fun <T> Result<T>.mapAuthError(): Result<T> = this
        .recoverCatching { throw mapException(it) }

    private fun mapException(throwable: Throwable): AuthException {
        return when (throwable) {
            is OfflineException -> throwable
            is AuthException -> throwable
            is FirebaseAuthException -> AuthException(
                message = mapFirebaseAuthMessage(throwable.errorCode),
                cause = throwable
            )
            is FirebaseFirestoreException -> AuthException(
                message = "Unable to save your profile. Please try again.",
                cause = throwable
            )
            else -> AuthException(
                message = throwable.localizedMessage ?: "Something went wrong. Please try again.",
                cause = throwable
            )
        }
    }

    private fun mapFirebaseAuthMessage(errorCode: String): String = when (errorCode) {
        "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Incorrect email or password."
        "ERROR_USER_NOT_FOUND" -> "No account found with this email."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
        "ERROR_WEAK_PASSWORD" -> "Password is too weak. Use at least 8 characters."
        "ERROR_USER_DISABLED" -> "This account has been disabled."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait and try again."
        "ERROR_NETWORK_REQUEST_FAILED" -> "No internet connection. Check your network and try again."
        else -> "Authentication failed. Please try again."
    }
}

open class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

class OfflineException : AuthException("No internet connection. Check your network and try again.")
