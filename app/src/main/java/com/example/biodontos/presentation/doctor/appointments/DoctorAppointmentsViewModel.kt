package com.example.biodontos.presentation.doctor.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.usecase.appointments.BookAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.CancelAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.GetDoctorAppointmentsUseCase
import com.example.biodontos.domain.usecase.appointments.RescheduleAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.ToggleAppointmentConfirmationUseCase
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import com.example.biodontos.domain.usecase.doctor.GetDoctorAvailabilityUseCase
import com.example.biodontos.domain.usecase.doctor.SaveDoctorAvailabilityUseCase
import com.example.biodontos.presentation.doctor.home.DoctorAppointmentItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DoctorAppointmentsViewModel(
    private val getCurrentDoctorUseCase: GetCurrentDoctorUseCase,
    private val getDoctorAppointmentsUseCase: GetDoctorAppointmentsUseCase,
    private val toggleAppointmentConfirmationUseCase: ToggleAppointmentConfirmationUseCase,
    private val getDoctorAvailabilityUseCase: GetDoctorAvailabilityUseCase,
    private val saveDoctorAvailabilityUseCase: SaveDoctorAvailabilityUseCase,
    private val bookAppointmentUseCase: BookAppointmentUseCase,
    private val rescheduleAppointmentUseCase: RescheduleAppointmentUseCase,
    private val cancelAppointmentUseCase: CancelAppointmentUseCase,
    private val patientRepository: PatientRepository,
    val clinicClock: ClinicClock
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DoctorAppointmentsUiState(
            isLoading = true,
            selectedDate = clinicClock.now().toLocalDate()
        )
    )
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
            is DoctorAppointmentsUiEvent.SelectDate -> {
                _uiState.update { current ->
                    val recalculated = computeTimelineSlots(
                        targetDate = event.date,
                        availability = current.weeklyAvailability,
                        appointments = current.appointments,
                        currentTime = clinicClock.now()
                    )
                    current.copy(
                        selectedDate = event.date,
                        timelineSlots = recalculated.first,
                        isDayOff = recalculated.second
                    )
                }
            }
            is DoctorAppointmentsUiEvent.ChangeDisplayMode -> {
                _uiState.update { it.copy(displayMode = event.mode) }
            }
            is DoctorAppointmentsUiEvent.SelectFilter -> {
                _uiState.update { it.copy(selectedFilter = event.filter) }
            }
            is DoctorAppointmentsUiEvent.ToggleConfirm -> {
                toggleConfirm(event.appointmentId, event.currentConfirmed)
            }
            is DoctorAppointmentsUiEvent.OpenAvailabilityEditor -> {
                _uiState.update { it.copy(showAvailabilityDialog = true) }
            }
            is DoctorAppointmentsUiEvent.DismissAvailabilityEditor -> {
                _uiState.update { it.copy(showAvailabilityDialog = false) }
            }
            is DoctorAppointmentsUiEvent.UpdateDayAvailability -> {
                updateDayConfig(event.day, event.isActive, event.startTime, event.endTime, event.slotMinutes)
            }
            is DoctorAppointmentsUiEvent.CopyWeekdayAvailability -> {
                copyWeekdayConfig(event.sourceDay)
            }
            is DoctorAppointmentsUiEvent.SaveAvailability -> {
                saveAvailability()
            }
            is DoctorAppointmentsUiEvent.OpenQuickBooking -> {
                _uiState.update { it.copy(bookingTargetSlot = event.slot) }
            }
            is DoctorAppointmentsUiEvent.DismissQuickBooking -> {
                _uiState.update { it.copy(bookingTargetSlot = null) }
            }
            is DoctorAppointmentsUiEvent.ConfirmQuickBooking -> {
                confirmQuickBooking(event.patientId, event.reason)
            }
            is DoctorAppointmentsUiEvent.OpenReschedule -> {
                openReschedule(event.appointment)
            }
            is DoctorAppointmentsUiEvent.DismissReschedule -> {
                _uiState.update { it.copy(reschedulingAppointment = null, rescheduleAvailableSlots = emptyList()) }
            }
            is DoctorAppointmentsUiEvent.ChangeRescheduleTargetDate -> {
                updateRescheduleTargetDate(event.date)
            }
            is DoctorAppointmentsUiEvent.ConfirmReschedule -> {
                confirmReschedule(event.newStartTime)
            }
            is DoctorAppointmentsUiEvent.RequestCancelAppointment -> {
                _uiState.update { it.copy(cancellingAppointment = event.appointment) }
            }
            is DoctorAppointmentsUiEvent.DismissCancelAppointment -> {
                _uiState.update { it.copy(cancellingAppointment = null) }
            }
            is DoctorAppointmentsUiEvent.ConfirmCancelAppointment -> {
                confirmCancelAppointment()
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

            // Pacientes registrados
            val patientsResult = patientRepository.getPatients()
            val patientsList = patientsResult.getOrDefault(emptyList())
            val patientsMap = patientsList.associateBy { it.id }

            // Citas del doctor
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

            // Disponibilidad semanal
            val rawAvailabilities = getDoctorAvailabilityUseCase(doctor.id.value).getOrDefault(emptyList())
            val weeklyConfig = normalizeWeeklyAvailability(rawAvailabilities)

            val currentDate = _uiState.value.selectedDate
            val (slots, isDayOff) = computeTimelineSlots(
                targetDate = currentDate,
                availability = weeklyConfig,
                appointments = items,
                currentTime = clinicClock.now()
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    doctorName = doctor.displayName,
                    doctorId = doctor.id.value,
                    appointments = items,
                    weeklyAvailability = weeklyConfig,
                    registeredPatients = patientsList,
                    timelineSlots = slots,
                    isDayOff = isDayOff,
                    errorMessage = null
                )
            }
        }
    }

    private fun normalizeWeeklyAvailability(raw: List<WeeklyAvailability>): List<DayAvailabilityConfig> {
        val byDay = raw.associateBy { it.dayOfWeek }
        return DayOfWeek.entries.map { day ->
            val existing = byDay[day]
            if (existing != null) {
                DayAvailabilityConfig(
                    dayOfWeek = day,
                    isActive = existing.isActive,
                    startTime = existing.startTime,
                    endTime = existing.endTime,
                    slotMinutes = existing.slotMinutes
                )
            } else {
                val isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
                DayAvailabilityConfig(
                    dayOfWeek = day,
                    isActive = !isWeekend,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(17, 0),
                    slotMinutes = 30
                )
            }
        }
    }

    private fun computeTimelineSlots(
        targetDate: LocalDate,
        availability: List<DayAvailabilityConfig>,
        appointments: List<DoctorAppointmentItem>,
        currentTime: LocalDateTime
    ): Pair<List<DoctorTimelineSlot>, Boolean> {
        val dayConfig = availability.find { it.dayOfWeek == targetDate.dayOfWeek }
        if (dayConfig == null || !dayConfig.isActive) {
            return Pair(emptyList(), true)
        }

        val slots = mutableListOf<DoctorTimelineSlot>()
        var slotStart = targetDate.atTime(dayConfig.startTime)
        val dayEnd = targetDate.atTime(dayConfig.endTime)
        val step = Duration.ofMinutes(dayConfig.slotMinutes.toLong())

        val dayAppointments = appointments.filter { it.startTime.toLocalDate() == targetDate }

        while (true) {
            val slotEnd = slotStart.plus(step)
            if (slotEnd.isAfter(dayEnd)) break

            val matchedAppt = dayAppointments.find { appt ->
                appt.startTime >= slotStart && appt.startTime < slotEnd
            }

            val status = when {
                matchedAppt != null -> DoctorSlotStatus.Occupied(matchedAppt)
                slotStart.isBefore(currentTime) -> DoctorSlotStatus.Past
                else -> DoctorSlotStatus.Available
            }

            slots.add(DoctorTimelineSlot(startTime = slotStart, endTime = slotEnd, status = status))
            slotStart = slotEnd
        }

        return Pair(slots, false)
    }

    private fun updateDayConfig(
        day: DayOfWeek,
        isActive: Boolean,
        startTime: LocalTime,
        endTime: LocalTime,
        slotMinutes: Int
    ) {
        _uiState.update { current ->
            val updated = current.weeklyAvailability.map { config ->
                if (config.dayOfWeek == day) {
                    config.copy(
                        isActive = isActive,
                        startTime = startTime,
                        endTime = endTime,
                        slotMinutes = slotMinutes
                    )
                } else config
            }
            current.copy(weeklyAvailability = updated)
        }
    }

    private fun copyWeekdayConfig(sourceDay: DayOfWeek) {
        _uiState.update { current ->
            val source = current.weeklyAvailability.find { it.dayOfWeek == sourceDay } ?: return@update current
            val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            val updated = current.weeklyAvailability.map { config ->
                if (config.dayOfWeek in weekdays) {
                    config.copy(
                        isActive = source.isActive,
                        startTime = source.startTime,
                        endTime = source.endTime,
                        slotMinutes = source.slotMinutes
                    )
                } else config
            }
            current.copy(weeklyAvailability = updated)
        }
    }

    private fun saveAvailability() {
        val doctorId = _uiState.value.doctorId
        if (doctorId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAvailability = true) }

            val domainList = _uiState.value.weeklyAvailability.map { config ->
                WeeklyAvailability(
                    doctorId = DoctorId(doctorId),
                    dayOfWeek = config.dayOfWeek,
                    startTime = config.startTime,
                    endTime = config.endTime,
                    slotMinutes = config.slotMinutes,
                    isActive = config.isActive
                )
            }

            val result = saveDoctorAvailabilityUseCase(doctorId, domainList)
            result.fold(
                onSuccess = {
                    val recalculated = computeTimelineSlots(
                        targetDate = _uiState.value.selectedDate,
                        availability = _uiState.value.weeklyAvailability,
                        appointments = _uiState.value.appointments,
                        currentTime = clinicClock.now()
                    )
                    _uiState.update {
                        it.copy(
                            isSavingAvailability = false,
                            showAvailabilityDialog = false,
                            timelineSlots = recalculated.first,
                            isDayOff = recalculated.second
                        )
                    }
                    _effects.send(DoctorAppointmentsUiEffect.ShowMessage("Horario semanal actualizado exitosamente"))
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSavingAvailability = false) }
                    _effects.send(DoctorAppointmentsUiEffect.ShowMessage(error.toUserFriendlyMessage("Error al guardar el horario")))
                }
            )
        }
    }

    private fun confirmQuickBooking(patientId: String, reason: String) {
        val slot = _uiState.value.bookingTargetSlot ?: return
        val doctorId = _uiState.value.doctorId
        if (doctorId.isBlank() || patientId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true) }

            val result = bookAppointmentUseCase(
                patientId = patientId,
                doctorId = doctorId,
                startTime = slot.startTime,
                endTime = slot.endTime,
                reason = reason.ifBlank { "Control odontológico" }
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isBooking = false, bookingTargetSlot = null) }
                    _effects.send(DoctorAppointmentsUiEffect.ShowMessage("Cita reservada con éxito"))
                    loadAppointments(isRefresh = true)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isBooking = false) }
                    _effects.send(DoctorAppointmentsUiEffect.ShowMessage(error.message ?: "No se pudo agendar la cita"))
                }
            )
        }
    }

    private fun openReschedule(appointment: DoctorAppointmentItem) {
        val initialTargetDate = appointment.startTime.toLocalDate()
        val availableSlots = computeAvailableStartTimes(initialTargetDate)
        _uiState.update {
            it.copy(
                reschedulingAppointment = appointment,
                rescheduleTargetDate = initialTargetDate,
                rescheduleAvailableSlots = availableSlots
            )
        }
    }

    private fun updateRescheduleTargetDate(date: LocalDate) {
        val availableSlots = computeAvailableStartTimes(date)
        _uiState.update {
            it.copy(
                rescheduleTargetDate = date,
                rescheduleAvailableSlots = availableSlots
            )
        }
    }

    private fun computeAvailableStartTimes(targetDate: LocalDate): List<LocalDateTime> {
        val dayConfig = _uiState.value.weeklyAvailability.find { it.dayOfWeek == targetDate.dayOfWeek }
        if (dayConfig == null || !dayConfig.isActive) return emptyList()

        val candidateSlots = mutableListOf<LocalDateTime>()
        var slotStart = targetDate.atTime(dayConfig.startTime)
        val dayEnd = targetDate.atTime(dayConfig.endTime)
        val step = Duration.ofMinutes(dayConfig.slotMinutes.toLong())
        val now = clinicClock.now()

        val dayAppointments = _uiState.value.appointments.filter { it.startTime.toLocalDate() == targetDate }

        while (true) {
            val slotEnd = slotStart.plus(step)
            if (slotEnd.isAfter(dayEnd)) break

            val isOccupied = dayAppointments.any { it.startTime >= slotStart && it.startTime < slotEnd }
            if (!isOccupied && slotStart.isAfter(now)) {
                candidateSlots.add(slotStart)
            }
            slotStart = slotEnd
        }

        return candidateSlots
    }

    private fun confirmReschedule(newStartTime: LocalDateTime) {
        val appointment = _uiState.value.reschedulingAppointment ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRescheduling = true) }

            val result = rescheduleAppointmentUseCase(
                appointmentId = appointment.appointmentId,
                newStartTime = newStartTime
            )

            result.fold(
                ifLeft = { error ->
                    _uiState.update { it.copy(isRescheduling = false) }
                    _effects.send(DoctorAppointmentsUiEffect.ShowMessage("No se pudo reagendar: $error"))
                },
                ifRight = {
                    _uiState.update {
                        it.copy(
                            isRescheduling = false,
                            reschedulingAppointment = null,
                            rescheduleAvailableSlots = emptyList()
                        )
                    }
                    _effects.send(DoctorAppointmentsUiEffect.ShowMessage("Cita reagendada exitosamente"))
                    loadAppointments(isRefresh = true)
                }
            )
        }
    }

    private fun confirmCancelAppointment() {
        val appointment = _uiState.value.cancellingAppointment ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true) }

            val result = cancelAppointmentUseCase(appointment.appointmentId)
            result.fold(
                ifLeft = { error ->
                    _uiState.update { it.copy(isCancelling = false) }
                    _effects.send(DoctorAppointmentsUiEffect.ShowMessage("No se pudo cancelar la cita: $error"))
                },
                ifRight = {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            cancellingAppointment = null
                        )
                    }
                    _effects.send(DoctorAppointmentsUiEffect.ShowMessage("Cita cancelada correctamente"))
                    loadAppointments(isRefresh = true)
                }
            )
        }
    }

    private fun toggleConfirm(appointmentId: String, currentConfirmed: Boolean) {
        viewModelScope.launch {
            val newStatus = !currentConfirmed
            _uiState.update { current ->
                val updated = current.appointments.map { item ->
                    if (item.appointmentId == appointmentId) item.copy(isConfirmed = newStatus) else item
                }
                val recalculated = computeTimelineSlots(
                    targetDate = current.selectedDate,
                    availability = current.weeklyAvailability,
                    appointments = updated,
                    currentTime = clinicClock.now()
                )
                current.copy(
                    appointments = updated,
                    timelineSlots = recalculated.first,
                    isDayOff = recalculated.second
                )
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
                        val recalculated = computeTimelineSlots(
                            targetDate = current.selectedDate,
                            availability = current.weeklyAvailability,
                            appointments = reverted,
                            currentTime = clinicClock.now()
                        )
                        current.copy(
                            appointments = reverted,
                            timelineSlots = recalculated.first,
                            isDayOff = recalculated.second
                        )
                    }
                    _effects.send(DoctorAppointmentsUiEffect.ShowMessage("No se pudo actualizar el estado de confirmación"))
                }
            )
        }
    }
}
