package com.example.parqueame.ui.register.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Role {
    CONDUCTOR,
    ADMINISTRADOR
}

class RegisterChoiceViewModel : ViewModel() {
    private val _selectedRole = MutableStateFlow<Role?>(null)
    val selectedRole: StateFlow<Role?> = _selectedRole

    fun selectRole(role: Role) {
        _selectedRole.value = role
    }
}
