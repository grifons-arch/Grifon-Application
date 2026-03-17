package com.example.grifon.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.grifon.R
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Επιλογή Καταστήματος
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Επιλογή καταστήματος", style = MaterialTheme.typography.titleMedium)
                        settings.shops.forEach { shop ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setActiveShop(shop) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = settings.activeShopId == shop.id,
                                    onClick = { viewModel.setActiveShop(shop) },
                                )
                                Text(text = shop.name)
                            }
                        }
                    }
                }

                // 2. Επιλογή Γλώσσας
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
                        
                        val languages = listOf("el" to "Ελληνικά", "en" to "English", "sv" to "Svenska")
                        languages.forEach { (code, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setLanguage(code) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.language == code,
                                    onClick = { viewModel.setLanguage(code) }
                                )
                                Text(text = label)
                            }
                        }
                    }
                }

                // 3. Εμφάνιση & Ειδοποιήσεις
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.dark_mode), modifier = Modifier.weight(1f))
                            Switch(
                                checked = settings.darkMode, 
                                onCheckedChange = { viewModel.setDarkMode(it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.notifications), modifier = Modifier.weight(1f))
                            Switch(checked = settings.notificationsEnabled, onCheckedChange = {})
                        }
                    }
                }
            }
        }
    }
}
