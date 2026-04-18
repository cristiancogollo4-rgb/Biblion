package com.cristiancogollo.biblion

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun LoginScreen(
    navController: NavController,
    uiState: AuthUiState,
    onIntent: (AuthIntent) -> Unit,
    onGoogleSignIn: () -> Unit
) {
    AuthScreenScaffold(
        navController = navController,
        title = stringResource(R.string.auth_login_title),
        subtitle = stringResource(R.string.auth_login_subtitle)
    ) {
        AuthForm(
            uiState = uiState,
            showConfirmPassword = false,
            primaryButtonText = stringResource(R.string.sign_in),
            secondaryLabel = stringResource(R.string.auth_no_account),
            secondaryActionText = stringResource(R.string.auth_go_to_register),
            onEmailChanged = { onIntent(AuthIntent.UpdateEmail(it)) },
            onPasswordChanged = { onIntent(AuthIntent.UpdatePassword(it)) },
            onConfirmPasswordChanged = {},
            onPrimaryAction = { onIntent(AuthIntent.SignIn) },
            onGoogleSignIn = onGoogleSignIn,
            onSecondaryAction = {
                onIntent(AuthIntent.ClearError)
                navController.navigateSingleTop(Screen.Register.route)
            }
        )
    }
}

@Composable
fun RegisterScreen(
    navController: NavController,
    uiState: AuthUiState,
    onIntent: (AuthIntent) -> Unit
) {
    AuthScreenScaffold(
        navController = navController,
        title = stringResource(R.string.auth_register_title),
        subtitle = stringResource(R.string.auth_register_subtitle)
    ) {
        AuthForm(
            uiState = uiState,
            showConfirmPassword = true,
            primaryButtonText = stringResource(R.string.auth_create_account),
            secondaryLabel = stringResource(R.string.auth_have_account),
            secondaryActionText = stringResource(R.string.auth_go_to_login),
            onEmailChanged = { onIntent(AuthIntent.UpdateEmail(it)) },
            onPasswordChanged = { onIntent(AuthIntent.UpdatePassword(it)) },
            onConfirmPasswordChanged = { onIntent(AuthIntent.UpdateConfirmPassword(it)) },
            onPrimaryAction = { onIntent(AuthIntent.Register) },
            onGoogleSignIn = null,
            onSecondaryAction = {
                onIntent(AuthIntent.ClearError)
                val returnedToLogin = navController.popBackStack()
                if (!returnedToLogin) {
                    navController.navigateSingleTop(Screen.Login.route)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthScreenScaffold(
    navController: NavController,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Image(
                painter = painterResource(id = R.drawable.logobiblion),
                contentDescription = stringResource(R.string.auth_logo_cd),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.55f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            content()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AuthForm(
    uiState: AuthUiState,
    showConfirmPassword: Boolean,
    primaryButtonText: String,
    secondaryLabel: String,
    secondaryActionText: String,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onPrimaryAction: () -> Unit,
    onGoogleSignIn: (() -> Unit)?,
    onSecondaryAction: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.errorMessageRes != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(uiState.errorMessageRes),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.auth_email_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            enabled = !uiState.isLoading
        )

        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.auth_password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (showConfirmPassword) ImeAction.Next else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onPrimaryAction() }),
            enabled = !uiState.isLoading
        )

        if (showConfirmPassword) {
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_confirm_password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onPrimaryAction() }),
                enabled = !uiState.isLoading
            )
        }

        Button(
            onClick = onPrimaryAction,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                Text(primaryButtonText)
            }
        }

        if (onGoogleSignIn != null) {
            OutlinedButton(
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(stringResource(R.string.auth_continue_with_google))
            }
        }

        TextButton(
            onClick = onSecondaryAction,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = !uiState.isLoading
        ) {
            Text("$secondaryLabel $secondaryActionText")
        }
    }
}
