package com.merino.ddfilms

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class DDFilmsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val themeMode = preferences.getString("theme_mode", "system") ?: "system"
        applyTheme(themeMode)

        val languageMode = preferences.getString("language_mode", "system") ?: "system"
        if (languageMode != "system") {
            applyLanguage(languageMode)
        }
    }

    companion object {
        @JvmStatic
        fun applyTheme(themeMode: String) {
            when (themeMode) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

        @JvmStatic
        fun applyLanguage(languageMode: String) {
            val appLocale = when (languageMode) {
                "es" -> LocaleListCompat.forLanguageTags("es")
                "en" -> LocaleListCompat.forLanguageTags("en")
                else -> LocaleListCompat.getEmptyLocaleList()
            }
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }
}
