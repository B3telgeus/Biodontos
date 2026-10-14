package com.example.biodontos.presentation.doctor.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.usecase.appointments.GetDoctorAppointmentsUseCase
import com.example.biodontos.domain.usecase.appointments.ToggleAppointmentConfirmationUseCase
import com.example.biodontos.domain.usecase.auth.LogoutUseCase
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DoctorHomeViewModel(
    private val getCurrentDoctorUseCase: GetCurrentDoctorUseCase,
    private val getDoctorAppointmentsUseCase: GetDoctorAppointmentsUseCase,
    private val toggleAppointmentConfirmationUseCase: ToggleAppointmentConfirmationUseCase,
    private val patientRepository: PatientRepository,
    private val logoutUseCase: LogoutUseCase,
    private val clinicClock: ClinicClock
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorHomeUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<DoctorHomeUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadDashboard()
    }

    fun onEvent(event: DoctorHomeUiEvent) {
        when (event) {
            is DoctorHomeUiEvent.Refresh -> {
                loadDashboard(isRefresh = true)
            }
            is DoctorHomeUiEvent.ToggleConfirmAppointment -> {
                toggleConfirm(event.appointmentId, event.currentConfirmed)
            }
            is DoctorHomeUiEvent.Logout -> {
                logout()
            }
        }
    }

    fun loadDashboard(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val doctorResult = getCurrentDoctorUseCase()
            val doctor = doctorResult.getOrNull()

            if (doctor == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "No se pudo identificar la sesión del profesional"
                    )
                }
                return@launch
            }

            // Pacientes registrados
            val patientsResult = patientRepository.getPatients()
            val patientsList = patientsResult.getOrDefault(emptyList())
            val patientsMap = patientsList.associateBy { it.id }

            // Citas del doctor
            val appointmentsResult = getDoctorAppointmentsUseCase(doctor.id.value)
            val appointmentsList = appointmentsResult.getOrDefault(emptyList())

            val now = clinicClock.now()
            val today = now.toLocalDate()

            val todayCount = appointmentsList.count { 
                it.startTime.toLocalDate() == today && it.status != AppointmentStatus.CANCELLED 
            }

            val upcoming = appointmentsList
                .filter { it.status != AppointmentStatus.CANCELLED }
                .sortedBy { it.startTime }
                .take(6)
                .map { appt ->
                    val pat = patientsMap[appt.patientId]
                    DoctorAppointmentItem(
                        appointmentId = appt.id,
                        patientName = pat?.fullName ?: "Paciente",
                        treatmentReason = appt.reason?.ifBlank { "Control dental" } ?: "Control dental",
                        startTime = appt.startTime,
                        isConfirmed = appt.isConfirmed
                    )
                }

            // Pagos de referencia de la clínica
            val samplePayments = listOf(
                DoctorPaymentItem("Dora Hernández", "16/10/2026", CopAmount(326_000)),
                DoctorPaymentItem("Carlos Troncoso", "19/03/2026", CopAmount(1_500_000)),
                DoctorPaymentItem("Luna Estrada", "18/03/2026", CopAmount(178_000))
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    doctor = doctor,
                    doctorName = doctor.displayName,
                    patientsCount = patientsList.size,
                    todayAppointmentsCount = todayCount,
                    upcomingAppointments = upcoming,
                    recentPayments = samplePayments,
                    errorMessage = null
                )
            }
        }
    }

    private fun toggleConfirm(appointmentId: String, currentConfirmed: Boolean) {
        viewModelScope.launch {
            val newStatus = !currentConfirmed
            _uiState.update { current ->
                val updated = current.upcomingAppointments.map { item ->
                    if (item.appointmentId == appointmentId) {
                        item.copy(isConfirmed = newStatus)
                    } else item
                }
                current.copy(upcomingAppointments = updated)
            }

            val result = toggleAppointmentConfirmationUseCase(appointmentId, newStatus)
            result.fold(
                onSuccess = {
                    val msg = if (newStatus) "Cita confirmada con éxito" else "Cita marcada como sin confirmar"
                    _effects.send(DoctorHomeUiEffect.ShowMessage(msg))
                },
                onFailure = {
                    _uiState.update { current ->
                        val reverted = current.upcomingAppointments.map { item ->
                            if (item.appointmentId == appointmentId) {
                                item.copy(isConfirmed = currentConfirmed)
                            } else item
                        }
                        current.copy(upcomingAppointments = reverted)
                    }
                    _effects.send(DoctorHomeUiEffect.ShowMessage("No se pudo actualizar el estado de confirmación"))
                }
            )
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _effects.send(DoctorHomeUiEffect.NavigateToLogin)
        }
    }
}
