package com.example.parqueame.ui.common_components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parqueame.R
import androidx.compose.runtime.remember
import com.example.parqueame.ui.theme.dmSans
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment

@Composable
fun PasswordTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    passwordVisible: Boolean,
    onVisibilityChange: () -> Unit
) {
    // Esto debe estar dentro de la función @Composable
    val interactionSource = remember { MutableInteractionSource() }

    // SOLUCIÓN: Solo mostrar el label si no está vacío
    if (label.isNotEmpty()) {
        Text(
            text = label,
            fontFamily = dmSans,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)
        )
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                fontFamily = dmSans,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black.copy(alpha = 0.15f),
                modifier = Modifier.padding(start = 0.dp)
            )
        },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0x0A000000),
            unfocusedContainerColor = Color(0x0A000000),
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = Color.Black
        ),
        trailingIcon = {
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .padding(end = 15.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onVisibilityChange
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = if (passwordVisible) R.drawable.visibility else R.drawable.visibilityoff),
                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = Color(0xFF115ED0),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        textStyle = TextStyle(fontFamily = dmSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    )
}