package com.example.grifon.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.grifon.LoginActivity
import com.example.grifon.RegisterActivity
import com.example.grifon.core.UiState
import com.example.grifon.viewmodel.AccountViewModel

@Composable
fun AccountScreen(viewModel: AccountViewModel, onSettings: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(modifier = Modifier.padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = if (account.loggedIn) "Καλώς ήρθες" else "Σύνδεση", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Λογαριασμός, παραγγελίες, διευθύνσεις, πληρωμές, wishlist")
                    }
                }
                if (!account.loggedIn) {
                    Card(modifier = Modifier.padding(8.dp)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    context.startActivity(Intent(context, LoginActivity::class.java))
                                },
                            ) {
                                Text(text = "Login")
                            }
                            ClickableText(
                                text = registerText,
                                style = MaterialTheme.typography.bodyMedium,
                                onClick = { offset ->
                                    registerText.getStringAnnotations("register", offset, offset)
                                        .firstOrNull()
                                        ?.let {
                                            context.startActivity(
                                                Intent(context, RegisterActivity::class.java),
                                            )
                                        }
                                },
                            )
                        }
                    }
                }
                Button(onClick = onSettings) {
                    Text(text = "Settings")
                }
            }
        }
    }
}
