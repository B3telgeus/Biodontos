package com.example.biodontos.presentation.doctor.appointments

import androidx.compose.runtime.Immutable
import com.example.biodontos.presentation.doctor.home.DoctorAppointmentItem
import java.time.LocalDate

enum class DoctorAppointmentFilter(val label: String) {
    ALL("Todas"),
    TODAY("Hoy"),
    CONFIRMED("Confirmadas"),
    UNCONFIRMED("Sin Confirmar")
}

@Immutable
data class DoctorAppointmentsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val doctorName: String = "",
    val doctorId: String = "",
    val selectedFilter: DoctorAppointmentFilter = DoctorAppointmentFilter.ALL,
    val appointments: List<DoctorAppointmentItem> = emptyList(),
    val errorMessage: String? = null
) {
    fun filteredAppointments(today: LocalDate): List<DoctorAppointmentItem> {
        return when (selectedFilter) {
            DoctorAppointmentFilter.ALL -> appointments
            DoctorAppointmentFilter.TODAY -> appointments.filter { it.startTime.toLocalDate() == today }
            DoctorAppointmentFilter.CONFIRMED -> appointments.filter { it.isConfirmed }
            DoctorAppointmentFilter.UNCONFIRMED -> appointments.filter { !it.isConfirmed }
        }
    }
}

sealed interface DoctorAppointmentsUiEvent {
    data object Refresh : DoctorAppointmentsUiEvent
    data class SelectFilter(val filter: DoctorAppointmentFilter) : DoctorAppointmentsUiEvent
    data class ToggleConfirm(val appointmentId: String, val currentConfirmed: Boolean) : DoctorAppointmentsUiEvent
}

sealed interface DoctorAppointmentsUiEffect {
    data class ShowMessage(val message: String) : DoctorAppointmentsUiEffect
}
