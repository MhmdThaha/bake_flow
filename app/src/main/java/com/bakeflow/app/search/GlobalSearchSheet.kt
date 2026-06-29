package com.bakeflow.app.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bakeflow.app.common.AppContainer
import com.bakeflow.app.ui.components.BakeFlowSearchBar
import com.bakeflow.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchSheet(
    appContainer: AppContainer,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val viewModel: GlobalSearchViewModel = viewModel(
        factory = GlobalSearchViewModelFactory(
            appContainer.productRepository,
            appContainer.ingredientRepository,
            appContainer.saleRepository,
            appContainer.purchaseRepository
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Search BakeFlow",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            BakeFlowSearchBar(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Try \"Bread\", \"Flour\", or a supplier…"
            )
            when {
                uiState.query.isBlank() -> EmptyState(
                    title = "Search everything",
                    message = "Find products, ingredients, sales, and purchases in one place.",
                    modifier = Modifier.padding(top = 24.dp)
                )
                uiState.results.isEmpty() -> EmptyState(
                    title = "No results",
                    message = "Try a product name, ingredient, or supplier.",
                    modifier = Modifier.padding(top = 24.dp)
                )
                else -> LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(uiState.results, key = { "${it.type}-${it.id}" }) { result ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(result.navigateRoute) }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(result.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${result.type.label} · ${result.subtitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
