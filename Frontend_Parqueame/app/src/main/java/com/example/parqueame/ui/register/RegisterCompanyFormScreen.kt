package com.example.parqueame.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parqueame.R
import com.example.parqueame.models.Usuario
import com.example.parqueame.ui.common_components.*
import com.example.parqueame.ui.register.viewmodels.RegisterCompanyFormViewModel
import com.example.parqueame.ui.theme.DmSans
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.GradientEnd
import com.example.parqueame.ui.theme.GradientStart
import com.example.parqueame.ui.theme.RtlRomman
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@Composable
fun RegisterCompanyFormScreen(
    viewModel: RegisterCompanyFormViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onLoginClick: () -> Unit,
    onRegistrationSuccess: () -> Unit,
) {
    val companyName by viewModel.companyName.collectAsState()
    val rnc by viewModel.rnc.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val termsAccepted by viewModel.termsAccepted.collectAsState()
    val registerStatus by viewModel.registerStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val rncErrorMessage by viewModel.rncErrorMessage.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showTermsModal by remember { mutableStateOf(false) }
    var showPasswordInfo by remember { mutableStateOf(false) }

    val mostrarError = rememberTopErrorBanner()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Manejo de registro exitoso mejorado
    LaunchedEffect(registerStatus) {
        when {
            registerStatus.contains("creado", true) ||
                    registerStatus.contains("exitoso", true) ||
                    registerStatus.contains("successful", true) ||
                    registerStatus.contains("success", true) -> {
                onRegistrationSuccess()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearRegisterStatus() }
    }

    fun isEmailValid(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
        return emailRegex.matches(email)
    }

    fun isPasswordValid(password: String): Boolean {
        val specialCharRegex = "[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/\\\\|]".toRegex()
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }

        return password.length >= 8 &&
                specialCharRegex.containsMatchIn(password) &&
                hasUppercase &&
                hasLowercase &&
                hasDigit
    }

    fun getPasswordStrengthMessage(password: String): String {
        val issues = mutableListOf<String>()

        if (password.length < 8) issues.add(context.getString(R.string.pw_need_len8))
        if (!password.any { it.isUpperCase() }) issues.add(context.getString(R.string.pw_need_upper))
        if (!password.any { it.isLowerCase() }) issues.add(context.getString(R.string.pw_need_lower))
        if (!password.any { it.isDigit() }) issues.add(context.getString(R.string.pw_need_digit))
        if (!"[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/\\\\|]".toRegex().containsMatchIn(password)) {
            issues.add(context.getString(R.string.pw_need_special))
        }

        return if (issues.isEmpty()) {
            context.getString(R.string.password_strong)
        } else {
            context.getString(R.string.password_missing_prefix, issues.joinToString(", "))
        }
    }

    fun handleRegister() {
        coroutineScope.launch {
            // Validaciones mejoradas
            when {
                companyName.isBlank() -> {
                    mostrarError(context.getString(R.string.err_company_name_required))
                }
                companyName.length < 3 -> {
                    mostrarError(context.getString(R.string.err_company_name_len))
                }
                rnc.isBlank() -> {
                    mostrarError(context.getString(R.string.err_rnc_required))
                }
                rnc.length != 9 && rnc.length != 11 -> {
                    mostrarError(context.getString(R.string.err_rnc_len))
                }
                email.isBlank() -> {
                    mostrarError(context.getString(R.string.err_email_required))
                }
                !isEmailValid(email) -> {
                    mostrarError(context.getString(R.string.err_email_invalid))
                }
                password.isBlank() -> {
                    mostrarError(context.getString(R.string.err_password_required))
                }
                !isPasswordValid(password) -> {
                    mostrarError(getPasswordStrengthMessage(password))
                }
                confirmPassword.isBlank() -> {
                    mostrarError(context.getString(R.string.err_confirm_password_required))
                }
                password != confirmPassword -> {
                    mostrarError(context.getString(R.string.err_passwords_not_equal))
                }
                !termsAccepted -> {
                    mostrarError(context.getString(R.string.err_terms_required))
                }
                else -> {
                    // Validar RNC con DGII
                    val rncValido = viewModel.validarRncParaRegistro { error ->
                        mostrarError(context.getString(R.string.err_rnc_invalid_prefix, error))
                    }

                    if (rncValido) {
                        // Crear usuario con datos validados
                        val usuario = Usuario(
                            nombre = companyName.trim(),
                            correo = email.trim().lowercase(),
                            contrasena = password,
                            tipo = "EMPRESA",
                            tipoDocumento = if (rnc.length == 9) "RNC" else "CEDULA",
                            documento = rnc,
                            imagenesURL = "",
                            estado = true
                        )

                        viewModel.registerCompany(usuario)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()

        .background(Color.White),


    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,


        ) {
            // Título
            Text(
                text = stringResource(R.string.brand_title),
                fontSize = 40.sp,
                fontFamily = RtlRomman,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                style = TextStyle(brush = GradientBrush),
                modifier = Modifier.fillMaxWidth()

            )

            Spacer(modifier = Modifier.height(32.dp))

            // Campo: Nombre de Empresa
            LabelledTextField(
                label = stringResource(R.string.company_name_label),
                value = companyName,
                onValueChange = viewModel::onCompanyNameChange,
                placeholder = stringResource(R.string.company_name_placeholder),
                keyboardType = KeyboardType.Text,

            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: RNC con indicador de validación
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
//                    Text(
//                        "RNC / Cédula *",
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Medium,
//                        fontFamily = DmSans,
//                        color = Color.Black,
//                        //modifier = Modifier.padding(start = 12.dp)
//                    )

                    // Indicador de validación
//                    if (rnc.isNotBlank()) {
//                        //Spacer(modifier = Modifier.width(8.dp))
//                        when {
//                            rncErrorMessage.contains("Consultando") -> {
//                                Text("⏳", fontSize = 12.sp, color = Color.Blue)
//                            }
//                            rncErrorMessage.isBlank() && rnc.length >= 9 -> {
//                                Text("✅", fontSize = 12.sp, color = Color.Green)
//                            }
//                            rncErrorMessage.isNotBlank() && !rncErrorMessage.contains("Consultando") -> {
//                                Text("❌", fontSize = 12.sp, color = Color.Red)
//                            }
//                        }
//                    }
                }

                //Spacer(modifier = Modifier.height(2.dp))

                LabelledTextField(
                    label = stringResource(R.string.rnc_or_id_label),
                    value = rnc,
                    onValueChange = viewModel::onRncChange,
                    placeholder = stringResource(R.string.rnc_or_id_placeholder),
                    keyboardType = KeyboardType.Number
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Email
            LabelledTextField(
                label = stringResource(R.string.email_label),
                value = email,
                onValueChange = viewModel::onEmailChange,
                placeholder = stringResource(R.string.email_company_placeholder),
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Contraseña con indicador de fortaleza
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.password_label),
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = DmSans,
                        color = Color.Black,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.symbols_info),
                        contentDescription = stringResource(R.string.password_info_cd),
                        modifier = Modifier
                            .size(17.dp)
                            .clickable { showPasswordInfo = true },
                        tint = Color.Unspecified
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                PasswordTextField(
                    label = "",
                    value = password,
                    onValueChange = viewModel::onPasswordChange,
                    placeholder = stringResource(R.string.password_min8_placeholder),
                    passwordVisible = passwordVisible
                ) {
                    passwordVisible = !passwordVisible
                }

                // Indicador de fortaleza de contraseña
                if (password.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getPasswordStrengthMessage(password),
                        fontSize = 12.sp,
                        fontFamily = DmSans,
                        color = if (isPasswordValid(password)) Color.Green else Color(0xFFFF9800), // naranja
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Confirmar Contraseña
            Column(modifier = Modifier.fillMaxWidth()) {
                PasswordTextField(
                    label = stringResource(R.string.confirm_password_label),
                    value = confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    placeholder = stringResource(R.string.repeat_password_placeholder),
                    passwordVisible = confirmPasswordVisible
                ) {
                    confirmPasswordVisible = !confirmPasswordVisible
                }

                // Indicador de coincidencia
                if (confirmPassword.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val matchText = if (password == confirmPassword)
                        stringResource(R.string.pw_match_yes)
                    else
                        stringResource(R.string.pw_match_no)
                    Text(
                        text = matchText,
                        fontSize = 12.sp,
                        fontFamily = DmSans,
                        color = if (password == confirmPassword) Color.Green else Color.Red,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Términos y Condiciones
            GradientCheckboxRow(
                checked = termsAccepted,
                onCheckedChange = viewModel::onTermsAcceptedChange,
                text = "",
                onTermsClick = { showTermsModal = true }
            )

            Spacer(modifier = Modifier.height(25.dp))

            // Botón de Registro
            GradientButton(
                text = when {
                    isLoading -> stringResource(R.string.registering_ellipsis)
                    rncErrorMessage.contains("Consultando") -> stringResource(R.string.validating_rnc_ellipsis)
                    else -> stringResource(R.string.register_company_action)
                },
                onClick = { handleRegister() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = !isLoading && !rncErrorMessage.contains("Consultando")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Enlaces de navegación
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.already_have_account_q),
                    fontSize = 16.sp,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.login_here),
                    fontSize = 16.sp,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF115ED0),
                    modifier = Modifier.clickable(onClick = onLoginClick)
                )
            }
        }

        // Modales y banners
        PasswordInfoBox(
            showPasswordInfo = showPasswordInfo,
            onDismiss = { showPasswordInfo = false }
        )

        if (showTermsModal) {
            TermsAndConditionsModal(onClose = { showTermsModal = false })
        }

        // Banner de error de RNC (solo si no está consultando)
        if (rncErrorMessage.isNotBlank() && !rncErrorMessage.contains("Consultando")) {
            TopErrorBanner(show = true, message = rncErrorMessage)
        }

        // Banner de estado de registro (sin isError para que coincida con tu firma actual)
        if (registerStatus.isNotBlank()) {
            TopErrorBanner(
                show = true,
                message = registerStatus
            )
        }
    }
}
