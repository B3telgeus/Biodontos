package com.example.biodontos.presentation.patientportal.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.AppointmentError
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.scheduling.TimeSlot
import com.example.biodontos.domain.usecase.appointments.BookAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.GetAvailableSlotsUseCase
import com.example.biodontos.domain.usecase.appointments.RescheduleAppointmentUseCase
import com.example.biodontos.domain.usecase.patient.GetCurrentPatientUseCase
import com.example.biodontos.presentation.navigation.AppointmentConfirmation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class BookAppointmentViewModel(
    private val doctorId: String,
    private val rescheduleAppointmentId: String? = null,
    private val getCurrentPatientUseCase: GetCurrentPatientUseCase,
    private val doctorRepository: DoctorRepository,
    private val getAvailableSlotsUseCase: GetAvailableSlotsUseCase,
    private val bookAppointmentUseCase: BookAppointmentUseCase,
    private val rescheduleAppointmentUseCase: RescheduleAppointmentUseCase,
    private val clinicClock: ClinicClock
) : ViewModel() {

    private val initialDate: LocalDate = clinicClock.now().toLocalDate()
    private val datesList: List<LocalDate> = (0..13).map { initialDate.plusDays(it.toLong()) }

    private val _uiState = MutableStateFlow(
        BookAppointmentUiState(
            doctorId = doctorId,
            rescheduleAppointmentId = rescheduleAppointmentId,
            selectedDate = initialDate,
            availableDates = datesList,
            isLoading = true
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<BookAppointmentUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadDoctorAndSlots()
    }

    fun onEvent(event: BookAppointmentUiEvent) {
        when (event) {
            is BookAppointmentUiEvent.SelectDate -> {
                _uiState.update { it.copy(selectedDate = event.date, selectedSlot = null) }
                loadSlotsForDate(event.date)
            }
            is BookAppointmentUiEvent.SelectSlot -> {
                _uiState.update { it.copy(selectedSlot = event.slot) }
            }
            is BookAppointmentUiEvent.SelectReason -> {
                _uiState.update { it.copy(selectedReason = event.reason) }
            }
            is BookAppointmentUiEvent.CustomReasonChanged -> {
                _uiState.update { it.copy(customReason = event.reason) }
            }
            is BookAppointmentUiEvent.ConfirmBooking -> {
                handleConfirmBooking()
            }
            is BookAppointmentUiEvent.ExecuteReschedule -> {
                if (_uiState.value.isSubmitting) return
                _uiState.update { it.copy(showRescheduleConfirmDialog = false) }
                executeReschedule()
            }
            is BookAppointmentUiEvent.DismissRescheduleDialog -> {
                _uiState.update { it.copy(showRescheduleConfirmDialog = false) }
            }
            is BookAppointmentUiEvent.Retry -> {
                loadDoctorAndSlots()
            }
            is BookAppointmentUiEvent.RetrySlots -> {
                loadSlotsForDate(_uiState.value.selectedDate)
            }
        }
    }

    private fun loadDoctorAndSlots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val doctorResult = doctorRepository.getDoctorById(doctorId)
            val doctor = doctorResult.getOrNull()

            _uiState.update {
                it.copy(
                    doctor = doctor,
                    isLoading = false,
                    errorMessage = if (doctor == null && doctorResult.isFailure) "No se pudo cargar la información del especialista" else null
                )
            }

            loadSlotsForDate(_uiState.value.selectedDate)
        }
    }

    private fun loadSlotsForDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSlotsLoading = true, errorMessage = null) }
            val slotsResult = getAvailableSlotsUseCase(DoctorId(doctorId), date)
            slotsResult.fold(
                onSuccess = { slots ->
                    _uiState.update {
                        it.copy(
                            slots = slots,
                            isSlotsLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            slots = emptyList(),
                            isSlotsLoading = false,
                            errorMessage = "No se pudieron consultar los horarios disponibles. Intenta nuevamente."
                        )
                    }
                }
            )
        }
    }

    private fun handleConfirmBooking() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val slot = state.selectedSlot ?: return

        // Defensive past slot check
        val now = clinicClock.now()
        if (slot.start.isBefore(now.plusHours(2))) {
            viewModelScope.launch {
                _effects.send(BookAppointmentUiEffect.ShowToast("El horario seleccionado ya no está disponible (mínimo 2 horas de anticipación)"))
                loadSlotsForDate(state.selectedDate)
            }
            return
        }

        if (state.rescheduleAppointmentId != null) {
            // Critical PP-02: Show 2-step confirmation dialog before mutating reschedule
            _uiState.update { it.copy(showRescheduleConfirmDialog = true) }
        } else {
            proceedWithNewBooking()
        }
    }

    private fun proceedWithNewBooking() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val slot = state.selectedSlot ?: return

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val effectiveReason = if (state.customReason.isNotBlank()) {
                state.customReason.trim()
            } else {
                state.selectedReason
            }

            val patient = getCurrentPatientUseCase().getOrNull()
            val patientId = patient?.id?.takeIf { it.isNotBlank() }

            if (patientId == null) {
                _uiState.update { it.copy(isSubmitting = false) }
                _effects.send(BookAppointmentUiEffect.ShowToast("Error: No se encontró perfil de paciente activo"))
                return@launch
            }

            val result = bookAppointmentUseCase(
                patientId = patientId,
                doctorId = doctorId,
                startTime = slot.start,
                endTime = slot.end,
                reason = effectiveReason
            )

            result.fold(
                onSuccess = { appointment ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    val confirmation = AppointmentConfirmation(
                        appointmentId = appointment.id ?: "appt-${System.currentTimeMillis()}",
                        doctorId = doctorId,
                        doctorName = state.doctor?.displayName ?: "Especialista Biodontos",
                        specialty = state.doctor?.specialty ?: "Odontología",
                        startIso = slot.start.toString(),
                        roomOrLocation = "Sede Principal - Consultorio 102",
                        reason = effectiveReason,
                        instructions = "Presentarse 10 minutos antes. Traer documento de identidad y cepillo dental.",
                        isRescheduled = false
                    )
                    _effects.send(BookAppointmentUiEffect.ShowToast("¡Cita reservada con éxito!"))
                    _effects.send(BookAppointmentUiEffect.NavigateToConfirmation(confirmation))
                },
                onFailure = {
                    _uiState.update { it.copy(isSubmitting = false, selectedSlot = null) }
                    _effects.send(BookAppointmentUiEffect.ShowToast("No se pudo agendar la cita. Intenta con otro horario."))
                    loadSlotsForDate(state.selectedDate)
                }
            )
        }
    }

    private fun executeReschedule() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val slot = state.selectedSlot ?: return
        val rescheduleId = state.rescheduleAppointmentId ?: return

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val effectiveReason = if (state.customReason.isNotBlank()) {
                state.customReason.trim()
            } else {
                state.selectedReason
            }

            val result = rescheduleAppointmentUseCase(
                appointmentId = rescheduleId,
                newStartTime = slot.start,
                newEndTime = slot.end
            )

            result.fold(
                ifLeft = { error ->
                    _uiState.update { it.copy(isSubmitting = false, selectedSlot = null) }
                    val message = when (error) {
                        is AppointmentError.SlotUnavailable -> "El horario ya no está disponible o ha sido reservado."
                        is AppointmentError.ModificationWindowClosed -> "No es posible modificar citas con menos de 2 horas de anticipación."
                        else -> "No se pudo reagendar: horario ocupado o límite de tiempo superado"
                    }
                    _effects.send(BookAppointmentUiEffect.ShowToast(message))
                    loadSlotsForDate(state.selectedDate)
                },
                ifRight = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    val confirmation = AppointmentConfirmation(
                        appointmentId = rescheduleId,
                        doctorId = doctorId,
                        doctorName = state.doctor?.displayName ?: "Especialista Biodontos",
                        specialty = state.doctor?.specialty ?: "Odontología",
                        startIso = slot.start.toString(),
                        roomOrLocation = "Sede Principal - Consultorio 102",
                        reason = effectiveReason,
                        instructions = "Presentarse 10 minutos antes. Traer documento de identidad y cepillo dental.",
                        isRescheduled = true
                    )
                    _effects.send(BookAppointmentUiEffect.ShowToast("Cita reagendada con éxito"))
                    _effects.send(BookAppointmentUiEffect.NavigateToConfirmation(confirmation))
                }
            )
        }
    }
}
