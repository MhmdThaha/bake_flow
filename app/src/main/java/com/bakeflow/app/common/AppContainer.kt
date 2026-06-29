package com.bakeflow.app.common

import android.content.Context
import com.bakeflow.app.auth.SessionManager
import com.bakeflow.app.common.BakeFlowPreferences
import com.bakeflow.app.data.network.NetworkMonitor
import com.bakeflow.app.data.repository.AuthRepositoryImpl
import com.bakeflow.app.data.repository.IngredientRepositoryImpl
import com.bakeflow.app.data.repository.ProductRepositoryImpl
import com.bakeflow.app.data.repository.ProductionRepositoryImpl
import com.bakeflow.app.data.repository.PurchaseRepositoryImpl
import com.bakeflow.app.data.repository.RecipeRepositoryImpl
import com.bakeflow.app.data.repository.SaleRepositoryImpl
import com.bakeflow.app.data.repository.StockAdjustmentRepositoryImpl
import com.bakeflow.app.data.repository.WasteRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Manual dependency container. Provides shared app-wide dependencies without extra DI libraries.
 */
class AppContainer(context: Context) {

    val networkMonitor: NetworkMonitor = NetworkMonitor(context.applicationContext)

    val preferences: BakeFlowPreferences = BakeFlowPreferences(context.applicationContext)

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    val authRepository: AuthRepositoryImpl = AuthRepositoryImpl(
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        networkMonitor = networkMonitor
    )

    val productRepository: ProductRepositoryImpl = ProductRepositoryImpl(
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        networkMonitor = networkMonitor
    )

    val ingredientRepository: IngredientRepositoryImpl = IngredientRepositoryImpl(
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        networkMonitor = networkMonitor
    )

    val recipeRepository: RecipeRepositoryImpl = RecipeRepositoryImpl(
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        networkMonitor = networkMonitor
    )

    val productionRepository: ProductionRepositoryImpl = ProductionRepositoryImpl(
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        networkMonitor = networkMonitor,
        recipeRepository = recipeRepository
    )

    val purchaseRepository: PurchaseRepositoryImpl = PurchaseRepositoryImpl(
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        networkMonitor = networkMonitor
    )

    val saleRepository: SaleRepositoryImpl = SaleRepositoryImpl(
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        networkMonitor = networkMonitor
    )

    val wasteRepository: WasteRepositoryImpl = WasteRepositoryImpl(
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        networkMonitor = networkMonitor
    )

    val stockAdjustmentRepository: StockAdjustmentRepositoryImpl = StockAdjustmentRepositoryImpl(
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        networkMonitor = networkMonitor
    )

    val sessionManager: SessionManager = SessionManager(authRepository)
}
