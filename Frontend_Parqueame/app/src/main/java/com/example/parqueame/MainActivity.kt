package com.example.parqueame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.example.parqueame.ui.*
import com.example.parqueame.ui.navigation.NavGraph
import com.example.parqueame.ui.navigation.Screen
import com.example.parqueame.ui.splash.SplashScreen
import com.example.parqueame.ui.theme.ParqueameTheme
import com.example.parqueame.utils.isLight
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    // Flag para detectar si es la primera vez que se crea la Activity
    private var isFirstLaunch = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Aplica el idioma guardado antes de crear la UI
        LanguageManager.applyStoredLanguage(this)

        // Si savedInstanceState no es null, significa que es una recreación
        if (savedInstanceState != null) {
            isFirstLaunch = false
        }

        setContent {
            // 👇 Envuelve TODO con tu tema
            ParqueameTheme(
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = false // ponlo en true si quieres Material You en Android 12+
            ) {
                // Tokens del tema
                val cs = MaterialTheme.colorScheme

                var systemBarColor by remember { mutableStateOf(cs.background) }
                var showSplash by remember { mutableStateOf(isFirstLaunch) }
                var startDestination by remember { mutableStateOf(Screen.Login.route) }

                // Splash solo la primera vez
                LaunchedEffect(Unit) {
                    if (isFirstLaunch) {
                        delay(500)
                        showSplash = false
                    }
                }

                // Actualiza status/nav bar según el color actual
                LaunchedEffect(systemBarColor) {
                    val isLight = systemBarColor.isLight()
                    val barColor = systemBarColor.toArgb()

                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            lightScrim = barColor,
                            darkScrim = barColor,
                            detectDarkMode = { !isLight }
                        ),
                        navigationBarStyle = SystemBarStyle.auto(
                            lightScrim = barColor,
                            darkScrim = barColor,
                            detectDarkMode = { !isLight }
                        )
                    )
                }

                // UI
                Box(Modifier.fillMaxSize()) {
                    LanguageProvider {
                        AnimatedVisibility(
                            visible = showSplash,
                            enter = fadeIn(animationSpec = tween(600)),
                            exit = fadeOut(animationSpec = tween(600))
                        ) {
                            SplashScreen(
                                onBackgroundColorChange = { color -> systemBarColor = color },
                                onSplashFinished = { showSplash = false }
                            )
                        }

                        AnimatedVisibility(
                            visible = !showSplash,
                            enter = fadeIn(animationSpec = tween(600)),
                            exit = fadeOut(animationSpec = tween(600))
                        ) {
                            NavGraph(startDestination = startDestination)
                        }
                    }
                }
            }
        }
    }
}