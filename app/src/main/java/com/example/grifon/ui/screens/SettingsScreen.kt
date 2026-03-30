package com.example.grifon.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grifon.core.ShopConfig
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
            val language = settings.language
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Επιλογή Καταστήματος
                Text(text = settingsLabel(SettingsLabel.SelectStore, language), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        settings.shops.forEach { shop ->
                            val isSelected = settings.activeShopId == ShopConfig.normalizeShopId(shop.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setActiveShop(shop) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setActiveShop(shop) },
                                )
                                Text(
                                    text = ShopConfig.displayName(shop.id),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                // 2. Επιλογή Γλώσσας με Σημαίες
                Text(text = settingsLabel(SettingsLabel.Language, language), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LanguageFlagButton(
                        flag = "🇬🇷",
                        label = settingsLabel(SettingsLabel.LanguageGreek, language),
                        isSelected = settings.language == "el"
                    ) {
                        viewModel.setLanguage("el")
                    }
                    LanguageFlagButton(
                        flag = "🇸🇪",
                        label = settingsLabel(SettingsLabel.LanguageSwedish, language),
                        isSelected = settings.language == "sv"
                    ) {
                        viewModel.setLanguage("sv")
                    }
                    LanguageFlagButton(
                        flag = "🇬🇧",
                        label = settingsLabel(SettingsLabel.LanguageEnglish, language),
                        isSelected = settings.language == "en"
                    ) {
                        viewModel.setLanguage("en")
                    }
                }

                // 3. Εμφάνιση & Ειδοποιήσεις
                Text(text = settingsLabel(SettingsLabel.Preferences, language), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = settingsLabel(SettingsLabel.DarkMode, language), modifier = Modifier.weight(1f))
                            Switch(
                                checked = settings.darkMode, 
                                onCheckedChange = { viewModel.setDarkMode(it) }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.Gray.copy(0.3f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = settingsLabel(SettingsLabel.Notifications, language), modifier = Modifier.weight(1f))
                            Switch(checked = settings.notificationsEnabled, onCheckedChange = {})
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageFlagButton(flag: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = flag, fontSize = 32.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
    }
}

private enum class SettingsLabel {
    SelectStore,
    Language,
    LanguageGreek,
    LanguageSwedish,
    LanguageEnglish,
    Preferences,
    DarkMode,
    Notifications,
}

private fun settingsLabel(label: SettingsLabel, language: String): String = when (label) {
    SettingsLabel.SelectStore -> when (language) {
        "sv" -> "Välj butik"
        "en" -> "Select Store"
        else -> "Επιλογή καταστήματος"
    }
    SettingsLabel.Language -> when (language) {
        "sv" -> "Språk"
        "en" -> "Language"
        else -> "Γλώσσα"
    }
    SettingsLabel.LanguageGreek -> when (language) {
        "sv" -> "Grekiska"
        "en" -> "Greek"
        else -> "Ελληνικά"
    }
    SettingsLabel.LanguageSwedish -> when (language) {
        "sv" -> "Svenska"
        "en" -> "Swedish"
        else -> "Σουηδικά"
    }
    SettingsLabel.LanguageEnglish -> when (language) {
        "sv" -> "Engelska"
        "en" -> "English"
        else -> "Αγγλικά"
    }
    SettingsLabel.Preferences -> when (language) {
        "sv" -> "Inställningar"
        "en" -> "Preferences"
        else -> "Προτιμήσεις"
    }
    SettingsLabel.DarkMode -> when (language) {
        "sv" -> "Mörkt läge"
        "en" -> "Dark Mode"
        else -> "Σκούρα εμφάνιση"
    }
    SettingsLabel.Notifications -> when (language) {
        "sv" -> "Aviseringar"
        "en" -> "Notifications"
        else -> "Ειδοποιήσεις"
    }
}
