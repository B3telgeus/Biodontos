package com.example.biodontos.presentation.doctor.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.usecase.appointments.GetDoctorAppointmentsUseCase
import com.example.biodontos.domain.usecase.appointments.ToggleAppointmentConfirmationUseCase
import com.example.biodontos.domain.usecase.auth.LogoutUseCase
import com.example.biodontos.domain.usecase.billing.CreateTreatmentUseCase
import com.example.biodontos.domain.usecase.billing.GetDoctorRecentPaymentsUseCase
import com.example.biodontos.domain.usecase.billing.GetPatientTreatmentsUseCase
import com.example.biodontos.domain.usecase.billing.RecordPaymentUseCase
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

class DoctorHomeViewModel(
    private val getCurrentDoctorUseCase: GetCurrentDoctorUseCase,
    private val getDoctorAppointmentsUseCase: GetDoctorAppointmentsUseCase,
    private val toggleAppointmentConfirmationUseCase: ToggleAppointmentConfirmationUseCase,
    private val patientRepository: PatientRepository,
    private val logoutUseCase: LogoutUseCase,
    private val clinicClock: ClinicClock,
    private val getDoctorRecentPaymentsUseCase: GetDoctorRecentPaymentsUseCase,
    private val recordPaymentUseCase: RecordPaymentUseCase,
    private val createTreatmentUseCase: CreateTreatmentUseCase,
    private val getPatientTreatmentsUseCase: GetPatientTreatmentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorHomeUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<DoctorHomeUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

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
            is DoctorHomeUiEvent.OpenRecordPaymentDialog -> {
                openRecordPaymentDialog()
            }
            is DoctorHomeUiEvent.DismissRecordPaymentDialog -> {
                dismissRecordPaymentDialog()
            }
            is DoctorHomeUiEvent.ClearSelectedPatient -> {
                clearSelectedPatient()
            }
            is DoctorHomeUiEvent.SelectPatientForPayment -> {
                selectPatientForPayment(event.patient)
            }
            is DoctorHomeUiEvent.SelectTreatmentForPayment -> {
                _uiState.update { it.copy(selectedTreatment = event.treatment) }
            }
            is DoctorHomeUiEvent.SubmitPayment -> {
                submitPayment(
                    patientId = event.patientId,
                    treatmentId = event.treatmentId,
                    newTreatmentDescription = event.newTreatmentDescription,
                    newTreatmentCost = event.newTreatmentCost,
                    amount = event.amount,
                    paymentMethod = event.paymentMethod
                )
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

            try {
                coroutineScope {
                    val patientsDeferred = async { patientRepository.getPatients() }
                    val appointmentsDeferred = async { getDoctorAppointmentsUseCase(doctor.id.value) }
                    val paymentsDeferred = async { getDoctorRecentPaymentsUseCase(limit = 10) }

                    val patientsResult = patientsDeferred.await()
                    val appointmentsResult = appointmentsDeferred.await()
                    val recentPaymentsResult = paymentsDeferred.await()

                    val patientsList = patientsResult.getOrDefault(emptyList())
                    val patientsMap = patientsList.associateBy { it.id }

                    val consentedCount = patientsList.count { it.consentSigned }
                    val pendingCount = patientsList.count { !it.consentSigned }

                    val now = clinicClock.now()
                    val today = now.toLocalDate()

                    val appointmentsList = appointmentsResult.getOrDefault(emptyList())

                    val todayCount = appointmentsList.count {
                        it.startTime.toLocalDate() == today && it.status != AppointmentStatus.CANCELLED
                    }

                    val upcoming = appointmentsList
                        .filter {
                            it.status != AppointmentStatus.CANCELLED &&
                            it.status != AppointmentStatus.COMPLETED &&
                            !it.startTime.toLocalDate().isBefore(today)
                        }
                        .sortedBy { it.startTime }
                        .take(6)
                        .map { appt ->
                            val pat = patientsMap[appt.patientId]
                            DoctorAppointmentItem(
                                appointmentId = appt.id,
                                patientId = appt.patientId,
                                patientName = pat?.fullName ?: "Paciente",
                                treatmentReason = appt.reason?.ifBlank { "Control dental" } ?: "Control dental",
                                startTime = appt.startTime,
                                isConfirmed = appt.isConfirmed
                            )
                        }

                    val paymentRecords = recentPaymentsResult.getOrDefault(emptyList())
                    val mappedPayments = paymentRecords.map { record ->
                        DoctorPaymentItem(
                            id = record.id,
                            patientId = record.patientId,
                            patientName = record.patientName,
                            treatmentDescription = record.treatmentDescription,
                            dateStr = record.createdAt.format(dateFormatter),
                            amountCop = record.amount,
                            paymentMethod = record.paymentMethod
                        )
                    }

                    val totalCollected = paymentRecords.fold(CopAmount.ZERO) { acc, p -> acc + p.amount }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            doctor = doctor,
                            doctorName = doctor.displayName,
                            doctorPhotoUrl = doctor.photoUrl,
                            patientsCount = patientsList.size,
                            consentedPatientsCount = consentedCount,
                            pendingConsentCount = pendingCount,
                            todayAppointmentsCount = todayCount,
                            upcomingAppointments = upcoming,
                            recentPayments = mappedPayments,
                            recentTotalCollectedCop = totalCollected,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.message ?: "Error al sincronizar el panel del doctor"
                    )
                }
            }
        }
    }

    private fun clearSelectedPatient() {
        _uiState.update {
            it.copy(
                selectedPatient = null,
                selectedTreatment = null,
                availableTreatments = emptyList(),
                isLoadingTreatments = false
            )
        }
    }

    private fun openRecordPaymentDialog() {
        viewModelScope.launch {
            val patients = patientRepository.getPatients().getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    isRecordPaymentDialogOpen = true,
                    availablePatients = patients,
                    availableTreatments = emptyList(),
                    selectedPatient = null,
                    selectedTreatment = null,
                    isLoadingTreatments = false
                )
            }
        }
    }

    private fun dismissRecordPaymentDialog() {
        _uiState.update {
            it.copy(
                isRecordPaymentDialogOpen = false,
                selectedPatient = null,
                selectedTreatment = null,
                availableTreatments = emptyList()
            )
        }
    }

    private fun selectPatientForPayment(patient: Patient) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedPatient = patient,
                    selectedTreatment = null,
                    isLoadingTreatments = true
                )
            }
            val treatments = getPatientTreatmentsUseCase(patient.id).getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    availableTreatments = treatments,
                    isLoadingTreatments = false
                )
            }
        }
    }

    private fun submitPayment(
        patientId: String,
        treatmentId: String?,
        newTreatmentDescription: String?,
        newTreatmentCost: Long?,
        amount: Long,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecordingPayment = true) }

            val effectiveTreatmentId: String? = if (treatmentId.isNullOrBlank()) {
                val desc = newTreatmentDescription?.trim().orEmpty().ifBlank { "Tratamiento Odontológico" }
                val cost = newTreatmentCost ?: amount
                val treatmentResult = createTreatmentUseCase(
                    patientId = patientId,
                    description = desc,
                    totalCostPesos = cost
                )
                treatmentResult.getOrNull()?.id
            } else {
                treatmentId
            }

            if (effectiveTreatmentId == null) {
                _uiState.update { it.copy(isRecordingPayment = false) }
                _effects.send(DoctorHomeUiEffect.ShowMessage("No se pudo vincular o crear el tratamiento para el abono"))
                return@launch
            }

            val paymentResult = recordPaymentUseCase(
                treatmentId = effectiveTreatmentId,
                amountPesos = amount,
                paymentMethod = paymentMethod
            )

            paymentResult.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isRecordingPayment = false,
                            isRecordPaymentDialogOpen = false,
                            selectedPatient = null,
                            selectedTreatment = null
                        )
                    }
                    _effects.send(DoctorHomeUiEffect.ShowMessage("¡Abono registrado con éxito!"))
                    loadDashboard(isRefresh = true)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isRecordingPayment = false) }
                    _effects.send(DoctorHomeUiEffect.ShowMessage(error.message ?: "Error al registrar el abono"))
                }
            )
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
