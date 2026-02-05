package com.example.parqueame.ui.register

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parqueame.R
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.ui.common_components.*
import com.example.parqueame.ui.register.logic.handleRegister
import com.example.parqueame.ui.register.viewmodels.RegisterIndividualFormViewModel
import com.example.parqueame.ui.theme.DmSans
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.RtlRomman
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext

@Composable
fun RegisterIndividualFormScreen(
    viewModel: RegisterIndividualFormViewModel = viewModel(),
    onLoginClick: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showPasswordInfo by remember { mutableStateOf(false) }
    var showTermsModal by remember { mutableStateOf(false) }

    var cedulaValidationJob by remember { mutableStateOf<Job?>(null) }
    var isCedulaValid by remember { mutableStateOf<Boolean?>(null) }

    val registerStatus by viewModel.registerStatus
    val isLoading by viewModel.isLoading

    val mostrarError = rememberTopErrorBanner()
    val mostrarExito = rememberTopSuccessBanner() // ✅ NUEVO
    val coroutineScope = rememberCoroutineScope()
    val errEmpty = stringResource(R.string.err_fields_empty)
    val errCedulaLen = stringResource(R.string.err_cedula_len)
    val errCedulaInvalid = stringResource(R.string.err_cedula_invalid)
    val errPwNotEqual = stringResource(R.string.err_passwords_not_equal)

    fun isValidCedulaFormat(cedula: String): Boolean {
        val cleaned = cedula.replace("-", "").replace(" ", "")
        return cleaned.length == 11 && cleaned.all { it.isDigit() }
    }

    suspend fun validateCedulaWithService(cedula: String): Boolean {
        return try {
            val response = withTimeoutOrNull(5000) {
                RetrofitInstance.apiService.validarCedula(cedula)
            }

            when {
                response == null -> false
                response.isSuccessful -> {
                    val body = response.body()
                    body != null && body.error == null
                }
                else -> false
            }
        } catch (e: Exception) {
            println("Error al validar cédula: ${e.message}")
            false
        }
    }

    LaunchedEffect(cedula) {
        cedulaValidationJob?.cancel()

        if (cedula.isBlank()) {
            isCedulaValid = null
            return@LaunchedEffect
        }

        if (!isValidCedulaFormat(cedula)) {
            isCedulaValid = false
            return@LaunchedEffect
        }

        cedulaValidationJob = launch {
            delay(1000)
            val isValid = validateCedulaWithService(cedula)
            isCedulaValid = isValid
        }
    }

    // ✅ NUEVO: Mostrar banner de éxito y redirigir a login
    LaunchedEffect(registerStatus) {
        if (registerStatus.contains("correctamente", ignoreCase = true) ||
            registerStatus.contains("exitoso", ignoreCase = true)
        ) {
            mostrarExito("¡Registro exitoso!")
            delay(3000)
            onLoginClick()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearRegisterStatus()
            cedulaValidationJob?.cancel()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().zIndex(0f).padding(horizontal = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .zIndex(0f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.brand_title),
                fontSize = 40.sp,
                fontFamily = RtlRomman,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                style = TextStyle(brush = GradientBrush),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
            LabelledTextField(
                label = stringResource(R.string.full_name_label),
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.full_name_placeholder),
                keyboardType = KeyboardType.Text
            )
            Spacer(Modifier.height(16.dp))
            LabelledTextField(
                label = stringResource(R.string.cedula_label),
                value = cedula,
                onValueChange = { cedula = it },
                placeholder = stringResource(R.string.cedula_placeholder),
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(16.dp))
            LabelledTextField(
                label = stringResource(R.string.email_label),
                value = email,
                onValueChange = { email = it },
                placeholder = stringResource(R.string.email_placeholder_user),
                keyboardType = KeyboardType.Email
            )

            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.password_label), fontSize = 16.5.sp, fontWeight = FontWeight.Light, fontFamily = DmSans, modifier = Modifier.padding(start = 12.dp))
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        painter = painterResource(R.drawable.symbols_info),
                        contentDescription = stringResource(R.string.password_info_cd),
                        modifier = Modifier.size(17.dp).clickable { showPasswordInfo = true },
                        tint = Color.Unspecified
                    )
                }
                Spacer(Modifier.height(10.dp))
                PasswordTextField("", password, { password = it }, placeholder = stringResource(R.string.password_placeholder_simple), passwordVisible, { passwordVisible = !passwordVisible })
            }

            Spacer(Modifier.height(16.dp))
            PasswordTextField(label = stringResource(R.string.confirm_password_label), confirmPassword, { confirmPassword = it }, placeholder = stringResource(R.string.confirm_password_placeholder), confirmPasswordVisible, { confirmPasswordVisible = !confirmPasswordVisible })

            Spacer(Modifier.height(16.dp))
            GradientCheckboxRow(termsAccepted, { termsAccepted = it }, "", { showTermsModal = true })

            Spacer(Modifier.height(25.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GradientButton(
                    text = if (isLoading) stringResource(R.string.registering_ellipsis) else stringResource(R.string.register_action),
                    onClick = {
                        coroutineScope.launch {
                            when {
                                name.isBlank() || cedula.isBlank() || email.isBlank() ||
                                        password.isBlank() || confirmPassword.isBlank() -> {
                                    mostrarError(errEmpty)
                                    return@launch
                                }
                                !isValidCedulaFormat(cedula) -> {
                                    mostrarError(errCedulaLen)
                                    return@launch
                                }
                                isCedulaValid != true -> {
                                    if (isCedulaValid == null) {
                                        val isValid = validateCedulaWithService(cedula)
                                        isCedulaValid = isValid
                                        if (!isValid) {
                                            mostrarError(errCedulaInvalid)
                                            return@launch
                                        }
                                    } else {
                                        mostrarError(errCedulaInvalid)
                                        return@launch
                                    }
                                }
                                password != confirmPassword -> {
                                    mostrarError(errPwNotEqual)
                                    return@launch
                                }
                            }

                            handleRegister(
                                name, cedula, email, password, confirmPassword,
                                termsAccepted, viewModel, mostrarError, mostrarError
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    enabled = !isLoading
                )
            }

            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.have_account_q), fontSize = 16.sp, fontFamily = DmSans)
                Text(stringResource(R.string.login_here), fontSize = 16.sp, fontFamily = DmSans, color = Color(0xFF115ED0), modifier = Modifier.clickable(onClick = onLoginClick))
            }
        }

        if (showPasswordInfo) {
            PasswordInfoBox(showPasswordInfo) {
                showPasswordInfo = false
            }
        }

        if (showTermsModal) {
            TermsAndConditionsModal {
                showTermsModal = false
            }
        }
    }
}
