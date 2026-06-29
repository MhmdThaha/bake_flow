package com.bakeflow.app.inventory.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.Ingredient
import com.bakeflow.app.common.BakeFlowPreferences
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.domain.repository.PurchaseRepository
import com.bakeflow.app.inventory.IngredientListViewModel
import com.bakeflow.app.purchase.PurchaseFormViewModel
import com.bakeflow.app.purchase.PurchaseFormViewModelFactory
import com.bakeflow.app.purchase.PurchaseListViewModel
import com.bakeflow.app.purchase.ui.PurchaseListContent
import com.bakeflow.app.purchase.ui.PurchaseReceiveBottomSheet
import com.bakeflow.app.ui.components.BakeFlowSearchBar
import com.bakeflow.app.ui.components.EmptyState
import com.bakeflow.app.ui.components.ErrorState
import com.bakeflow.app.ui.components.LoadingState
import com.bakeflow.app.ui.components.SpreadsheetCellText
import com.bakeflow.app.ui.components.SpreadsheetColumn
import com.bakeflow.app.ui.components.SpreadsheetTable
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

private val ingredientColumns = listOf(
    SpreadsheetColumn("Ingredient", weight = 1.3f, minWidth = 110.dp),
    SpreadsheetColumn("Stock", weight = 0.9f, minWidth = 72.dp),
    SpreadsheetColumn("Unit", weight = 0.7f, minWidth = 64.dp),
    SpreadsheetColumn("Cost", weight = 0.8f, minWidth = 72.dp),
    SpreadsheetColumn("Low @", weight = 0.8f, minWidth = 72.dp)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientListScreen(
    viewModel: IngredientListViewModel,
    purchaseListViewModel: PurchaseListViewModel,
    purchaseRepository: PurchaseRepository,
    ingredientRepository: IngredientRepository,
    preferences: BakeFlowPreferences,
    onPurchaseClick: (String) -> Unit,
    onNavigateToAdjustments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val purchaseUiState by purchaseListViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var sheetIngredientId by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showReceiveSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(purchaseUiState.snackbarMessage) {
        purchaseUiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            purchaseListViewModel.clearSnackbar()
        }
    }

    if (showAddSheet) {
        IngredientEditBottomSheet(
            ingredientId = null,
            ingredientRepository = ingredientRepository,
            onDismiss = { showAddSheet = false },
            onSaved = { showAddSheet = false },
            onDeleted = { showAddSheet = false }
        )
    }
    sheetIngredientId?.let { id ->
        IngredientEditBottomSheet(
            ingredientId = id,
            ingredientRepository = ingredientRepository,
            onDismiss = { sheetIngredientId = null },
            onSaved = { sheetIngredientId = null },
            onDeleted = { sheetIngredientId = null }
        )
    }
    if (showReceiveSheet) {
        val purchaseFormViewModel: PurchaseFormViewModel = viewModel(
            key = "purchase_receive_sheet",
            factory = PurchaseFormViewModelFactory(
                purchaseRepository,
                ingredientRepository,
                FirebaseAuth.getInstance(),
                preferences
            )
        )
        PurchaseReceiveBottomSheet(
            viewModel = purchaseFormViewModel,
            onDismiss = { showReceiveSheet = false },
            onSaved = {
                purchaseListViewModel.showSnackbar("Stock received — inventory updated")
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showReceiveSheet = true },
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "Receive stock"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = "Inventory",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Stock") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Purchases") })
                Tab(selected = selectedTab == 2, onClick = { onNavigateToAdjustments() }, text = { Text("Adjustments") })
            }
            when (selectedTab) {
                0 -> IngredientStockTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    onRowClick = { sheetIngredientId = it.id },
                    onAddIngredient = { showAddSheet = true }
                )
                1 -> PurchaseListContent(
                    uiState = purchaseUiState,
                    viewModel = purchaseListViewModel,
                    onPurchaseClick = onPurchaseClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun IngredientStockTab(
    uiState: com.bakeflow.app.inventory.IngredientListUiState,
    viewModel: IngredientListViewModel,
    onRowClick: (Ingredient) -> Unit,
    onAddIngredient: () -> Unit
) {
    BakeFlowSearchBar(
        value = uiState.searchQuery,
        onValueChange = viewModel::onSearchQueryChange,
        placeholder = "Search ingredients…",
        modifier = Modifier.padding(vertical = 12.dp)
    )
    when (val state = uiState.ingredientsState) {
        UiState.Loading -> LoadingState(message = "Loading stock…")
        is UiState.Error -> Column {
            ErrorState(message = state.message)
            TextButton(onClick = viewModel::retryLoad) { Text("Try Again") }
        }
        UiState.Empty -> Column {
            EmptyState(
                title = "No ingredients yet",
                message = "Receive stock or add ingredients to start tracking.",
                modifier = Modifier.fillMaxSize()
            )
            TextButton(onClick = onAddIngredient) { Text("Add ingredient") }
        }
        is UiState.Success -> {
            if (uiState.filteredIngredients.isEmpty()) {
                EmptyState(title = "No matches", message = "Try another search.", modifier = Modifier.fillMaxSize())
            } else {
                val lowStockTint = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                SpreadsheetTable(
                    columns = ingredientColumns,
                    rows = uiState.filteredIngredients,
                    rowKey = { it.id },
                    onRowClick = onRowClick,
                    rowBackground = { ingredient ->
                        if (ingredient.isLowStock) lowStockTint else Color.Transparent
                    },
                    modifier = Modifier.fillMaxSize()
                ) { columnIndex, ingredient ->
                    when (columnIndex) {
                        0 -> SpreadsheetCellText(
                            text = ingredient.name,
                            emphasized = true,
                            color = if (ingredient.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        1 -> SpreadsheetCellText(formatNumber(ingredient.currentStock))
                        2 -> SpreadsheetCellText(ingredient.unit.displayName)
                        3 -> SpreadsheetCellText(formatCurrency(ingredient.costPerUnit))
                        else -> SpreadsheetCellText(formatNumber(ingredient.reorderLevel))
                    }
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String =
    String.format(Locale.getDefault(), "$%.2f", value)

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.1f", value)
