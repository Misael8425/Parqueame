package com.example.parqueame.ui.recover_password

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.common_components.rememberTopErrorBanner
import com.example.parqueame.ui.recover_password.viewmodels.RecoverPasswordViewModel
import com.example.parqueame.ui.theme.MidnightBlue
import com.example.parqueame.ui.theme.dmSans
import com.example.parqueame.ui.theme.rtlRomman
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun RecoverPasswordScreen(
    onBackClick: () -> Unit = {},
    onSendClick: (String) -> Unit = {}
) {
    val viewModel: RecoverPasswordViewModel = viewModel()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val errorBanner = rememberTopErrorBanner()
    var email by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val systemUiController = rememberSystemUiController()
    val context = LocalContext.current

    SideEffect {
        systemUiController.setStatusBarColor(Color.White, darkIcons = true)
        systemUiController.setNavigationBarColor(Color.White, darkIcons = true)
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            when (msg) {
                is RecoverPasswordViewModel.UiMessage.Res ->
                    errorBanner(
                        if (msg.args.isEmpty()) context.getString(msg.id)
                        else context.getString(msg.id, *msg.args.toTypedArray())
                    )
                is RecoverPasswordViewModel.UiMessage.Text ->
                    errorBanner(msg.value)
            }
            viewModel.clearMessage()
            sending = false
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.recover_info_message),
                    fontFamily = dmSans,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = MidnightBlue
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.email_label),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = dmSans,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text(stringResource(R.string.email_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF2F2F2),
                    unfocusedContainerColor = Color(0xFFF2F2F2),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            GradientButton(
                text = if (sending) stringResource(R.string.sending_ellipsis) else stringResource(R.string.send_action),
                onClick = {
                    if (sending || email.isBlank()) return@GradientButton
                    sending = true
                    viewModel.solicitarCodigo(
                        email = email,
                        onSuccess = {
                            sending = false
                            onSendClick(email)
                        }
                    )
                },
                enabled = !sending,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.back_with_arrow),
                color = Color.Black,
                fontSize = 14.sp,
                fontFamily = dmSans,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onBackClick() }
            )
        }
    }
}
