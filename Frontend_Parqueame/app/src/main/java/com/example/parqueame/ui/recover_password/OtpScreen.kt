package com.example.parqueame.ui.recover_password

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.common_components.OtpCodeInput
import com.example.parqueame.ui.common_components.rememberTopErrorBanner
import com.example.parqueame.ui.common_components.rememberTopSuccessBanner
import com.example.parqueame.ui.recover_password.viewmodels.RecoverPasswordViewModel
import com.example.parqueame.ui.theme.MidnightBlue
import com.example.parqueame.ui.theme.dmSans
import com.example.parqueame.ui.theme.rtlRomman
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun OtpScreen(
    onBackClick: () -> Unit = {},
    onCodeSubmit: (String) -> Unit = {},
    viewModel: RecoverPasswordViewModel = viewModel()
) {
    val systemUiController = rememberSystemUiController()
    var code by remember { mutableStateOf("") }
    var verifying by remember { mutableStateOf(false) }
    val errorBanner = rememberTopErrorBanner()
    val successBanner = rememberTopSuccessBanner()
    val uiMessage by viewModel.uiMessage.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    SideEffect {
        systemUiController.setStatusBarColor(Color.White, darkIcons = true)
        systemUiController.setNavigationBarColor(Color.White, darkIcons = true)
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            isLoading = false
            when (msg) {
                is RecoverPasswordViewModel.UiMessage.Res ->
                    errorBanner(
                        // usa stringResource con args si hay
                        if (msg.args.isEmpty()) context.getString(msg.id)
                        else context.getString(msg.id, *msg.args.toTypedArray())
                    )
                is RecoverPasswordViewModel.UiMessage.Text ->
                    errorBanner(msg.value)
            }
            viewModel.clearMessage()
        }
    }

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
                color = Color(0xFF005BC1),
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.symbols_info),
                    contentDescription = stringResource(R.string.info_cd),
                    tint = Color(0xFF005BC1),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.otp_info_message),
                    fontFamily = dmSans,
                    fontSize = 15.5.sp,
                    color = MidnightBlue
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.otp_recovery_code_label),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = dmSans,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))


            // Componente OTP mejorado
            OtpCodeInput(
                onCodeFilled = { enteredCode ->
                    code = enteredCode
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.otp_didnt_receive),
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    fontFamily = dmSans
                )

                val resendSuccessMsg = stringResource(R.string.otp_resend_success)

                Text(
                    text = stringResource(R.string.otp_resend),
                    fontSize = 14.sp,
                    color = Color(0xFF005BC1),
                    fontFamily = dmSans,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.reenviarCodigo(
                                onSuccess = {
                                    successBanner(resendSuccessMsg)  // usamos el valor capturado
                                }
                            )
                        }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            GradientButton(
                text = if (isLoading)
                    stringResource(R.string.otp_verifying_ellipsis)
                else
                    stringResource(R.string.otp_verify_button),
                onClick = {
                    if (code.isNotBlank() && code.length == 6 && !isLoading) {
                        isLoading = true
                        viewModel.verificarCodigo(
                            code = code,
                            onSuccess = {
                                isLoading = false
                                onCodeSubmit(code)
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = code.length == 6 && !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.back_with_arrow),
                color = Color.Black,
                fontSize = 14.sp,
                fontFamily = dmSans,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isLoading) {
                            onBackClick()
                        }
                    }
            )
        }
    }
}
