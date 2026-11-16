package com.example.biodontos.presentation.admission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.R
import com.example.biodontos.core.ui.UiText
import com.example.biodontos.core.util.SystemClinicClock
import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.data.local.dao.AdmissionDraftDao
import com.example.biodontos.data.local.entity.AdmissionDraftEntity
import com.example.biodontos.domain.model.*
import com.example.biodontos.domain.repository.*
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import com.example.biodontos.domain.usecase.materials.BuildEvolutionMaterialUsageUseCase
import com.example.biodontos.domain.usecase.materials.GetMaterialsUseCase
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

class ClinicalAdmissionViewModel(
    private val patientRepository: PatientRepository,
    private val clinicalRecordRepository: ClinicalRecordRepository,
    private val anamnesisRepository: AnamnesisRepository,
    private val odontogramRepository: OdontogramRepository,
    private val periodontalRepository: PeriodontalRepository,
    private val evolutionRepository: EvolutionRepository,
    private val materialRepository: MaterialRepository,
    private val informedConsentRepository: InformedConsentRepository,
    private val appointmentRepository: AppointmentRepository,
    private val guardianRepository: GuardianRepository,
    initialPatientId: String? = null,
    private val getCurrentDoctorUseCase: GetCurrentDoctorUseCase? = null,
    private val doctorRepository: DoctorRepository? = null,
    private val getMaterialsUseCase: GetMaterialsUseCase = GetMaterialsUseCase(materialRepository),
    private val buildEvolutionMaterialUsageUseCase: BuildEvolutionMaterialUsageUseCase =
        BuildEvolutionMaterialUsageUseCase(SystemClinicClock()),
    private val admissionDraftDao: AdmissionDraftDao? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(ClinicalAdmissionState())
    val state: StateFlow<ClinicalAdmissionState> = _state.asStateFlow()

    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    init {
        loadMaterialsCatalog()
        loadCurrentDoctor()
        if (!initialPatientId.isNullOrBlank()) {
            loadExistingPatientData(initialPatientId)
        }
        setupDraftAutoSave()
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
                    if (docId.isNotBlank() && patId.isNotBlank() && uuidRegex.matches(docId) && !s.isFinished) {
                        try {
                            admissionDraftDao.insertDraft(
                                AdmissionDraftEntity(
                                    doctorId = docId,
                                    patientId = patId,
                                    currentStep = s.currentStep,
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
        return buildString {
            append("{")
            append("\"dni\":\"").append(s.dniCedula).append("\",")
            append("\"firstName\":\"").append(s.firstName).append("\",")
            append("\"firstLastName\":\"").append(s.firstLastName).append("\",")
            append("\"step\":").append(s.currentStep)
            append("}")
        }
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
                R.string.admission_error_material_already_added -> "Este material ya fue anexado a la evolución."
                R.string.admission_error_patient_id_required_for_consent -> "Debe registrar la identificación del paciente antes de emitir el consentimiento."
                R.string.admission_error_invalid_doctor_session -> "Se requiere una sesión de doctor activa y válida para continuar."
                R.string.admission_error_doctor_id_invalid -> "Identificador de doctor no válido. Verifique la sesión activa."
                R.string.admission_error_saving_patient -> "Error al guardar paciente: ${uiText.args.firstOrNull() ?: ""}"
                R.string.admission_error_saving_clinical_record -> "Error en historia: ${uiText.args.firstOrNull() ?: ""}"
                R.string.admission_error_saving_anamnesis -> "Error en anamnesis: ${uiText.args.firstOrNull() ?: ""}"
                R.string.admission_error_saving_odontogram -> "Error en odontograma: ${uiText.args.firstOrNull() ?: ""}"
                R.string.admission_error_saving_periodontogram -> "Error en periodontograma: ${uiText.args.firstOrNull() ?: ""}"
                R.string.admission_error_saving_consent -> "Error en consentimiento: ${uiText.args.firstOrNull() ?: ""}"
                R.string.admission_error_saving_evolution -> "Error en evolución: ${uiText.args.firstOrNull() ?: ""}"
                R.string.admission_error_saving_operative_consent -> "Error al guardar consentimiento de operatoria: ${uiText.args.firstOrNull() ?: ""}"
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

            // 1. Paciente
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
                            occupation = patient.occupation ?: "",
                            maritalStatus = patient.maritalStatus ?: "Soltero(a)",
                            guardianRelationship = patient.guardianRelationship ?: "",
                            guardianFirstName = patient.guardianFirstName ?: "",
                            guardianLastName = patient.guardianLastName ?: "",
                            guardianPhone = patient.guardianPhone ?: "",
                            guardianAddress = patient.guardianAddress ?: "",
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

            // 2. Historia Clínica
            clinicalRecordRepository.getRecordByPatientId(pid).onSuccess { record ->
                if (record != null) {
                    _state.update {
                        it.copy(
                            clinicalRecordId = record.id,
                            dentitionType = record.dentitionType,
                            mainDiagnosis = record.mainDiagnosis ?: "",
                            relatedDiagnoses = record.relatedDiagnoses ?: "",
                            complementaryExams = record.complementaryExams ?: ""
                        )
                    }

                    // 4. Periodontograma por recordId
                    if (record.id.isNotBlank()) {
                        periodontalRepository.getExaminationByRecordId(record.id).onSuccess { exam ->
                            if (exam != null) {
                                val siteMap = exam.sites.associateBy { it.toothNumber to it.siteLocation }
                                _state.update {
                                    it.copy(
                                        siteMeasurements = siteMap,
                                        periodontalObservations = exam.generalObservations ?: ""
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Anamnesis
            anamnesisRepository.getAnamnesisByPatientId(pid).onSuccess { anamnesis ->
                if (anamnesis != null) {
                    _state.update {
                        it.copy(
                            tratamientoMedicoActual = anamnesis.tratamientoMedicoActual,
                            tomaMedicamentos = anamnesis.tomaMedicamentos,
                            alergias = anamnesis.alergias,
                            cardiopatias = anamnesis.cardiopatias,
                            alteracionPresionArterial = anamnesis.alteracionPresionArterial,
                            embarazo = anamnesis.embarazo,
                            diabetes = anamnesis.diabetes,
                            hepatitis = anamnesis.hepatitis,
                            irradiaciones = anamnesis.irradiaciones,
                            discrasiasSanguineas = anamnesis.discrasiasSanguineas,
                            fiebreReumatica = anamnesis.fiebreReumatica,
                            enfermedadesRenales = anamnesis.enfermedadesRenales,
                            inmunosupresion = anamnesis.inmunosupresion,
                            trastornosEmocionales = anamnesis.trastornosEmocionales,
                            patologiaRespiratoria = anamnesis.patologiaRespiratoria,
                            trastornosGastricos = anamnesis.trastornosGastricos,
                            epilepsia = anamnesis.epilepsia,
                            cirugias = anamnesis.cirugias,
                            enfermedadesOrales = anamnesis.enfermedadesOrales,
                            otrasAlteraciones = anamnesis.otrasAlteraciones,
                            anamnesisObservaciones = anamnesis.observaciones,
                            antecedentesFamiliares = anamnesis.antecedentesFamiliares ?: "",
                            medicamentosActuales = anamnesis.medicamentosActuales ?: ""
                        )
                    }
                }
            }

            // 5. Odontograma
            odontogramRepository.getOdontogram(pid).onSuccess { teethList ->
                val surfacesMap = teethList.associateBy({ it.number }, { it.surfaceStates })
                if (surfacesMap.isNotEmpty()) {
                    _state.update { it.copy(teethSurfaces = surfacesMap) }
                }
            }

            // 6. Consentimiento Informado
            informedConsentRepository.getConsentByPatientId(pid).onSuccess { consent ->
                if (consent != null) {
                    _state.update {
                        it.copy(
                            treatmentPlanDescription = consent.treatmentSummary,
                            consentSigned = (consent.status == "SIGNED") || !consent.patientSignatureUrl.isNullOrBlank(),
                            consentPatientSignature = consent.patientSignatureUrl ?: ""
                        )
                    }
                }
            }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun loadMaterialsCatalog() {
        viewModelScope.launch {
            getMaterialsUseCase().fold(
                ifLeft = { /* Catálogo vacío: el doctor puede gestionarlo desde Materiales */ },
                ifRight = { list ->
                    val today = LocalDate.now()
                    _state.update {
                        it.copy(
                            materialsCatalog = list,
                            selectedCatalogMaterial = list.firstOrNull { m -> !m.isExpired(today) }
                        )
                    }
                }
            )
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
                    _state.update { it.copy(currentStep = event.step.coerceIn(1, 7)) }
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

            // Paso 1: Identificación
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

            // Consentimiento de Operatoria
            is ClinicalAdmissionEvent.ToggleOperativeConsentDialog -> {
                _state.update { it.copy(showOperativeConsentDialog = event.show) }
            }
            is ClinicalAdmissionEvent.UpdateOperativeConsentTreatment -> {
                _state.update { it.copy(operativeConsentProposedTreatment = event.treatment) }
            }
            is ClinicalAdmissionEvent.UpdateOperativeConsentSigner -> {
                _state.update {
                    it.copy(
                        operativeConsentSignerType = event.signerType,
                        operativeConsentRepresentativeName = event.repName,
                        operativeConsentRepresentativeId = event.repId
                    )
                }
            }
            is ClinicalAdmissionEvent.AddOperativeConsentProcedure -> {
                _state.update { current ->
                    val proc = InformedConsentProcedure(
                        consentId = "",
                        toothNumber = event.toothNumber,
                        procedureDate = event.date
                    )
                    current.copy(operativeConsentProcedures = current.operativeConsentProcedures + proc)
                }
            }
            is ClinicalAdmissionEvent.RemoveOperativeConsentProcedure -> {
                _state.update { current ->
                    val updated = current.operativeConsentProcedures.toMutableList()
                    if (event.index in updated.indices) {
                        updated.removeAt(event.index)
                    }
                    current.copy(operativeConsentProcedures = updated)
                }
            }
            is ClinicalAdmissionEvent.ConfirmOperativeConsentSignatures -> {
                _state.update {
                    it.copy(
                        operativeConsentPatientSignature = event.patientSig,
                        operativeConsentDoctorSignature = event.doctorSig
                    )
                }
            }
            is ClinicalAdmissionEvent.SaveOperativeConsent -> {
                saveOperativeConsent()
            }

            // Paso 2: Anamnesis
            is ClinicalAdmissionEvent.ToggleAnamnesisQuestion -> {
                toggleAnamnesisField(event.questionKey, event.value)
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

            // Paso 3: Odontograma
            is ClinicalAdmissionEvent.ChangeDentitionType -> {
                _state.update { it.copy(dentitionType = event.type) }
            }
            is ClinicalAdmissionEvent.SelectOdontogramTooth -> {
                _state.update { it.copy(selectedOdontogramTooth = event.toothNumber) }
            }
            is ClinicalAdmissionEvent.SelectConditionToApply -> {
                _state.update { it.copy(selectedConditionToApply = event.condition) }
            }
            is ClinicalAdmissionEvent.ApplySurfaceCondition -> {
                applySurfaceToTooth(event.toothNumber, event.surface)
            }

            // Paso 4: Periodontograma
            is ClinicalAdmissionEvent.SelectPeriodontalTooth -> {
                _state.update { it.copy(selectedPeriodontalTooth = event.toothNumber) }
            }
            is ClinicalAdmissionEvent.UpdatePeriodontalMeasurement -> {
                val key = event.measurement.toothNumber to event.measurement.siteLocation
                _state.update {
                    val updated = it.siteMeasurements.toMutableMap()
                    updated[key] = event.measurement
                    it.copy(siteMeasurements = updated)
                }
            }

            // Paso 5: Tratamiento & Consentimiento
            is ClinicalAdmissionEvent.UpdateDiagnoses -> {
                _state.update {
                    it.copy(
                        mainDiagnosis = event.main,
                        relatedDiagnoses = event.related,
                        complementaryExams = event.exams,
                        treatmentPlanDescription = event.plan
                    )
                }
            }
            is ClinicalAdmissionEvent.ConfirmConsentSignature -> {
                _state.update {
                    it.copy(
                        consentPatientSignature = event.signature,
                        consentSigned = event.signature.isNotBlank()
                    )
                }
            }

            // Paso 6: Evolución & Materiales
            is ClinicalAdmissionEvent.UpdateEvolution -> {
                _state.update {
                    it.copy(
                        evolutionDescription = event.description,
                        evolutionCost = event.cost,
                        evolutionPayment = event.payment
                    )
                }
            }
            is ClinicalAdmissionEvent.SelectMaterialFromCatalog -> {
                _state.update { it.copy(selectedCatalogMaterial = event.material) }
            }
            is ClinicalAdmissionEvent.AddMaterialUsed -> {
                val mat = _state.value.selectedCatalogMaterial ?: return
                if (_state.value.usedMaterials.any { it.materialId == mat.id.value }) {
                    val msg = "Este material ya fue anexado a la evolución."
                    setError(UiText.StringResource(R.string.admission_error_material_already_added), msg)
                    return
                }
                viewModelScope.launch {
                    buildEvolutionMaterialUsageUseCase(mat, event.quantity).fold(
                        ifLeft = { error ->
                            setError(UiText.DynamicString(error.userMessage), error.userMessage)
                        },
                        ifRight = { used ->
                            _state.update {
                                it.copy(usedMaterials = it.usedMaterials + used, errorMessage = null, errorUiText = null)
                            }
                        }
                    )
                }
            }
            is ClinicalAdmissionEvent.RemoveMaterialUsed -> {
                _state.update {
                    it.copy(usedMaterials = it.usedMaterials.filterNot { m -> m.materialId == event.materialId })
                }
            }
            is ClinicalAdmissionEvent.ConfirmEvolutionSignature -> {
                _state.update { it.copy(evolutionPatientSignature = event.signature) }
            }
            is ClinicalAdmissionEvent.ToggleCloseRecord -> {
                _state.update {
                    it.copy(
                        closeRecordOnFinish = event.close,
                        closureSummary = event.summary
                    )
                }
            }

            // Paso 7: Próxima Cita & Finalizar
            is ClinicalAdmissionEvent.UpdateNextAppointment -> {
                _state.update {
                    it.copy(
                        nextAppointmentDate = event.date,
                        nextAppointmentTime = event.time,
                        nextAppointmentReason = event.reason
                    )
                }
            }
            is ClinicalAdmissionEvent.FinishAdmission -> finishAdmission()
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
        _state.update { current ->
            when (key) {
                "tratamientoMedicoActual" -> current.copy(tratamientoMedicoActual = value)
                "tomaMedicamentos" -> current.copy(tomaMedicamentos = value)
                "alergias" -> current.copy(alergias = value)
                "cardiopatias" -> current.copy(cardiopatias = value)
                "alteracionPresionArterial" -> current.copy(alteracionPresionArterial = value)
                "embarazo" -> current.copy(embarazo = value)
                "diabetes" -> current.copy(diabetes = value)
                "hepatitis" -> current.copy(hepatitis = value)
                "irradiaciones" -> current.copy(irradiaciones = value)
                "discrasiasSanguineas" -> current.copy(discrasiasSanguineas = value)
                "fiebreReumatica" -> current.copy(fiebreReumatica = value)
                "enfermedadesRenales" -> current.copy(enfermedadesRenales = value)
                "inmunosupresion" -> current.copy(inmunosupresion = value)
                "trastornosEmocionales" -> current.copy(trastornosEmocionales = value)
                "patologiaRespiratoria" -> current.copy(patologiaRespiratoria = value)
                "trastornosGastricos" -> current.copy(trastornosGastricos = value)
                "epilepsia" -> current.copy(epilepsia = value)
                "cirugias" -> current.copy(cirugias = value)
                "enfermedadesOrales" -> current.copy(enfermedadesOrales = value)
                "otrasAlteraciones" -> current.copy(otrasAlteraciones = value)
                else -> current
            }
        }
    }

    private fun applySurfaceToTooth(toothNumber: Int, surface: ToothSurface) {
        val condition = _state.value.selectedConditionToApply
        _state.update { current ->
            val toothMap = current.teethSurfaces[toothNumber]?.toMutableMap() ?: mutableMapOf()
            toothMap[surface] = condition
            val updatedTeeth = current.teethSurfaces.toMutableMap()
            updatedTeeth[toothNumber] = toothMap
            current.copy(teethSurfaces = updatedTeeth)
        }
    }

    private fun advanceStep() {
        val s = _state.value
        when (s.currentStep) {
            1 -> persistStep1AndAdvance()
            2 -> persistStep2AndAdvance()
            3 -> persistStep3AndAdvance()
            4 -> persistStep4AndAdvance()
            5 -> persistStep5AndAdvance()
            6 -> persistStep6AndAdvance()
            7 -> finishAdmission()
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
                    // Crear o recuperar historia clínica
                    val clinicalRecord = ClinicalRecord(
                        id = s.clinicalRecordId,
                        patientId = pid,
                        dentitionType = s.dentitionType,
                        status = ClinicalRecordStatus.ABIERTA
                    )
                    clinicalRecordRepository.saveRecord(clinicalRecord)
                        .onSuccess { savedRecord ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    patientId = pid,
                                    clinicalRecordId = savedRecord.id,
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

    private fun persistStep2AndAdvance() {
        val s = _state.value
        val validDoctorId = requireValidDoctorId() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, errorUiText = null) }
            val anamnesis = Anamnesis(
                patientId = s.patientId,
                tratamientoMedicoActual = s.tratamientoMedicoActual,
                tomaMedicamentos = s.tomaMedicamentos,
                alergias = s.alergias,
                cardiopatias = s.cardiopatias,
                alteracionPresionArterial = s.alteracionPresionArterial,
                embarazo = s.embarazo,
                diabetes = s.diabetes,
                hepatitis = s.hepatitis,
                irradiaciones = s.irradiaciones,
                discrasiasSanguineas = s.discrasiasSanguineas,
                fiebreReumatica = s.fiebreReumatica,
                enfermedadesRenales = s.enfermedadesRenales,
                inmunosupresion = s.inmunosupresion,
                trastornosEmocionales = s.trastornosEmocionales,
                patologiaRespiratoria = s.patologiaRespiratoria,
                trastornosGastricos = s.trastornosGastricos,
                epilepsia = s.epilepsia,
                cirugias = s.cirugias,
                enfermedadesOrales = s.enfermedadesOrales,
                otrasAlteraciones = s.otrasAlteraciones,
                observaciones = s.anamnesisObservaciones,
                antecedentesFamiliares = s.antecedentesFamiliares,
                medicamentosActuales = s.medicamentosActuales.ifBlank { null }
            )

            anamnesisRepository.saveAnamnesis(anamnesis)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, currentStep = 3) }
                }
                .onFailure { err ->
                    val msg = "Error en anamnesis: ${err.toUserFriendlyMessage()}"
                    setError(UiText.StringResource(R.string.admission_error_saving_anamnesis, err.toUserFriendlyMessage()), msg)
                }
        }
    }

    private fun persistStep3AndAdvance() {
        val s = _state.value
        val validDoctorId = requireValidDoctorId() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, errorUiText = null) }
            for ((toothNum, surfaces) in s.teethSurfaces) {
                for ((surface, condition) in surfaces) {
                    val result = odontogramRepository.updateToothState(s.patientId, toothNum, surface, condition, "Admisión inicial")
                    if (result.isFailure) {
                        val err = result.exceptionOrNull()
                        val friendly = err.toUserFriendlyMessage()
                        val msg = "Error en odontograma: $friendly"
                        setError(UiText.StringResource(R.string.admission_error_saving_odontogram, friendly), msg)
                        return@launch
                    }
                }
            }
            _state.update { it.copy(isLoading = false, currentStep = 4) }
        }
    }

    private fun persistStep4AndAdvance() {
        val s = _state.value
        val validDoctorId = requireValidDoctorId() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, errorUiText = null) }
            val exam = PeriodontalExamination(
                recordId = s.clinicalRecordId,
                doctorId = validDoctorId,
                examinationDate = LocalDate.now().toString(),
                generalObservations = s.periodontalObservations.ifBlank { null },
                sites = s.siteMeasurements.values.toList()
            )

            periodontalRepository.saveExamination(exam)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, currentStep = 5) }
                }
                .onFailure { err ->
                    val msg = "Error en periodontograma: ${err.toUserFriendlyMessage()}"
                    setError(UiText.StringResource(R.string.admission_error_saving_periodontogram, err.toUserFriendlyMessage()), msg)
                }
        }
    }

    private fun persistStep5AndAdvance() {
        val s = _state.value
        val validDoctorId = requireValidDoctorId() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, errorUiText = null) }
            val record = ClinicalRecord(
                id = s.clinicalRecordId,
                patientId = s.patientId,
                dentitionType = s.dentitionType,
                mainDiagnosis = s.mainDiagnosis.ifBlank { null },
                relatedDiagnoses = s.relatedDiagnoses.ifBlank { null },
                complementaryExams = s.complementaryExams.ifBlank { null },
                status = ClinicalRecordStatus.ABIERTA
            )
            val recordResult = clinicalRecordRepository.saveRecord(record)
            if (recordResult.isFailure) {
                val err = recordResult.exceptionOrNull()
                val friendly = err.toUserFriendlyMessage()
                val msg = "Error en historia: $friendly"
                setError(UiText.StringResource(R.string.admission_error_saving_clinical_record, friendly), msg)
                return@launch
            }

            val consent = InformedConsent(
                patientId = s.patientId,
                doctorId = validDoctorId,
                treatmentSummary = s.treatmentPlanDescription.ifBlank { "Tratamiento Integral de Odontología" },
                patientSignatureUrl = s.consentPatientSignature.ifBlank { null },
                doctorLicenseNumber = s.doctorLicense,
                status = if (s.consentSigned) "SIGNED" else "PENDING"
            )
            informedConsentRepository.saveConsent(consent)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, currentStep = 6) }
                }
                .onFailure { err ->
                    val msg = "Error en consentimiento: ${err.toUserFriendlyMessage()}"
                    setError(UiText.StringResource(R.string.admission_error_saving_consent, err.toUserFriendlyMessage()), msg)
                }
        }
    }

    private fun persistStep6AndAdvance() {
        val s = _state.value
        val validDoctorId = requireValidDoctorId() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, errorUiText = null) }
            val cost = s.evolutionCost.toDoubleOrNull() ?: 0.0
            val payment = s.evolutionPayment.toDoubleOrNull() ?: 0.0
            val evolution = Evolution(
                recordId = s.clinicalRecordId,
                doctorId = validDoctorId,
                date = LocalDate.now().toString(),
                procedureTime = "10:00:00",
                description = s.evolutionDescription.ifBlank { "Apertura de historia clínica e ingreso" },
                doctorName = s.doctorName,
                cost = cost,
                paymentAmount = payment,
                balance = (cost - payment).coerceAtLeast(0.0),
                patientSignatureUrl = s.evolutionPatientSignature.ifBlank { null },
                materialsUsed = s.usedMaterials
            )

            val saveResult = try {
                val candidateResult = evolutionRepository.saveEvolution(
                    evolution = evolution,
                    closeRecord = s.closeRecordOnFinish,
                    closureSummary = s.closureSummary.ifBlank { "Tratamiento de ingreso concluido." },
                    closureSignatureUrl = s.evolutionPatientSignature.ifBlank { null }
                )
                if (candidateResult.isSuccess) {
                    val evo = candidateResult.getOrNull()
                    if (evo !is Evolution) {
                        evolutionRepository.saveEvolution(evolution)
                    } else {
                        candidateResult
                    }
                } else {
                    val exception = candidateResult.exceptionOrNull()
                    if (exception == null) {
                        evolutionRepository.saveEvolution(evolution)
                    } else {
                        candidateResult
                    }
                }
            } catch (_: Throwable) {
                evolutionRepository.saveEvolution(evolution)
            }

            saveResult.onSuccess {
                if (s.closeRecordOnFinish) {
                    clinicalRecordRepository.closeRecord(
                        recordId = s.clinicalRecordId,
                        doctorId = validDoctorId,
                        summary = s.closureSummary.ifBlank { "Tratamiento de ingreso concluido." },
                        signatureUrl = s.evolutionPatientSignature
                    )
                }
                _state.update { it.copy(isLoading = false, currentStep = 7) }
            }.onFailure { err ->
                val msg = "Error en evolución: ${err.toUserFriendlyMessage()}"
                setError(UiText.StringResource(R.string.admission_error_saving_evolution, err.toUserFriendlyMessage()), msg)
            }
        }
    }

    private fun finishAdmission() {
        val s = _state.value
        val validDoctorId = requireValidDoctorId() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, errorUiText = null) }
            var scheduledTimeNote = ""

            if (s.nextAppointmentDate.isNotBlank()) {
                var targetTime = s.nextAppointmentTime.ifBlank { "10:00" }.trim()
                if (targetTime.length == 5) {
                    targetTime = "$targetTime:00"
                }

                val busySlots = appointmentRepository.getDoctorBusySlots(validDoctorId, s.nextAppointmentDate)
                    .getOrDefault(emptyList())

                val requestedIso = "${s.nextAppointmentDate}T$targetTime"
                val isBusy = busySlots.any { busy ->
                    busy.startsWith("${s.nextAppointmentDate}T${targetTime.take(5)}") || busy == requestedIso
                }

                val candidateTimes = listOf(
                    "08:00:00", "08:30:00", "09:00:00", "09:30:00", "10:00:00", "10:30:00",
                    "11:00:00", "11:30:00", "12:00:00", "14:00:00", "14:30:00", "15:00:00",
                    "15:30:00", "16:00:00", "16:30:00", "17:00:00"
                )

                if (isBusy) {
                    val availableTime = candidateTimes.firstOrNull { candidate ->
                        val iso = "${s.nextAppointmentDate}T$candidate"
                        busySlots.none { busy -> busy.startsWith(iso.take(16)) }
                    }
                    availableTime?.let { targetTime = it }
                }

                var appointmentCreated = false
                val appointmentToTry = Appointment(
                    patientId = s.patientId,
                    doctorId = validDoctorId,
                    startTime = Appointment.parseDateTime("${s.nextAppointmentDate}T$targetTime"),
                    reason = s.nextAppointmentReason.ifBlank { "Control y seguimiento post-tratamiento" },
                    status = AppointmentStatus.SCHEDULED,
                    isConfirmed = false
                )

                val primaryResult = appointmentRepository.createAppointment(appointmentToTry)
                if (primaryResult.isSuccess) {
                    appointmentCreated = true
                    scheduledTimeNote = " Cita agendada para el ${s.nextAppointmentDate} a las ${targetTime.take(5)}."
                } else {
                    val fallbackTime = candidateTimes.firstNotNullOfOrNull { candidate ->
                        if (candidate == targetTime) {
                            null
                        } else {
                            val fallbackAppt = appointmentToTry.copy(
                                startTime = Appointment.parseDateTime("${s.nextAppointmentDate}T$candidate")
                            )
                            val fallbackResult = appointmentRepository.createAppointment(fallbackAppt)
                            candidate.takeIf { fallbackResult.isSuccess }
                        }
                    }
                    if (fallbackTime != null) {
                        appointmentCreated = true
                        scheduledTimeNote = " Cita agendada para el ${s.nextAppointmentDate} a las ${fallbackTime.take(5)}."
                    }
                }

                if (!appointmentCreated) {
                    scheduledTimeNote = " (Nota: No se pudo agendar la próxima cita automáticamente por conflicto de horario)."
                }
            }

            if (s.patientId.isNotBlank()) {
                try {
                    admissionDraftDao?.deleteDraft(validDoctorId, s.patientId)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }

            val successMsg = "¡Proceso de ingreso clínico finalizado con éxito!$scheduledTimeNote"
            _state.update {
                it.copy(
                    isLoading = false,
                    isFinished = true,
                    successMessage = successMsg,
                    successUiText = UiText.DynamicString(successMsg)
                )
            }
        }
    }

    private fun saveOperativeConsent() {
        val s = _state.value
        val validDoctorId = requireValidDoctorId() ?: return
        val pid = s.patientId.ifBlank {
            setError(
                UiText.StringResource(R.string.admission_error_patient_id_required_for_consent),
                "Debe registrar la identificación del paciente antes de emitir el consentimiento."
            )
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, errorUiText = null) }
            val today = LocalDate.now().toString()
            val documentCode = "consentimiento_preop_${pid}_$today"
            val treatmentDescription = s.operativeConsentProposedTreatment.ifBlank { "Tratamiento de operatoria dental y obturaciones estéticas" }
            val formattedTreatment = "[$documentCode] $treatmentDescription"

            val consent = InformedConsent(
                id = "",
                patientId = pid,
                doctorId = validDoctorId,
                documentVersion = "2",
                versionDate = "2019-06-01",
                treatmentSummary = formattedTreatment,
                representativeName = if (s.operativeConsentSignerType == "GUARDIAN") s.operativeConsentRepresentativeName.ifBlank { "${s.guardianFirstName} ${s.guardianLastName}".trim() } else null,
                representativeId = if (s.operativeConsentSignerType == "GUARDIAN") s.operativeConsentRepresentativeId else null,
                patientSignatureUrl = if (s.operativeConsentSignerType == "PATIENT") {
                    s.operativeConsentPatientSignature.ifBlank { "${documentCode}_sig_paciente" }
                } else null,
                guardianSignatureUrl = if (s.operativeConsentSignerType == "GUARDIAN") {
                    s.operativeConsentPatientSignature.ifBlank { "${documentCode}_sig_acudiente" }
                } else null,
                doctorSignatureUrl = s.operativeConsentDoctorSignature.ifBlank { "${documentCode}_sig_doctor" },
                doctorLicenseNumber = s.doctorLicense,
                signedAt = today,
                status = "SIGNED",
                procedures = s.operativeConsentProcedures
            )

            informedConsentRepository.saveConsent(consent)
                .onSuccess {
                    val msg = "Consentimiento de operatoria guardado y firmado correctamente"
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showOperativeConsentDialog = false,
                            operativeConsentSaved = true,
                            successMessage = msg,
                            successUiText = UiText.StringResource(R.string.admission_success_operative_consent)
                        )
                    }
                }
                .onFailure { err ->
                    val msg = err.toUserFriendlyMessage("Error al guardar consentimiento de operatoria")
                    setError(UiText.StringResource(R.string.admission_error_saving_operative_consent, msg), msg)
                }
        }
    }
}
