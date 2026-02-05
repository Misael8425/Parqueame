package com.example.parqueame.ui.login.components_login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.common_components.LabelledTextField
import com.example.parqueame.ui.common_components.PasswordTextField
import com.example.parqueame.ui.theme.dmSans
import com.example.parqueame.R
import androidx.compose.ui.res.stringResource

@Composable
fun LoginForm(
    userInput: String,
    onUserInputChange: (String) -> Unit,
    passwordInput: String,
    onPasswordInputChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: () -> Unit,
    textColor: Color = Color.Black

) {
    val whiteBackground = Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 450.dp)
            .shadow(6.dp, RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp))
            .background(whiteBackground, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        LabelledTextField(
            label = stringResource(R.string.id_or_email_label),
            value = userInput,
            onValueChange = onUserInputChange,
            placeholder = stringResource(R.string.id_or_email_label),
            keyboardType = KeyboardType.Email,
        )

        Spacer(modifier = Modifier.height(14.dp))

        PasswordTextField(
            label = stringResource(R.string.password_label),
            value = passwordInput,
            onValueChange = onPasswordInputChange,
            placeholder = stringResource(R.string.password_placeholder_simple),
            passwordVisible = passwordVisible,
            onVisibilityChange = onPasswordVisibilityChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        GradientButton(
            text = stringResource(R.string.login_action),
            onClick = onLoginClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 16.dp)
                .width(215.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.dont_have_account_q),
            fontFamily = dmSans,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = stringResource(R.string.register_here),
            fontFamily = dmSans,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF115ED0),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRegisterClick
                )
                .padding(bottom = 30.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.forgot_password_link),
            fontFamily = dmSans,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF115ED0),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onForgotPasswordClick
                )
        )
    }
}

