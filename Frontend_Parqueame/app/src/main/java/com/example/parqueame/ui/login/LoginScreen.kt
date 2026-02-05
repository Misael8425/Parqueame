package com.example.parqueame.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.parqueame.R
import com.example.parqueame.ui.login.viewmodels.LoginViewModel
import com.example.parqueame.ui.login.components_login.LoginForm
import com.example.parqueame.ui.theme.loginBackgroundGradient
import com.example.parqueame.ui.theme.loginTopGradientColor
import com.example.parqueame.ui.theme.rtlRomman
import com.example.parqueame.utils.isLight
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.example.parqueame.ui.common_components.rememberTopErrorBanner
import com.example.parqueame.ui.common_components.rememberTopSuccessBanner
import com.example.parqueame.ui.navigation.Screen
import com.example.parqueame.data.PreferencesManager
import com.example.parqueame.session.SessionStore

@Suppress("DEPRECATION") // Accompanist SystemUiController está deprecado; ver nota más abajo.
@Composable
fun LoginScreen(
    navController: NavController,
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    val viewModel: LoginViewModel = viewModel()
    val loginState = viewModel.loginState.collectAsState().value

    var userInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val backgroundGradient = loginBackgroundGradient()
    val systemUiController = rememberSystemUiController()
    val topGradientColor = loginTopGradientColor()
    val whiteBackground = Color.White

    val useDarkIconsStatusBar = topGradientColor.luminance() > 0.5f
    val useDarkIconsNavBar = whiteBackground.luminance() > 0.5f
    val useDarkText = topGradientColor.isLight()

    val showError = rememberTopErrorBanner()
    val showSuccess = rememberTopSuccessBanner()

    val context = LocalContext.current
    val preferencesManager = remember(context) { PreferencesManager(context) }
    val sessionStore = remember { SessionStore(context) }

    // Evita navegación doble si el estado se re-emite
    var navigated by rememberSaveable { mutableStateOf(false) }

    SideEffect {
        systemUiController.setStatusBarColor(
            color = topGradientColor,
            darkIcons = useDarkIconsStatusBar
        )
        systemUiController.setNavigationBarColor(
            color = whiteBackground,
            darkIcons = useDarkIconsNavBar
        )
    }

    // Navegación por tipo con persistencia de sesión (DataStore + Preferences)
    LaunchedEffect(loginState) {
        when (val result = loginState) {
            is LoginViewModel.LoginResult.Success -> {
                if (navigated) return@LaunchedEffect

                val user = result.data.user
                val tipo: String = (user.tipo ?: "").uppercase()

                if (tipo.isBlank()) {
                    showError(context.getString(R.string.login_user_type_unknown))
                    return@LaunchedEffect
                }

                // Guarda en DataStore de sesión
                val userId: String = (user.id?.toString() ?: "").trim()
                val documento: String = (user.documento ?: "").trim()

                sessionStore.save(
                    userId = userId,
                    documento = documento,
                    tipo = tipo
                )

                // Preferencias existentes
                val correo: String = (user.correo ?: "").trim()
                if (correo.isNotEmpty()) preferencesManager.saveCorreo(correo)
                preferencesManager.saveRol(tipo)

                showSuccess(context.getString(R.string.login_success))

                val destino = when (tipo) {
                    "CONDUCTOR" -> Screen.HomeConductor.route
                    "EMPRESA"   -> Screen.HomeCompany.route
                    else        -> Screen.HomeConductor.route
                }

                navigated = true
                navController.navigate(destino) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is LoginViewModel.LoginResult.Error -> {
                navigated = false
                val msg = when (val m = result.message) {
                    is LoginViewModel.UiMessage.Res ->
                        if (m.args.isEmpty()) context.getString(m.id)
                        else context.getString(m.id, *m.args.toTypedArray())
                    is LoginViewModel.UiMessage.Text -> m.value
                }
                showError(msg)
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Spacer(modifier = Modifier.height(140.dp))
            HeaderTitle()
            Spacer(modifier = Modifier.weight(1f))

            LoginForm(
                userInput = userInput,
                onUserInputChange = { userInput = it },
                passwordInput = passwordInput,
                onPasswordInputChange = { passwordInput = it },
                passwordVisible = passwordVisible,
                onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                onRegisterClick = onRegisterClick,
                onForgotPasswordClick = onForgotPasswordClick,
                onLoginClick = {
                    if (userInput.isBlank() || passwordInput.isBlank()) {
                        showError(context.getString(R.string.login_both_required))
                    } else {
                        viewModel.login(userInput.trim(), passwordInput)
                    }
                },
                textColor = if (useDarkText) Color.Black else Color.White,


            )
        }
    }
}

@Composable
fun HeaderTitle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.brand_title),
            fontFamily = rtlRomman,
            fontSize = 55.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
