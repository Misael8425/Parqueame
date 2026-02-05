package com.example.parqueame.ui.register.logic

import com.example.parqueame.models.Usuario
import com.example.parqueame.ui.register.viewmodels.RegisterIndividualFormViewModel
import com.example.parqueame.utils.isCedulaValid
import com.example.parqueame.utils.isPasswordValid

fun handleRegister(
    name: String,
    cedula: String,
    email: String,
    password: String,
    confirmPassword: String,
    termsAccepted: Boolean,
    viewModel: RegisterIndividualFormViewModel,
    mostrarError: (String) -> Unit,
    onError: (String) -> Unit // NUEVO parámetro
) {
    when {
        name.isBlank() || cedula.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
            mostrarError("Los campos no pueden estar vacíos.")
        }
        !isCedulaValid(cedula) -> {
            mostrarError("Cédula inválida")
        }
        !isPasswordValid(password) -> {
            mostrarError("La contraseña debe contener 8 caracteres o más, mayúsculas y al menos un carácter especial (*!#\$%&+-)")
        }
        password != confirmPassword -> {
            mostrarError("Las contraseñas no coinciden.")
        }
        !termsAccepted -> {
            mostrarError("Debes aceptar los términos y condiciones")
        }
        else -> {
            viewModel.registerUser(
                Usuario(
                    nombre = name,
                    correo = email,
                    contrasena = password,
                    tipo = "CONDUCTOR",
                    tipoDocumento = "CEDULA",
                    documento = cedula,
                    imagenesURL = "",
                    estado = true
                ),
                onError
            )
        }
    }
}
