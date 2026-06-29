package com.bakeflow.app.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.common.AppContainer
import com.bakeflow.app.search.GlobalSearchSheet
import com.bakeflow.app.setup.SetupOnboardingSheet
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import com.bakeflow.app.ui.components.LargeActionButton
import com.bakeflow.app.ui.components.LoadingState
import com.bakeflow.app.ui.components.MetricCard
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    appContainer: AppContainer,
    viewModel: DashboardViewModel,
    onNavigateToSales: () -> Unit = {},
    onNavigateToProduction: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToWaste: () -> Unit = {},
    onNavigateToProducts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val needsSetup = remember(userId) {
        userId != null && !appContainer.preferences.isSetupComplete(userId)
    }
    var showSetupOverlay by remember { mutableStateOf(false) }
    LaunchedEffect(needsSetup) {
        if (needsSetup) {
            delay(400)
            showSetupOverlay = true
        }
    }
    if (showSetupOverlay) {
        SetupOnboardingSheet(appContainer = appContainer)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGlobalSearch by remember { mutableStateOf(false) }

    if (showGlobalSearch) {
        GlobalSearchSheet(
            appContainer = appContainer,
            onDismiss = { showGlobalSearch = false },
            onNavigate = { route ->
                showGlobalSearch = false
                when (route) {
                    "sales" -> onNavigateToSales()
                    "inventory" -> onNavigateToInventory()
                    "products" -> onNavigateToProducts()
                    "production" -> onNavigateToProduction()
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val greeting = uiState.bakeryName.ifBlank { "BakeFlow" }
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "What should you do next?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            OutlinedSearchPrompt(
                onClick = { showGlobalSearch = true }
            )
        }
        if (uiState.isLoading) {
            item { LoadingState(message = "Loading today's overview…") }
        } else {
            if (uiState.lowStockCount > 0) {
                item {
                    AttentionCard(
                        title = "${uiState.lowStockCount} ingredients low on stock",
                        items = uiState.lowStockNames,
                        actionLabel = "Receive stock",
                        onAction = onNavigateToInventory
                    )
                }
            }
            item {
                PriorityMetricsGrid(uiState = uiState)
            }
            if (uiState.todayWasteCount > 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Today's Waste",
                            value = formatQty(uiState.todayWasteQuantity),
                            subtitle = "${uiState.todayWasteCount} entries · ${formatMoney(uiState.todayWasteLoss)} loss",
                            icon = Icons.Default.Delete,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            if (uiState.todayAdjustmentCount > 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Today's Adjustments",
                            value = uiState.todayAdjustmentCount.toString(),
                            subtitle = "Net change ${formatSignedQty(uiState.todayAdjustmentNetChange)}",
                            icon = Icons.Default.Tune,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = "Quick actions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        item {
            LargeActionButton(
                text = "New Sale",
                onClick = onNavigateToSales,
                icon = Icons.Default.AddShoppingCart
            )
        }
        item {
            LargeActionButton(
                text = "Start Production",
                onClick = onNavigateToProduction,
                icon = Icons.Default.PlayArrow
            )
        }
        item {
            LargeActionButton(
                text = "Receive Stock",
                onClick = onNavigateToInventory,
                icon = Icons.Default.LocalShipping,
                outlined = true
            )
        }
        item {
            LargeActionButton(
                text = "Record Waste",
                onClick = onNavigateToWaste,
                icon = Icons.Default.Delete,
                outlined = true
            )
        }
        if (!uiState.isLoading && uiState.bestSellingProduct != "—") {
            item {
                Text(
                    text = "Best seller today: ${uiState.bestSellingProduct}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PriorityMetricsGrid(uiState: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Low Stock",
                value = uiState.lowStockCount.toString(),
                subtitle = if (uiState.lowStockCount > 0) "needs attention" else "all good",
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Today's Production",
                value = uiState.todayProductionCount.toString(),
                subtitle = "batches",
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Today's Sales",
                value = uiState.todaySalesCount.toString(),
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Today's Revenue",
                value = formatMoney(uiState.todayRevenue),
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AttentionCard(
    title: String,
    items: List<String>,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            Text(
                text = actionLabel,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onAction).padding(top = 4.dp)
            )
        }
    }
}

private fun formatMoney(value: Double): String =
    String.format(Locale.getDefault(), "$%.0f", value)

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.1f", value)

private fun formatSignedQty(value: Double): String {
    val formatted = formatQty(kotlin.math.abs(value))
    return when {
        value > 0 -> "+$formatted"
        value < 0 -> "-$formatted"
        else -> formatted
    }
}

@Composable
private fun OutlinedSearchPrompt(onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Search products, sales, stock…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
