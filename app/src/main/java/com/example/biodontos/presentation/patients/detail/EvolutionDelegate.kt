package com.example.biodontos.presentation.patients.detail

import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.domain.model.Cie10Code
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.CupsCode
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.repository.EvolutionRepository
import com.example.biodontos.domain.usecase.clinical.ValidateEvolutionConsentRequirementUseCase
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import com.example.biodontos.domain.usecase.materials.BuildEvolutionMaterialUsageUseCase
import java.time.LocalDate

class EvolutionDelegate(
    private val evolutionRepository: EvolutionRepository,
    private val getCurrentDoctorUseCase: GetCurrentDoctorUseCase?,
    private val validateEvolutionConsentRequirementUseCase: ValidateEvolutionConsentRequirementUseCase,
    private val buildEvolutionMaterialUsageUseCase: BuildEvolutionMaterialUsageUseCase,
    private val updateState: ((PatientDetailState) -> PatientDetailState) -> Unit
) {
    fun openNewEvolutionDialog(catalog: List<ClinicalMaterial>) {
        val nonExpired = catalog.firstOrNull { !it.isExpired() } ?: catalog.firstOrNull()
        updateState {
            it.copy(
                historyState = it.historyState.copy(showQuickActionSheet = false),
                showNewEvolutionDialog = true,
                newEvolutionCie10 = null,
                newEvolutionCups = null,
                newEvolutionCie10Error = null,
                newEvolutionCupsError = null,
                newEvolutionDescription = "",
                newEvolutionToothTreated = null,
                newEvolutionCost = "0",
                newEvolutionPayment = "0",
                newEvolutionMaterials = emptyList(),
                selectedCatalogMaterial = nonExpired,
                errorMessage = null
            )
        }
    }

    fun dismissNewEvolutionDialog() {
        updateState { it.copy(showNewEvolutionDialog = false) }
    }

    fun onDescriptionChange(value: String, state: PatientDetailState) {
        updateState { it.copy(newEvolutionDescription = value) }
        recheckConsent(state.copy(newEvolutionDescription = value))
    }

    fun onCie10Change(cie10: Cie10Code?) {
        updateState { it.copy(newEvolutionCie10 = cie10, newEvolutionCie10Error = null, errorMessage = null) }
    }

    fun onCupsChange(cups: CupsCode?, state: PatientDetailState) {
        updateState { it.copy(newEvolutionCups = cups, newEvolutionCupsError = null, errorMessage = null) }
        recheckConsent(state.copy(newEvolutionCups = cups))
    }

    fun onToothChange(tooth: Int?, state: PatientDetailState) {
        updateState { it.copy(newEvolutionToothTreated = tooth) }
        recheckConsent(state.copy(newEvolutionToothTreated = tooth))
    }

    fun recheckConsent(s: PatientDetailState) {
        val combinedDescription = listOfNotNull(
            s.newEvolutionCups?.description,
            s.newEvolutionDescription.takeIf { it.isNotBlank() }
        ).joinToString(" ")
        val status = validateEvolutionConsentRequirementUseCase(
            description = combinedDescription,
            toothTreated = s.newEvolutionToothTreated,
            consents = s.consents
        )
        updateState { it.copy(evolutionConsentStatus = status) }
    }

    fun onCostChange(value: String) {
        updateState { it.copy(newEvolutionCost = value) }
    }

    fun onPaymentChange(value: String) {
        updateState { it.copy(newEvolutionPayment = value) }
    }

    fun onSelectCatalogMaterial(material: ClinicalMaterial) {
        if (material.isExpired()) {
            updateState {
                it.copy(errorMessage = "El material '${material.name}' se encuentra vencido y no puede ser seleccionado.")
            }
            return
        }
        updateState { it.copy(selectedCatalogMaterial = material, errorMessage = null) }
    }

    suspend fun addMaterial(quantity: Double, state: PatientDetailState) {
        val mat = state.selectedCatalogMaterial ?: run {
            updateState { it.copy(errorMessage = "Seleccione un material del catálogo.") }
            return
        }

        buildEvolutionMaterialUsageUseCase(material = mat, quantityUsed = quantity).fold(
            ifLeft = { err ->
                updateState { it.copy(errorMessage = err.userMessage) }
            },
            ifRight = { used ->
                updateState { current ->
                    val existingIndex = current.newEvolutionMaterials.indexOfFirst { it.materialId == used.materialId }
                    val updatedList = if (existingIndex >= 0) {
                        current.newEvolutionMaterials.toMutableList().apply {
                            this[existingIndex] = this[existingIndex].copy(
                                quantityUsed = this[existingIndex].quantityUsed + quantity
                            )
                        }
                    } else {
                        current.newEvolutionMaterials + used
                    }
                    current.copy(newEvolutionMaterials = updatedList, errorMessage = null)
                }
            }
        )
    }

    fun removeMaterial(materialId: String) {
        updateState {
            it.copy(newEvolutionMaterials = it.newEvolutionMaterials.filterNot { m -> m.materialId == materialId })
        }
    }

    suspend fun saveEvolution(state: PatientDetailState, onSaved: suspend () -> Unit) {
        val recordId = state.clinicalRecord?.id ?: ""
        if (recordId.isBlank()) {
            updateState { it.copy(errorMessage = "El paciente no tiene una historia clínica abierta aún.") }
            return
        }
        if (state.newEvolutionDescription.isBlank()) {
            updateState { it.copy(errorMessage = "La descripción de la evolución es obligatoria.") }
            return
        }

        // CIE-10 selection validation
        if (state.newEvolutionCie10 == null) {
            updateState {
                it.copy(
                    newEvolutionCie10Error = "Seleccione un código CIE-10",
                    errorMessage = "Debe seleccionar un código CIE-10."
                )
            }
            return
        }

        // CUPS is optional - non-blocking! ("sin bloquear guardado")
        updateState { it.copy(newEvolutionCupsError = null) }

        var doctorId = ""
        var doctorName = "Doctor Tratante"
        val doctorRes = getCurrentDoctorUseCase?.invoke()
        if (doctorRes != null && doctorRes.isSuccess) {
            val doc = doctorRes.getOrNull()
            if (doc != null) {
                doctorId = doc.id.value.trim()
                doctorName = doc.fullName.ifBlank { "Doctor Tratante" }
            }
        }

        val cost = state.newEvolutionCost.toDoubleOrNull() ?: 0.0
        val payment = state.newEvolutionPayment.toDoubleOrNull() ?: 0.0

        updateState { it.copy(isSavingEvolution = true, errorMessage = null) }

        val evolution = Evolution(
            recordId = recordId,
            doctorId = doctorId,
            date = LocalDate.now().toString(),
            procedureTime = "10:00:00",
            description = state.newEvolutionDescription.trim(),
            doctorName = doctorName,
            toothTreated = state.newEvolutionToothTreated,
            cost = cost,
            paymentAmount = payment,
            balance = (cost - payment).coerceAtLeast(0.0),
            materialsUsed = state.newEvolutionMaterials,
            cie10Code = state.newEvolutionCie10?.code,
            cie10Description = state.newEvolutionCie10?.description,
            cupsCode = state.newEvolutionCups?.code,
            cupsDescription = state.newEvolutionCups?.description
        )

        evolutionRepository.saveEvolution(evolution)
            .onSuccess {
                updateState {
                    it.copy(
                        isSavingEvolution = false,
                        showNewEvolutionDialog = false,
                        newEvolutionCie10 = null,
                        newEvolutionCups = null,
                        newEvolutionCie10Error = null,
                        newEvolutionCupsError = null,
                        newEvolutionDescription = "",
                        newEvolutionToothTreated = null,
                        newEvolutionCost = "0",
                        newEvolutionPayment = "0",
                        newEvolutionMaterials = emptyList(),
                        successMessage = "Evolución clínica registrada exitosamente"
                    )
                }
                onSaved()
            }
            .onFailure { err ->
                updateState {
                    it.copy(
                        isSavingEvolution = false,
                        errorMessage = "Error al guardar evolución: ${err.toUserFriendlyMessage()}"
                    )
                }
            }
    }
}
