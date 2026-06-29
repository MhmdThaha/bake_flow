package com.bakeflow.app.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.domain.repository.SaleRepository
import com.bakeflow.app.domain.repository.StockAdjustmentRepository
import com.bakeflow.app.domain.repository.WasteRepository
import com.bakeflow.app.inventory.IngredientListViewModel
import com.bakeflow.app.inventory.IngredientListViewModelFactory
import com.bakeflow.app.sales.SalesListViewModel
import com.bakeflow.app.sales.SalesListViewModelFactory
import com.bakeflow.app.stockadjustment.StockAdjustmentListViewModel
import com.bakeflow.app.stockadjustment.StockAdjustmentListViewModelFactory
import com.bakeflow.app.waste.WasteListViewModel
import com.bakeflow.app.waste.WasteListViewModelFactory
import com.bakeflow.app.ui.components.MetricCard
import java.util.Locale

@Composable
fun ReportsScreen(
    ingredientRepository: IngredientRepository,
    saleRepository: SaleRepository,
    wasteRepository: WasteRepository,
    stockAdjustmentRepository: StockAdjustmentRepository,
    modifier: Modifier = Modifier
) {
    val salesVm: SalesListViewModel = viewModel(
        factory = SalesListViewModelFactory(saleRepository)
    )
    val wasteVm: WasteListViewModel = viewModel(
        factory = WasteListViewModelFactory(wasteRepository)
    )
    val adjustmentVm: StockAdjustmentListViewModel = viewModel(
        factory = StockAdjustmentListViewModelFactory(stockAdjustmentRepository)
    )
    val ingredientVm: IngredientListViewModel = viewModel(
        factory = IngredientListViewModelFactory(ingredientRepository)
    )
    val salesState by salesVm.uiState.collectAsStateWithLifecycle()
    val wasteState by wasteVm.uiState.collectAsStateWithLifecycle()
    val adjustmentState by adjustmentVm.uiState.collectAsStateWithLifecycle()
    val ingredientState by ingredientVm.uiState.collectAsStateWithLifecycle()

    val ingredients = (ingredientState.ingredientsState as? UiState.Success)?.let {
        ingredientState.filteredIngredients
    }.orEmpty()
    val inventoryValue = ingredients.sumOf { it.currentStock * it.costPerUnit }
    val lowStock = ingredients.count { it.isLowStock }
    val summary = salesState.summary
    val wasteSummary = wasteState.summary
    val adjustmentSummary = adjustmentState.summary

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Reports",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Today's Sales",
                value = summary.todayCount.toString(),
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Today's Revenue",
                value = formatMoney(summary.todayRevenue),
                icon = Icons.Default.TrendingUp,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Today's Waste Qty",
                value = formatQty(wasteSummary.todayQuantity),
                subtitle = "${wasteSummary.todayCount} entries",
                icon = Icons.Default.Delete,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Today's Waste Value",
                value = formatMoney(wasteSummary.todayEstimatedLoss),
                icon = Icons.Default.Delete,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Today's Adjustments",
                value = adjustmentSummary.todayCount.toString(),
                subtitle = "Net ${formatSignedQty(adjustmentSummary.todayDifferenceTotal)}",
                icon = Icons.Default.Tune,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Best seller today",
                value = summary.bestSellingProductName,
                subtitle = if (summary.bestSellingQuantity > 0) {
                    "${formatQty(summary.bestSellingQuantity)} sold"
                } else {
                    null
                },
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Low stock",
                value = lowStock.toString(),
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Inventory value",
                value = formatMoney(inventoryValue),
                icon = Icons.Default.Inventory,
                subtitle = "Stock × cost",
                modifier = Modifier.weight(1f)
            )
        }
        Text("Products sold today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (summary.productsSoldToday.isEmpty()) {
            Text(
                "No sales recorded today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            summary.productsSoldToday.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(item.productName, fontWeight = FontWeight.Medium)
                        Text("${formatQty(item.quantitySold)} units")
                    }
                    Text(formatMoney(item.revenue), color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider()
            }
        }
        Text("Recent waste", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (wasteSummary.recentWaste.isEmpty()) {
            Text(
                "No waste recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            wasteSummary.recentWaste.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(entry.productName, fontWeight = FontWeight.Medium)
                        Text("${formatQty(entry.quantity)} · ${entry.reason.displayName}")
                    }
                    Text(
                        formatMoney(entry.estimatedLoss),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                HorizontalDivider()
            }
        }
        Text("Recent adjustments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (adjustmentSummary.recentAdjustments.isEmpty()) {
            Text(
                "No stock adjustments recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            adjustmentSummary.recentAdjustments.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(entry.itemName, fontWeight = FontWeight.Medium)
                        Text(
                            "${entry.itemType.displayName} · ${formatSignedQty(entry.difference)} · ${entry.adjustmentReason.displayName}"
                        )
                    }
                    Text(
                        formatQty(entry.adjustedQuantity),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

private fun formatMoney(value: Double): String =
    String.format(Locale.getDefault(), "$%.2f", value)

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
