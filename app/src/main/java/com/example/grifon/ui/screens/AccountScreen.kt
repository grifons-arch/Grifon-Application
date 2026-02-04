package com.example.grifon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.grifon.core.UiState
import com.example.grifon.viewmodel.AccountViewModel

@Composable
fun AccountScreen(viewModel: AccountViewModel, onSettings: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val account = state.data
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
                Button(onClick = onSettings) {
                    Text(text = "Settings")
                }
            }
        }
    }
}
