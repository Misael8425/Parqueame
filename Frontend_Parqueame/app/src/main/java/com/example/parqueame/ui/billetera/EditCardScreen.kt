// EditCardScreen.kt
package com.example.parqueame.ui.billetera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.GradientIcon
import com.example.parqueame.ui.theme.DmSans
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.annotation.DrawableRes
import com.example.parqueame.ui.common_components.GradientButton


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardScreen(
    navController: NavController,
    card: UiCard,
    onCardUpdated: (UiCard) -> Unit
) {
    var name by remember { mutableStateOf(card.holder) }
    var expiry by remember { mutableStateOf(card.expiry) }
    var cvc by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf(card.nickname ?: "") }

    var showExpiryHelp by remember { mutableStateOf(false) }
    var showCvvHelp by remember { mutableStateOf(false) }

    val canSubmit = remember(name, expiry, cvc) {
        name.isNotBlank() &&
                expiry.matches(Regex("""^\d{2}/\d{2}$""")) &&
                cvc.length in 3..4
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            stringResource(R.string.edit_payment_method_action),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = DmSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                                color = Color(0xFF002D62)
                            )
                        )
                    }
                },
                navigationIcon = {
                    GradientIcon(
                        imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = R.drawable.back_icon),
                        contentDescription = stringResource(R.string.go_back_action),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { navController.popBackStack() }
                    )
                },
                colors = topAppBarColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp)
                    .background(Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .padding(horizontal = 25.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.card_number_label),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = DmSans, fontSize = 18.sp, fontWeight = FontWeight.Medium
                )
            )
            TextField(
                value = stringResource(R.string.card_number_masked_format, card.last4),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSans),
                leadingIcon = { LeadingBrandIcon(card.brand) }
            )

            FilledField(
                label = stringResource(R.string.card_name_label),
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.card_name_placeholder)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.expiry_date_label),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DmSans, fontSize = 18.sp, fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(15.dp))
                    TextField(
                        value = expiry,
                        onValueChange = { s ->
                            val clean = s.filter(Char::isDigit).take(4)
                            expiry = if (clean.length <= 2) clean
                            else clean.substring(0, 2) + "/" + clean.substring(2)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSans),
                        placeholder = { Text(text = stringResource(R.string.expiry_date_placeholder), color = FieldPlaceholder, fontFamily = DmSans) },
                        trailingIcon = {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .background(FieldIconBg, RoundedCornerShape(6.dp))
                                    .clickable { showExpiryHelp = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(R.drawable.info_icon_gray),
                                    contentDescription = stringResource(R.string.expiry_date_help_cd),
                                    tint = FieldPlaceholder,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cvv_label),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DmSans, fontSize = 18.sp, fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(15.dp))
                    TextField(
                        value = cvc,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) cvc = it },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSans),
                        placeholder = { Text(stringResource(R.string.cvv_placeholder), color = FieldPlaceholder, fontFamily = DmSans) },
                        visualTransformation = PasswordVisualTransformation(),
                        trailingIcon = {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .background(FieldIconBg, RoundedCornerShape(6.dp))
                                    .clickable { showCvvHelp = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(R.drawable.info_icon_gray),
                                    contentDescription = stringResource(R.string.cvv_help_cd),
                                    tint = FieldPlaceholder,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            }

            FilledField(
                label = stringResource(R.string.nickname_optional_label),
                value = nickname,
                onValueChange = { nickname = it },
                placeholder = stringResource(R.string.nickname_placeholder),
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Spacer(Modifier.height(204.dp))

            Button(
                onClick = {
                    onCardUpdated(
                        card.copy(
                            holder = name.trim(),
                            expiry = expiry.trim(),
                            nickname = nickname.ifBlank { null }
                        )
                    )
                    navController.popBackStack()
                },
                enabled = canSubmit,
                modifier = Modifier
                    .width(220.dp)
                    .align(Alignment.CenterHorizontally)
                    .height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF2DA6FF), Color(0xFF0B55D6))),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.edit_action),
                        color = Color.White,
                        fontFamily = DmSans,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HelpModalBottomSheet(
        show: Boolean,
        title: String,
        description: String,
        @DrawableRes imageRes: Int,
        onDismiss: () -> Unit
    ) {
        if (!show) return

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color(0xFF0056D2)
                    )
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = DmSans,
                            fontSize = 18.sp,
                            color = Color(0xFF1B1B1B)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Icon(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(100.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))

                GradientButton(
                    text = stringResource(R.string.close_action),
                    onClick = onDismiss,
                    modifier = Modifier
                        .width(220.dp)
                        .align(Alignment.CenterHorizontally)
                        .height(52.dp)
                )

                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

/* Helpers locales (mismos que ya usas) */

@Composable
private fun FilledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    leadingIcon: (@Composable (() -> Unit))? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = DmSans, fontSize = 18.sp, fontWeight = FontWeight.Medium
        )
    )
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        shape = RoundedCornerShape(18.dp),
        colors = fieldColors(),
        modifier = modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSans),
        placeholder = { Text(placeholder, color = FieldPlaceholder, fontFamily = DmSans) },
        leadingIcon = leadingIcon,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation
    )
}

private val FieldBg = Color(0xFFF5F5F5)
private val FieldIconBg = Color(0x00E7ECF3)
private val FieldPlaceholder = Color(0xFFD0D0D0)
private val FieldText = Color(0xFF1B1B1B)

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = FieldBg,
    unfocusedContainerColor = FieldBg,
    disabledContainerColor = FieldBg,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = Color(0xFF0B55D6),
    focusedTextColor = FieldText,
    unfocusedTextColor = FieldText,
    focusedPlaceholderColor = FieldPlaceholder,
    unfocusedPlaceholderColor = FieldPlaceholder,
    focusedLeadingIconColor = Color.Unspecified,
    unfocusedLeadingIconColor = Color.Unspecified,
    disabledLeadingIconColor = Color.Unspecified
)

@Composable
private fun LeadingBrandIcon(brand: String) {
    Icon(
        painter = painterResource(brandMenuIconRes(brand)),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun brandMenuIconRes(brand: String): Int = when (brand.trim().lowercase()) {
    "visa" -> R.drawable.visa_card_icon
    "mastercard", "master card", "mc" -> R.drawable.mc_card_icon
    "american express", "american_express", "amex" -> R.drawable.amex_card_icon
    else -> R.drawable.card_icon
}