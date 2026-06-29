package com.bakeflow.app.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bakeflow.app.common.AppContainer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private enum class SetupStep(val title: String) {
    WELCOME("Welcome"),
    BAKERY_NAME("Your bakery"),
    BAKERY_TYPE("What you bake"),
    GENERATE("Ready to go")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SetupOnboardingSheet(appContainer: AppContainer) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId == null) return

    var showSheet by remember { mutableStateOf(!appContainer.preferences.isSetupComplete(userId)) }
    if (!showSheet) return

    var step by remember { mutableIntStateOf(SetupStep.WELCOME.ordinal) }
    var bakeryName by remember { mutableStateOf(appContainer.preferences.getBakeryName()) }
    var selectedType by remember { mutableStateOf(BakeryType.BREAD_BAKERY) }
    var isSeeding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentStep = SetupStep.entries[step]
    val progress = (step + 1).toFloat() / SetupStep.entries.size

    ModalBottomSheet(onDismissRequest = { /* first-time setup */ }, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = currentStep.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (currentStep) {
                SetupStep.WELCOME -> {
                    Text(
                        text = "BakeFlow helps you track stock, production, and sales — without spreadsheets or calculators.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We'll set up sample products, ingredients, and recipes you can edit anytime.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SetupStep.BAKERY_NAME -> {
                    Text(
                        text = "What is your bakery called?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = bakeryName,
                        onValueChange = { bakeryName = it },
                        label = { Text("Bakery name") },
                        placeholder = { Text("e.g. Sunrise Bakery") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = MaterialTheme.shapes.large,
                        singleLine = true
                    )
                }
                SetupStep.BAKERY_TYPE -> {
                    Text(
                        text = "Pick the closest match — we'll tailor your starter menu.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BakeryType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.label) }
                            )
                        }
                    }
                }
                SetupStep.GENERATE -> {
                    val name = bakeryName.trim().ifBlank { "your bakery" }
                    Text(
                        text = "Ready to set up $name as a ${selectedType.label}?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We'll add common ingredients, sample products, and recipes. You can change everything later from Products and Inventory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            RowActions(
                step = step,
                isSeeding = isSeeding,
                canContinue = bakeryName.trim().isNotBlank() || currentStep != SetupStep.BAKERY_NAME,
                onBack = { if (step > 0) step -= 1 },
                onNext = {
                    when (currentStep) {
                        SetupStep.GENERATE -> {
                            isSeeding = true
                            scope.launch {
                                appContainer.preferences.setBakeryName(bakeryName.trim())
                                BakerySetupSeeder.seed(
                                    bakeryType = selectedType,
                                    productRepository = appContainer.productRepository,
                                    ingredientRepository = appContainer.ingredientRepository,
                                    recipeRepository = appContainer.recipeRepository
                                )
                                appContainer.preferences.markSetupComplete(userId)
                                isSeeding = false
                                showSheet = false
                            }
                        }
                        else -> step += 1
                    }
                }
            )
        }
    }
}

@Composable
private fun RowActions(
    step: Int,
    isSeeding: Boolean,
    canContinue: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onNext,
            enabled = !isSeeding && canContinue,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                when {
                    isSeeding -> "Setting up…"
                    step >= SetupStep.GENERATE.ordinal -> "Set up my bakery"
                    else -> "Continue"
                }
            )
        }
        if (step > 0 && !isSeeding) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Back")
            }
        }
    }
}
