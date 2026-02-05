package com.example.parqueame.ui.recover_password

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.common_components.PasswordTextField
import com.example.parqueame.ui.common_components.rememberTopErrorBanner
import com.example.parqueame.ui.common_components.rememberTopSuccessBanner
import com.example.parqueame.ui.recover_password.viewmodels.RecoverPasswordViewModel
import com.example.parqueame.ui.theme.dmSans
import com.example.parqueame.ui.theme.rtlRomman
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import android.content.Context

// 🔹 Conversión de UiMessage a String
private fun RecoverPasswordViewModel.UiMessage.asText(ctx: Context): String = when (this) {
    is RecoverPasswordViewModel.UiMessage.Text -> value
    is RecoverPasswordViewModel.UiMessage.Res ->
        if (args.isEmpty()) ctx.getString(id)
        else ctx.getString(id, *args.toTypedArray())
}

@Composable
fun ResetPasswordScreen(
    onBackClick: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {} // Cambio: Nueva función para navegar al login
) {
    val viewModel: RecoverPasswordViewModel = viewModel()
    val errorBanner = rememberTopErrorBanner()
    val successBanner = rememberTopSuccessBanner()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(Color.White, darkIcons = true)
        systemUiController.setNavigationBarColor(Color.White, darkIcons = true)
    }

    // ✅ Se corrige el error aquí
    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            isLoading = false
            errorBanner(msg.asText(context))
            viewModel.clearMessage()
            // Removido: changing = false (variable no declarada)
        }
    }

    val successMsg = stringResource(R.string.password_reset_success)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(WindowInsets.safeDrawing.asPaddingValues()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.brand_title),
                fontFamily = rtlRomman,
                fontSize = 40.sp,
                color = Color(0xFF005BC1)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.reset_password_title),
                fontSize = 16.sp,
                color = Color.Gray,
                fontFamily = dmSans
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.new_password_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = dmSans,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                PasswordTextField(
                    label = "",
                    value = password,
                    onValueChange = { password = it },
                    placeholder = stringResource(R.string.password_placeholder),
                    passwordVisible = passwordVisible,
                    onVisibilityChange = { passwordVisible = !passwordVisible }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.confirm_password_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = dmSans,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                PasswordTextField(
                    label = "",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = stringResource(R.string.password_placeholder),
                    passwordVisible = confirmPasswordVisible,
                    onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            GradientButton(
                text = if (isLoading)
                    stringResource(R.string.resetting_ellipsis)
                else
                    stringResource(R.string.reset_action),
                onClick = {
                    if (!isLoading && password.isNotBlank() && confirmPassword.isNotBlank()) {

                        // 🔒 Validación local de coincidencia
                        if (password != confirmPassword) {
                            errorBanner(context.getString(R.string.passwords_not_equal_error))
                            return@GradientButton
                        }

                        isLoading = true
                        viewModel.cambiarContrasena(
                            nueva = password,
                            confirmar = confirmPassword,
                            onSuccess = {
                                successBanner(successMsg)
                                scope.launch {
                                    delay(2000)
                                    viewModel.clearRecoveryData()
                                    onNavigateToLogin()
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && password.isNotBlank() && confirmPassword.isNotBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.back_with_arrow),
                color = if (isLoading) Color.Gray else Color.Black,
                fontSize = 14.sp,
                fontFamily = dmSans,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isLoading
                ) {
                    if (!isLoading) {
                        onBackClick()
                    }
                }
            )
        }
    }
}