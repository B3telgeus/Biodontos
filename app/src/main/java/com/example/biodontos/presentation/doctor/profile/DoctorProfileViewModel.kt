package com.example.biodontos.presentation.doctor.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.usecase.appointment.CancelAppointmentUseCase
import com.example.biodontos.domain.usecase.appointment.ConfirmAppointmentUseCase
import com.example.biodontos.domain.usecase.appointment.GetDoctorAppointmentsUseCase
import com.example.biodontos.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.biodontos.domain.usecase.auth.LogoutUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DoctorProfileViewModel(
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val doctorRepository: DoctorRepository,
    private val getDoctorAppointmentsUseCase: GetDoctorAppointmentsUseCase,
    private val confirmAppointmentUseCase: ConfirmAppointmentUseCase,
    private val cancelAppointmentUseCase: CancelAppointmentUseCase,
    private val patientRepository: PatientRepository,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DoctorProfileState())
    val state = _state.asStateFlow()

    private val _effects = Channel<DoctorProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadData()
    }

    fun onEvent(event: DoctorProfileEvent) {
        when (event) {
            DoctorProfileEvent.LoadData -> loadData()
            is DoctorProfileEvent.SelectTab -> _state.update { it.copy(selectedTab = event.tab) }
            is DoctorProfileEvent.ConfirmAppointment -> confirmAppointment(event.appointmentId)
            is DoctorProfileEvent.CancelAppointment -> cancelAppointment(event.appointmentId, event.reason)
            DoctorProfileEvent.Logout -> logout()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val session = getCurrentSessionUseCase()
            if (session == null) {
                _effects.send(DoctorProfileEffect.NavigateToLogin)
                return@launch
            }

            val doctorResult = doctorRepository.getDoctorByUserId(session.id)
            val doctor = doctorResult.getOrNull() ?: Doctor(
                id = DoctorId(session.id),
                userId = session.id,
                firstName = session.displayName.split(" ").firstOrNull() ?: "Doctor",
                lastName = session.displayName.split(" ").drop(1).joinToString(" ").ifEmpty { "Especialista" },
                specialty = "Odontología Integral",
                email = session.email
            )

            _state.update { it.copy(doctor = doctor) }
            loadAppointmentsForDoctor(doctor.id.value)
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadAppointmentsForDoctor(doctorId: String) {
        val result = getDoctorAppointmentsUseCase(doctorId)
        result.onSuccess { appointmentsList ->
            // Cargar pacientes para asociar nombres a las citas
            val patientIds = appointmentsList.map { it.patientId }.distinct()
            val patientsMap = mutableMapOf<String, Patient>()
            patientIds.forEach { pid ->
                patientRepository.getPatientById(pid).getOrNull()?.let { patient ->
                    patientsMap[pid] = patient
                }
            }

            _state.update {
                it.copy(
                    appointments = appointmentsList,
                    patientsMap = patientsMap,
                    errorMessage = null
                )
            }
        }.onFailure { ex ->
            _state.update { it.copy(errorMessage = "Error al cargar citas: ${ex.toUserFriendlyMessage()}") }
        }
    }

    private fun confirmAppointment(appointmentId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isActionInProgress = true) }
            val result = confirmAppointmentUseCase(appointmentId)
            result.onSuccess {
                _effects.send(DoctorProfileEffect.ShowToast("Cita confirmada correctamente"))
                val doctorId = _state.value.doctor?.id?.value ?: ""
                if (doctorId.isNotEmpty()) {
                    loadAppointmentsForDoctor(doctorId)
                }
            }.onFailure { ex ->
                _effects.send(DoctorProfileEffect.ShowToast(ex.toUserFriendlyMessage()))
            }
            _state.update { it.copy(isActionInProgress = false) }
        }
    }

    private fun cancelAppointment(appointmentId: String, reason: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isActionInProgress = true) }
            val result = cancelAppointmentUseCase(appointmentId, reason ?: "Cancelada por el odontólogo")
            result.onSuccess {
                _effects.send(DoctorProfileEffect.ShowToast("Cita cancelada"))
                val doctorId = _state.value.doctor?.id?.value ?: ""
                if (doctorId.isNotEmpty()) {
                    loadAppointmentsForDoctor(doctorId)
                }
            }.onFailure { ex ->
                _effects.send(DoctorProfileEffect.ShowToast(ex.toUserFriendlyMessage()))
            }
            _state.update { it.copy(isActionInProgress = false) }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _effects.send(DoctorProfileEffect.NavigateToLogin)
        }
    }
}
