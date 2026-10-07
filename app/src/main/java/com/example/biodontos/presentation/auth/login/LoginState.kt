package com.example.biodontos.presentation.auth.login

import com.example.biodontos.domain.model.UserSession

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false
)

sealed interface LoginEvent {
    data class LoginSuccess(val session: UserSession) : LoginEvent
}
