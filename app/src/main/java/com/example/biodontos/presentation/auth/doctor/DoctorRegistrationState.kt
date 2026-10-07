package com.example.biodontos.presentation.auth.doctor

import com.example.biodontos.domain.model.UserSession

data class DoctorRegistrationState(
    val currentStep: Int = 1,
    val firstName: String = "",
    val lastName: String = "",
    val licenseNumber: String = "",
    val specialty: String = "Odontología General",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val licenseError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

sealed interface DoctorRegistrationEvent {
    data class RegisteredSuccess(val session: UserSession) : DoctorRegistrationEvent
}
