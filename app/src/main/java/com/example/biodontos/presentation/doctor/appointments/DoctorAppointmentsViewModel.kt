package com.example.biodontos.presentation.doctor.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.usecase.appointments.GetDoctorAppointmentsUseCase
import com.example.biodontos.domain.usecase.appointments.ToggleAppointmentConfirmationUseCase
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import com.example.biodontos.presentation.doctor.home.DoctorAppointmentItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DoctorAppointmentsViewModel(
    private val getCurrentDoctorUseCase: GetCurrentDoctorUseCase,
    private val getDoctorAppointmentsUseCase: GetDoctorAppointmentsUseCase,
    private val toggleAppointmentConfirmationUseCase: ToggleAppointmentConfirmationUseCase,
    private val patientRepository: PatientRepository,
    val clinicClock: ClinicClock
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorAppointmentsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<DoctorAppointmentsUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadAppointments()
    }

    fun onEvent(event: DoctorAppointmentsUiEvent) {
        when (event) {
            is DoctorAppointmentsUiEvent.Refresh -> {
                loadAppointments(isRefresh = true)
            }
            is DoctorAppointmentsUiEvent.SelectFilter -> {
                _uiState.update { it.copy(selectedFilter = event.filter) }
            }
            is DoctorAppointmentsUiEvent.ToggleConfirm -> {
                toggleConfirm(event.appointmentId, event.currentConfirmed)
            }
        }
    }

    fun loadAppointments(isRefresh: Boolean = false) {
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

            val patientsResult = patientRepository.getPatients()
            val patientsMap = patientsResult.getOrDefault(emptyList()).associateBy { it.id }

            val appointmentsResult = getDoctorAppointmentsUseCase(doctor.id.value)
            val appointmentsList = appointmentsResult.getOrDefault(emptyList())

            val items = appointmentsList
                .filter { it.status != AppointmentStatus.CANCELLED }
                .sortedBy { it.startTime }
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

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    doctorName = doctor.displayName,
                    doctorId = doctor.id.value,
                    appointments = items,
                    errorMessage = null
                )
            }
        }
    }

    private fun toggleConfirm(appointmentId: String, currentConfirmed: Boolean) {
        viewModelScope.launch {
            val newStatus = !currentConfirmed
            _uiState.update { current ->
                val updated = current.appointments.map { item ->
                    if (item.appointmentId == appointmentId) item.copy(isConfirmed = newStatus) else item
                }
                current.copy(appointments = updated)
            }

            val result = toggleAppointmentConfirmationUseCase(appointmentId, newStatus)
            result.fold(
                onSuccess = {
                    val msg = if (newStatus) "Cita confirmada correctamente" else "Cita marcada como sin confirmar"
                    _effects.send(DoctorAppointmentsUiEffect.ShowMessage(msg))
                },
                onFailure = {
                    _uiState.update { current ->
                        val reverted = current.appointments.map { item ->
                            if (item.appointmentId == appointmentId) item.copy(isConfirmed = currentConfirmed) else item
                        }
                        current.copy(appointments = reverted)
                    }
                    _effects.send(DoctorAppointmentsUiEffect.ShowMessage("No se pudo actualizar el estado de confirmación"))
                }
            )
        }
    }
}
