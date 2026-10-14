package com.example.biodontos.presentation.patients.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.repository.AnamnesisRepository
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.BillingRepository
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import com.example.biodontos.domain.repository.EvolutionRepository
import com.example.biodontos.domain.repository.InformedConsentRepository
import com.example.biodontos.domain.repository.MaterialRepository
import com.example.biodontos.domain.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class PatientDetailViewModel(
    private val patientId: String,
    private val patientRepository: PatientRepository,
    private val clinicalRecordRepository: ClinicalRecordRepository,
    private val anamnesisRepository: AnamnesisRepository,
    private val informedConsentRepository: InformedConsentRepository,
    private val evolutionRepository: EvolutionRepository,
    private val billingRepository: BillingRepository,
    private val appointmentRepository: AppointmentRepository,
    private val materialRepository: MaterialRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PatientDetailState(patientId = patientId))
    val state: StateFlow<PatientDetailState> = _state.asStateFlow()

    init {
        loadPatientDetail()
        loadMaterialsCatalog()
    }

    fun loadPatientDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Paciente
            patientRepository.getPatientById(patientId).onSuccess { p ->
                _state.update { it.copy(patient = p) }
            }.onFailure { err ->
                _state.update { it.copy(errorMessage = "Error al cargar paciente: ${err.toUserFriendlyMessage()}") }
            }

            // 2. Historia clínica
            val recordResult = clinicalRecordRepository.getRecordByPatientId(patientId)
            val record = recordResult.getOrNull()
            _state.update { it.copy(clinicalRecord = record) }

            // 3. Anamnesis
            anamnesisRepository.getAnamnesisByPatientId(patientId).onSuccess { a ->
                _state.update { it.copy(anamnesis = a) }
            }

            // 4. Consentimiento
            informedConsentRepository.getConsentByPatientId(patientId).onSuccess { c ->
                _state.update { it.copy(consent = c) }
            }

            // 5. Evoluciones (usando recordId si existe)
            if (record != null && record.id.isNotBlank()) {
                evolutionRepository.getEvolutionsByRecordId(record.id).onSuccess { evos ->
                    _state.update { it.copy(evolutions = evos) }
                }
            }

            // 6. Tratamientos
            billingRepository.getTreatmentsByPatientId(patientId).onSuccess { treats ->
                _state.update { it.copy(treatments = treats) }
            }

            // 7. Citas
            appointmentRepository.getAppointmentsByPatientId(patientId).onSuccess { appts ->
                _state.update { it.copy(appointments = appts) }
            }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun loadMaterialsCatalog() {
        viewModelScope.launch {
            materialRepository.getMaterials().onSuccess { mats ->
                _state.update {
                    it.copy(
                        materialsCatalog = mats,
                        selectedCatalogMaterial = mats.firstOrNull()
                    )
                }
            }
        }
    }

    fun selectTab(tab: PatientDetailTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // --- Control de Diálogo de Nueva Evolución ---
    fun openNewEvolutionDialog() {
        _state.update {
            it.copy(
                showNewEvolutionDialog = true,
                newEvolutionDescription = "",
                newEvolutionToothTreated = null,
                newEvolutionCost = "0",
                newEvolutionPayment = "0",
                newEvolutionMaterials = emptyList()
            )
        }
    }

    fun dismissNewEvolutionDialog() {
        _state.update { it.copy(showNewEvolutionDialog = false) }
    }

    fun onNewEvolutionDescriptionChange(value: String) {
        _state.update { it.copy(newEvolutionDescription = value) }
    }

    fun onNewEvolutionToothChange(tooth: Int?) {
        _state.update { it.copy(newEvolutionToothTreated = tooth) }
    }

    fun onNewEvolutionCostChange(value: String) {
        _state.update { it.copy(newEvolutionCost = value) }
    }

    fun onNewEvolutionPaymentChange(value: String) {
        _state.update { it.copy(newEvolutionPayment = value) }
    }

    fun onSelectCatalogMaterial(material: ClinicalMaterial) {
        _state.update { it.copy(selectedCatalogMaterial = material) }
    }

    fun addMaterialToEvolution(quantity: Double) {
        val mat = _state.value.selectedCatalogMaterial ?: return
        val used = EvolutionMaterialUsed(
            materialId = mat.id,
            materialName = mat.name,
            quantityUsed = quantity,
            lotNumberSnapshot = mat.batchLotNumber,
            invimaSnapshot = mat.invimaRegistration,
            expirationDateSnapshot = mat.expirationDate
        )
        _state.update { it.copy(newEvolutionMaterials = it.newEvolutionMaterials + used) }
    }

    fun removeMaterialFromEvolution(materialId: String) {
        _state.update {
            it.copy(newEvolutionMaterials = it.newEvolutionMaterials.filterNot { m -> m.materialId == materialId })
        }
    }

    fun saveEvolution() {
        val s = _state.value
        val recordId = s.clinicalRecord?.id ?: ""
        if (recordId.isBlank()) {
            _state.update { it.copy(errorMessage = "El paciente no tiene una historia clínica abierta aún.") }
            return
        }
        if (s.newEvolutionDescription.isBlank()) {
            _state.update { it.copy(errorMessage = "La descripción de la evolución es obligatoria.") }
            return
        }

        val cost = s.newEvolutionCost.toDoubleOrNull() ?: 0.0
        val payment = s.newEvolutionPayment.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            _state.update { it.copy(isSavingEvolution = true, errorMessage = null) }
            val evolution = Evolution(
                recordId = recordId,
                doctorId = "",
                date = LocalDate.now().toString(),
                procedureTime = "10:00:00",
                description = s.newEvolutionDescription.trim(),
                doctorName = "Doctor Tratante",
                toothTreated = s.newEvolutionToothTreated,
                cost = cost,
                paymentAmount = payment,
                balance = (cost - payment).coerceAtLeast(0.0),
                materialsUsed = s.newEvolutionMaterials
            )

            evolutionRepository.saveEvolution(evolution)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSavingEvolution = false,
                            showNewEvolutionDialog = false,
                            successMessage = "¡Nota de evolución registrada con éxito!"
                        )
                    }
                    loadPatientDetail()
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isSavingEvolution = false,
                            errorMessage = "Error al guardar evolución: ${err.toUserFriendlyMessage()}"
                        )
                    }
                }
        }
    }

    // --- Control de Diálogo de Nueva Cita ---
    fun openNewAppointmentDialog() {
        _state.update {
            it.copy(
                showNewAppointmentDialog = true,
                newAppointmentDate = LocalDate.now().plusDays(1).toString(),
                newAppointmentTime = "10:00",
                newAppointmentReason = "Control y seguimiento post-tratamiento"
            )
        }
    }

    fun dismissNewAppointmentDialog() {
        _state.update { it.copy(showNewAppointmentDialog = false) }
    }

    fun onNewAppointmentDateChange(date: String) {
        _state.update { it.copy(newAppointmentDate = date) }
    }

    fun onNewAppointmentTimeChange(time: String) {
        _state.update { it.copy(newAppointmentTime = time) }
    }

    fun onNewAppointmentReasonChange(reason: String) {
        _state.update { it.copy(newAppointmentReason = reason) }
    }

    fun saveAppointment() {
        val s = _state.value
        if (s.newAppointmentDate.isBlank()) {
            _state.update { it.copy(errorMessage = "La fecha de la cita es obligatoria") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSavingAppointment = true, errorMessage = null) }
            val appt = Appointment(
                patientId = patientId,
                doctorId = null,
                startTime = Appointment.parseDateTime("${s.newAppointmentDate}T${s.newAppointmentTime}:00"),
                reason = s.newAppointmentReason.trim().ifBlank { "Control dental" },
                status = AppointmentStatus.SCHEDULED,
                isConfirmed = false
            )

            appointmentRepository.createAppointment(appt)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSavingAppointment = false,
                            showNewAppointmentDialog = false,
                            successMessage = "¡Cita programada con éxito!"
                        )
                    }
                    loadPatientDetail()
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isSavingAppointment = false,
                            errorMessage = "Error al programar cita: ${err.toUserFriendlyMessage()}"
                        )
                    }
                }
        }
    }
}
