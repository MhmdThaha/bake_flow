package com.bakeflow.app.auth.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.auth.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuthScaffold(
        title = "Reset Password",
        subtitle = "Enter your email and we'll send you a reset link",
        isOffline = uiState.isOffline,
        errorMessage = uiState.errorMessage,
        successMessage = uiState.successMessage,
        onDismissMessage = viewModel::clearMessages,
        modifier = modifier
    ) {
        AuthTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            errorMessage = uiState.emailError,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::sendPasswordReset
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            AuthPrimaryButton(
                text = "Send Reset Email",
                onClick = viewModel::sendPasswordReset,
                enabled = !uiState.isOffline
            )
            AuthSecondaryButton(
                text = "Back to Sign In",
                onClick = onNavigateToLogin
            )
        }
    }
}
