package com.example.biodontos.presentation.patientportal.appointments

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.Doctor

enum class AppointmentTab(val title: String) {
    UPCOMING("Próximas"),
    COMPLETED("Completadas"),
    CANCELLED("Canceladas");

    companion object {
        @Deprecated("Usa COMPLETED o CANCELLED", ReplaceWith("COMPLETED"))
        val PAST: AppointmentTab = COMPLETED
    }
}

@Immutable
data class MyAppointmentsUiState(
    val selectedTab: AppointmentTab = AppointmentTab.UPCOMING,
    val patientId: String? = null,
    val upcomingAppointments: List<Appointment> = emptyList(),
    val completedAppointments: List<Appointment> = emptyList(),
    val cancelledAppointments: List<Appointment> = emptyList(),
    val pastAppointments: List<Appointment> = emptyList(),
    val doctorsMap: Map<String, Doctor> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val appointmentToCancel: Appointment? = null,
    val isCancelling: Boolean = false
) {
    val upcomingCount: Int get() = upcomingAppointments.size
    val completedCount: Int get() = completedAppointments.size
    val cancelledCount: Int get() = cancelledAppointments.size
}

sealed interface MyAppointmentsUiEvent {
    data class SelectTab(val tab: AppointmentTab) : MyAppointmentsUiEvent
    data class RequestCancelAppointment(val appointment: Appointment) : MyAppointmentsUiEvent
    data object DismissCancelDialog : MyAppointmentsUiEvent
    data object ConfirmCancelAppointment : MyAppointmentsUiEvent
    data class RescheduleAppointment(val appointment: Appointment) : MyAppointmentsUiEvent
    data object BookNewAppointment : MyAppointmentsUiEvent
    data object Refresh : MyAppointmentsUiEvent
}

sealed interface MyAppointmentsUiEffect {
    data class NavigateToReschedule(val doctorId: String, val appointmentId: String) : MyAppointmentsUiEffect
    data object NavigateToDoctorSearch : MyAppointmentsUiEffect
    data class ShowToast(val message: String) : MyAppointmentsUiEffect
}
