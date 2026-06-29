package com.bakeflow.app.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.auth.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AuthScaffold(
        title = "Welcome Back",
        subtitle = "Sign in to manage your bakery",
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
            imeAction = ImeAction.Next
        )
        AuthTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            errorMessage = uiState.passwordError,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::signIn
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onNavigateToForgotPassword) {
                Text("Forgot password?")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            AuthPrimaryButton(
                text = "Sign In",
                onClick = viewModel::signIn,
                enabled = !uiState.isOffline
            )
            AuthSecondaryButton(
                text = "Continue with Google",
                onClick = { viewModel.signInWithGoogle(context) },
                enabled = !uiState.isOffline
            )
            AuthSecondaryButton(
                text = "Create Account",
                onClick = onNavigateToRegister
            )
        }
    }
}
