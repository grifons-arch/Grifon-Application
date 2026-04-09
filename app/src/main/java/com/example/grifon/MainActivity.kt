package com.example.grifon

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.appcompat.app.AppCompatActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.example.grifon.ui.GrifonApp
import com.example.grifon.ui.theme.GrifonTheme
import com.example.grifon.core.AppLanguage
import com.example.grifon.data.local.ShopPreferences
import com.example.grifon.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var shopPreferences: ShopPreferences

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguage.apply(AppLanguage.getStoredLanguage(this))
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Require an explicit in-app login on each fresh launch before showing wholesale prices.
            shopPreferences.clearCustomerSession()
        }

        lifecycleScope.launch {
            shopPreferences.appLanguage
                .distinctUntilChanged()
                .collect { langCode ->
                    val normalizedLanguage = AppLanguage.normalize(langCode)
                    val currentResourceLanguage =
                        AppLanguage.normalize(resources.configuration.locales[0]?.language)

                    AppLanguage.apply(normalizedLanguage)

                    if (currentResourceLanguage != normalizedLanguage) {
                        recreate()
                    }
                }
        }

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            val isDark = (settingsState as? com.example.grifon.core.UiState.Success)?.data?.darkMode ?: false

            GrifonTheme(darkTheme = isDark) {
                GrifonApp()
            }
        } catch (e: Exception) {
            Log.e("CrashLog", "MainActivity CRASH in setContent", e)
        }
    }
}
