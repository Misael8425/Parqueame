package com.example.parqueame.ui.common_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parqueame.ui.theme.dmSans

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpCodeInput(
    onCodeFilled: (String) -> Unit,
    modifier: Modifier = Modifier,
    codeLength: Int = 6
) {
    var codes by remember { mutableStateOf(Array(codeLength) { "" }) }
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember { Array(codeLength) { FocusRequester() } }
    var focusedIndex by remember { mutableStateOf(-1) }

    // Observar cambios en el código completo
    LaunchedEffect(codes.toList()) {
        val fullCode = codes.joinToString("")
        if (fullCode.length == codeLength) {
            onCodeFilled(fullCode)
            focusManager.clearFocus()
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        codes.forEachIndexed { index, code ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { newValue ->
                        // Solo permitir un dígito
                        if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                            codes = codes.copyOf().apply { this[index] = newValue }

                            // Auto-avanzar al siguiente campo si se ingresó un dígito
                            if (newValue.isNotEmpty() && index < codeLength - 1) {
                                focusRequesters[index + 1].requestFocus()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequesters[index])
                        .onFocusChanged { focusState ->
                            focusedIndex = if (focusState.isFocused) index else -1
                        },
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = dmSans
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        focusedBorderColor = Color(0xFF005BC1),
                        unfocusedBorderColor = if (code.isNotEmpty()) Color(0xFF005BC1) else Color(0xFFE0E0E0),
                        cursorColor = Color(0xFF005BC1),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = {
                        Text(
                            text = "•",
                            style = TextStyle(
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                                color = Color(0xFFCCCCCC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
        }
    }

    // Auto-focus en el primer campo cuando se monta el componente
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}