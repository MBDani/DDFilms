package com.merino.ddfilms.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "Preferences"
    private const val KEY_LANGUAGE_MODE = "language_mode"

    /**
     * Gets the raw preference value stored in user options: "es", "en", or "system".
     */
    fun getLanguageMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE_MODE, "system") ?: "system"
    }

    /**
     * Resolves the effective language code ("es" or "en") according to user options.
     * If set to "system", it determines whether the system language is Spanish or English.
     */
    fun getLanguage(context: Context): String {
        return when (getLanguageMode(context)) {
            "es" -> "es"
            "en" -> "en"
            else -> {
                val appLocales = AppCompatDelegate.getApplicationLocales()
                if (!appLocales.isEmpty) {
                    val tag = appLocales.toLanguageTags()
                    if (tag.startsWith("es", ignoreCase = true)) "es" else "en"
                } else {
                    val sysLang = Locale.getDefault().language
                    if (sysLang.startsWith("es", ignoreCase = true)) "es" else "en"
                }
            }
        }
    }

    /**
     * Returns the language format expected by TMDB API ("es-ES" or "en-US")
     * based dynamically on the user's selected options.
     */
    fun getTmdbLanguage(context: Context): String {
        return if (getLanguage(context) == "es") "es-ES" else "en-US"
    }
}
