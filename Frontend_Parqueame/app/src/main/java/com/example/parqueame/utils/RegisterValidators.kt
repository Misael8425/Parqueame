package com.example.parqueame.utils

fun isCedulaValid(cedula: String): Boolean {
    val clean = cedula.replace("-", "")
    return clean.length == 11 && clean.all { it.isDigit() }
}

fun isPasswordValid(password: String): Boolean {
    val specialChar = Regex("[!@#\$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/\\\\|]")
    val hasUpper = password.any { it.isUpperCase() }
    return password.length >= 8 && specialChar.containsMatchIn(password) && hasUpper
}
