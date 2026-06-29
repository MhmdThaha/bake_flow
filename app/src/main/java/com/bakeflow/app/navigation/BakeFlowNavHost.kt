package com.bakeflow.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bakeflow.app.auth.AuthSessionState
import com.bakeflow.app.auth.AuthViewModel
import com.bakeflow.app.auth.AuthViewModelFactory
import com.bakeflow.app.auth.ui.ForgotPasswordScreen
import com.bakeflow.app.auth.ui.LoginScreen
import com.bakeflow.app.auth.ui.RegisterScreen
import com.bakeflow.app.common.AppContainer
import com.bakeflow.app.dashboard.DashboardScreen
import com.bakeflow.app.dashboard.DashboardViewModel
import com.bakeflow.app.dashboard.DashboardViewModelFactory
import com.bakeflow.app.production.ProductionDetailViewModel
import com.bakeflow.app.production.ProductionDetailViewModelFactory
import com.bakeflow.app.production.ProductionHistoryViewModel
import com.bakeflow.app.production.ProductionHistoryViewModelFactory
import com.bakeflow.app.production.ProductionListViewModel
import com.bakeflow.app.production.ProductionListViewModelFactory
import com.bakeflow.app.production.ProductionWizardViewModel
import com.bakeflow.app.production.ProductionWizardViewModelFactory
import com.bakeflow.app.production.ui.BatchDetailScreen
import com.bakeflow.app.production.ui.ProductionDashboardScreen
import com.bakeflow.app.production.ui.ProductionHistoryScreen
import com.bakeflow.app.production.ui.ProductionWizardScreen
import com.bakeflow.app.reports.ReportsScreen
import com.bakeflow.app.sales.SaleFormViewModel
import com.bakeflow.app.sales.SaleFormViewModelFactory
import com.bakeflow.app.sales.SalesListViewModel
import com.bakeflow.app.sales.SalesListViewModelFactory
import com.bakeflow.app.sales.ui.SalesScreen
import com.bakeflow.app.stockadjustment.StockAdjustmentDetailViewModel
import com.bakeflow.app.stockadjustment.StockAdjustmentDetailViewModelFactory
import com.bakeflow.app.stockadjustment.StockAdjustmentFormViewModel
import com.bakeflow.app.stockadjustment.StockAdjustmentFormViewModelFactory
import com.bakeflow.app.stockadjustment.StockAdjustmentListViewModel
import com.bakeflow.app.stockadjustment.StockAdjustmentListViewModelFactory
import com.bakeflow.app.stockadjustment.ui.StockAdjustmentDetailScreen
import com.bakeflow.app.stockadjustment.ui.StockAdjustmentScreen
import com.bakeflow.app.waste.WasteDetailViewModel
import com.bakeflow.app.waste.WasteDetailViewModelFactory
import com.bakeflow.app.waste.WasteFormViewModel
import com.bakeflow.app.waste.WasteFormViewModelFactory
import com.bakeflow.app.waste.WasteListViewModel
import com.bakeflow.app.waste.WasteListViewModelFactory
import com.bakeflow.app.waste.ui.WasteDetailScreen
import com.bakeflow.app.waste.ui.WasteScreen
import com.bakeflow.app.inventory.IngredientFormViewModel
import com.bakeflow.app.inventory.IngredientFormViewModelFactory
import com.bakeflow.app.inventory.IngredientListViewModel
import com.bakeflow.app.inventory.IngredientListViewModelFactory
import com.bakeflow.app.inventory.ui.IngredientFormScreen
import com.bakeflow.app.inventory.ui.IngredientListScreen
import com.bakeflow.app.purchase.PurchaseDetailViewModel
import com.bakeflow.app.purchase.PurchaseDetailViewModelFactory
import com.bakeflow.app.purchase.PurchaseFormViewModel
import com.bakeflow.app.purchase.PurchaseFormViewModelFactory
import com.bakeflow.app.purchase.PurchaseListViewModel
import com.bakeflow.app.purchase.PurchaseListViewModelFactory
import com.bakeflow.app.purchase.ui.PurchaseDetailScreen
import com.bakeflow.app.purchase.ui.PurchaseFormScreen
import com.bakeflow.app.products.ProductFormViewModel
import com.bakeflow.app.products.ProductFormViewModelFactory
import com.bakeflow.app.products.ProductListViewModel
import com.bakeflow.app.products.ProductListViewModelFactory
import com.bakeflow.app.products.ui.ProductFormScreen
import com.bakeflow.app.products.ui.ProductListScreen
import com.bakeflow.app.recipes.RecipeDetailViewModel
import com.bakeflow.app.recipes.RecipeDetailViewModelFactory
import com.bakeflow.app.recipes.RecipeListViewModel
import com.bakeflow.app.recipes.RecipeListViewModelFactory
import com.bakeflow.app.recipes.RecipeWizardViewModel
import com.bakeflow.app.recipes.RecipeWizardViewModelFactory
import com.bakeflow.app.recipes.ui.RecipeDetailScreen
import com.bakeflow.app.recipes.ui.RecipeListScreen
import com.bakeflow.app.recipes.ui.RecipeWizardScreen
import com.bakeflow.app.ui.components.LoadingState
import kotlinx.coroutines.launch

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "Home", Icons.Default.Home),
    BottomNavItem(Routes.PRODUCTS, "Products", Icons.Default.BakeryDining),
    BottomNavItem(Routes.INVENTORY, "Inventory", Icons.Default.Inventory),
    BottomNavItem(Routes.PRODUCTION, "Production", Icons.Default.ShoppingBag),
    BottomNavItem(Routes.SALES, "Sales", Icons.Default.PointOfSale),
    BottomNavItem(Routes.WASTE, "Waste", Icons.Default.Delete),
    BottomNavItem(Routes.REPORTS, "Reports", Icons.Default.Assessment)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BakeFlowNavHost(appContainer: AppContainer) {
    val sessionState by appContainer.sessionManager.sessionState.collectAsStateWithLifecycle()

    when (sessionState) {
        AuthSessionState.Loading -> {
            LoadingState(message = "Checking your session…")
        }

        else -> {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route
            val scope = rememberCoroutineScope()

            val isAuthenticated = sessionState is AuthSessionState.Authenticated
            val showBottomBar = isAuthenticated && currentRoute in Routes.mainDestinations

            LaunchedEffect(sessionState) {
                when (sessionState) {
                    AuthSessionState.Loading -> Unit
                    AuthSessionState.Unauthenticated -> {
                        navController.navigate(Routes.AUTH_LOGIN) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    is AuthSessionState.Authenticated -> {
                        val route = navController.currentBackStackEntry?.destination?.route
                        if (route == null || route in Routes.authDestinations || route == Routes.AUTH_GRAPH) {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.AUTH_GRAPH) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }

            BackHandler(
                enabled = isAuthenticated && !Routes.isMainTabRoute(currentRoute)
            ) {
                navController.popBackStack()
            }

            Scaffold(
                topBar = {
                    if (isAuthenticated) {
                        TopAppBar(
                            title = { Text("BakeFlow") },
                            actions = {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            appContainer.authRepository.signOut()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign out"
                                    )
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            bottomNavItems.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == item.route
                                } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.AUTH_GRAPH,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    navigation(
                        route = Routes.AUTH_GRAPH,
                        startDestination = Routes.AUTH_LOGIN
                    ) {
                        composable(Routes.AUTH_LOGIN) {
                            val parentEntry = remember(navController) {
                                navController.getBackStackEntry(Routes.AUTH_GRAPH)
                            }
                            val authViewModel: AuthViewModel = viewModel(
                                parentEntry,
                                factory = AuthViewModelFactory(
                                    appContainer.authRepository,
                                    appContainer.networkMonitor
                                )
                            )
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToRegister = {
                                    navController.navigate(Routes.AUTH_REGISTER)
                                },
                                onNavigateToForgotPassword = {
                                    navController.navigate(Routes.AUTH_FORGOT_PASSWORD)
                                }
                            )
                        }
                        composable(Routes.AUTH_REGISTER) {
                            val parentEntry = remember(navController) {
                                navController.getBackStackEntry(Routes.AUTH_GRAPH)
                            }
                            val authViewModel: AuthViewModel = viewModel(
                                parentEntry,
                                factory = AuthViewModelFactory(
                                    appContainer.authRepository,
                                    appContainer.networkMonitor
                                )
                            )
                            RegisterScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = {
                                    navController.popBackStack(Routes.AUTH_LOGIN, inclusive = false)
                                }
                            )
                        }
                        composable(Routes.AUTH_FORGOT_PASSWORD) {
                            val parentEntry = remember(navController) {
                                navController.getBackStackEntry(Routes.AUTH_GRAPH)
                            }
                            val authViewModel: AuthViewModel = viewModel(
                                parentEntry,
                                factory = AuthViewModelFactory(
                                    appContainer.authRepository,
                                    appContainer.networkMonitor
                                )
                            )
                            ForgotPasswordScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = {
                                    navController.popBackStack(Routes.AUTH_LOGIN, inclusive = false)
                                }
                            )
                        }
                    }

                    composable(Routes.DASHBOARD) {
                        val dashboardViewModel: DashboardViewModel = viewModel(
                            key = "dashboard",
                            factory = DashboardViewModelFactory(
                                appContainer.saleRepository,
                                appContainer.productionRepository,
                                appContainer.ingredientRepository,
                                appContainer.wasteRepository,
                                appContainer.stockAdjustmentRepository,
                                appContainer.preferences
                            )
                        )
                        DashboardScreen(
                            appContainer = appContainer,
                            viewModel = dashboardViewModel,
                            onNavigateToSales = {
                                navController.navigate(Routes.SALES) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToProduction = {
                                navController.navigate(Routes.PRODUCTION) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToInventory = {
                                navController.navigate(Routes.INVENTORY) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToWaste = {
                                navController.navigate(Routes.WASTE) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToProducts = {
                                navController.navigate(Routes.PRODUCTS) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(Routes.PRODUCTS) {
                        val viewModel: ProductListViewModel = viewModel(
                            factory = ProductListViewModelFactory(appContainer.productRepository)
                        )
                        ProductListScreen(
                            viewModel = viewModel,
                            productRepository = appContainer.productRepository
                        )
                    }
                    composable(Routes.PRODUCTS_ADD) {
                        val viewModel: ProductFormViewModel = viewModel(
                            factory = ProductFormViewModelFactory(
                                appContainer.productRepository,
                                productId = null
                            )
                        )
                        ProductFormScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Routes.PRODUCTS_EDIT,
                        arguments = listOf(
                            navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val productId = backStackEntry.arguments?.getString(Routes.ARG_PRODUCT_ID)
                        val viewModel: ProductFormViewModel = viewModel(
                            factory = ProductFormViewModelFactory(
                                appContainer.productRepository,
                                productId = productId
                            )
                        )
                        ProductFormScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.INVENTORY) {
                        val viewModel: IngredientListViewModel = viewModel(
                            factory = IngredientListViewModelFactory(appContainer.ingredientRepository)
                        )
                        val purchaseListViewModel: PurchaseListViewModel = viewModel(
                            factory = PurchaseListViewModelFactory(appContainer.purchaseRepository)
                        )
                        IngredientListScreen(
                            viewModel = viewModel,
                            purchaseListViewModel = purchaseListViewModel,
                            purchaseRepository = appContainer.purchaseRepository,
                            ingredientRepository = appContainer.ingredientRepository,
                            preferences = appContainer.preferences,
                            onPurchaseClick = { purchaseId ->
                                navController.navigate(Routes.purchaseDetailRoute(purchaseId))
                            },
                            onNavigateToAdjustments = {
                                navController.navigate(Routes.STOCK_ADJUSTMENTS)
                            }
                        )
                    }
                    composable(Routes.STOCK_ADJUSTMENTS) {
                        val listViewModel: StockAdjustmentListViewModel = viewModel(
                            factory = StockAdjustmentListViewModelFactory(
                                appContainer.stockAdjustmentRepository
                            )
                        )
                        val formViewModel: StockAdjustmentFormViewModel = viewModel(
                            key = "stock_adjustment_form",
                            factory = StockAdjustmentFormViewModelFactory(
                                appContainer.stockAdjustmentRepository,
                                appContainer.ingredientRepository,
                                appContainer.productRepository,
                                com.google.firebase.auth.FirebaseAuth.getInstance()
                            )
                        )
                        StockAdjustmentScreen(
                            listViewModel = listViewModel,
                            formViewModel = formViewModel,
                            onAdjustmentClick = { adjustmentId ->
                                navController.navigate(Routes.stockAdjustmentDetailRoute(adjustmentId))
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Routes.STOCK_ADJUSTMENT_DETAIL,
                        arguments = listOf(
                            navArgument(Routes.ARG_ADJUSTMENT_ID) { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val adjustmentId = backStackEntry.arguments?.getString(Routes.ARG_ADJUSTMENT_ID)
                            ?: return@composable
                        val viewModel: StockAdjustmentDetailViewModel = viewModel(
                            factory = StockAdjustmentDetailViewModelFactory(
                                appContainer.stockAdjustmentRepository,
                                adjustmentId
                            )
                        )
                        StockAdjustmentDetailScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.PURCHASE_RECEIVE) {
                        val viewModel: PurchaseFormViewModel = viewModel(
                            factory = PurchaseFormViewModelFactory(
                                appContainer.purchaseRepository,
                                appContainer.ingredientRepository,
                                com.google.firebase.auth.FirebaseAuth.getInstance(),
                                appContainer.preferences
                            )
                        )
                        PurchaseFormScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Routes.PURCHASE_DETAIL,
                        arguments = listOf(
                            navArgument(Routes.ARG_PURCHASE_ID) { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val purchaseId = backStackEntry.arguments?.getString(Routes.ARG_PURCHASE_ID)
                            ?: return@composable
                        val viewModel: PurchaseDetailViewModel = viewModel(
                            factory = PurchaseDetailViewModelFactory(
                                appContainer.purchaseRepository,
                                purchaseId
                            )
                        )
                        PurchaseDetailScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.INVENTORY_ADD) {
                        val viewModel: IngredientFormViewModel = viewModel(
                            factory = IngredientFormViewModelFactory(
                                appContainer.ingredientRepository,
                                ingredientId = null
                            )
                        )
                        IngredientFormScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Routes.INVENTORY_EDIT,
                        arguments = listOf(
                            navArgument(Routes.ARG_INGREDIENT_ID) { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val ingredientId = backStackEntry.arguments?.getString(Routes.ARG_INGREDIENT_ID)
                        val viewModel: IngredientFormViewModel = viewModel(
                            factory = IngredientFormViewModelFactory(
                                appContainer.ingredientRepository,
                                ingredientId = ingredientId
                            )
                        )
                        IngredientFormScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.PRODUCTION) {
                        val viewModel: ProductionListViewModel = viewModel(
                            factory = ProductionListViewModelFactory(appContainer.productionRepository)
                        )
                        ProductionDashboardScreen(
                            viewModel = viewModel,
                            onStartProduction = { navController.navigate(Routes.PRODUCTION_WIZARD) },
                            onViewHistory = { navController.navigate(Routes.PRODUCTION_HISTORY) },
                            onManageRecipes = { navController.navigate(Routes.PRODUCTION_RECIPES) }
                        )
                    }
                    composable(Routes.PRODUCTION_WIZARD) {
                        val viewModel: ProductionWizardViewModel = viewModel(
                            factory = ProductionWizardViewModelFactory(
                                appContainer.productionRepository,
                                appContainer.recipeRepository,
                                appContainer.productRepository,
                                appContainer.ingredientRepository,
                                appContainer.preferences
                            )
                        )
                        ProductionWizardScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.PRODUCTION_HISTORY) {
                        val viewModel: ProductionHistoryViewModel = viewModel(
                            factory = ProductionHistoryViewModelFactory(appContainer.productionRepository)
                        )
                        ProductionHistoryScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onBatchClick = { batchId ->
                                navController.navigate(Routes.productionBatchRoute(batchId))
                            }
                        )
                    }
                    composable(
                        route = Routes.PRODUCTION_BATCH,
                        arguments = listOf(
                            navArgument(Routes.ARG_BATCH_ID) { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val batchId = backStackEntry.arguments?.getString(Routes.ARG_BATCH_ID)
                            ?: return@composable
                        val viewModel: ProductionDetailViewModel = viewModel(
                            factory = ProductionDetailViewModelFactory(
                                appContainer.productionRepository,
                                batchId
                            )
                        )
                        BatchDetailScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.PRODUCTION_RECIPES) {
                        val viewModel: RecipeListViewModel = viewModel(
                            factory = RecipeListViewModelFactory(
                                appContainer.recipeRepository,
                                appContainer.productRepository
                            )
                        )
                        RecipeListScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onCreateRecipe = { navController.navigate(Routes.RECIPE_CREATE) },
                            onViewRecipe = { recipeId ->
                                navController.navigate(Routes.recipeViewRoute(recipeId))
                            }
                        )
                    }
                    composable(Routes.RECIPE_CREATE) {
                        val viewModel: RecipeWizardViewModel = viewModel(
                            factory = RecipeWizardViewModelFactory(
                                appContainer.recipeRepository,
                                appContainer.productRepository,
                                appContainer.ingredientRepository,
                                recipeId = null
                            )
                        )
                        RecipeWizardScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Routes.RECIPE_EDIT,
                        arguments = listOf(
                            navArgument(Routes.ARG_RECIPE_ID) { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val recipeId = backStackEntry.arguments?.getString(Routes.ARG_RECIPE_ID)
                        val viewModel: RecipeWizardViewModel = viewModel(
                            factory = RecipeWizardViewModelFactory(
                                appContainer.recipeRepository,
                                appContainer.productRepository,
                                appContainer.ingredientRepository,
                                recipeId = recipeId
                            )
                        )
                        RecipeWizardScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Routes.RECIPE_VIEW,
                        arguments = listOf(
                            navArgument(Routes.ARG_RECIPE_ID) { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val recipeId = backStackEntry.arguments?.getString(Routes.ARG_RECIPE_ID)
                            ?: return@composable
                        val viewModel: RecipeDetailViewModel = viewModel(
                            factory = RecipeDetailViewModelFactory(
                                appContainer.recipeRepository,
                                appContainer.productRepository,
                                appContainer.ingredientRepository,
                                recipeId = recipeId
                            )
                        )
                        RecipeDetailScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onEditRecipe = { id ->
                                navController.navigate(Routes.recipeEditRoute(id))
                            }
                        )
                    }
                    composable(Routes.SALES) {
                        val listViewModel: SalesListViewModel = viewModel(
                            factory = SalesListViewModelFactory(appContainer.saleRepository)
                        )
                        val formViewModel: SaleFormViewModel = viewModel(
                            key = "sale_form",
                            factory = SaleFormViewModelFactory(
                                appContainer.saleRepository,
                                appContainer.productRepository,
                                com.google.firebase.auth.FirebaseAuth.getInstance(),
                                appContainer.preferences
                            )
                        )
                        SalesScreen(
                            listViewModel = listViewModel,
                            formViewModel = formViewModel
                        )
                    }
                    composable(Routes.WASTE) {
                        val listViewModel: WasteListViewModel = viewModel(
                            factory = WasteListViewModelFactory(appContainer.wasteRepository)
                        )
                        val formViewModel: WasteFormViewModel = viewModel(
                            key = "waste_form",
                            factory = WasteFormViewModelFactory(
                                appContainer.wasteRepository,
                                appContainer.productRepository,
                                com.google.firebase.auth.FirebaseAuth.getInstance(),
                                appContainer.preferences
                            )
                        )
                        WasteScreen(
                            listViewModel = listViewModel,
                            formViewModel = formViewModel,
                            onWasteClick = { wasteId ->
                                navController.navigate(Routes.wasteDetailRoute(wasteId))
                            }
                        )
                    }
                    composable(
                        route = Routes.WASTE_DETAIL,
                        arguments = listOf(
                            navArgument(Routes.ARG_WASTE_ID) { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val wasteId = backStackEntry.arguments?.getString(Routes.ARG_WASTE_ID)
                            ?: return@composable
                        val viewModel: WasteDetailViewModel = viewModel(
                            factory = WasteDetailViewModelFactory(
                                appContainer.wasteRepository,
                                wasteId
                            )
                        )
                        WasteDetailScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.REPORTS) {
                        ReportsScreen(
                            ingredientRepository = appContainer.ingredientRepository,
                            saleRepository = appContainer.saleRepository,
                            wasteRepository = appContainer.wasteRepository,
                            stockAdjustmentRepository = appContainer.stockAdjustmentRepository
                        )
                    }
                }
            }
        }
    }
}

