package com.example.biodontos.presentation.doctor.appointments

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.presentation.doctor.home.DoctorAppointmentItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class DoctorAppointmentFilter(val label: String) {
    ALL("Todas"),
    TODAY("Hoy"),
    CONFIRMED("Confirmadas"),
    UNCONFIRMED("Sin Confirmar")
}

enum class DoctorScheduleMode(val label: String) {
    TIMELINE("Cronograma"),
    LIST("Lista de Citas")
}

sealed interface DoctorSlotStatus {
    data class Occupied(val appointment: DoctorAppointmentItem) : DoctorSlotStatus
    data object Available : DoctorSlotStatus
    data object Past : DoctorSlotStatus
}

@Immutable
data class DoctorTimelineSlot(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val status: DoctorSlotStatus
) {
    val durationMinutes: Long
        get() = java.time.Duration.between(startTime, endTime).toMinutes()
}

@Immutable
data class DayAvailabilityConfig(
    val dayOfWeek: DayOfWeek,
    val isActive: Boolean = true,
    val startTime: LocalTime = LocalTime.of(8, 0),
    val endTime: LocalTime = LocalTime.of(17, 0),
    val slotMinutes: Int = 30
)

@Immutable
data class DoctorAppointmentsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSavingAvailability: Boolean = false,
    val isBooking: Boolean = false,
    val isRescheduling: Boolean = false,
    val isCancelling: Boolean = false,
    val doctorName: String = "",
    val doctorId: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val displayMode: DoctorScheduleMode = DoctorScheduleMode.TIMELINE,
    val selectedFilter: DoctorAppointmentFilter = DoctorAppointmentFilter.ALL,
    val appointments: List<DoctorAppointmentItem> = emptyList(),
    val timelineSlots: List<DoctorTimelineSlot> = emptyList(),
    val isDayOff: Boolean = false,
    val weeklyAvailability: List<DayAvailabilityConfig> = emptyList(),
    val registeredPatients: List<Patient> = emptyList(),
    // Diálogos activos
    val showAvailabilityDialog: Boolean = false,
    val bookingTargetSlot: DoctorTimelineSlot? = null,
    val reschedulingAppointment: DoctorAppointmentItem? = null,
    val rescheduleTargetDate: LocalDate = LocalDate.now(),
    val rescheduleAvailableSlots: List<LocalDateTime> = emptyList(),
    val cancellingAppointment: DoctorAppointmentItem? = null,
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
    data class SelectDate(val date: LocalDate) : DoctorAppointmentsUiEvent
    data class ChangeDisplayMode(val mode: DoctorScheduleMode) : DoctorAppointmentsUiEvent
    data class SelectFilter(val filter: DoctorAppointmentFilter) : DoctorAppointmentsUiEvent
    data class ToggleConfirm(val appointmentId: String, val currentConfirmed: Boolean) : DoctorAppointmentsUiEvent

    // Disponibilidad
    data object OpenAvailabilityEditor : DoctorAppointmentsUiEvent
    data object DismissAvailabilityEditor : DoctorAppointmentsUiEvent
    data class UpdateDayAvailability(
        val day: DayOfWeek,
        val isActive: Boolean,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val slotMinutes: Int
    ) : DoctorAppointmentsUiEvent
    data class CopyWeekdayAvailability(val sourceDay: DayOfWeek) : DoctorAppointmentsUiEvent
    data object SaveAvailability : DoctorAppointmentsUiEvent

    // Reserva Directa en Slot Libre
    data class OpenQuickBooking(val slot: DoctorTimelineSlot) : DoctorAppointmentsUiEvent
    data object DismissQuickBooking : DoctorAppointmentsUiEvent
    data class ConfirmQuickBooking(val patientId: String, val reason: String) : DoctorAppointmentsUiEvent

    // Reagendamiento
    data class OpenReschedule(val appointment: DoctorAppointmentItem) : DoctorAppointmentsUiEvent
    data object DismissReschedule : DoctorAppointmentsUiEvent
    data class ChangeRescheduleTargetDate(val date: LocalDate) : DoctorAppointmentsUiEvent
    data class ConfirmReschedule(val newStartTime: LocalDateTime) : DoctorAppointmentsUiEvent

    // Cancelación
    data class RequestCancelAppointment(val appointment: DoctorAppointmentItem) : DoctorAppointmentsUiEvent
    data object DismissCancelAppointment : DoctorAppointmentsUiEvent
    data object ConfirmCancelAppointment : DoctorAppointmentsUiEvent
}

sealed interface DoctorAppointmentsUiEffect {
    data class ShowMessage(val message: String) : DoctorAppointmentsUiEffect
}
