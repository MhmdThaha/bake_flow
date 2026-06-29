package com.bakeflow.app.data.repository

import com.bakeflow.app.data.model.IngredientDocument
import com.bakeflow.app.data.model.IngredientUsageDocument
import com.bakeflow.app.data.model.ProductDocument
import com.bakeflow.app.data.model.ProductionBatchDocument
import com.bakeflow.app.data.network.NetworkMonitor
import com.bakeflow.app.data.remote.IngredientFirestoreConstants
import com.bakeflow.app.data.remote.ProductFirestoreConstants
import com.bakeflow.app.data.remote.FirestoreListenerHelper
import com.bakeflow.app.data.remote.ProductionFirestoreConstants
import com.bakeflow.app.domain.model.ProductionBatch
import com.bakeflow.app.domain.model.ProductionDashboardStats
import com.bakeflow.app.domain.model.ProductionStatus
import com.bakeflow.app.domain.repository.ExecuteProductionRequest
import com.bakeflow.app.domain.repository.ProductionRepository
import com.bakeflow.app.domain.repository.RecipeRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

class ProductionRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor,
    private val recipeRepository: RecipeRepository
) : ProductionRepository {

    override fun observeBatches(): Flow<List<ProductionBatch>> = callbackFlow {
        val ownerId = firebaseAuth.currentUser?.uid
        if (ownerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(ProductionFirestoreConstants.COLLECTION)
            .whereEqualTo(ProductionFirestoreConstants.FIELD_OWNER_ID, ownerId)
            .orderBy(ProductionFirestoreConstants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (FirestoreListenerHelper.isMissingCompositeIndex(error)) {
                        FirestoreListenerHelper.logMissingIndex(
                            error,
                            ProductionFirestoreConstants.COLLECTION,
                            "ownerId ASC + createdAt DESC"
                        )
                        trySend(emptyList())
                    } else {
                        close(mapFirestoreException(error))
                    }
                    return@addSnapshotListener
                }
                val batches = snapshot?.documents
                    ?.mapNotNull { ProductionBatchDocument.fromSnapshot(it)?.toDomain() }
                    .orEmpty()
                trySend(batches)
            }

        awaitClose { listener.remove() }
    }

    override fun observeDashboardStats(): Flow<ProductionDashboardStats> =
        observeBatches()
            .map { batches -> computeDashboardStats(batches) }
            .flowOn(Dispatchers.Default)

    override suspend fun getBatch(batchId: String): Result<ProductionBatch> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(ProductionException("No internet connection. Check your network and try again."))
        }
        return runCatching {
            val snapshot = firestore.collection(ProductionFirestoreConstants.COLLECTION)
                .document(batchId)
                .get()
                .await()
            val document = ProductionBatchDocument.fromSnapshot(snapshot)
                ?: throw ProductionException("Production batch not found.")
            verifyOwnership(document.ownerId)
            document.toDomain()
        }.mapProductionError()
    }

    override suspend fun executeProduction(request: ExecuteProductionRequest): Result<ProductionBatch> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(ProductionException("No internet connection. Check your network and try again."))
        }
        if (request.quantityProduced <= 0) {
            return Result.failure(ProductionException("Quantity must be greater than zero."))
        }

        val ownerId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(ProductionException("You must be signed in to start production."))

        val recipeWithItems = recipeRepository.getRecipeWithItems(request.recipeId)
            .getOrElse { return Result.failure(mapRecipeError(it)) }

        if (recipeWithItems.recipe.productId != request.productId) {
            return Result.failure(ProductionException("Recipe does not match the selected product."))
        }

        return runCatching {
            val now = Timestamp.now()
            val batchRef = firestore.collection(ProductionFirestoreConstants.COLLECTION).document()
            val batchId = batchRef.id

            firestore.runTransaction { transaction ->
                val usageEntries = mutableListOf<IngredientUsageDocument>()
                var estimatedCost = 0.0

                recipeWithItems.items.forEach { recipeItem ->
                    val ingredientRef = firestore
                        .collection(IngredientFirestoreConstants.INGREDIENTS_COLLECTION)
                        .document(recipeItem.ingredientId)
                    val ingredientSnapshot = transaction.get(ingredientRef)
                    val ingredientDoc = IngredientDocument.fromSnapshot(ingredientSnapshot)
                        ?: throw ProductionException("Ingredient not found for recipe.")
                    if (ingredientDoc.ownerId != ownerId) {
                        throw ProductionException("You do not have access to this ingredient.")
                    }

                    val requiredQuantity = recipeItem.quantity * request.quantityProduced
                    if (ingredientDoc.currentStock < requiredQuantity) {
                        throw ProductionException(
                            "Insufficient stock for ${ingredientDoc.name}. " +
                                "Need ${formatQty(requiredQuantity)}, have ${formatQty(ingredientDoc.currentStock)}."
                        )
                    }

                    val lineCost = requiredQuantity * ingredientDoc.costPerUnit
                    estimatedCost += lineCost
                    usageEntries.add(
                        IngredientUsageDocument(
                            ingredientId = ingredientDoc.id,
                            ingredientName = ingredientDoc.name,
                            requiredQuantity = requiredQuantity,
                            unit = recipeItem.unit.ifBlank { ingredientDoc.unit },
                            costPerUnit = ingredientDoc.costPerUnit,
                            totalCost = lineCost
                        )
                    )

                    transaction.update(
                        ingredientRef,
                        mapOf(
                            IngredientFirestoreConstants.FIELD_CURRENT_STOCK to
                                ingredientDoc.currentStock - requiredQuantity,
                            IngredientFirestoreConstants.FIELD_UPDATED_AT to now
                        )
                    )
                }

                val productRef = firestore
                    .collection(ProductFirestoreConstants.PRODUCTS_COLLECTION)
                    .document(request.productId)
                val productSnapshot = transaction.get(productRef)
                val productDoc = ProductDocument.fromSnapshot(productSnapshot)
                    ?: throw ProductionException("Product not found.")
                if (productDoc.ownerId != ownerId) {
                    throw ProductionException("You do not have access to this product.")
                }
                transaction.update(
                    productRef,
                    mapOf(
                        ProductFirestoreConstants.FIELD_FINISHED_STOCK to
                            productDoc.finishedStock + request.quantityProduced,
                        ProductFirestoreConstants.FIELD_UPDATED_AT to now
                    )
                )

                val batchDocument = ProductionBatchDocument(
                    batchId = batchId,
                    ownerId = ownerId,
                    productId = request.productId,
                    productName = request.productName,
                    recipeId = request.recipeId,
                    quantityProduced = request.quantityProduced,
                    status = ProductionStatus.COMPLETED.name,
                    estimatedCost = estimatedCost,
                    createdAt = now,
                    completedAt = now,
                    createdBy = request.createdBy,
                    ingredientUsage = usageEntries
                )
                transaction.set(batchRef, batchDocument.toFirestoreMap())
            }.await()

            getBatch(batchId).getOrThrow()
        }.mapProductionError()
    }

    private fun computeDashboardStats(batches: List<ProductionBatch>): ProductionDashboardStats {
        val startOfDay = startOfTodayMillis()
        val todayBatches = batches.filter { it.createdAt >= startOfDay }
        return ProductionDashboardStats(
            todayCompleted = todayBatches.count { it.status == ProductionStatus.COMPLETED },
            todayPending = todayBatches.count { it.status == ProductionStatus.PENDING },
            todayBatchCount = todayBatches.size
        )
    }

    private fun startOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun verifyOwnership(ownerId: String) {
        val currentOwner = firebaseAuth.currentUser?.uid
        if (currentOwner == null || currentOwner != ownerId) {
            throw ProductionException("You do not have access to this production batch.")
        }
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.2f", value)

    private fun mapRecipeError(throwable: Throwable): ProductionException =
        ProductionException(throwable.message ?: "Unable to load recipe.")

    private fun <T> Result<T>.mapProductionError(): Result<T> = this
        .recoverCatching { throw mapException(it) }

    private fun mapException(throwable: Throwable): ProductionException = when (throwable) {
        is ProductionException -> throwable
        is FirebaseFirestoreException -> ProductionException(
            message = when (throwable.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Unable to save production. Check Firestore permissions."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No internet connection. Check your network and try again."
                else -> throwable.localizedMessage ?: "Production failed. Please try again."
            },
            cause = throwable
        )
        else -> ProductionException(
            message = throwable.localizedMessage ?: "Production failed. Please try again.",
            cause = throwable
        )
    }

    private fun mapFirestoreException(exception: FirebaseFirestoreException): ProductionException =
        ProductionException(
            message = exception.localizedMessage ?: "Failed to load production history.",
            cause = exception
        )
}

class ProductionException(message: String, cause: Throwable? = null) : Exception(message, cause)
