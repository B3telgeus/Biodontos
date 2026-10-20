package com.example.biodontos.presentation.admission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.*
import com.example.biodontos.domain.repository.*
import com.example.biodontos.core.util.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

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
    private val initialPatientId: String? = null
) : ViewModel() {

    private val _state = MutableStateFlow(ClinicalAdmissionState())
    val state: StateFlow<ClinicalAdmissionState> = _state.asStateFlow()

    init {
        loadMaterialsCatalog()
        if (!initialPatientId.isNullOrBlank()) {
            loadExistingPatientData(initialPatientId)
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
                            anamnesisObservaciones = anamnesis.observaciones ?: "",
                            antecedentesFamiliares = anamnesis.antecedentesFamiliares ?: "",
                            medicamentosActuales = anamnesis.medicamentosActuales ?: ""
                        )
                    }
                }
            }

            // 5. Odontograma
            odontogramRepository.getOdontogram(pid).onSuccess { teethList ->
                val surfacesMap = teethList.associate { it.number to it.surfaceStates }
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
                            consentSigned = consent.status == "SIGNED" || !consent.patientSignatureUrl.isNullOrBlank(),
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
            materialRepository.getMaterials()
                .onSuccess { list ->
                    _state.update {
                        it.copy(
                            materialsCatalog = list,
                            selectedCatalogMaterial = list.firstOrNull()
                        )
                    }
                }
        }
    }

    fun onEvent(event: ClinicalAdmissionEvent) {
        when (event) {
            is ClinicalAdmissionEvent.NextStep -> advanceStep()
            is ClinicalAdmissionEvent.PreviousStep -> {
                _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1)) }
            }
            is ClinicalAdmissionEvent.JumpToStep -> {
                if (event.step < _state.value.currentStep || _state.value.patientId.isNotBlank()) {
                    _state.update { it.copy(currentStep = event.step.coerceIn(1, 7)) }
                }
            }

            // Paso 1: Identificación
            is ClinicalAdmissionEvent.UpdateDni -> {
                _state.update { it.copy(dniCedula = event.value, dniError = null) }
            }
            is ClinicalAdmissionEvent.UpdateFirstName -> {
                _state.update { it.copy(firstName = event.value, nameError = null) }
            }
            is ClinicalAdmissionEvent.UpdateFirstLastName -> {
                _state.update { it.copy(firstLastName = event.value, nameError = null) }
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
            is ClinicalAdmissionEvent.UpdateOccupation -> {
                _state.update { it.copy(occupation = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateMaritalStatus -> {
                _state.update { it.copy(maritalStatus = event.value) }
            }
            is ClinicalAdmissionEvent.UpdateGuardian -> {
                _state.update {
                    it.copy(
                        guardianRelationship = event.relationship,
                        guardianFirstName = event.firstName,
                        guardianLastName = event.lastName,
                        guardianPhone = event.phone,
                        guardianAddress = event.address
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
                val used = EvolutionMaterialUsed(
                    materialId = mat.id,
                    materialName = mat.name,
                    quantityUsed = event.quantity,
                    lotNumberSnapshot = mat.batchLotNumber,
                    invimaSnapshot = mat.invimaRegistration,
                    expirationDateSnapshot = mat.expirationDate
                )
                _state.update { it.copy(usedMaterials = it.usedMaterials + used) }
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
            _state.update { it.copy(dniError = "El número de documento es obligatorio") }
            return
        }
        if (s.firstName.isBlank() || s.firstLastName.isBlank()) {
            _state.update { it.copy(nameError = "El nombre y primer apellido son obligatorios") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val effectivePatientId = s.patientId.ifBlank { Patient.generateIdFromDni(s.dniCedula) }
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
                occupation = s.occupation.ifBlank { null },
                maritalStatus = s.maritalStatus.ifBlank { null },
                guardianRelationship = s.guardianRelationship.ifBlank { null },
                guardianFirstName = s.guardianFirstName.ifBlank { null },
                guardianLastName = s.guardianLastName.ifBlank { null },
                guardianPhone = s.guardianPhone.ifBlank { null },
                guardianAddress = s.guardianAddress.ifBlank { null },
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
                            _state.update { it.copy(isLoading = false, errorMessage = "Error en historia: ${err.toUserFriendlyMessage()}") }
                        }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoading = false, errorMessage = "Error al guardar paciente: ${err.toUserFriendlyMessage()}") }
                }
        }
    }

    private fun persistStep2AndAdvance() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
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
                    _state.update { it.copy(isLoading = false, errorMessage = "Error en anamnesis: ${err.toUserFriendlyMessage()}") }
                }
        }
    }

    private fun persistStep3AndAdvance() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            // Persistir superficies dentales en el repositorio de odontograma
            for ((toothNum, surfaces) in s.teethSurfaces) {
                for ((surface, condition) in surfaces) {
                    odontogramRepository.updateToothState(s.patientId, toothNum, surface, condition, "Admisión inicial")
                }
            }
            _state.update { it.copy(isLoading = false, currentStep = 4) }
        }
    }

    private fun persistStep4AndAdvance() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val exam = PeriodontalExamination(
                recordId = s.clinicalRecordId,
                doctorId = s.doctorId.ifBlank { "00000000-0000-0000-0000-000000000000" },
                examinationDate = LocalDate.now().toString(),
                generalObservations = s.periodontalObservations.ifBlank { null },
                sites = s.siteMeasurements.values.toList()
            )

            periodontalRepository.saveExamination(exam)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, currentStep = 5) }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoading = false, errorMessage = "Error en periodontograma: ${err.message}") }
                }
        }
    }

    private fun persistStep5AndAdvance() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            // 1. Guardar diagnósticos en ClinicalRecord
            val record = ClinicalRecord(
                id = s.clinicalRecordId,
                patientId = s.patientId,
                dentitionType = s.dentitionType,
                mainDiagnosis = s.mainDiagnosis.ifBlank { null },
                relatedDiagnoses = s.relatedDiagnoses.ifBlank { null },
                complementaryExams = s.complementaryExams.ifBlank { null },
                status = ClinicalRecordStatus.ABIERTA
            )
            clinicalRecordRepository.saveRecord(record)

            // 2. Guardar Consentimiento Informado
            val consent = InformedConsent(
                patientId = s.patientId,
                doctorId = s.doctorId.ifBlank { "00000000-0000-0000-0000-000000000000" },
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
                    _state.update { it.copy(isLoading = false, errorMessage = "Error en consentimiento: ${err.toUserFriendlyMessage()}") }
                }
        }
    }

    private fun persistStep6AndAdvance() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val cost = s.evolutionCost.toDoubleOrNull() ?: 0.0
            val payment = s.evolutionPayment.toDoubleOrNull() ?: 0.0
            val evolution = Evolution(
                recordId = s.clinicalRecordId,
                doctorId = s.doctorId.ifBlank { "00000000-0000-0000-0000-000000000000" },
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

            evolutionRepository.saveEvolution(evolution)
                .onSuccess {
                    // Cierre médico-legal si el doctor lo seleccionó
                    if (s.closeRecordOnFinish) {
                        clinicalRecordRepository.closeRecord(
                            recordId = s.clinicalRecordId,
                            doctorId = s.doctorId,
                            summary = s.closureSummary.ifBlank { "Tratamiento de ingreso concluido." },
                            signatureUrl = s.evolutionPatientSignature
                        )
                    }
                    _state.update { it.copy(isLoading = false, currentStep = 7) }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoading = false, errorMessage = "Error en evolución: ${err.toUserFriendlyMessage()}") }
                }
        }
    }

    private fun finishAdmission() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            // Si el doctor programó fecha para la próxima cita
            if (s.nextAppointmentDate.isNotBlank()) {
                val appointment = Appointment(
                    patientId = s.patientId,
                    doctorId = s.doctorId.ifBlank { null },
                    startTime = Appointment.parseDateTime("${s.nextAppointmentDate}T${s.nextAppointmentTime}:00"),
                    reason = s.nextAppointmentReason,
                    status = AppointmentStatus.SCHEDULED,
                    isConfirmed = false
                )
                appointmentRepository.createAppointment(appointment)
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    isFinished = true,
                    successMessage = "¡Proceso de ingreso clínico finalizado con éxito!"
                )
            }
        }
    }
}
