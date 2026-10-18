package com.example.biodontos.presentation.admission

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DentitionType
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.model.PeriodontalSiteLocation
import com.example.biodontos.domain.model.PeriodontalSiteMeasurement
import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface

@Immutable
data class ClinicalAdmissionState(
    val currentStep: Int = 1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isFinished: Boolean = false,

    // Identificadores de sesión clínica
    val patientId: String = "",
    val clinicalRecordId: String = "",
    val doctorId: String = "",
    val doctorName: String = "Dr. Odontólogo",
    val doctorLicense: String = "OD-98234-COL",

    // --- PASO 1: Identificación del Paciente ---
    val dniCedula: String = "",
    val dniError: String? = null,
    val firstName: String = "",
    val firstLastName: String = "",
    val secondLastName: String = "",
    val nameError: String? = null,
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
    // Acudiente (guardians)
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

    // --- CONSENTIMIENTO INFORMADO DE OPERATORIA (Modal Especializado) ---
    val showOperativeConsentDialog: Boolean = false,
    val operativeConsentProposedTreatment: String = "",
    val operativeConsentSignerType: String = "PATIENT", // "PATIENT" o "GUARDIAN"
    val operativeConsentRepresentativeName: String = "",
    val operativeConsentRepresentativeId: String = "",
    val operativeConsentPatientSignature: String = "",
    val operativeConsentDoctorSignature: String = "",
    val operativeConsentProcedures: List<com.example.biodontos.domain.model.InformedConsentProcedure> = emptyList(),
    val operativeConsentSaved: Boolean = false,

    // --- PASO 2: Anamnesis (20 Preguntas) ---
    val tratamientoMedicoActual: Boolean = false,
    val tomaMedicamentos: Boolean = false,
    val alergias: Boolean = false,
    val cardiopatias: Boolean = false,
    val alteracionPresionArterial: Boolean = false,
    val embarazo: Boolean = false,
    val diabetes: Boolean = false,
    val hepatitis: Boolean = false,
    val irradiaciones: Boolean = false,
    val discrasiasSanguineas: Boolean = false,
    val fiebreReumatica: Boolean = false,
    val enfermedadesRenales: Boolean = false,
    val inmunosupresion: Boolean = false,
    val trastornosEmocionales: Boolean = false,
    val patologiaRespiratoria: Boolean = false,
    val trastornosGastricos: Boolean = false,
    val epilepsia: Boolean = false,
    val cirugias: Boolean = false,
    val enfermedadesOrales: Boolean = false,
    val otrasAlteraciones: Boolean = false,
    val anamnesisObservaciones: String = "",
    val antecedentesFamiliares: String = "",
    val medicamentosActuales: String = "",

    // --- PASO 3: Odontograma ---
    val dentitionType: DentitionType = DentitionType.ADULT,
    val teethSurfaces: Map<Int, Map<ToothSurface, ToothCondition>> = emptyMap(),
    val selectedOdontogramTooth: Int = 11,
    val selectedConditionToApply: ToothCondition = ToothCondition.CARIES,

    // --- PASO 4: Periodontograma ---
    val selectedPeriodontalTooth: Int = 11,
    val siteMeasurements: Map<Pair<Int, PeriodontalSiteLocation>, PeriodontalSiteMeasurement> = emptyMap(),
    val periodontalObservations: String = "",

    // --- PASO 5: Plan de Tratamiento & Consentimiento ---
    val mainDiagnosis: String = "",
    val relatedDiagnoses: String = "",
    val complementaryExams: String = "",
    val treatmentPlanDescription: String = "",
    val consentSigned: Boolean = false,
    val consentPatientSignature: String = "",

    // --- PASO 6: Evolución, Pagos & Materiales INVIMA ---
    val evolutionDescription: String = "",
    val evolutionCost: String = "0",
    val evolutionPayment: String = "0",
    val evolutionPatientSignature: String = "",
    val materialsCatalog: List<ClinicalMaterial> = emptyList(),
    val selectedCatalogMaterial: ClinicalMaterial? = null,
    val usedMaterials: List<EvolutionMaterialUsed> = emptyList(),
    val closeRecordOnFinish: Boolean = false,
    val closureSummary: String = "",

    // --- PASO 7: Agendamiento Próxima Cita ---
    val nextAppointmentDate: String = "",
    val nextAppointmentTime: String = "10:00",
    val nextAppointmentReason: String = "Control y seguimiento post-tratamiento",
    val nextAppointmentScheduled: Boolean = false
) {
    val evolutionBalance: Double
        get() {
            val c = evolutionCost.toDoubleOrNull() ?: 0.0
            val p = evolutionPayment.toDoubleOrNull() ?: 0.0
            return (c - p).coerceAtLeast(0.0)
        }

    val availableFdiTeeth: List<Int>
        get() = when (dentitionType) {
            DentitionType.ADULT -> listOf(
                18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28,
                48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38
            )
            DentitionType.CHILD -> listOf(
                55, 54, 53, 52, 51, 61, 62, 63, 64, 65,
                85, 84, 83, 82, 81, 71, 72, 73, 74, 75
            )
            DentitionType.MIXED -> listOf(
                16, 55, 54, 53, 52, 11, 21, 62, 63, 64, 65, 26,
                46, 85, 84, 83, 82, 41, 31, 72, 73, 74, 75, 36
            )
        }

    val isMinor: Boolean
        get() = try {
            val date = java.time.LocalDate.parse(birthDate)
            val years = java.time.Period.between(date, java.time.LocalDate.now()).years
            years < 18
        } catch (_: Exception) {
            false
        }
}
