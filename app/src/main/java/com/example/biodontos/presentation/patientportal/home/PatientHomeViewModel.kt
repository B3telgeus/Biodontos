package com.example.biodontos.presentation.patientportal.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.AuthRepository
import com.example.biodontos.domain.repository.BillingRepository
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.repository.PatientRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class PatientHomeViewModel(
    private val authRepository: AuthRepository,
    private val patientRepository: PatientRepository,
    private val appointmentRepository: AppointmentRepository,
    private val billingRepository: BillingRepository,
    private val doctorRepository: DoctorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientHomeUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<PatientHomeUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadDashboard()
    }

    fun onEvent(event: PatientHomeUiEvent) {
        when (event) {
            is PatientHomeUiEvent.Refresh -> loadDashboard()
            is PatientHomeUiEvent.NavigateToAppointments -> {
                viewModelScope.launch { _effects.send(PatientHomeUiEffect.NavigateToAppointments) }
            }
            is PatientHomeUiEvent.NavigateToPayments -> {
                viewModelScope.launch { _effects.send(PatientHomeUiEffect.NavigateToPayments) }
            }
            is PatientHomeUiEvent.NavigateToDoctors -> {
                viewModelScope.launch { _effects.send(PatientHomeUiEffect.NavigateToDoctors) }
            }
            is PatientHomeUiEvent.BookWithDoctor -> {
                viewModelScope.launch { _effects.send(PatientHomeUiEffect.NavigateToBooking(event.doctorId)) }
            }
            is PatientHomeUiEvent.Logout -> {
                logout()
            }
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val session = authRepository.getCurrentSession()
            if (session == null || session.role != UserRole.PATIENT) {
                _effects.send(PatientHomeUiEffect.NavigateToLogin)
                return@launch
            }

            // Obtener el paciente asociado
            val patientResult = patientRepository.getPatientByUserId(session.id)
            val patient = patientResult.getOrNull()
            val effectivePatientId = patient?.id ?: session.id
            val patientName = patient?.firstName ?: session.displayName.ifBlank { "Usuario" }

            // Cargas en paralelo con concurrencia estructurada
            val (appointmentsResult, billingResult, doctorsResult) = coroutineScope {
                val appointmentsDeferred = async { appointmentRepository.getAppointmentsByPatientId(effectivePatientId) }
                val billingDeferred = async { billingRepository.getPaymentSummaryByPatientId(effectivePatientId) }
                val doctorsDeferred = async { doctorRepository.getDoctors() }

                Triple(appointmentsDeferred.await(), billingDeferred.await(), doctorsDeferred.await())
            }

            if (appointmentsResult.isFailure && billingResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        patientName = patientName,
                        errorMessage = "No fue posible actualizar tu información clínica. Comprueba tu conexión."
                    )
                }
                return@launch
            }

            val now = LocalDateTime.now()
            val allAppointments = appointmentsResult.getOrDefault(emptyList())

            val upcoming = allAppointments
                .filter { it.status != AppointmentStatus.CANCELLED && it.status != AppointmentStatus.COMPLETED && it.startTime >= now }
                .sortedBy { it.startTime }

            val nextAppt = upcoming.firstOrNull()
            val followingAppts = upcoming.drop(1).take(3)
            val completedCount = allAppointments.count { it.status == AppointmentStatus.COMPLETED }

            val billingSummary = billingResult.getOrNull()
            val pendingBalance = billingSummary?.totalPending ?: CopAmount(0)

            val doctorsList = doctorsResult.getOrDefault(emptyList())

            // Determinar el último doctor con el que el paciente agendó una cita
            val lastAppointmentWithDoctor = allAppointments
                .filter { it.doctorId?.isNotBlank() == true }
                .maxByOrNull { it.startTime }

            val bookedDoctorId = lastAppointmentWithDoctor?.doctorId?.trim()
            val lastDoctor = if (!bookedDoctorId.isNullOrBlank()) {
                val matched = doctorsList.find { it.id.value == bookedDoctorId || it.userId == bookedDoctorId }
                matched ?: lastAppointmentWithDoctor.doctorName?.takeIf { it.isNotBlank() }?.let { rawName ->
                    val cleanName = rawName
                        .replace(Regex("""^(Dr\(a\)|Dra?|Doctor\(a\)|Doctora?)\.?\s*""", RegexOption.IGNORE_CASE), "")
                        .trim()
                    val parts = cleanName.split(Regex("""\s+""")).filter { it.isNotBlank() }
                    Doctor(
                        id = DoctorId(bookedDoctorId),
                        firstName = parts.firstOrNull() ?: "Doctor",
                        lastName = parts.drop(1).joinToString(" ").ifBlank { "Especialista" },
                        specialty = "Especialista Odontológico"
                    )
                } ?: Doctor(
                    id = DoctorId(bookedDoctorId),
                    firstName = "Doctor",
                    lastName = "Especialista",
                    specialty = "Especialista Odontológico"
                )
            } else {
                null
            }

            val patientDoctors = if (lastDoctor != null) listOf(lastDoctor) else emptyList()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    patientName = patientName,
                    patientId = effectivePatientId,
                    nextAppointment = nextAppt,
                    upcomingAppointments = followingAppts,
                    completedAppointmentsCount = completedCount,
                    pendingBalance = pendingBalance,
                    doctors = patientDoctors,
                    errorMessage = null
                )
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _effects.send(PatientHomeUiEffect.NavigateToLogin)
        }
    }
}
