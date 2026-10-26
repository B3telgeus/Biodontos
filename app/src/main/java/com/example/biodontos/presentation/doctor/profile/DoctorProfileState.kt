package com.example.biodontos.presentation.doctor.profile

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.Doctor

@Immutable
data class DoctorProfileState(
    val doctor: Doctor? = null,
    // Professional Profile Form Fields
    val firstName: String = "",
    val lastName: String = "",
    val specialty: String = "",
    val licenseNumber: String = "",
    val phone: String = "",
    val bio: String = "",
    val photoUrl: String? = null,
    val email: String = "",
    // Security Form Fields
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    // Status Flags
    val isLoading: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isUpdatingPassword: Boolean = false,
    val isSendingResetEmail: Boolean = false,
    val showLogoutConfirmation: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed interface DoctorProfileEvent {
    data object LoadProfile : DoctorProfileEvent
    data class UpdateFirstName(val value: String) : DoctorProfileEvent
    data class UpdateLastName(val value: String) : DoctorProfileEvent
    data class UpdateSpecialty(val value: String) : DoctorProfileEvent
    data class UpdateLicenseNumber(val value: String) : DoctorProfileEvent
    data class UpdatePhone(val value: String) : DoctorProfileEvent
    data class UpdateBio(val value: String) : DoctorProfileEvent
    data class UpdatePhotoUrl(val photoUrl: String) : DoctorProfileEvent
    data object SaveProfile : DoctorProfileEvent
    data class UpdateNewPassword(val value: String) : DoctorProfileEvent
    data class UpdateConfirmPassword(val value: String) : DoctorProfileEvent
    data object TogglePasswordVisibility : DoctorProfileEvent
    data object ToggleConfirmPasswordVisibility : DoctorProfileEvent
    data object SubmitPasswordChange : DoctorProfileEvent
    data object SendPasswordResetEmail : DoctorProfileEvent
    data object RequestLogout : DoctorProfileEvent
    data object ConfirmLogout : DoctorProfileEvent
    data object DismissLogoutDialog : DoctorProfileEvent
    data object DismissError : DoctorProfileEvent
}

sealed interface DoctorProfileEffect {
    data class ShowToast(val message: String) : DoctorProfileEffect
    data object NavigateToLogin : DoctorProfileEffect
}
