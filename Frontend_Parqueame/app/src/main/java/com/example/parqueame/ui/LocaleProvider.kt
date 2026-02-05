package com.example.parqueame.ui

import androidx.compose.runtime.*

/**
 * CompositionLocal para propagar cambios de idioma sin reiniciar
 */
val LocalAppLanguage = staticCompositionLocalOf { "es" }

/**
 * Estado global para el idioma de la app
 */
object LanguageState {
    var currentLanguage by mutableStateOf("es")
        private set

    fun setLanguage(lang: String) {
        currentLanguage = lang
    }
}

@Composable
fun LanguageProvider(content: @Composable () -> Unit) {
    // Provee el idioma actual a todos los composables hijos
    CompositionLocalProvider(
        LocalAppLanguage provides LanguageState.currentLanguage
    ) {
        content()
    }
}