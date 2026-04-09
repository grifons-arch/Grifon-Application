package com.example.grifon.core

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object AppLanguage {
    private const val prefsName = "app_language_prefs"
    private const val languageKey = "app_language"
    private val supportedLanguages = setOf("el", "en", "sv")

    fun normalize(languageCode: String?): String {
        val normalized = languageCode?.trim()?.lowercase()
        return if (normalized in supportedLanguages) normalized!! else "el"
    }

    fun toLocaleList(languageCode: String): LocaleListCompat =
        LocaleListCompat.forLanguageTags(normalize(languageCode))

    fun currentLanguage(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val appLanguage = appLocales[0]?.language
        return normalize(appLanguage ?: Locale.getDefault().language)
    }

    fun getStoredLanguage(context: Context): String =
        normalize(context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).getString(languageKey, null))

    fun persist(context: Context, languageCode: String) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(languageKey, normalize(languageCode))
            .apply()
    }

    fun wrapContext(context: Context): Context {
        val languageCode = getStoredLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    fun apply(languageCode: String) {
        val normalized = normalize(languageCode)
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        Locale.setDefault(Locale(normalized))
        if (current != normalized) {
            AppCompatDelegate.setApplicationLocales(toLocaleList(normalized))
        }
    }
}
