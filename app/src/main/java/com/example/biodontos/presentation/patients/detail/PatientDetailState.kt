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
import com.example.biodontos.domain.model.Treatment

enum class PatientDetailTab(val label: String) {
    SUMMARY("Ficha & Salud"),
    ODONTOGRAM("Odontograma"),
    EVOLUTIONS("Evoluciones"),
    TREATMENTS("Tratamientos & Citas")
}

@Immutable
data class PatientDetailState(
    val patientId: String = "",
    val patient: Patient? = null,
    val clinicalRecord: ClinicalRecord? = null,
    val anamnesis: Anamnesis? = null,
    val consent: InformedConsent? = null,
    val evolutions: List<Evolution> = emptyList(),
    val treatments: List<Treatment> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val materialsCatalog: List<ClinicalMaterial> = emptyList(),
    val selectedTab: PatientDetailTab = PatientDetailTab.SUMMARY,
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
    val isSavingAppointment: Boolean = false
) {
    val totalBilled: Double
        get() = treatments.sumOf { it.totalCost.pesos.toDouble() }

    val totalPaid: Double
        get() = 0.0

    val pendingBalance: Double
        get() = totalBilled
}
