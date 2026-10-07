package com.example.biodontos.presentation.auth.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.core.util.Validators
import com.example.biodontos.domain.usecase.auth.RegisterDoctorUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DoctorRegistrationViewModel(
    private val registerDoctorUseCase: RegisterDoctorUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DoctorRegistrationState())
    val state = _state.asStateFlow()

    private val _events = Channel<DoctorRegistrationEvent>()
    val events = _events.receiveAsFlow()

    fun onFirstNameChange(value: String) {
        val error = if (value.isNotEmpty() && !Validators.isValidName(value)) {
            "Solo se permiten letras y espacios (carácter inválido)"
        } else null
        _state.update { it.copy(firstName = value, firstNameError = error) }
    }

    fun onLastNameChange(value: String) {
        val error = if (value.isNotEmpty() && !Validators.isValidName(value)) {
            "Solo se permiten letras y espacios (carácter inválido)"
        } else null
        _state.update { it.copy(lastName = value, lastNameError = error) }
    }

    fun onLicenseNumberChange(value: String) {
        val error = if (value.isNotEmpty() && !Validators.isValidLicenseNumber(value)) {
            "Solo se permiten letras, números y guiones (carácter inválido)"
        } else null
        _state.update { it.copy(licenseNumber = value, licenseError = error) }
    }

    fun onSpecialtyChange(value: String) = _state.update { it.copy(specialty = value) }

    fun onEmailChange(value: String) {
        val error = if (value.isNotEmpty() && value.contains(" ")) {
            "El correo no puede contener espacios"
        } else null
        _state.update { it.copy(email = value, emailError = error) }
    }

    fun onPhoneChange(value: String) {
        val error = if (value.isNotEmpty() && !Validators.isValidPhone(value)) {
            "Solo se permiten números (carácter inválido)"
        } else null
        _state.update { it.copy(phone = value, phoneError = error) }
    }

    fun onPasswordChange(value: String) {
        val confirmError = if (_state.value.confirmPassword.isNotEmpty() && !Validators.doPasswordsMatch(value, _state.value.confirmPassword)) {
            "Las contraseñas no coinciden"
        } else null
        _state.update {
            it.copy(
                password = value,
                passwordError = null,
                confirmPasswordError = confirmError
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        val error = if (value.isNotEmpty() && !Validators.doPasswordsMatch(_state.value.password, value)) {
            "Las contraseñas no coinciden"
        } else null
        _state.update { it.copy(confirmPassword = value, confirmPasswordError = error) }
    }

    fun togglePasswordVisibility() = _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun previousStep(onBackClick: () -> Unit) {
        val current = _state.value.currentStep
        if (current > 1) {
            _state.update { it.copy(currentStep = current - 1, error = null) }
        } else {
            onBackClick()
        }
    }

    fun nextStep() {
        val s = _state.value
        when (s.currentStep) {
            1 -> {
                // Validar Identidad y Acceso (Paso 1)
                var hasError = false
                var firstNameErr = s.firstNameError
                var lastNameErr = s.lastNameError
                var emailErr = s.emailError
                var phoneErr = s.phoneError
                var passErr = s.passwordError
                var confirmErr = s.confirmPasswordError

                if (s.firstName.isBlank()) {
                    firstNameErr = "El nombre es obligatorio"
                    hasError = true
                } else if (!Validators.isValidName(s.firstName)) {
                    firstNameErr = "Solo se permiten letras y espacios"
                    hasError = true
                }

                if (s.lastName.isBlank()) {
                    lastNameErr = "El apellido es obligatorio"
                    hasError = true
                } else if (!Validators.isValidName(s.lastName)) {
                    lastNameErr = "Solo se permiten letras y espacios"
                    hasError = true
                }

                if (s.email.isBlank() || !Validators.isValidEmail(s.email)) {
                    emailErr = "Ingresa un correo electrónico válido"
                    hasError = true
                }

                if (s.phone.isNotBlank() && !Validators.isValidPhone(s.phone)) {
                    phoneErr = "Solo dígitos numéricos"
                    hasError = true
                }

                if (!Validators.isValidPassword(s.password)) {
                    passErr = "La contraseña debe tener mínimo 6 caracteres"
                    hasError = true
                }

                if (!Validators.doPasswordsMatch(s.password, s.confirmPassword)) {
                    confirmErr = "Las contraseñas no coinciden"
                    hasError = true
                }

                _state.update {
                    it.copy(
                        firstNameError = firstNameErr,
                        lastNameError = lastNameErr,
                        emailError = emailErr,
                        phoneError = phoneErr,
                        passwordError = passErr,
                        confirmPasswordError = confirmErr
                    )
                }

                if (!hasError) {
                    _state.update { it.copy(currentStep = 2, error = null) }
                }
            }
            2 -> {
                // Validar Perfil Profesional (Paso 2)
                var hasError = false
                var licenseErr = s.licenseError

                if (s.licenseNumber.isBlank()) {
                    licenseErr = "Ingresa tu número de registro o cédula médica"
                    hasError = true
                } else if (!Validators.isValidLicenseNumber(s.licenseNumber)) {
                    licenseErr = "Formato de cédula médica inválido"
                    hasError = true
                }

                _state.update { it.copy(licenseError = licenseErr) }

                if (!hasError) {
                    register()
                }
            }
        }
    }

    fun register() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = registerDoctorUseCase(
                email = s.email.trim(),
                password = s.password,
                firstName = s.firstName.trim(),
                lastName = s.lastName.trim(),
                licenseNumber = s.licenseNumber.trim(),
                specialty = s.specialty,
                phone = s.phone.trim().ifBlank { null }
            )

            result.onSuccess { session ->
                _state.update { it.copy(isLoading = false) }
                _events.send(DoctorRegistrationEvent.RegisteredSuccess(session))
            }.onFailure { ex ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = ex.message ?: "Error al registrar doctor en el sistema"
                    )
                }
            }
        }
    }
}
