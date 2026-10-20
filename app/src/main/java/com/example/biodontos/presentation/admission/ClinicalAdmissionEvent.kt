package com.example.biodontos.presentation.admission

import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DentitionType
import com.example.biodontos.domain.model.PeriodontalSiteMeasurement
import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface

sealed interface ClinicalAdmissionEvent {
    data object NextStep : ClinicalAdmissionEvent
    data object PreviousStep : ClinicalAdmissionEvent
    data class JumpToStep(val step: Int) : ClinicalAdmissionEvent

    // Paso 1
    data class UpdateDni(val value: String) : ClinicalAdmissionEvent
    data class UpdateFirstName(val value: String) : ClinicalAdmissionEvent
    data class UpdateFirstLastName(val value: String) : ClinicalAdmissionEvent
    data class UpdateSecondLastName(val value: String) : ClinicalAdmissionEvent
    data class UpdateBirthDate(val value: String) : ClinicalAdmissionEvent
    data class UpdateBirthPlace(val value: String) : ClinicalAdmissionEvent
    data class UpdateEps(val value: String) : ClinicalAdmissionEvent
    data class UpdateBloodGroup(val group: String, val rh: String) : ClinicalAdmissionEvent
    data class UpdatePhone(val value: String) : ClinicalAdmissionEvent
    data class UpdateEmail(val value: String) : ClinicalAdmissionEvent
    data class UpdateAddress(val value: String) : ClinicalAdmissionEvent
    data class UpdateLocality(val value: String) : ClinicalAdmissionEvent
    data class UpdateOccupation(val value: String) : ClinicalAdmissionEvent
    data class UpdateMaritalStatus(val value: String) : ClinicalAdmissionEvent
    data class ToggleHasGuardian(val value: Boolean) : ClinicalAdmissionEvent
    data class UpdateGuardianRelationship(val value: String) : ClinicalAdmissionEvent
    data class UpdateGuardianLocality(val value: String) : ClinicalAdmissionEvent
    data object CopyAddressToGuardian : ClinicalAdmissionEvent
    data class UpdateGuardian(
        val relationship: String,
        val firstName: String,
        val lastName: String,
        val phone: String,
        val address: String,
        val locality: String = ""
    ) : ClinicalAdmissionEvent
    data class UpdateAntecedents(
        val lastVisit: String,
        val reason: String,
        val currentReason: String,
        val illnessHistory: String
    ) : ClinicalAdmissionEvent
    data class UpdateAllergies(val hasAllergies: Boolean, val description: String) : ClinicalAdmissionEvent

    // Consentimiento de Operatoria (Modal Especializado)
    data class ToggleOperativeConsentDialog(val show: Boolean) : ClinicalAdmissionEvent
    data class UpdateOperativeConsentTreatment(val treatment: String) : ClinicalAdmissionEvent
    data class UpdateOperativeConsentSigner(val signerType: String, val repName: String, val repId: String) : ClinicalAdmissionEvent
    data class AddOperativeConsentProcedure(val toothNumber: Int, val date: String) : ClinicalAdmissionEvent
    data class RemoveOperativeConsentProcedure(val index: Int) : ClinicalAdmissionEvent
    data class ConfirmOperativeConsentSignatures(val patientSig: String, val doctorSig: String) : ClinicalAdmissionEvent
    data object SaveOperativeConsent : ClinicalAdmissionEvent

    // Paso 2: Anamnesis
    data class ToggleAnamnesisQuestion(val questionKey: String, val value: Boolean) : ClinicalAdmissionEvent
    data class UpdateAnamnesisObservations(val observations: String, val familiares: String, val meds: String) : ClinicalAdmissionEvent

    // Paso 3: Odontograma
    data class ChangeDentitionType(val type: DentitionType) : ClinicalAdmissionEvent
    data class SelectOdontogramTooth(val toothNumber: Int) : ClinicalAdmissionEvent
    data class SelectConditionToApply(val condition: ToothCondition) : ClinicalAdmissionEvent
    data class ApplySurfaceCondition(val toothNumber: Int, val surface: ToothSurface) : ClinicalAdmissionEvent

    // Paso 4: Periodontograma
    data class SelectPeriodontalTooth(val toothNumber: Int) : ClinicalAdmissionEvent
    data class UpdatePeriodontalMeasurement(val measurement: PeriodontalSiteMeasurement) : ClinicalAdmissionEvent

    // Paso 5: Tratamiento & Consentimiento
    data class UpdateDiagnoses(val main: String, val related: String, val exams: String, val plan: String) : ClinicalAdmissionEvent
    data class ConfirmConsentSignature(val signature: String) : ClinicalAdmissionEvent

    // Paso 6: Evolución & Materiales
    data class UpdateEvolution(val description: String, val cost: String, val payment: String) : ClinicalAdmissionEvent
    data class SelectMaterialFromCatalog(val material: ClinicalMaterial) : ClinicalAdmissionEvent
    data class AddMaterialUsed(val quantity: Double) : ClinicalAdmissionEvent
    data class RemoveMaterialUsed(val materialId: String) : ClinicalAdmissionEvent
    data class ConfirmEvolutionSignature(val signature: String) : ClinicalAdmissionEvent
    data class ToggleCloseRecord(val close: Boolean, val summary: String) : ClinicalAdmissionEvent

    // Paso 7: Próxima Cita
    data class UpdateNextAppointment(val date: String, val time: String, val reason: String) : ClinicalAdmissionEvent
    data object FinishAdmission : ClinicalAdmissionEvent
}
