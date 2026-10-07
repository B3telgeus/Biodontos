package com.example.biodontos.presentation.auth.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.usecase.auth.ResetPasswordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state = _state.asStateFlow()

    fun onEmailChange(value: String) {
        val error = if (value.contains(" ")) "El correo no puede contener espacios (carácter inválido)" else null
        _state.update { it.copy(email = value, emailError = error, error = null) }
    }

    fun submit() {
        val email = _state.value.email.trim()
        if (email.isBlank() || !email.contains("@")) {
            _state.update { it.copy(emailError = "Ingresa un correo electrónico válido") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = resetPasswordUseCase(email)
            result.onSuccess {
                _state.update { it.copy(isLoading = false, isSubmittedSuccess = true) }
            }.onFailure { ex ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = ex.message ?: "No se pudo enviar el correo de recuperación. Verifica la dirección ingresada."
                    )
                }
            }
        }
    }

    fun resetState() {
        _state.update { ForgotPasswordState() }
    }
}
