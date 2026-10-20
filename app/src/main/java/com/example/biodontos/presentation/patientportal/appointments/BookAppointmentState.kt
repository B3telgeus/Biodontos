package com.example.biodontos.presentation.patientportal.appointments

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.scheduling.TimeSlot
import java.time.LocalDate

@Immutable
data class BookAppointmentUiState(
    val doctorId: String = "",
    val rescheduleAppointmentId: String? = null,
    val doctor: Doctor? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val availableDates: List<LocalDate> = emptyList(),
    val slots: List<TimeSlot> = emptyList(),
    val selectedSlot: TimeSlot? = null,
    val reasonOptions: List<String> = listOf(
        "Control de rutina",
        "Limpieza dental",
        "Dolor o urgencia",
        "Valoración primera vez",
        "Ortodoncia / Ajuste"
    ),
    val selectedReason: String = "Control de rutina",
    val customReason: String = "",
    val isLoading: Boolean = false,
    val isSlotsLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface BookAppointmentUiEvent {
    data class SelectDate(val date: LocalDate) : BookAppointmentUiEvent
    data class SelectSlot(val slot: TimeSlot) : BookAppointmentUiEvent
    data class SelectReason(val reason: String) : BookAppointmentUiEvent
    data class CustomReasonChanged(val reason: String) : BookAppointmentUiEvent
    data object ConfirmBooking : BookAppointmentUiEvent
    data object Retry : BookAppointmentUiEvent
}

sealed interface BookAppointmentUiEffect {
    data object NavigateToMyAppointments : BookAppointmentUiEffect
    data class ShowToast(val message: String) : BookAppointmentUiEffect
}
