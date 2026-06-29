package com.bakeflow.app.auth.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.auth.AuthViewModel
import com.bakeflow.app.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var roleMenuExpanded by remember { mutableStateOf(false) }

    AuthScaffold(
        title = "Create Account",
        subtitle = "Set up your bakery team profile",
        isOffline = uiState.isOffline,
        errorMessage = uiState.errorMessage,
        successMessage = uiState.successMessage,
        onDismissMessage = viewModel::clearMessages,
        modifier = modifier
    ) {
        AuthTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = "Full Name",
            errorMessage = uiState.nameError,
            imeAction = ImeAction.Next
        )
        AuthTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            errorMessage = uiState.emailError,
            imeAction = ImeAction.Next
        )
        AuthTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            errorMessage = uiState.passwordError,
            isPassword = true,
            imeAction = ImeAction.Next
        )
        AuthTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = "Confirm Password",
            errorMessage = uiState.confirmPasswordError,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::register
        )

        ExposedDropdownMenuBox(
            expanded = roleMenuExpanded,
            onExpandedChange = { roleMenuExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.role.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Role") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenuExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.large
            )
            DropdownMenu(
                expanded = roleMenuExpanded,
                onDismissRequest = { roleMenuExpanded = false }
            ) {
                UserRole.entries.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.displayName) },
                        onClick = {
                            viewModel.onRoleChange(role)
                            roleMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            AuthPrimaryButton(
                text = "Create Account",
                onClick = viewModel::register,
                enabled = !uiState.isOffline
            )
            AuthSecondaryButton(
                text = "Already have an account? Sign In",
                onClick = onNavigateToLogin
            )
        }
    }
}
