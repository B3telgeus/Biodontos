package com.example.biodontos.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.domain.usecase.auth.LoginUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val _events = Channel<LoginEvent>()
    val events = _events.receiveAsFlow()

    fun onEmailChange(email: String) {
        val error = if (email.contains(" ")) "El correo no puede contener espacios (carácter inválido)" else null
        _state.update { it.copy(email = email, emailError = error, error = null) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, passwordError = null, error = null) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login() {
        val email = _state.value.email.trim()
        val password = _state.value.password

        var hasError = false
        if (email.isBlank() || !email.contains("@")) {
            _state.update { it.copy(emailError = "Ingresa un correo electrónico válido") }
            hasError = true
        }
        if (password.isBlank()) {
            _state.update { it.copy(passwordError = "Ingresa tu contraseña") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = loginUseCase(email, password)
            result.onSuccess { session ->
                _state.update { it.copy(isLoading = false) }
                _events.send(LoginEvent.LoginSuccess(session))
            }.onFailure { ex ->
                val errorMsg = ex.toUserFriendlyMessage("Usuario o contraseña incorrectos")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = errorMsg,
                        emailError = if (errorMsg.contains("incorrectos", ignoreCase = true) || errorMsg.contains("verif", ignoreCase = true)) "Verifica tu correo" else null,
                        passwordError = if (errorMsg.contains("incorrectos", ignoreCase = true) || errorMsg.contains("verif", ignoreCase = true)) "Verifica tu contraseña" else null
                    )
                }
            }
        }
    }
}
