package com.example.grifon.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.grifon.core.UiState
import com.example.grifon.viewmodel.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel, 
    onSettings: () -> Unit,
    onRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val account = state.data
            val registerText = buildAnnotatedString {
                append("Αν δεν έχεις λογαριασμό δημιούργησε ")
                pushStringAnnotation(tag = "register", annotation = "register")
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append("εδώ")
                }
                pop()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = if (account.loggedIn) "Καλώς ήρθες" else "Σύνδεση", style = MaterialTheme.typography.titleLarge)
                        Text(text = "Λογαριασμός, παραγγελίες, διευθύνσεις, πληρωμές, wishlist", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (!account.loggedIn) {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Κωδικός") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )

                            Button(
                                onClick = { viewModel.login(email, password) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Login")
                            }

                            ClickableText(
                                text = registerText,
                                style = MaterialTheme.typography.bodyMedium,
                                onClick = { offset ->
                                    registerText.getStringAnnotations("register", offset, offset)
                                        .firstOrNull()?.let { onRegister() }
                                }
                            )
                        }
                    }
                } else {
                    // Πληροφορίες προφίλ αν είναι logged in
                    Button(onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Logout")
                    }
                }

                OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Settings")
                }
            }
        }
    }
}
