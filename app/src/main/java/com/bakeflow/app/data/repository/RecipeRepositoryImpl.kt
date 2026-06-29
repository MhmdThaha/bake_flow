package com.bakeflow.app.data.repository

import com.bakeflow.app.data.model.RecipeDocument
import com.bakeflow.app.data.model.RecipeItemDocument
import com.bakeflow.app.data.network.NetworkMonitor
import com.bakeflow.app.data.remote.FirestoreListenerHelper
import com.bakeflow.app.data.remote.RecipeFirestoreConstants
import com.bakeflow.app.domain.model.Recipe
import com.bakeflow.app.domain.model.RecipeItem
import com.bakeflow.app.domain.model.RecipeWithItems
import com.bakeflow.app.domain.repository.RecipeRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RecipeRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor
) : RecipeRepository {

    override fun observeRecipes(): Flow<List<Recipe>> = callbackFlow {
        val ownerId = firebaseAuth.currentUser?.uid
        if (ownerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(RecipeFirestoreConstants.RECIPES_COLLECTION)
            .whereEqualTo(RecipeFirestoreConstants.FIELD_OWNER_ID, ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (FirestoreListenerHelper.isMissingCompositeIndex(error)) {
                        FirestoreListenerHelper.logMissingIndex(
                            error,
                            RecipeFirestoreConstants.RECIPES_COLLECTION,
                            "ownerId filter"
                        )
                        trySend(emptyList())
                    } else {
                        close(mapFirestoreException(error))
                    }
                    return@addSnapshotListener
                }
                val recipes = snapshot?.documents
                    ?.mapNotNull { RecipeDocument.fromSnapshot(it)?.toDomain() }
                    .orEmpty()
                trySend(recipes)
            }

        awaitClose { listener.remove() }
    }

    override fun observeRecipeItems(): Flow<List<RecipeItem>> = callbackFlow {
        val ownerId = firebaseAuth.currentUser?.uid
        if (ownerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(RecipeFirestoreConstants.RECIPE_ITEMS_COLLECTION)
            .whereEqualTo(RecipeFirestoreConstants.FIELD_OWNER_ID, ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (FirestoreListenerHelper.isMissingCompositeIndex(error)) {
                        FirestoreListenerHelper.logMissingIndex(
                            error,
                            RecipeFirestoreConstants.RECIPE_ITEMS_COLLECTION,
                            "ownerId filter"
                        )
                        trySend(emptyList())
                    } else {
                        close(mapFirestoreException(error))
                    }
                    return@addSnapshotListener
                }
                val items = snapshot?.documents
                    ?.mapNotNull { RecipeItemDocument.fromSnapshot(it)?.toDomain() }
                    .orEmpty()
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getRecipeWithItems(recipeId: String): Result<RecipeWithItems> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(RecipeException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val recipeSnapshot = firestore.collection(RecipeFirestoreConstants.RECIPES_COLLECTION)
                .document(recipeId)
                .get()
                .await()
            val recipeDocument = RecipeDocument.fromSnapshot(recipeSnapshot)
                ?: throw RecipeException("Recipe not found.")
            verifyOwnership(recipeDocument.ownerId)

            val itemsSnapshot = firestore.collection(RecipeFirestoreConstants.RECIPE_ITEMS_COLLECTION)
                .whereEqualTo(RecipeFirestoreConstants.FIELD_RECIPE_ID, recipeId)
                .get()
                .await()

            val items = itemsSnapshot.documents
                .mapNotNull { RecipeItemDocument.fromSnapshot(it)?.toDomain() }

            RecipeWithItems(recipe = recipeDocument.toDomain(), items = items)
        }.mapRecipeError()
    }

    override suspend fun saveRecipe(recipe: Recipe, items: List<RecipeItem>): Result<RecipeWithItems> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(RecipeException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val ownerId = requireOwnerId()
            verifyProductRecipeUniqueness(recipe.productId, recipe.id)

            val recipeId = recipe.id.ifBlank {
                firestore.collection(RecipeFirestoreConstants.RECIPES_COLLECTION).document().id
            }
            val now = Timestamp.now()

            val existingRecipe = if (recipe.id.isNotBlank()) {
                firestore.collection(RecipeFirestoreConstants.RECIPES_COLLECTION)
                    .document(recipe.id)
                    .get()
                    .await()
            } else {
                null
            }
            val createdAt = existingRecipe
                ?.getTimestamp(RecipeFirestoreConstants.FIELD_CREATED_AT)
                ?: now

            val recipeDocument = RecipeDocument(
                id = recipeId,
                ownerId = ownerId,
                productId = recipe.productId,
                createdAt = createdAt,
                updatedAt = now
            )

            val batch = firestore.batch()
            val recipeRef = firestore.collection(RecipeFirestoreConstants.RECIPES_COLLECTION)
                .document(recipeId)
            batch.set(recipeRef, recipeDocument.toFirestoreMap())

            val existingItems = firestore.collection(RecipeFirestoreConstants.RECIPE_ITEMS_COLLECTION)
                .whereEqualTo(RecipeFirestoreConstants.FIELD_RECIPE_ID, recipeId)
                .get()
                .await()
            existingItems.documents.forEach { batch.delete(it.reference) }

            val savedItems = items.map { item ->
                val itemId = item.id.ifBlank {
                    firestore.collection(RecipeFirestoreConstants.RECIPE_ITEMS_COLLECTION).document().id
                }
                val itemDocument = RecipeItemDocument(
                    id = itemId,
                    recipeId = recipeId,
                    ownerId = ownerId,
                    ingredientId = item.ingredientId,
                    quantity = item.quantity,
                    unit = item.unit
                )
                val itemRef = firestore.collection(RecipeFirestoreConstants.RECIPE_ITEMS_COLLECTION)
                    .document(itemId)
                batch.set(itemRef, itemDocument.toFirestoreMap())
                itemDocument.toDomain()
            }

            batch.commit().await()
            RecipeWithItems(recipe = recipeDocument.toDomain(), items = savedItems)
        }.mapRecipeError()
    }

    override suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(RecipeException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val recipeSnapshot = firestore.collection(RecipeFirestoreConstants.RECIPES_COLLECTION)
                .document(recipeId)
                .get()
                .await()
            val recipeDocument = RecipeDocument.fromSnapshot(recipeSnapshot)
                ?: throw RecipeException("Recipe not found.")
            verifyOwnership(recipeDocument.ownerId)

            val batch = firestore.batch()
            val itemsSnapshot = firestore.collection(RecipeFirestoreConstants.RECIPE_ITEMS_COLLECTION)
                .whereEqualTo(RecipeFirestoreConstants.FIELD_RECIPE_ID, recipeId)
                .get()
                .await()
            itemsSnapshot.documents.forEach { batch.delete(it.reference) }
            batch.delete(
                firestore.collection(RecipeFirestoreConstants.RECIPES_COLLECTION).document(recipeId)
            )
            batch.commit().await()
            Unit
        }.mapRecipeError()
    }

    private suspend fun verifyProductRecipeUniqueness(productId: String, currentRecipeId: String) {
        val ownerId = requireOwnerId()
        val existing = firestore.collection(RecipeFirestoreConstants.RECIPES_COLLECTION)
            .whereEqualTo(RecipeFirestoreConstants.FIELD_OWNER_ID, ownerId)
            .whereEqualTo(RecipeFirestoreConstants.FIELD_PRODUCT_ID, productId)
            .get()
            .await()
        val conflict = existing.documents.any { doc ->
            val id = doc.getString(RecipeFirestoreConstants.FIELD_ID) ?: doc.id
            id != currentRecipeId
        }
        if (conflict) {
            throw RecipeException("This product already has a recipe. Edit the existing recipe instead.")
        }
    }

    private fun requireOwnerId(): String =
        firebaseAuth.currentUser?.uid
            ?: throw RecipeException("You must be signed in to manage recipes.")

    private fun verifyOwnership(ownerId: String) {
        val currentOwnerId = firebaseAuth.currentUser?.uid
        if (currentOwnerId == null || currentOwnerId != ownerId) {
            throw RecipeException("You do not have permission to modify this recipe.")
        }
    }

    private fun mapFirestoreException(exception: FirebaseFirestoreException): RecipeException =
        RecipeException(
            when (exception.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No internet connection. Check your network and try again."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied. Check your Firestore security rules."
                else -> "Failed to load recipes. Please try again."
            },
            exception
        )

    private fun <T> Result<T>.mapRecipeError(): Result<T> =
        recoverCatching { throw mapException(it) }

    private fun mapException(throwable: Throwable): RecipeException =
        when (throwable) {
            is RecipeException -> throwable
            is FirebaseFirestoreException -> mapFirestoreException(throwable)
            else -> RecipeException(
                throwable.localizedMessage ?: "Something went wrong. Please try again.",
                throwable
            )
        }
}

class RecipeException(message: String, cause: Throwable? = null) : Exception(message, cause)
