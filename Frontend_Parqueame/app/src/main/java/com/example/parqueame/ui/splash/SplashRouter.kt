package com.example.parqueame.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.example.parqueame.data.PreferencesManager
import com.example.parqueame.ui.navigation.Screen

@Composable
fun SplashRouter(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    LaunchedEffect(Unit) {
        // Leemos datos persistidos
        val rol = prefs.getRol().uppercase()
        val correo = prefs.getCorreo()

        // Si tenemos correo y rol válidos, roteamos directo
        val destino = when {
            correo.isNotBlank() && rol == "CONDUCTOR" -> Screen.HomeConductor.route
            correo.isNotBlank() && rol == "EMPRESA"   -> Screen.HomeCompany.route
            else -> Screen.Login.route
        }

        navController.navigate(destino) {
            popUpTo(Screen.Splash.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    // UI mínima de Splash (loader centrado)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
