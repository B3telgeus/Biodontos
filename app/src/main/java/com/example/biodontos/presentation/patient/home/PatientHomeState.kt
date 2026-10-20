package com.example.biodontos.presentation.patient.home

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.Doctor

enum class AppointmentTab {
    UPCOMING,
    HISTORY
}

@Immutable
data class BookingDialogState(
    val selectedDoctorId: String? = null,
    val selectedDate: String = "",
    val availableSlots: List<String> = emptyList(),
    val selectedSlot: String? = null,
    val reason: String = "",
    val isLoadingSlots: Boolean = false,
    val error: String? = null
)

@Immutable
data class RescheduleDialogState(
    val appointment: Appointment,
    val doctor: Doctor? = null,
    val selectedDate: String = "",
    val availableSlots: List<String> = emptyList(),
    val selectedSlot: String? = null,
    val isLoadingSlots: Boolean = false,
    val error: String? = null
)

@Immutable
data class CancelConfirmDialogState(
    val appointment: Appointment,
    val selectedReasonChip: String? = null,
    val customReason: String = "",
    val isLoading: Boolean = false
)

@Immutable
data class SuccessDialogState(
    val title: String,
    val message: String,
    val doctorName: String,
    val specialty: String,
    val dateTimeFormatted: String
)

@Immutable
data class PatientHomeState(
    val patientId: String = "",
    val patientName: String = "",
    val doctors: List<Doctor> = emptyList(),
    val upcomingAppointments: List<Appointment> = emptyList(),
    val historyAppointments: List<Appointment> = emptyList(),
    val selectedTab: AppointmentTab = AppointmentTab.UPCOMING,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val bookingState: BookingDialogState? = null,
    val rescheduleState: RescheduleDialogState? = null,
    val cancelConfirmState: CancelConfirmDialogState? = null,
    val successDialogState: SuccessDialogState? = null
) {
    val nextAppointment: Appointment?
        get() = upcomingAppointments.firstOrNull()
}
