package com.example.biodontos.presentation.admission

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.R
import com.example.biodontos.core.ui.UiText
import com.example.biodontos.core.util.SystemClinicClock
import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.data.local.dao.AdmissionDraftDao
import com.example.biodontos.data.local.entity.AdmissionDraftEntity
import com.example.biodontos.domain.model.*
import com.example.biodontos.domain.model.history.AuditAction
import com.example.biodontos.domain.repository.AnamnesisRepository
import com.example.biodontos.domain.repository.ClinicalAuditRepository
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import com.example.biodontos.domain.repository.GuardianRepository
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ClinicalAdmissionViewModel(
    private val patientRepository: PatientRepository,
    private val clinicalRecordRepository: ClinicalRecordRepository,
    private val anamnesisRepository: AnamnesisRepository,
    private val guardianRepository: GuardianRepository,
    private val initialPatientId: String? = null,
    private val getCurrentDoctorUseCase: GetCurrentDoctorUseCase? = null,
    private val admissionDraftDao: AdmissionDraftDao? = null,
    private val savedStateHandle: SavedStateHandle? = null,
    private val clinicalAuditRepository: ClinicalAuditRepository? = null
) : ViewModel() {

    private val _state = MutableStateFlow(
        ClinicalAdmissionState(
            patientId = savedStateHandle?.get<String>(KEY_PATIENT_ID).orEmpty(),
            clinicalRecordId = savedStateHandle?.get<String>(KEY_CLINICAL_RECORD_ID).orEmpty(),
            currentStep = (savedStateHandle?.get<Int>(KEY_CURRENT_STEP) ?: 1).coerceIn(1, AdmissionStep.MAX_STEP)
        )
    )
    val state: StateFlow<ClinicalAdmissionState> = _state.asStateFlow()

    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    init {
        loadCurrentDoctor()
        if (!initialPatientId.isNullOrBlank()) {
            loadExistingPatientData(initialPatientId)
        }
        setupSavedStateSync()
        setupDraftAutoSave()
    }

    private fun setupSavedStateSync() {
        val handle = savedStateHandle ?: return
        viewModelScope.launch {
            _state.collect { s ->
                if (s.patientId.isNotBlank()) {
                    handle[KEY_PATIENT_ID] = s.patientId
                }
                if (s.clinicalRecordId.isNotBlank()) {
                    handle[KEY_CLINICAL_RECORD_ID] = s.clinicalRecordId
                }
                handle[KEY_CURRENT_STEP] = s.currentStep.coerceIn(1, AdmissionStep.MAX_STEP)
            }
        }
    }

    companion object {
        const val KEY_PATIENT_ID = "admission_patient_id"
        const val KEY_CLINICAL_RECORD_ID = "admission_clinical_record_id"
        const val KEY_CURRENT_STEP = "admission_current_step"
    }

    private fun setupDraftAutoSave() {
        if (admissionDraftDao == null) return
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            _state
                .debounce(500L)
                .collect { s ->
                    val docId = s.doctorId.trim()
                    val patId = s.patientId.trim()
                    if (docId.isNotBlank() && patId.isNotBlank() && uuidRegex.matches(docId) && !s.isFinished && s.completionResult == null) {
                        try {
                            admissionDraftDao.insertDraft(
                                AdmissionDraftEntity(
                                    doctorId = docId,
                                    patientId = patId,
                                    currentStep = s.currentStep.coerceIn(1, AdmissionStep.MAX_STEP),
                                    draftJson = buildDraftJson(s)
                                )
                            )
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                        }
                    }
                }
        }
    }

    private fun buildDraftJson(s: ClinicalAdmissionState): String {
        return buildJsonObject {
            put("dni", s.dniCedula)
            put("firstName", s.firstName)
            put("firstLastName", s.firstLastName)
            put("step", s.currentStep.coerceIn(1, AdmissionStep.MAX_STEP))
        }.toString()
    }

    private fun loadCurrentDoctor() {
        viewModelScope.launch {
            val useCase = getCurrentDoctorUseCase ?: return@launch
            useCase().fold(
                onSuccess = { doctor ->
                    if (doctor != null && uuidRegex.matches(doctor.id.value.trim())) {
                        _state.update {
                            it.copy(
                                doctorId = doctor.id.value.trim(),
                                doctorUserId = doctor.userId,
                                doctorName = "${doctor.firstName} ${doctor.lastName}".trim(),
                                doctorLicense = doctor.licenseNumber
                            )
                        }
                    }
                },
                onFailure = { /* Sesión de doctor expirada o nula */ }
            )
        }
    }

    private fun setError(uiText: UiText, legacyMessage: String? = null) {
        val fallback = legacyMessage ?: when (uiText) {
            is UiText.DynamicString -> uiText.value
            is UiText.StringResource -> when (uiText.resId) {
                R.string.admission_error_dni_required -> "El número de documento es obligatorio"
                R.string.admission_error_name_required -> "El nombre y primer apellido son obligatorios"
                R.string.admission_error_invalid_doctor_session -> "Se requiere una sesión de doctor activa y válida para continuar."
                R.string.admission_error_doctor_id_invalid -> "Identificador de doctor no válido. Verifique la sesión activa."
                R.string.admission_error_saving_patient -> "Error al guardar paciente: ${uiText.args.firstOrNull() ?: ""}"
                R.string.admission_error_saving_clinical_record -> "Error en historia: ${uiText.args.firstOrNull() ?: ""}"
                R.string.admission_error_saving_anamnesis -> "Error en anamnesis: ${uiText.args.firstOrNull() ?: ""}"
                else -> "Error inesperado"
            }
        }
        _state.update {
            it.copy(
                isLoading = false,
                errorMessage = fallback,
                errorUiText = uiText
            )
        }
    }

    private fun requireValidDoctorId(): String? {
        val candidate = _state.value.doctorId.trim()
        return if (candidate.isNotBlank() && uuidRegex.matches(candidate)) {
            candidate
        } else {
            val (resId, msg) = if (candidate.isNotBlank()) {
                R.string.admission_error_doctor_id_invalid to "Identificador de doctor no válido. Verifique la sesión activa."
            } else {
                R.string.admission_error_invalid_doctor_session to "Se requiere una sesión de doctor activa y válida para continuar."
            }
            setError(UiText.StringResource(resId), msg)
            null
        }
    }

    fun loadExistingPatientData(pid: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, patientId = pid) }

            patientRepository.getPatientById(pid).onSuccess { patient ->
                if (patient != null) {
                    val detectedDentition = detectDentitionFromBirthDate(patient.birthDate)
                    _state.update {
                        it.copy(
                            patientId = patient.id,
                            dniCedula = patient.dniCedula,
                            firstName = patient.firstName,
                            firstLastName = patient.firstLastName,
                            secondLastName = patient.secondLastName,
                            birthDate = patient.birthDate,
                            birthPlace = patient.birthPlace,
                            eps = patient.eps,
                            bloodGroup = patient.bloodGroup,
                            rhFactor = patient.rhFactor,
                            phone = patient.phone,
                            email = patient.email ?: "",
                            address = patient.address ?: "",
                            locality = patient.locality ?: "",
                            occupation = patient.occupation ?: "",
                            maritalStatus = patient.maritalStatus ?: "Soltero(a)",
                            hasGuardian = !patient.guardianFirstName.isNullOrBlank() || patient.guardian != null,
                            guardianId = patient.guardianId,
                            guardianRelationship = patient.guardianRelationship ?: "",
                            guardianFirstName = patient.guardianFirstName ?: "",
                            guardianLastName = patient.guardianLastName ?: "",
                            guardianPhone = patient.guardianPhone ?: "",
                            guardianAddress = patient.guardianAddress ?: "",
                            guardianLocality = patient.guardian?.locality ?: "",
                            lastDentalVisit = patient.lastDentalVisit ?: "",
                            consultationReason = patient.consultationReason ?: "",
                            currentConsultationReason = patient.currentConsultationReason ?: "",
                            illnessHistory = patient.illnessHistory ?: "",
                            hasCriticalAllergies = patient.hasCriticalAllergies,
                            allergiesDescription = patient.allergiesDescription,
                            consentSigned = patient.consentSigned,
                            dentitionType = detectedDentition
                        )
                    }
                }
            }

            clinicalRecordRepository.getRecordByPatientId(pid).onSuccess { record ->
                if (record != null) {
                    _state.update {
                        it.copy(
                            clinicalRecordId = record.id,
                            existingClinicalRecord = record,
                            dentitionType = record.dentitionType
                        )
                    }
                }
            }

            anamnesisRepository.getAnamnesisByPatientId(pid).onSuccess { anamnesis ->
                if (anamnesis != null) {
                    _state.update {
                        it.copy(
                            activeAnamnesisConditions = anamnesis.activeConditions(),
                            anamnesisObservaciones = anamnesis.observaciones,
                            antecedentesFamiliares = anamnesis.antecedentesFamiliares,
                            medicamentosActuales = anamnesis.medicamentosActuales ?: "",
                            anamnesisOptionNotes = anamnesis.optionNotes
                        )
                    }
                }
            }

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun onEvent(event: ClinicalAdmissionEvent) {
        when (event) {
            is ClinicalAdmissionEvent.NextStep -> advanceStep()
            is ClinicalAdmissionEvent.PreviousStep -> {
                if (_state.value.isLoading) return
                _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1)) }
            }
            is ClinicalAdmissionEvent.JumpToStep -> {
                if (_state.value.isLoading) return
                if (event.step < _state.value.currentStep || _state.value.patientId.isNotBlank()) {
                    _state.update { it.copy(currentStep = event.step.coerceIn(1, AdmissionStep.MAX_STEP)) }
                }
            }
            is ClinicalAdmissionEvent.ClearMessages -> {
                _state.update {
                    it.copy(
                        errorMessage = null,
                        errorUiText = null,
                        successMessage = null,
                        successUiText = null
                    )
                }
            }
            is ClinicalAdmissionEvent.DismissSuccessDialog -> {
                _state.update { it.copy(completionResult = null) }
            }

            is ClinicalAdmissionEvent.UpdateDni -> {
                _state.update { it.copy(dniCedula = event.value, dniError = null, dniErrorUiText = null) }
            }
            is ClinicalAdmissionEvent.UpdateFirstName -> {
                _state.update { it.copy(firstName = event.value, nameError = null, nameErrorUiText = null) }
            }
            is ClinicalAdmissionEvent.UpdateFirstLastName -> {
                _state.update { it.copy(firstLastName = event.value, nameError = null, nameErrorUiText = null) }
            }
            is ClinicalAdmissionEvent.UpdateSecondLastName -> {
                _state.update { it.copy(secondLastName = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateBirthDate -> {
                val detectedDentition = detectDentitionFromBirthDate(event.value)
                _state.update {
                    it.copy(
                        birthDate = event.value,
                        dentitionType = detectedDentition
                    )
                }
            }
            is ClinicalAdmissionEvent.UpdateBirthPlace -> {
                _state.update { it.copy(birthPlace = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateEps -> {
                _state.update { it.copy(eps = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateBloodGroup -> {
                _state.update { it.copy(bloodGroup = event.group, rhFactor = event.rh) }
            }
            is ClinicalAdmissionEvent.UpdatePhone -> {
                _state.update { it.copy(phone = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateEmail -> {
                _state.update { it.copy(email = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateAddress -> {
                _state.update { it.copy(address = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateLocality -> {
                _state.update { it.copy(locality = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateOccupation -> {
                _state.update { it.copy(occupation = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateMaritalStatus -> {
                _state.update { it.copy(maritalStatus = event.value) }
            }
            is ClinicalAdmissionEvent.ToggleHasGuardian -> {
                _state.update { it.copy(hasGuardian = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateGuardianRelationship -> {
                _state.update { it.copy(guardianRelationship = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateGuardianLocality -> {
                _state.update { it.copy(guardianLocality = event.value) }
            }
            is ClinicalAdmissionEvent.CopyAddressToGuardian -> {
                _state.update {
                    it.copy(
                        guardianAddress = it.address,
                        guardianLocality = it.locality
                    )
                }
            }
            is ClinicalAdmissionEvent.UpdateGuardian -> {
                _state.update {
                    it.copy(
                        guardianRelationship = event.relationship,
                        guardianFirstName = event.firstName,
                        guardianLastName = event.lastName,
                        guardianPhone = event.phone,
                        guardianAddress = event.address,
                        guardianLocality = event.locality
                    )
                }
            }
            is ClinicalAdmissionEvent.UpdateAntecedents -> {
                _state.update {
                    it.copy(
                        lastDentalVisit = event.lastVisit,
                        consultationReason = event.reason,
                        currentConsultationReason = event.currentReason,
                        illnessHistory = event.illnessHistory
                    )
                }
            }
            is ClinicalAdmissionEvent.UpdateAllergies -> {
                _state.update {
                    it.copy(
                        hasCriticalAllergies = event.hasAllergies,
                        allergiesDescription = event.description
                    )
                }
            }

            is ClinicalAdmissionEvent.ToggleAnamnesisQuestion -> {
                toggleAnamnesisField(event.questionKey, event.value)
            }
            is ClinicalAdmissionEvent.UpdateAnamnesisOptionNote -> {
                _state.update { current ->
                    val updatedNotes = current.anamnesisOptionNotes.toMutableMap()
                    updatedNotes[event.questionKey] = event.note
                    current.copy(anamnesisOptionNotes = updatedNotes)
                }
            }
            is ClinicalAdmissionEvent.UpdateAnamnesisObservations -> {
                _state.update {
                    it.copy(
                        anamnesisObservaciones = event.observations,
                        antecedentesFamiliares = event.familiares,
                        medicamentosActuales = event.meds
                    )
                }
            }

            is ClinicalAdmissionEvent.ChangeDentitionType -> {
                _state.update { it.copy(dentitionType = event.type) }
            }
            is ClinicalAdmissionEvent.FinishAdmission -> persistStep2AndFinish()
        }
    }

    private fun detectDentitionFromBirthDate(birthDateStr: String): DentitionType {
        return try {
            val date = LocalDate.parse(birthDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            val age = Period.between(date, LocalDate.now()).years
            if (age < 12) DentitionType.CHILD else DentitionType.ADULT
        } catch (_: Exception) {
            DentitionType.ADULT
        }
    }

    private fun toggleAnamnesisField(key: String, value: Boolean) {
        val condition = AnamnesisCondition.fromKey(key) ?: return
        _state.update { current ->
            val updated = if (value) {
                current.activeAnamnesisConditions + condition
            } else {
                current.activeAnamnesisConditions - condition
            }
            current.copy(activeAnamnesisConditions = updated)
        }
    }

    private fun advanceStep() {
        val s = _state.value
        when (s.currentStep) {
            1 -> persistStep1AndAdvance()
            2 -> persistStep2AndFinish()
        }
    }

    private fun persistStep1AndAdvance() {
        val s = _state.value
        if (s.dniCedula.isBlank()) {
            _state.update {
                it.copy(
                    dniError = "El número de documento es obligatorio",
                    dniErrorUiText = UiText.StringResource(R.string.admission_error_dni_required)
                )
            }
            return
        }
        if (s.firstName.isBlank() || s.firstLastName.isBlank()) {
            _state.update {
                it.copy(
                    nameError = "El nombre y primer apellido son obligatorios",
                    nameErrorUiText = UiText.StringResource(R.string.admission_error_name_required)
                )
            }
            return
        }

        val validDoctorId = requireValidDoctorId() ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, errorUiText = null) }
            val effectivePatientId = s.patientId.ifBlank { Patient.generateIdFromDni(s.dniCedula) }

            var resolvedGuardianId = s.guardianId
            var resolvedGuardian: Guardian? = null
            if (s.hasGuardian && s.guardianFirstName.isNotBlank()) {
                val guardianToSave = Guardian(
                    id = s.guardianId.orEmpty(),
                    firstName = s.guardianFirstName.trim(),
                    lastName = s.guardianLastName.trim(),
                    phone = s.guardianPhone.trim(),
                    address = s.guardianAddress.trim(),
                    locality = s.guardianLocality.trim()
                )
                guardianRepository.saveGuardian(guardianToSave)
                    .onSuccess { savedG ->
                        resolvedGuardianId = savedG.id
                        resolvedGuardian = savedG
                    }
            }

            val patient = Patient(
                id = effectivePatientId,
                dniCedula = s.dniCedula.trim(),
                firstName = s.firstName.trim(),
                firstLastName = s.firstLastName.trim(),
                secondLastName = s.secondLastName.trim(),
                birthDate = s.birthDate.ifBlank { LocalDate.now().toString() },
                birthPlace = s.birthPlace,
                eps = s.eps,
                bloodGroup = s.bloodGroup,
                rhFactor = s.rhFactor,
                phone = s.phone.trim(),
                email = s.email.trim().ifBlank { null },
                address = s.address.trim().ifBlank { null },
                locality = s.locality.trim().ifBlank { null },
                occupation = s.occupation.ifBlank { null },
                maritalStatus = s.maritalStatus.ifBlank { null },
                guardianId = resolvedGuardianId,
                guardianRelationship = if (s.hasGuardian) s.guardianRelationship.ifBlank { null } else null,
                guardian = resolvedGuardian,
                guardianFirstName = if (s.hasGuardian) s.guardianFirstName.ifBlank { null } else null,
                guardianLastName = if (s.hasGuardian) s.guardianLastName.ifBlank { null } else null,
                guardianPhone = if (s.hasGuardian) s.guardianPhone.ifBlank { null } else null,
                guardianAddress = if (s.hasGuardian) s.guardianAddress.ifBlank { null } else null,
                lastDentalVisit = s.lastDentalVisit.ifBlank { null },
                consultationReason = s.consultationReason.ifBlank { null },
                currentConsultationReason = s.currentConsultationReason.ifBlank { null },
                illnessHistory = s.illnessHistory.ifBlank { null },
                hasCriticalAllergies = s.hasCriticalAllergies,
                allergiesDescription = s.allergiesDescription.ifBlank { "Ninguna" },
                consentSigned = s.consentSigned
            )

            patientRepository.createOrUpdatePatient(patient)
                .onSuccess { savedPatient ->
                    val pid = savedPatient.id
                    val clinicalRecord = if (s.existingClinicalRecord != null) {
                        s.existingClinicalRecord.copy(
                            dentitionType = s.dentitionType
                        )
                    } else {
                        ClinicalRecord(
                            id = s.clinicalRecordId,
                            patientId = pid,
                            dentitionType = s.dentitionType,
                            status = ClinicalRecordStatus.ABIERTA
                        )
                    }
                    clinicalRecordRepository.saveRecord(clinicalRecord)
                        .onSuccess { savedRecord ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    patientId = pid,
                                    clinicalRecordId = savedRecord.id,
                                    existingClinicalRecord = savedRecord,
                                    currentStep = 2
                                )
                            }
                        }
                        .onFailure { err ->
                            val msg = "Error en historia: ${err.toUserFriendlyMessage()}"
                            setError(UiText.StringResource(R.string.admission_error_saving_clinical_record, err.toUserFriendlyMessage()), msg)
                        }
                }
                .onFailure { err ->
                    val msg = "Error al guardar paciente: ${err.toUserFriendlyMessage()}"
                    setError(UiText.StringResource(R.string.admission_error_saving_patient, err.toUserFriendlyMessage()), msg)
                }
        }
    }

    private fun persistStep2AndFinish() {
        val s = _state.value
        val validDoctorId = requireValidDoctorId() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, errorUiText = null) }
            val anamnesis = Anamnesis(
                patientId = s.patientId,
                observaciones = s.anamnesisObservaciones,
                antecedentesFamiliares = s.antecedentesFamiliares,
                medicamentosActuales = s.medicamentosActuales.ifBlank { null },
                optionNotes = filterActiveAnamnesisNotes(s)
            ).withConditions(s.activeAnamnesisConditions)

            anamnesisRepository.saveAnamnesis(anamnesis)
                .onSuccess {
                    val isNew = initialPatientId.isNullOrBlank()
                    try {
                        clinicalAuditRepository?.recordAudit(
                            patientId = s.patientId,
                            entityTable = "clinical_records",
                            entityId = s.clinicalRecordId,
                            action = if (isNew) AuditAction.CREATE else AuditAction.UPDATE,
                            changedFields = listOf("patient_admission", "anamnesis"),
                            actorUserId = s.doctorUserId ?: validDoctorId,
                            actorName = s.doctorName.ifBlank { null }
                        )
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }

                    if (s.patientId.isNotBlank()) {
                        try {
                            admissionDraftDao?.deleteDraft(validDoctorId, s.patientId)
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                        }
                    }

                    val completion = AdmissionCompletionResult(
                        patientId = s.patientId,
                        patientFullName = s.patientFullName,
                        dniCedula = s.dniCedula,
                        clinicalRecordId = s.clinicalRecordId,
                        isNewAdmission = isNew
                    )

                    val successMsg = "¡Proceso de ingreso clínico finalizado con éxito!"
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isFinished = true,
                            completionResult = completion,
                            successMessage = successMsg,
                            successUiText = UiText.DynamicString(successMsg)
                        )
                    }
                }
                .onFailure { err ->
                    val msg = "Error en anamnesis: ${err.toUserFriendlyMessage()}"
                    setError(UiText.StringResource(R.string.admission_error_saving_anamnesis, err.toUserFriendlyMessage()), msg)
                }
        }
    }

    private fun filterActiveAnamnesisNotes(s: ClinicalAdmissionState): Map<String, String> {
        val activeKeys = s.activeAnamnesisConditions.map { it.key }.toSet()
        return s.anamnesisOptionNotes
            .filter { (key, value) -> key in activeKeys && value.isNotBlank() }
            .mapValues { (_, value) -> value.trim() }
    }
}
