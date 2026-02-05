package com.example.parqueame.ui.navigation

import com.example.parqueame.R

enum class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: Int
) {
    Home(
        route = Screen.HomeCompany.route,
        label = "Inicio",
        icon = R.drawable.inicio_icon
    ),
    Parqueos(
        route = Screen.AdminParqueosScreen.route,
        label = "Parqueos",
        icon = R.drawable.parqueo_icon
    ),
    Cuenta(
        route = Screen.CuentaCompany.route,
        label = "Cuenta",
        icon = R.drawable.cuenta_icon
    );
}
