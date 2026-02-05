// AddCardScreen.kt
@file:Suppress("UnusedImport")

package com.example.parqueame.ui.billetera

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.session.SessionStore
import com.example.parqueame.ui.common_components.GradientIcon
import com.example.parqueame.ui.common_components.rememberTopErrorBanner
import com.example.parqueame.ui.common_components.rememberTopSuccessBanner
import com.example.parqueame.ui.theme.DmSans
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethod   // <- BillingDetails está aquí en tu SDK
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.rememberPaymentLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    navController: NavController,
    onCardAdded: () -> Unit
) {
    val context = LocalContext.current
    val vm: AddCardViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val showSuccess = rememberTopSuccessBanner()
    val showError = rememberTopErrorBanner()

    // ---- Estados del form
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") } // MM/AA
    var cvc by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var showExpiryHelp by remember { mutableStateOf(false) }
    var showCvvHelp by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val digits = number.filter(Char::isDigit)
    val brand = remember(digits) { detectBrand(digits) }

    val canSubmit = remember(number, name, expiry, cvc, isSubmitting) {
        !isSubmitting &&
                digits.length in 15..19 &&
                name.isNotBlank() &&
                expiry.matches(Regex("""^\d{2}/\d{2}$""")) &&
                cvc.length in 3..4
    }

    // ---- userId de la sesión
    val session = remember { SessionStore(context) }
    val userIdState by session.userId.collectAsStateWithLifecycle(initialValue = null)

    var publishableKey by remember { mutableStateOf<String?>(null) }
    var customerId by remember { mutableStateOf<String?>(null) }
    var isBootstrapping by remember { mutableStateOf(true) }

    // Bootstrap de Stripe
    LaunchedEffect(userIdState) {
        val uid = userIdState.orEmpty()
        if (uid.isBlank()) {
            isBootstrapping = false
            return@LaunchedEffect
        }
        try {
            val resp = vm.bootstrap(uid)
            if (resp.isSuccessful && resp.body() != null) {
                val data = resp.body()!!
                publishableKey = data.publishableKey
                customerId = data.customerId
                // Inicializa configuración global
                PaymentConfiguration.init(context, data.publishableKey)
            } else {
                showError(context.getString(R.string.stripe_bootstrap_error_http_es, resp.code()))
            }
        } catch (t: Throwable) {
            showError(context.getString(R.string.network_error_with_reason, t.localizedMessage ?: ""))
        } finally {
            isBootstrapping = false
        }
    }

    // Crea el launcher SOLO cuando tengas publishableKey (firma de tu SDK lo exige)
    val paymentLauncher =
        if (!publishableKey.isNullOrBlank()) {
            rememberPaymentLauncher(
                publishableKey = publishableKey!!,
                stripeAccountId = null,
                callback = { result: PaymentResult ->
                    isSubmitting = false
                    when (result) {
                        is PaymentResult.Completed -> {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("cardAdded", true)
                            if (nickname.isNotBlank()) {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("newNickname", nickname)
                            }
                            onCardAdded()
                            showSuccess(context.getString(R.string.add_payment_method_success))
                        }
                        is PaymentResult.Canceled -> Unit
                        is PaymentResult.Failed -> {
                            showError(context.getString(R.string.add_payment_method_error))
                        }
                    }
                }
            )
        } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            stringResource(R.string.add_payment_method_title),
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
                        imageVector = ImageVector.vectorResource(id = R.drawable.back_icon),
                        contentDescription = stringResource(R.string.close_action),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { navController.popBackStack() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
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
            if (isBootstrapping) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Número
            Text(
                stringResource(R.string.card_number_label),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = DmSans,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            TextField(
                value = number,
                onValueChange = { raw ->
                    val only = raw.filter(Char::isDigit).take(19)
                    number = only.chunked(4).joinToString(" ")
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSans),
                placeholder = {
                    Text(
                        stringResource(R.string.card_number_placeholder),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = DmSans, color = FieldPlaceholder
                        )
                    )
                },
                leadingIcon = { LeadingBrandIcon(brand.takeIf { digits.isNotEmpty() }) }
            )

            // Nombre
            FilledField(
                label = stringResource(R.string.card_name_label),
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.card_name_placeholder),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Expiry Date
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.expiry_date_label),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DmSans,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
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
                        placeholder = {
                            Text(stringResource(R.string.expiry_date_placeholder), color = FieldPlaceholder, fontFamily = DmSans)
                        },
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
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // CVV
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.cvv_label),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = DmSans)
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
                        placeholder = {
                            Text(stringResource(R.string.cvv_placeholder), color = FieldPlaceholder, fontFamily = DmSans)
                        },
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
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            }

            HelpModalBottomSheet(
                show = showExpiryHelp,
                title = stringResource(R.string.expiry_date_label),
                description = stringResource(R.string.expiry_date_help_description),
                imageRes = R.drawable.fechavto_help,
                onDismiss = { showExpiryHelp = false }
            )

            HelpModalBottomSheet(
                show = showCvvHelp,
                title = stringResource(R.string.cvv_label),
                description = stringResource(R.string.cvv_help_description),
                imageRes = R.drawable.cvv_help,
                onDismiss = { showCvvHelp = false }
            )

            // Sobrenombre (opcional)
            FilledField(
                label = stringResource(R.string.nickname_optional_label),
                value = nickname,
                onValueChange = { nickname = it },
                placeholder = stringResource(R.string.nickname_placeholder),
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val cid = customerId
                    val pk = publishableKey
                    if (cid.isNullOrBlank() || pk.isNullOrBlank()) {
                        showError(context.getString(R.string.stripe_not_ready_error))
                        return@Button
                    }
                    if (paymentLauncher == null) {
                        showError(context.getString(R.string.stripe_not_ready_error))
                        return@Button
                    }

                    // MM/AA → enteros
                    val parts = expiry.split("/")
                    val mm = parts.getOrNull(0)?.toIntOrNull()
                    val yy = parts.getOrNull(1)?.let { "20$it".toIntOrNull() }

                    if (mm == null || yy == null) {
                        showError(context.getString(R.string.invalid_date_error))
                        return@Button
                    }

                    // Construye PaymentMethod con datos de facturación (name)
                    val cardParams = PaymentMethodCreateParams.Card.Builder()
                        .setNumber(digits)
                        .setExpiryMonth(mm)
                        .setExpiryYear(yy)
                        .setCvc(cvc)
                        .build()

                    val billing = PaymentMethod.BillingDetails.Builder()
                        .setName(name)
                        .build()


                    // 👇 Añade el sobrenombre al metadata
                    val metadata: Map<String, String> =
                        if (nickname.isNotBlank()) mapOf("nickname" to nickname) else emptyMap()

//                  val pmParams = PaymentMethodCreateParams.create(cardParams, billing)
                    val pmParams = PaymentMethodCreateParams.create(cardParams, billing, metadata)

                    isSubmitting = true
                    scope.launch {
                        val resp = vm.createSetupIntent(cid)
                        val secret = resp.body()?.clientSecret
                        if (resp.isSuccessful && !secret.isNullOrBlank()) {
                            val confirmParams = ConfirmSetupIntentParams.create(pmParams, secret)
                            paymentLauncher.confirm(confirmParams)
                        } else {
                            isSubmitting = false
                            showError(context.getString(R.string.setup_intent_creation_failed_http_es, resp.code()))
                        }
                    }
                },
                enabled = canSubmit && !isBootstrapping,
                modifier = Modifier
                    .width(220.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2DA6FF), Color(0xFF0B55D6))
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isSubmitting) stringResource(R.string.processing_ellipsis_es)
                        else stringResource(R.string.add_card_action),
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
}

/* ===== Helpers UI ===== */

@Composable
private fun brandMenuIconRes(brand: String): Int {
    val amex = stringResource(R.string.brand_amex)
    val mastercard = stringResource(R.string.brand_mastercard)
    val visa = stringResource(R.string.brand_visa)
    return when (brand) {
        visa -> R.drawable.visa_card_icon
        mastercard -> R.drawable.mc_card_icon
        amex -> R.drawable.amex_card_icon
        else -> R.drawable.card_icon
    }
}

private fun detectBrand(digits: String): String = when {
    digits.startsWith("34") || digits.startsWith("37") -> "American Express"
    digits.startsWith("5") -> "Mastercard"
    digits.startsWith("4") -> "Visa"
    else -> "Desconocida"
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
private fun LeadingBrandIcon(brand: String?) {
    if (brand.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(FieldIconBg, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.card_icon),
                contentDescription = null,
                tint = FieldPlaceholder,
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        Icon(
            painter = painterResource(brandMenuIconRes(brand)),
            contentDescription = brand,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
    }
}

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
    visualTransformation: VisualTransformation = VisualTransformation.None,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpModalBottomSheet(
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