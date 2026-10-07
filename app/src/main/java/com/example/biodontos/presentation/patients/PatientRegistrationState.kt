package com.example.biodontos.presentation.patients

data class PatientRegistrationState(
    val dniCedula: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val consentSigned: Boolean = false,
    val hasCriticalAllergies: Boolean = false,
    val allergiesDescription: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val dniError: String? = null,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val isSaved: Boolean = false
)

sealed class PatientRegistrationEvent {
    object SavedSuccessfully : PatientRegistrationEvent()
}
