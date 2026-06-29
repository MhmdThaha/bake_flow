package com.bakeflow.app.production.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.production.ProductionListViewModel
import com.bakeflow.app.ui.components.LargeActionButton
import com.bakeflow.app.ui.components.LoadingState
import com.bakeflow.app.ui.components.MetricCard

@Composable
fun ProductionDashboardScreen(
    viewModel: ProductionListViewModel,
    onStartProduction: () -> Unit,
    onViewHistory: () -> Unit,
    onManageRecipes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Production",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            when {
                uiState.isLoading -> LoadingState(message = "Loading production stats…")
                uiState.errorMessage != null -> Column {
                    Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::retryLoad) { Text("Try Again") }
                }
                else -> ProductionStatsCards(stats = uiState.stats)
            }
        }
        item {
            LargeActionButton(
                text = "Start Production",
                onClick = onStartProduction,
                icon = Icons.Default.PlayArrow
            )
        }
        item {
            LargeActionButton(
                text = "Production History",
                onClick = onViewHistory,
                icon = Icons.Default.History,
                outlined = true
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recipes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Manage product recipes used for automatic ingredient calculation.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    TextButton(onClick = onManageRecipes) {
                        Text("Open Recipes")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductionStatsCards(stats: com.bakeflow.app.production.ProductionDashboardStatsUi) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Today's Production",
                value = stats.todayBatchCount.toString(),
                subtitle = "batches",
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Completed",
                value = stats.todayCompleted.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        MetricCard(
            title = "Pending",
            value = stats.todayPending.toString(),
            subtitle = "awaiting completion"
        )
    }
}
