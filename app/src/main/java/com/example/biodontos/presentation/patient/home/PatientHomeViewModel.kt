package com.example.biodontos.presentation.patient.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.core.util.DateTimeUtils
import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.usecase.appointment.BookAppointmentUseCase
import com.example.biodontos.domain.usecase.appointment.CancelAppointmentUseCase
import com.example.biodontos.domain.usecase.appointment.GetDoctorBusySlotsUseCase
import com.example.biodontos.domain.usecase.appointment.GetPatientAppointmentsUseCase
import com.example.biodontos.domain.usecase.appointment.RescheduleAppointmentUseCase
import com.example.biodontos.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.biodontos.domain.usecase.auth.LogoutUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PatientHomeViewModel(
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val patientRepository: PatientRepository,
    private val doctorRepository: DoctorRepository,
    private val getPatientAppointmentsUseCase: GetPatientAppointmentsUseCase,
    private val getDoctorBusySlotsUseCase: GetDoctorBusySlotsUseCase,
    private val bookAppointmentUseCase: BookAppointmentUseCase,
    private val rescheduleAppointmentUseCase: RescheduleAppointmentUseCase,
    private val cancelAppointmentUseCase: CancelAppointmentUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PatientHomeState())
    val state = _state.asStateFlow()

    private val _effects = Channel<PatientHomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadInitialData()
    }

    fun onEvent(event: PatientHomeEvent) {
        when (event) {
            PatientHomeEvent.LoadData -> loadInitialData()
            PatientHomeEvent.Refresh -> refreshData()
            is PatientHomeEvent.SelectTab -> _state.update { it.copy(selectedTab = event.tab) }

            // Booking
            PatientHomeEvent.OpenBookingDialog -> openBookingDialog()
            PatientHomeEvent.DismissBookingDialog -> _state.update { it.copy(bookingState = null) }
            is PatientHomeEvent.SelectBookingDoctor -> onBookingDoctorSelected(event.doctorId)
            is PatientHomeEvent.SelectBookingDate -> onBookingDateSelected(event.date)
            is PatientHomeEvent.SelectBookingSlot -> _state.update { current ->
                current.copy(bookingState = current.bookingState?.copy(selectedSlot = event.slot, error = null))
            }
            is PatientHomeEvent.UpdateBookingReason -> _state.update { current ->
                current.copy(bookingState = current.bookingState?.copy(reason = event.reason, error = null))
            }
            PatientHomeEvent.ConfirmBooking -> confirmBooking()

            // Reschedule
            is PatientHomeEvent.OpenRescheduleDialog -> openRescheduleDialog(event.appointment)
            PatientHomeEvent.DismissRescheduleDialog -> _state.update { it.copy(rescheduleState = null) }
            is PatientHomeEvent.SelectRescheduleDate -> onRescheduleDateSelected(event.date)
            is PatientHomeEvent.SelectRescheduleSlot -> _state.update { current ->
                current.copy(rescheduleState = current.rescheduleState?.copy(selectedSlot = event.slot, error = null))
            }
            PatientHomeEvent.ConfirmReschedule -> confirmReschedule()

            // Cancel
            is PatientHomeEvent.OpenCancelDialog -> {
                _state.update { it.copy(cancelConfirmState = CancelConfirmDialogState(appointment = event.appointment)) }
            }
            PatientHomeEvent.DismissCancelDialog -> _state.update { it.copy(cancelConfirmState = null) }
            is PatientHomeEvent.SelectCancelChip -> _state.update { current ->
                current.copy(cancelConfirmState = current.cancelConfirmState?.copy(selectedReasonChip = event.chip))
            }
            is PatientHomeEvent.UpdateCancelCustomReason -> _state.update { current ->
                current.copy(cancelConfirmState = current.cancelConfirmState?.copy(customReason = event.reason))
            }
            PatientHomeEvent.ConfirmCancellation -> confirmCancellation()

            // Success dialog
            PatientHomeEvent.DismissSuccessDialog -> _state.update { it.copy(successDialogState = null) }

            // Logout
            PatientHomeEvent.Logout -> logout()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val session = getCurrentSessionUseCase()
            if (session == null) {
                _effects.send(PatientHomeEffect.NavigateToLogin)
                return@launch
            }

            val patientProfileResult = patientRepository.getPatientByUserId(session.id)
            val patient = patientProfileResult.getOrNull()
            val patientId = patient?.id?.ifEmpty { null } ?: session.id
            val patientName = patient?.let { "${it.firstName} ${it.firstLastName}".trim() }
                ?.ifEmpty { null } ?: session.displayName.ifEmpty { "Paciente" }

            val doctorsResult = doctorRepository.getDoctors()
            val loadedDoctors = doctorsResult.getOrDefault(emptyList()).ifEmpty {
                // Si la BD aún no tiene doctores registrados, se proveen perfiles del staff clínico
                listOf(
                    Doctor(id = "11111111-1111-1111-1111-111111111111", firstName = "Laura", lastName = "Gómez", specialty = "Odontología General", email = "laura.gomez@biodontos.com"),
                    Doctor(id = "22222222-2222-2222-2222-222222222222", firstName = "Carlos", lastName = "Mendoza", specialty = "Ortodoncia & Estética", email = "carlos.mendoza@biodontos.com"),
                    Doctor(id = "33333333-3333-3333-3333-333333333333", firstName = "Valentina", lastName = "Ríos", specialty = "Endodoncia & Periodoncia", email = "valentina.rios@biodontos.com")
                )
            }

            _state.update {
                it.copy(
                    patientId = patientId,
                    patientName = patientName,
                    doctors = loadedDoctors
                )
            }

            loadAppointments(patientId)
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val patientId = _state.value.patientId
            if (patientId.isNotEmpty()) {
                loadAppointments(patientId)
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadAppointments(patientId: String) {
        val result = getPatientAppointmentsUseCase(patientId)
        result.onSuccess { allAppointments ->
            val upcoming = allAppointments
                .filter { it.status == AppointmentStatus.SCHEDULED && DateTimeUtils.isUpcoming(it.startTime) }
                .sortedBy { it.startTime }

            val history = allAppointments
                .filter { it.status != AppointmentStatus.SCHEDULED || !DateTimeUtils.isUpcoming(it.startTime) }
                .sortedByDescending { it.startTime }

            _state.update {
                it.copy(
                    upcomingAppointments = upcoming,
                    historyAppointments = history,
                    errorMessage = null
                )
            }
        }.onFailure { ex ->
            _state.update { it.copy(errorMessage = "Error al cargar citas: ${ex.toUserFriendlyMessage()}") }
        }
    }

    private fun openBookingDialog() {
        val initialDoctor = _state.value.doctors.firstOrNull()
        val initialDate = DateTimeUtils.getTodayDate()
        _state.update {
            it.copy(
                bookingState = BookingDialogState(
                    selectedDoctorId = initialDoctor?.id,
                    selectedDate = initialDate,
                    isLoadingSlots = true
                )
            )
        }
        if (initialDoctor != null) {
            updateBookingSlots(initialDoctor.id, initialDate)
        }
    }

    private fun onBookingDoctorSelected(doctorId: String) {
        val currentDate = _state.value.bookingState?.selectedDate ?: DateTimeUtils.getTodayDate()
        _state.update { current ->
            current.copy(
                bookingState = current.bookingState?.copy(
                    selectedDoctorId = doctorId,
                    selectedSlot = null,
                    isLoadingSlots = true,
                    error = null
                )
            )
        }
        updateBookingSlots(doctorId, currentDate)
    }

    private fun onBookingDateSelected(date: String) {
        val doctorId = _state.value.bookingState?.selectedDoctorId
        _state.update { current ->
            current.copy(
                bookingState = current.bookingState?.copy(
                    selectedDate = date,
                    selectedSlot = null,
                    isLoadingSlots = true,
                    error = null
                )
            )
        }
        if (doctorId != null) {
            updateBookingSlots(doctorId, date)
        }
    }

    private fun updateBookingSlots(doctorId: String, date: String) {
        viewModelScope.launch {
            val busySlotsResult = getDoctorBusySlotsUseCase(doctorId, date)
            val taken = busySlotsResult.getOrDefault(emptyList())
            val available = DateTimeUtils.computeAvailableSlots(DateTimeUtils.CLINICAL_SLOTS, taken, selectedDate = date)
            _state.update { current ->
                current.copy(
                    bookingState = current.bookingState?.copy(
                        availableSlots = available,
                        isLoadingSlots = false
                    )
                )
            }
        }
    }

    private fun confirmBooking() {
        val booking = _state.value.bookingState ?: return
        val doctorId = booking.selectedDoctorId
        val date = booking.selectedDate
        val slot = booking.selectedSlot
        val reason = booking.reason.trim()

        if (doctorId == null) {
            _state.update { it.copy(bookingState = booking.copy(error = "Debes seleccionar un odontólogo")) }
            return
        }
        if (date.isBlank()) {
            _state.update { it.copy(bookingState = booking.copy(error = "Selecciona una fecha")) }
            return
        }
        if (slot == null) {
            _state.update { it.copy(bookingState = booking.copy(error = "Selecciona una franja horaria")) }
            return
        }
        if (reason.isBlank()) {
            _state.update { it.copy(bookingState = booking.copy(error = "Especifica el motivo de tu consulta")) }
            return
        }

        val patientId = _state.value.patientId
        val fullDateTime = DateTimeUtils.buildDateTimeString(date, slot)
        val doctor = _state.value.doctors.firstOrNull { it.id == doctorId }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val newAppointment = Appointment(
                patientId = patientId,
                doctorId = doctorId,
                startTime = fullDateTime,
                status = AppointmentStatus.SCHEDULED,
                isConfirmed = false,
                reason = reason
            )

            val result = bookAppointmentUseCase(newAppointment)
            result.onSuccess {
                _state.update {
                    it.copy(
                        bookingState = null,
                        isLoading = false,
                        successDialogState = SuccessDialogState(
                            title = "¡Solicitud de Cita Enviada!",
                            message = "Tu solicitud ha sido enviada al especialista. El doctor confirmará tu cita desde su perfil.",
                            doctorName = doctor?.fullName ?: "Especialista asignado",
                            specialty = doctor?.specialty ?: "Odontología Integral",
                            dateTimeFormatted = "${DateTimeUtils.formatToReadableDate(date)} - $slot"
                        )
                    )
                }
                loadAppointments(patientId)
            }.onFailure { ex ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        bookingState = booking.copy(error = ex.toUserFriendlyMessage())
                    )
                }
            }
        }
    }

    private fun openRescheduleDialog(appointment: Appointment) {
        val doctor = _state.value.doctors.firstOrNull { it.id == appointment.doctorId }
        val initialDate = DateTimeUtils.getTodayDate()
        _state.update {
            it.copy(
                rescheduleState = RescheduleDialogState(
                    appointment = appointment,
                    doctor = doctor,
                    selectedDate = initialDate,
                    isLoadingSlots = true
                )
            )
        }
        val docId = appointment.doctorId ?: _state.value.doctors.firstOrNull()?.id
        if (docId != null) {
            updateRescheduleSlots(docId, initialDate)
        }
    }

    private fun onRescheduleDateSelected(date: String) {
        val doctorId = _state.value.rescheduleState?.doctor?.id
            ?: _state.value.rescheduleState?.appointment?.doctorId
            ?: _state.value.doctors.firstOrNull()?.id

        _state.update { current ->
            current.copy(
                rescheduleState = current.rescheduleState?.copy(
                    selectedDate = date,
                    selectedSlot = null,
                    isLoadingSlots = true,
                    error = null
                )
            )
        }
        if (doctorId != null) {
            updateRescheduleSlots(doctorId, date)
        }
    }

    private fun updateRescheduleSlots(doctorId: String, date: String) {
        viewModelScope.launch {
            val busySlotsResult = getDoctorBusySlotsUseCase(doctorId, date)
            val taken = busySlotsResult.getOrDefault(emptyList())
            val available = DateTimeUtils.computeAvailableSlots(DateTimeUtils.CLINICAL_SLOTS, taken, selectedDate = date)
            _state.update { current ->
                current.copy(
                    rescheduleState = current.rescheduleState?.copy(
                        availableSlots = available,
                        isLoadingSlots = false
                    )
                )
            }
        }
    }

    private fun confirmReschedule() {
        val reschedule = _state.value.rescheduleState ?: return
        val date = reschedule.selectedDate
        val slot = reschedule.selectedSlot

        if (slot == null) {
            _state.update { it.copy(rescheduleState = reschedule.copy(error = "Selecciona una nueva franja horaria")) }
            return
        }

        val fullDateTime = DateTimeUtils.buildDateTimeString(date, slot)
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = rescheduleAppointmentUseCase(
                appointmentId = reschedule.appointment.id,
                newStartTime = fullDateTime
            )
            result.onSuccess {
                val doctor = reschedule.doctor
                _state.update {
                    it.copy(
                        rescheduleState = null,
                        isLoading = false,
                        successDialogState = SuccessDialogState(
                            title = "¡Cita Reagendada con Éxito!",
                            message = "Tu cita ha sido reprogramada satisfactoriamente.",
                            doctorName = doctor?.fullName ?: "Especialista asignado",
                            specialty = doctor?.specialty ?: "Odontología Integral",
                            dateTimeFormatted = "${DateTimeUtils.formatToReadableDate(date)} - $slot"
                        )
                    )
                }
                loadAppointments(_state.value.patientId)
            }.onFailure { ex ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        rescheduleState = reschedule.copy(error = ex.toUserFriendlyMessage())
                    )
                }
            }
        }
    }

    private fun confirmCancellation() {
        val cancelState = _state.value.cancelConfirmState ?: return
        val appointmentId = cancelState.appointment.id
        val reason = cancelState.selectedReasonChip
            ?: cancelState.customReason.trim().ifEmpty { "Cancelada a petición del paciente" }

        viewModelScope.launch {
            _state.update { it.copy(cancelConfirmState = cancelState.copy(isLoading = true)) }
            val result = cancelAppointmentUseCase(appointmentId, reason)
            result.onSuccess {
                _state.update { it.copy(cancelConfirmState = null) }
                _effects.send(PatientHomeEffect.ShowToast("Tu cita ha sido cancelada correctamente"))
                loadAppointments(_state.value.patientId)
            }.onFailure { ex ->
                _state.update {
                    it.copy(cancelConfirmState = cancelState.copy(isLoading = false))
                }
                _effects.send(PatientHomeEffect.ShowToast(ex.toUserFriendlyMessage()))
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _effects.send(PatientHomeEffect.NavigateToLogin)
        }
    }
}
