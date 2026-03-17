package com.example.grifon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.grifon.ui.GrifonApp
import com.example.grifon.ui.theme.GrifonTheme
import com.example.grifon.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()
            
            val isDark = (settingsState as? com.example.grifon.core.UiState.Success)?.data?.darkMode ?: false
            
            GrifonTheme(darkTheme = isDark) {
                GrifonApp()
            }
        }
    }
}
