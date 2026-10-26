package com.example.biodontos.presentation.doctor.home

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.Treatment
import java.time.LocalDateTime

@Immutable
data class DoctorAppointmentItem(
    val appointmentId: String,
    val patientId: String = "",
    val patientName: String,
    val treatmentReason: String,
    val startTime: LocalDateTime,
    val isConfirmed: Boolean
)

@Immutable
data class DoctorPaymentItem(
    val id: String = "",
    val patientId: String = "",
    val patientName: String,
    val treatmentDescription: String = "",
    val dateStr: String,
    val amountCop: CopAmount,
    val paymentMethod: String = "EFECTIVO"
)

@Immutable
data class DoctorHomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val doctor: Doctor? = null,
    val doctorName: String = "",
    val doctorPhotoUrl: String? = null,
    val patientsCount: Int = 0,
    val consentedPatientsCount: Int = 0,
    val pendingConsentCount: Int = 0,
    val todayAppointmentsCount: Int = 0,
    val upcomingAppointments: List<DoctorAppointmentItem> = emptyList(),
    val recentPayments: List<DoctorPaymentItem> = emptyList(),
    val recentTotalCollectedCop: CopAmount = CopAmount.ZERO,
    val errorMessage: String? = null,
    // Estado del diálogo de registro de abono/pago
    val isRecordPaymentDialogOpen: Boolean = false,
    val isRecordingPayment: Boolean = false,
    val availablePatients: List<Patient> = emptyList(),
    val availableTreatments: List<Treatment> = emptyList(),
    val selectedPatient: Patient? = null,
    val selectedTreatment: Treatment? = null,
    val isLoadingTreatments: Boolean = false
)

sealed interface DoctorHomeUiEvent {
    data object Refresh : DoctorHomeUiEvent
    data class ToggleConfirmAppointment(val appointmentId: String, val currentConfirmed: Boolean) : DoctorHomeUiEvent
    data object Logout : DoctorHomeUiEvent

    // Eventos para gestión de abonos y pagos
    data object OpenRecordPaymentDialog : DoctorHomeUiEvent
    data object DismissRecordPaymentDialog : DoctorHomeUiEvent
    data object ClearSelectedPatient : DoctorHomeUiEvent
    data class SelectPatientForPayment(val patient: Patient) : DoctorHomeUiEvent
    data class SelectTreatmentForPayment(val treatment: Treatment?) : DoctorHomeUiEvent
    data class SubmitPayment(
        val patientId: String,
        val treatmentId: String?,
        val newTreatmentDescription: String?,
        val newTreatmentCost: Long?,
        val amount: Long,
        val paymentMethod: String
    ) : DoctorHomeUiEvent
}

sealed interface DoctorHomeUiEffect {
    data class ShowMessage(val message: String) : DoctorHomeUiEffect
    data object NavigateToLogin : DoctorHomeUiEffect
}
