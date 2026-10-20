package com.example.biodontos.presentation.patientportal.home

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Doctor

@Immutable
data class PatientHomeUiState(
    val patientName: String = "",
    val patientId: String = "",
    val nextAppointment: Appointment? = null,
    val upcomingAppointments: List<Appointment> = emptyList(),
    val pendingBalance: CopAmount = CopAmount(0),
    val nextPaymentDueDate: String? = null,
    val completedAppointmentsCount: Int = 0,
    val doctors: List<Doctor> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface PatientHomeUiEvent {
    data object Refresh : PatientHomeUiEvent
    data object NavigateToAppointments : PatientHomeUiEvent
    data object NavigateToPayments : PatientHomeUiEvent
    data object NavigateToDoctors : PatientHomeUiEvent
    data class BookWithDoctor(val doctorId: String) : PatientHomeUiEvent
    data object Logout : PatientHomeUiEvent
}

sealed interface PatientHomeUiEffect {
    data object NavigateToAppointments : PatientHomeUiEffect
    data object NavigateToPayments : PatientHomeUiEffect
    data object NavigateToDoctors : PatientHomeUiEffect
    data class NavigateToBooking(val doctorId: String) : PatientHomeUiEffect
    data object NavigateToLogin : PatientHomeUiEffect
    data class ShowToast(val message: String) : PatientHomeUiEffect
}
