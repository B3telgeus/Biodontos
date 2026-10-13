package com.example.biodontos.presentation.patientportal.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.scheduling.TimeSlot
import com.example.biodontos.domain.usecase.appointments.BookAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.GetAvailableSlotsUseCase
import com.example.biodontos.domain.usecase.appointments.RescheduleAppointmentUseCase
import com.example.biodontos.domain.usecase.patient.GetCurrentPatientUseCase
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
                confirmBooking()
            }
            is BookAppointmentUiEvent.Retry -> {
                loadDoctorAndSlots()
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
                    isLoading = false
                )
            }

            loadSlotsForDate(_uiState.value.selectedDate)
        }
    }

    private fun loadSlotsForDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSlotsLoading = true) }
            val slotsResult = getAvailableSlotsUseCase(DoctorId(doctorId), date)
            val slots = slotsResult.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    slots = slots,
                    isSlotsLoading = false
                )
            }
        }
    }

    private fun confirmBooking() {
        val state = _uiState.value
        val slot = state.selectedSlot ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            val effectiveReason = if (state.customReason.isNotBlank()) {
                state.customReason.trim()
            } else {
                state.selectedReason
            }

            if (state.rescheduleAppointmentId != null) {
                // Flujo de reagendamiento
                val result = rescheduleAppointmentUseCase(
                    appointmentId = state.rescheduleAppointmentId,
                    newStartTime = slot.start,
                    newEndTime = slot.end
                )
                result.fold(
                    ifLeft = { error ->
                        _uiState.update { it.copy(isSubmitting = false) }
                        _effects.send(BookAppointmentUiEffect.ShowToast("No se pudo reagendar: horario ocupado o límite de tiempo superado"))
                    },
                    ifRight = {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _effects.send(BookAppointmentUiEffect.ShowToast("Cita reagendada con éxito"))
                        _effects.send(BookAppointmentUiEffect.NavigateToMyAppointments)
                    }
                )
            } else {
                // Flujo de nueva cita
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
                    onSuccess = {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _effects.send(BookAppointmentUiEffect.ShowToast("¡Cita reservada con éxito!"))
                        _effects.send(BookAppointmentUiEffect.NavigateToMyAppointments)
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isSubmitting = false) }
                        _effects.send(BookAppointmentUiEffect.ShowToast("No se pudo agendar la cita. Intenta con otro horario."))
                    }
                )
            }
        }
    }
}
