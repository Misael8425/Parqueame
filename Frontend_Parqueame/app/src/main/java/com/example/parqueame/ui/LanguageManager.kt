package com.example.parqueame.ui

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale

private val Context.languageDataStore by preferencesDataStore(name = "language_prefs")

object LanguageManager {

    private val LANGUAGE_KEY = stringPreferencesKey("app_language")

    /** Aplica el idioma guardado al iniciar la app */
    fun applyStoredLanguage(context: Context) {
        val stored = getStoredLanguage(context)
        if (stored.isNotEmpty()) {
            setLocale(context, stored)
            LanguageState.setLanguage(stored)
        } else {
            LanguageState.setLanguage("es")
        }
    }

    /** Cambia el idioma SIN reiniciar (para uso con LanguageProvider) */
    fun setAppLanguageWithoutRestart(context: Context, langTag: String) {
        val currentNorm = normalize(LanguageState.currentLanguage)
        val targetNorm = normalize(langTag)

        if (currentNorm == targetNorm) return

        // Guarda y aplica
        saveLanguage(context, langTag)
        setLocale(context, langTag)
        LanguageState.setLanguage(langTag)
    }

    /** Obtiene el idioma actual del contexto */
    fun current(context: Context): String {
        val locale = context.resources.configuration.locales[0]
        return locale.language
    }

    /** Aplica el locale directamente a la configuración */
    private fun setLocale(context: Context, langTag: String) {
        val locale = Locale(langTag)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        context.createConfigurationContext(config)

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    /** Guarda el idioma en DataStore */
    private fun saveLanguage(context: Context, langTag: String) {
        runBlocking {
            context.languageDataStore.edit { prefs ->
                prefs[LANGUAGE_KEY] = langTag
            }
        }
    }

    /** Obtiene el idioma guardado de DataStore */
    private fun getStoredLanguage(context: Context): String {
        return runBlocking {
            context.languageDataStore.data.map { prefs ->
                prefs[LANGUAGE_KEY] ?: ""
            }.first()
        }
    }

    /** Normaliza el tag: "en-US" -> "en" */
    private fun normalize(tag: String): String =
        tag.lowercase().substringBefore('-')
}