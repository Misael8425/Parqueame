package com.example.parqueame.ui.common_components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parqueame.ui.theme.dmSans

@Composable
fun LabelledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Text(
        text = label,
        fontFamily = dmSans,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)
    )
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
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0x0A000000),
            unfocusedContainerColor = Color(0x0A000000),
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = Color.Black
        ),
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType),
        textStyle = TextStyle(fontFamily = dmSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        trailingIcon = trailingIcon
    )
}