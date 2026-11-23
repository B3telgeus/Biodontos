package com.example.biodontos.presentation.admission

import com.example.biodontos.domain.model.DentitionType

sealed interface ClinicalAdmissionEvent {
    data object NextStep : ClinicalAdmissionEvent
    data object PreviousStep : ClinicalAdmissionEvent
    data class JumpToStep(val step: Int) : ClinicalAdmissionEvent
    data object ClearMessages : ClinicalAdmissionEvent
    data object DismissSuccessDialog : ClinicalAdmissionEvent

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
        val locality: String = "",
    ) : ClinicalAdmissionEvent
    data class UpdateAntecedents(
        val lastVisit: String,
        val reason: String,
        val currentReason: String,
        val illnessHistory: String
    ) : ClinicalAdmissionEvent
    data class UpdateAllergies(val hasAllergies: Boolean, val description: String) : ClinicalAdmissionEvent

    data class ToggleAnamnesisQuestion(val questionKey: String, val value: Boolean) : ClinicalAdmissionEvent
    data class UpdateAnamnesisOptionNote(val questionKey: String, val note: String) : ClinicalAdmissionEvent
    data class UpdateAnamnesisObservations(val observations: String, val familiares: String, val meds: String) : ClinicalAdmissionEvent

    data class ChangeDentitionType(val type: DentitionType) : ClinicalAdmissionEvent
    data object FinishAdmission : ClinicalAdmissionEvent
}
