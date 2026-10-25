package com.example.biodontos.presentation.patients.detail

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.Anamnesis
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import com.example.biodontos.domain.model.history.ActivityFilter
import com.example.biodontos.domain.model.history.ActivityType
import com.example.biodontos.domain.model.history.PatientActivity
import com.example.biodontos.domain.model.history.PatientHistorySummary
import java.time.YearMonth

enum class PatientDetailTab(val label: String) {
    HISTORY("Historial"),
    SUMMARY("Ficha & Salud"),
    ODONTOGRAM("Odontograma"),
    EVOLUTIONS("Evoluciones"),
    TREATMENTS("Tratamientos & Citas")
}

sealed interface PatientDetailEffect {
    data class ShowSnackbar(val message: String) : PatientDetailEffect
}

@Immutable
data class PatientHistoryUiState(
    val allActivities: List<PatientActivity> = emptyList(),
    val filteredActivities: List<PatientActivity> = emptyList(),
    val filter: ActivityFilter = ActivityFilter(),
    val availableMonths: List<YearMonth> = emptyList(),
    val summary: PatientHistorySummary = PatientHistorySummary(),
    val failedSources: Set<ActivityType> = emptySet(),
    val isTimelineLoading: Boolean = false,
    val timelineError: String? = null,
    val expandedActivityId: String? = null,
    val showQuickActionSheet: Boolean = false,
    val voidDialogTarget: PatientActivity? = null,
    val voidReason: String = "",
    val voidConfirmationStep: Int = 1,
    val isProcessingVoid: Boolean = false,
    val clarificationTarget: Evolution? = null,
    val clarificationText: String = "",
    val isSavingClarification: Boolean = false,
    val statusDialogTarget: Treatment? = null,
    val selectedTargetStatus: TreatmentStatus? = null,
    val statusChangeReason: String = "",
    val isUpdatingTreatmentStatus: Boolean = false
)

@Immutable
data class PatientDetailState(
    val patientId: String = "",
    val patient: Patient? = null,
    val clinicalRecord: ClinicalRecord? = null,
    val anamnesis: Anamnesis? = null,
    val consent: InformedConsent? = null,
    val evolutions: List<Evolution> = emptyList(),
    val treatments: List<Treatment> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val materialsCatalog: List<ClinicalMaterial> = emptyList(),
    val selectedTab: PatientDetailTab = PatientDetailTab.HISTORY,
    val historyState: PatientHistoryUiState = PatientHistoryUiState(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Diálogo de Nueva Evolución
    val showNewEvolutionDialog: Boolean = false,
    val newEvolutionDescription: String = "",
    val newEvolutionToothTreated: Int? = null,
    val newEvolutionCost: String = "0",
    val newEvolutionPayment: String = "0",
    val newEvolutionMaterials: List<EvolutionMaterialUsed> = emptyList(),
    val selectedCatalogMaterial: ClinicalMaterial? = null,
    val isSavingEvolution: Boolean = false,

    // Diálogo de Nueva Cita para este paciente
    val showNewAppointmentDialog: Boolean = false,
    val newAppointmentDate: String = "",
    val newAppointmentTime: String = "10:00",
    val newAppointmentReason: String = "Control y seguimiento post-tratamiento",
    val isSavingAppointment: Boolean = false,

    // Diálogo de Nuevo Presupuesto de Tratamiento
    val showNewTreatmentDialog: Boolean = false,
    val newTreatmentDescription: String = "",
    val newTreatmentTotalCost: String = "",
    val isSavingTreatment: Boolean = false,

    // Diálogo de Registrar Pago / Abono
    val showNewPaymentDialog: Boolean = false,
    val selectedPaymentTreatmentId: String = "",
    val newPaymentAmount: String = "",
    val newPaymentMethod: String = "EFECTIVO",
    val isSavingPayment: Boolean = false
) {
    val totalBilled: Double
        get() = treatments
            .filter { TreatmentStatus.fromString(it.status) != TreatmentStatus.CANCELADO }
            .sumOf { it.totalCost.pesos.toDouble() }

    val totalPaid: Double
        get() = payments.filter { !it.isVoided }.sumOf { it.amount.pesos.toDouble() }

    val pendingBalance: Double
        get() = (totalBilled - totalPaid).coerceAtLeast(0.0)
}
