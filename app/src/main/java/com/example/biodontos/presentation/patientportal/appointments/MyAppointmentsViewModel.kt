package com.example.biodontos.presentation.patientportal.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.AppointmentError
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.usecase.appointments.CancelAppointmentUseCase
import com.example.biodontos.domain.usecase.patient.GetCurrentPatientUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyAppointmentsViewModel(
    private val getCurrentPatientUseCase: GetCurrentPatientUseCase,
    private val appointmentRepository: AppointmentRepository,
    private val doctorRepository: DoctorRepository,
    private val cancelAppointmentUseCase: CancelAppointmentUseCase,
    private val clinicClock: ClinicClock
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyAppointmentsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<MyAppointmentsUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadAppointments()
    }

    fun onEvent(event: MyAppointmentsUiEvent) {
        when (event) {
            is MyAppointmentsUiEvent.SelectTab -> {
                _uiState.update { it.copy(selectedTab = event.tab) }
            }
            is MyAppointmentsUiEvent.RequestCancelAppointment -> {
                _uiState.update { it.copy(appointmentToCancel = event.appointment) }
            }
            is MyAppointmentsUiEvent.DismissCancelDialog -> {
                _uiState.update { it.copy(appointmentToCancel = null) }
            }
            is MyAppointmentsUiEvent.ConfirmCancelAppointment -> {
                confirmCancel()
            }
            is MyAppointmentsUiEvent.RescheduleAppointment -> {
                val doctorId = event.appointment.doctorId
                viewModelScope.launch {
                    if (doctorId.isNullOrBlank()) {
                        _effects.send(MyAppointmentsUiEffect.ShowToast("Selecciona un especialista para reagendar tu cita"))
                        _effects.send(MyAppointmentsUiEffect.NavigateToDoctorSearch)
                    } else {
                        _effects.send(MyAppointmentsUiEffect.NavigateToReschedule(doctorId, event.appointment.id))
                    }
                }
            }
            is MyAppointmentsUiEvent.BookNewAppointment -> {
                viewModelScope.launch {
                    _effects.send(MyAppointmentsUiEffect.NavigateToDoctorSearch)
                }
            }
            is MyAppointmentsUiEvent.Refresh -> {
                loadAppointments()
            }
        }
    }

    fun loadAppointments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val patient = getCurrentPatientUseCase().getOrNull()
            val patientId = patient?.id?.takeIf { it.isNotBlank() }

            if (patientId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudo identificar la sesión del paciente"
                    )
                }
                return@launch
            }

            val appointmentsResult = appointmentRepository.getAppointmentsByPatientId(patientId)
            if (appointmentsResult.isFailure) {
                val errorMsg = appointmentsResult.exceptionOrNull()?.localizedMessage
                    ?: "No se pudieron cargar tus citas programadas"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
                return@launch
            }

            val allAppointments = appointmentsResult.getOrDefault(emptyList())

            val doctorsResult = doctorRepository.getDoctors()
            val doctorsMap = doctorsResult.getOrDefault(emptyList()).associateBy { it.id.value }

            val now = clinicClock.now()

            val upcoming = allAppointments
                .filter { it.status != AppointmentStatus.CANCELLED && it.status != AppointmentStatus.COMPLETED && it.startTime >= now }
                .sortedBy { it.startTime }

            val completed = allAppointments
                .filter { it.status == AppointmentStatus.COMPLETED || (it.status != AppointmentStatus.CANCELLED && it.startTime < now) }
                .sortedByDescending { it.startTime }

            val cancelled = allAppointments
                .filter { it.status == AppointmentStatus.CANCELLED }
                .sortedByDescending { it.startTime }

            val past = allAppointments
                .filter { it.status == AppointmentStatus.CANCELLED || it.status == AppointmentStatus.COMPLETED || it.startTime < now }
                .sortedByDescending { it.startTime }

            _uiState.update {
                it.copy(
                    patientId = patientId,
                    upcomingAppointments = upcoming,
                    completedAppointments = completed,
                    cancelledAppointments = cancelled,
                    pastAppointments = past,
                    doctorsMap = doctorsMap,
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    private fun confirmCancel() {
        val appointment = _uiState.value.appointmentToCancel ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true) }
            val result = cancelAppointmentUseCase(appointment.id)

            result.fold(
                ifLeft = { error ->
                    _uiState.update { it.copy(isCancelling = false, appointmentToCancel = null) }
                    val message = when (error) {
                        is AppointmentError.ModificationWindowClosed ->
                            "No es posible cancelar: se requiere un mínimo de ${error.minHoursBefore} horas de anticipación."
                        is AppointmentError.AlreadyCancelled ->
                            "Esta cita ya se encuentra cancelada en el sistema."
                        is AppointmentError.NotOwner ->
                            "No tienes permisos para modificar esta cita clínica."
                        is AppointmentError.AppointmentNotFound ->
                            "No se encontró la cita especificada."
                        else ->
                            "No fue posible cancelar la cita. Comunícate con recepción."
                    }
                    _effects.send(MyAppointmentsUiEffect.ShowToast(message))
                },
                ifRight = {
                    _uiState.update { it.copy(isCancelling = false, appointmentToCancel = null) }
                    _effects.send(MyAppointmentsUiEffect.ShowToast("Cita cancelada con éxito"))
                    loadAppointments()
                }
            )
        }
    }
}
