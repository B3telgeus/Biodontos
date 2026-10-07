package com.example.biodontos.presentation.auth.patient

import com.example.biodontos.domain.model.UserSession

data class PatientSignUpState(
    val currentStep: Int = 1,
    val firstName: String = "",
    val lastName: String = "",
    val dniCedula: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val termsAccepted: Boolean = true,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val dniError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

sealed interface PatientSignUpEvent {
    data class RegisteredSuccess(val session: UserSession) : PatientSignUpEvent
}
