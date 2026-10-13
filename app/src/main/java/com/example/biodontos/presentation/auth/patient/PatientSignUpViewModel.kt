package com.example.biodontos.presentation.auth.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.core.util.Validators
import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.domain.usecase.auth.RegisterPatientAuthUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PatientSignUpViewModel(
    private val registerPatientAuthUseCase: RegisterPatientAuthUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PatientSignUpState())
    val state = _state.asStateFlow()

    private val _events = Channel<PatientSignUpEvent>()
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

    fun onDniChange(value: String) {
        val error = if (value.isNotEmpty() && !value.matches(Regex("^[0-9]*$"))) {
            "Solo se permiten números (carácter inválido)"
        } else null
        _state.update { it.copy(dniCedula = value, dniError = error) }
    }

    fun onEmailChange(value: String) {
        val error = if (value.isNotEmpty() && value.contains(" ")) {
            "El correo no puede contener espacios"
        } else null
        _state.update { it.copy(email = value, emailError = error) }
    }

    fun onPhoneChange(value: String) {
        val error = if (value.isNotEmpty() && !value.matches(Regex("^[0-9+]*$"))) {
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

    fun onTermsAcceptedChange(accepted: Boolean) {
        _state.update { it.copy(termsAccepted = accepted) }
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
                // Validar datos personales del Paso 1
                var hasError = false
                var firstNameErr = s.firstNameError
                var lastNameErr = s.lastNameError
                var dniErr = s.dniError
                var phoneErr = s.phoneError

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

                if (s.dniCedula.isBlank()) {
                    dniErr = "El DNI / Documento es obligatorio"
                    hasError = true
                } else if (!Validators.isValidDni(s.dniCedula)) {
                    dniErr = "Solo se permiten números (entre 6 y 12 dígitos)"
                    hasError = true
                }

                if (s.phone.isNotBlank() && !Validators.isValidPhone(s.phone)) {
                    phoneErr = "Número telefónico inválido (solo dígitos)"
                    hasError = true
                }

                _state.update {
                    it.copy(
                        firstNameError = firstNameErr,
                        lastNameError = lastNameErr,
                        dniError = dniErr,
                        phoneError = phoneErr
                    )
                }

                if (!hasError) {
                    _state.update { it.copy(currentStep = 2, error = null) }
                }
            }
            2 -> {
                // Validar credenciales de acceso del Paso 2
                var hasError = false
                var emailErr = s.emailError
                var passErr = s.passwordError
                var confirmErr = s.confirmPasswordError

                if (s.email.isBlank() || !Validators.isValidEmail(s.email)) {
                    emailErr = "Ingresa un correo electrónico válido"
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
                        emailError = emailErr,
                        passwordError = passErr,
                        confirmPasswordError = confirmErr
                    )
                }

                if (!hasError) {
                    _state.update { it.copy(currentStep = 3, error = null) }
                }
            }
            3 -> {
                // Paso 3: Términos y confirmación -> Ejecutar registro
                if (!s.termsAccepted) {
                    _state.update { it.copy(error = "Debes aceptar la política de tratamiento de datos para continuar") }
                    return
                }
                register()
            }
        }
    }

    fun register() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = registerPatientAuthUseCase(
                email = s.email.trim(),
                password = s.password,
                firstName = s.firstName.trim(),
                lastName = s.lastName.trim(),
                dniCedula = s.dniCedula.trim(),
                phone = s.phone.trim().ifBlank { null }
            )

            result.onSuccess { session ->
                _state.update { it.copy(isLoading = false, isSuccess = true) }
                _events.send(PatientSignUpEvent.RegisteredSuccess(session))
            }.onFailure { ex ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = ex.toUserFriendlyMessage("Error al registrar la cuenta de paciente")
                    )
                }
            }
        }
    }
}
