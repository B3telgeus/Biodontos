package com.example.biodontos.presentation.admission

import androidx.compose.runtime.Immutable
import com.example.biodontos.core.ui.UiText
import com.example.biodontos.domain.model.AnamnesisCondition
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.DentitionType
import java.time.LocalDate

enum class AdmissionStep(val number: Int, val title: String) {
    PATIENT_DATA(1, "Datos del Paciente"),
    ANAMNESIS(2, "Anamnesis Médica");

    companion object {
        const val MAX_STEP = 2
        fun fromNumber(number: Int): AdmissionStep =
            if (number >= 2) ANAMNESIS else PATIENT_DATA
    }
}

@Immutable
data class AdmissionCompletionResult(
    val patientId: String,
    val patientFullName: String,
    val dniCedula: String,
    val clinicalRecordId: String,
    val isNewAdmission: Boolean
)

@Immutable
data class ClinicalAdmissionState(
    val currentStep: Int = 1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val errorUiText: UiText? = null,
    val successMessage: String? = null,
    val successUiText: UiText? = null,
    val isFinished: Boolean = false,
    val completionResult: AdmissionCompletionResult? = null,

    val patientId: String = "",
    val clinicalRecordId: String = "",
    val doctorId: String = "",
    val doctorUserId: String? = null,
    val doctorName: String = "Dr. Odontólogo",
    val doctorLicense: String = "OD-98234-COL",
    val existingClinicalRecord: ClinicalRecord? = null,

    // Step 1: Patient Data
    val dniCedula: String = "",
    val dniError: String? = null,
    val dniErrorUiText: UiText? = null,
    val firstName: String = "",
    val firstLastName: String = "",
    val secondLastName: String = "",
    val nameError: String? = null,
    val nameErrorUiText: UiText? = null,
    val birthDate: String = "",
    val birthPlace: String = "Bogotá D.C.",
    val eps: String = "Particular",
    val bloodGroup: String = "O",
    val rhFactor: String = "+",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val locality: String = "",
    val occupation: String = "",
    val maritalStatus: String = "Soltero(a)",
    val hasGuardian: Boolean = false,
    val guardianId: String? = null,
    val guardianRelationship: String = "",
    val guardianFirstName: String = "",
    val guardianLastName: String = "",
    val guardianPhone: String = "",
    val guardianAddress: String = "",
    val guardianLocality: String = "",
    val lastDentalVisit: String = "",
    val consultationReason: String = "",
    val currentConsultationReason: String = "",
    val illnessHistory: String = "",
    val hasCriticalAllergies: Boolean = false,
    val allergiesDescription: String = "",
    val dentitionType: DentitionType = DentitionType.ADULT,
    val consentSigned: Boolean = false,

    // Step 2: Anamnesis
    val activeAnamnesisConditions: Set<AnamnesisCondition> = emptySet(),
    val anamnesisObservaciones: String = "",
    val antecedentesFamiliares: String = "",
    val medicamentosActuales: String = "",
    val anamnesisOptionNotes: Map<String, String> = emptyMap()
) {
    val tratamientoMedicoActual: Boolean get() = AnamnesisCondition.TRATAMIENTO_MEDICO in activeAnamnesisConditions
    val tomaMedicamentos: Boolean get() = AnamnesisCondition.TOMA_MEDICAMENTOS in activeAnamnesisConditions
    val alergias: Boolean get() = AnamnesisCondition.ALERGIAS in activeAnamnesisConditions
    val cardiopatias: Boolean get() = AnamnesisCondition.CARDIOPATIAS in activeAnamnesisConditions
    val alteracionPresionArterial: Boolean get() = AnamnesisCondition.ALTERACION_PRESION in activeAnamnesisConditions
    val embarazo: Boolean get() = AnamnesisCondition.EMBARAZO in activeAnamnesisConditions
    val diabetes: Boolean get() = AnamnesisCondition.DIABETES in activeAnamnesisConditions
    val hepatitis: Boolean get() = AnamnesisCondition.HEPATITIS in activeAnamnesisConditions
    val irradiaciones: Boolean get() = AnamnesisCondition.IRRADIACIONES in activeAnamnesisConditions
    val discrasiasSanguineas: Boolean get() = AnamnesisCondition.DISCRASIAS_SANGUINEAS in activeAnamnesisConditions
    val fiebreReumatica: Boolean get() = AnamnesisCondition.FIEBRE_REUMATICA in activeAnamnesisConditions
    val enfermedadesRenales: Boolean get() = AnamnesisCondition.ENFERMEDADES_RENALES in activeAnamnesisConditions
    val inmunosupresion: Boolean get() = AnamnesisCondition.INMUNOSUPRESION in activeAnamnesisConditions
    val trastornosEmocionales: Boolean get() = AnamnesisCondition.TRASTORNOS_EMOCIONALES in activeAnamnesisConditions
    val patologiaRespiratoria: Boolean get() = AnamnesisCondition.PATOLOGIA_RESPIRATORIA in activeAnamnesisConditions
    val trastornosGastricos: Boolean get() = AnamnesisCondition.TRASTORNOS_GASTRICOS in activeAnamnesisConditions
    val epilepsia: Boolean get() = AnamnesisCondition.EPILEPSIA in activeAnamnesisConditions
    val cirugias: Boolean get() = AnamnesisCondition.CIRUGIAS in activeAnamnesisConditions
    val enfermedadesOrales: Boolean get() = AnamnesisCondition.ENFERMEDADES_ORALES in activeAnamnesisConditions
    val otrasAlteraciones: Boolean get() = AnamnesisCondition.OTRAS_ALTERACIONES in activeAnamnesisConditions

    val patientFullName: String
        get() = listOf(firstName, firstLastName, secondLastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    val isMinor: Boolean
        get() = try {
            val date = LocalDate.parse(birthDate)
            java.time.Period.between(date, LocalDate.now()).years < 18
        } catch (_: Exception) {
            false
        }
}
