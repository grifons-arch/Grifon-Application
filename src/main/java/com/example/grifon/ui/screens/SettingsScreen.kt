package com.example.grifon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.grifon.core.UiState
import com.example.grifon.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val settings = state.data
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(modifier = Modifier.padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Επιλογή καταστήματος", style = MaterialTheme.typography.titleMedium)
                        settings.shops.forEach { shop ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = settings.activeShopId == shop.id,
                                    onClick = { viewModel.setActiveShop(shop) },
                                )
                                Text(text = shop.name)
                            }
                        }
                    }
                }
                Card(modifier = Modifier.padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Γλώσσα", style = MaterialTheme.typography.titleMedium)
                        Text(text = settings.language)
                        Text(text = "Νόμισμα", style = MaterialTheme.typography.titleMedium)
                        Text(text = settings.currency)
                    }
                }
                Card(modifier = Modifier.padding(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Dark mode", modifier = Modifier.weight(1f))
                            Switch(checked = settings.darkMode, onCheckedChange = {})
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Ειδοποιήσεις", modifier = Modifier.weight(1f))
                            Switch(checked = settings.notificationsEnabled, onCheckedChange = {})
                        }
                    }
                }
            }
        }
    }
}
