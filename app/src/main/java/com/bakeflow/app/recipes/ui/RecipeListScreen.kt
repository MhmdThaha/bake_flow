package com.bakeflow.app.recipes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.common.UiState
import com.bakeflow.app.recipes.RecipeListEntry
import com.bakeflow.app.recipes.RecipeListViewModel
import com.bakeflow.app.ui.components.EmptyState
import com.bakeflow.app.ui.components.ErrorState
import com.bakeflow.app.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    viewModel: RecipeListViewModel,
    onNavigateBack: () -> Unit,
    onCreateRecipe: () -> Unit,
    onViewRecipe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var recipeToDelete by remember { mutableStateOf<RecipeListEntry?>(null) }

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    recipeToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            title = { Text("Delete Recipe") },
            text = { Text("Delete recipe for \"${entry.productName}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecipe(entry.recipe.id)
                        recipeToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Recipes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRecipe) {
                Icon(Icons.Default.Add, contentDescription = "Create recipe")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by product name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            when (val state = uiState.recipesState) {
                UiState.Loading -> LoadingState(message = "Loading recipes…")

                is UiState.Error -> Column(modifier = Modifier.fillMaxSize()) {
                    ErrorState(message = state.message, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = viewModel::retryLoad,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) { Text("Try Again") }
                }

                UiState.Empty -> EmptyState(
                    title = "No Recipes Yet",
                    message = "Tap + to create a bill of materials for a product.",
                    modifier = Modifier.fillMaxSize()
                )

                is UiState.Success -> {
                    if (uiState.filteredRecipes.isEmpty()) {
                        EmptyState(
                            title = "No Results",
                            message = "No recipes match \"${uiState.searchQuery}\".",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                        ) {
                            items(uiState.filteredRecipes, key = { it.recipe.id }) { entry ->
                                RecipeListItem(
                                    entry = entry,
                                    isDeleting = uiState.deleteInProgressId == entry.recipe.id,
                                    onClick = { onViewRecipe(entry.recipe.id) },
                                    onDelete = { recipeToDelete = entry }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeListItem(
    entry: RecipeListEntry,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${entry.itemCount} ingredient${if (entry.itemCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (isDeleting) {
                CircularProgressIndicator()
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete recipe",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
