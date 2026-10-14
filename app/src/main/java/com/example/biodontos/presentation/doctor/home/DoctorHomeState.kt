package com.example.biodontos.presentation.doctor.home

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Doctor
import java.time.LocalDateTime

@Immutable
data class DoctorAppointmentItem(
    val appointmentId: String,
    val patientName: String,
    val treatmentReason: String,
    val startTime: LocalDateTime,
    val isConfirmed: Boolean
)

@Immutable
data class DoctorPaymentItem(
    val patientName: String,
    val dateStr: String,
    val amountCop: CopAmount
)

@Immutable
data class DoctorHomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val doctor: Doctor? = null,
    val doctorName: String = "",
    val patientsCount: Int = 0,
    val todayAppointmentsCount: Int = 0,
    val upcomingAppointments: List<DoctorAppointmentItem> = emptyList(),
    val recentPayments: List<DoctorPaymentItem> = emptyList(),
    val errorMessage: String? = null
)

sealed interface DoctorHomeUiEvent {
    data object Refresh : DoctorHomeUiEvent
    data class ToggleConfirmAppointment(val appointmentId: String, val currentConfirmed: Boolean) : DoctorHomeUiEvent
    data object Logout : DoctorHomeUiEvent
}

sealed interface DoctorHomeUiEffect {
    data class ShowMessage(val message: String) : DoctorHomeUiEffect
    data object NavigateToLogin : DoctorHomeUiEffect
}
