package com.example.biodontos.presentation.doctor.profile

import androidx.compose.runtime.Immutable
import com.example.biodontos.core.util.DateTimeUtils
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.Patient

enum class DoctorAppointmentTab {
    PENDING,
    CONFIRMED,
    HISTORY
}

@Immutable
data class DoctorProfileState(
    val doctor: Doctor? = null,
    val appointments: List<Appointment> = emptyList(),
    val patientsMap: Map<String, Patient> = emptyMap(),
    val selectedTab: DoctorAppointmentTab = DoctorAppointmentTab.PENDING,
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val errorMessage: String? = null
) {
    val pendingAppointments: List<Appointment>
        get() = appointments.filter { !it.isConfirmed && it.status == AppointmentStatus.SCHEDULED && DateTimeUtils.isUpcoming(it.startTime) }

    val confirmedAppointments: List<Appointment>
        get() = appointments.filter { it.isConfirmed && it.status == AppointmentStatus.SCHEDULED && DateTimeUtils.isUpcoming(it.startTime) }

    val historyAppointments: List<Appointment>
        get() = appointments.filter { it.status != AppointmentStatus.SCHEDULED || !DateTimeUtils.isUpcoming(it.startTime) }
}

sealed interface DoctorProfileEvent {
    data object LoadData : DoctorProfileEvent
    data class SelectTab(val tab: DoctorAppointmentTab) : DoctorProfileEvent
    data class ConfirmAppointment(val appointmentId: String) : DoctorProfileEvent
    data class CancelAppointment(val appointmentId: String, val reason: String? = null) : DoctorProfileEvent
    data object Logout : DoctorProfileEvent
}

sealed interface DoctorProfileEffect {
    data class ShowToast(val message: String) : DoctorProfileEffect
    data object NavigateToLogin : DoctorProfileEffect
}
