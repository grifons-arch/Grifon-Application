package com.example.grifon.core

object PrestaLanguage {
    fun toLangId(languageCode: String): Int = when (AppLanguage.normalize(languageCode)) {
        "en" -> 1
        "el" -> 2
        "sv" -> 3
        else -> 2
    }
}
