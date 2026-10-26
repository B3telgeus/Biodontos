package com.example.biodontos.presentation.patients.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.ClinicalHistoryError
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.DomainError
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import com.example.biodontos.domain.model.history.ActivityFilter
import com.example.biodontos.domain.model.history.ActivityType
import com.example.biodontos.domain.model.history.AuditEntry
import com.example.biodontos.domain.model.history.PatientActivity
import com.example.biodontos.domain.repository.AnamnesisRepository
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.BillingRepository
import com.example.biodontos.domain.repository.ClinicalAuditRepository
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import com.example.biodontos.domain.repository.EvolutionRepository
import com.example.biodontos.domain.repository.InformedConsentRepository
import com.example.biodontos.domain.repository.MaterialRepository
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.usecase.history.AddEvolutionClarificationUseCase
import com.example.biodontos.domain.usecase.history.GetPatientActivityTimelineUseCase
import com.example.biodontos.domain.usecase.history.UpdateTreatmentStatusUseCase
import com.example.biodontos.domain.usecase.history.VoidEvolutionUseCase
import com.example.biodontos.domain.usecase.history.VoidPaymentUseCase
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PatientDetailViewModel(
    private val patientId: String,
    initialTab: String = "HISTORY",
    private val patientRepository: PatientRepository,
    private val clinicalRecordRepository: ClinicalRecordRepository,
    private val anamnesisRepository: AnamnesisRepository,
    private val informedConsentRepository: InformedConsentRepository,
    private val evolutionRepository: EvolutionRepository,
    private val billingRepository: BillingRepository,
    private val appointmentRepository: AppointmentRepository,
    private val materialRepository: MaterialRepository,
    private val clinicalAuditRepository: ClinicalAuditRepository = NoOpClinicalAuditRepository,
    private val getPatientActivityTimelineUseCase: GetPatientActivityTimelineUseCase =
        GetPatientActivityTimelineUseCase(
            appointmentRepository = appointmentRepository,
            billingRepository = billingRepository,
            evolutionRepository = evolutionRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            informedConsentRepository = informedConsentRepository,
            clinicalAuditRepository = clinicalAuditRepository
        ),
    private val voidEvolutionUseCase: VoidEvolutionUseCase = VoidEvolutionUseCase(evolutionRepository),
    private val voidPaymentUseCase: VoidPaymentUseCase = VoidPaymentUseCase(billingRepository),
    private val addEvolutionClarificationUseCase: AddEvolutionClarificationUseCase =
        AddEvolutionClarificationUseCase(evolutionRepository),
    private val updateTreatmentStatusUseCase: UpdateTreatmentStatusUseCase =
        UpdateTreatmentStatusUseCase(billingRepository)
) : ViewModel() {

    private val _state = MutableStateFlow(
        PatientDetailState(
            patientId = patientId,
            selectedTab = parseTab(initialTab)
        )
    )
    val state: StateFlow<PatientDetailState> = _state.asStateFlow()

    private val _effectChannel = Channel<PatientDetailEffect>(Channel.BUFFERED)
    val effects: Flow<PatientDetailEffect> = _effectChannel.receiveAsFlow()

    init {
        loadPatientDetail()
        loadMaterialsCatalog()
    }

    fun loadPatientDetail() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    historyState = it.historyState.copy(isTimelineLoading = true, timelineError = null)
                )
            }

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

            // 6. Tratamientos y Pagos
            billingRepository.getTreatmentsByPatientId(patientId).onSuccess { treats ->
                _state.update {
                    it.copy(
                        treatments = treats,
                        selectedPaymentTreatmentId = it.selectedPaymentTreatmentId.ifBlank {
                            treats.firstOrNull { t ->
                                TreatmentStatus.fromString(t.status) != TreatmentStatus.CANCELADO
                            }?.id ?: ""
                        }
                    )
                }
            }
            billingRepository.getPaymentsByPatientId(patientId).onSuccess { pays ->
                _state.update { it.copy(payments = pays) }
            }

            // 7. Citas
            appointmentRepository.getAppointmentsByPatientId(patientId).onSuccess { appts ->
                _state.update { it.copy(appointments = appts) }
            }

            // 8. Línea de Tiempo Unificada con Trazabilidad Clínica
            loadActivityTimelineInternal()

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun refreshTimeline() {
        viewModelScope.launch {
            _state.update {
                it.copy(historyState = it.historyState.copy(isTimelineLoading = true, timelineError = null))
            }
            loadActivityTimelineInternal()
        }
    }

    private suspend fun loadActivityTimelineInternal() {
        getPatientActivityTimelineUseCase(patientId)
            .onSuccess { timelineResult ->
                _state.update { current ->
                    val currentFilter = current.historyState.filter
                    val filtered = currentFilter.apply(timelineResult.activities)
                    current.copy(
                        historyState = current.historyState.copy(
                            allActivities = timelineResult.activities,
                            filteredActivities = filtered,
                            availableMonths = timelineResult.availableMonths,
                            summary = timelineResult.summary,
                            failedSources = timelineResult.failedSources,
                            isTimelineLoading = false,
                            timelineError = null
                        )
                    )
                }
            }
            .onFailure { err ->
                _state.update { current ->
                    current.copy(
                        historyState = current.historyState.copy(
                            isTimelineLoading = false,
                            timelineError = "No se pudo cargar el historial clínico: ${err.toUserFriendlyMessage()}"
                        )
                    )
                }
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

    // --- Filtros del Historial Clínico ---
    fun selectMonthFilter(month: YearMonth?) {
        _state.update { current ->
            val newFilter = current.historyState.filter.copy(month = month)
            current.copy(
                historyState = current.historyState.copy(
                    filter = newFilter,
                    filteredActivities = newFilter.apply(current.historyState.allActivities)
                )
            )
        }
    }

    fun toggleActivityTypeFilter(type: ActivityType) {
        _state.update { current ->
            val currentTypes = current.historyState.filter.types
            val updatedTypes = if (type in currentTypes) {
                if (currentTypes.size > 1) currentTypes - type else currentTypes
            } else {
                currentTypes + type
            }
            val newFilter = current.historyState.filter.copy(types = updatedTypes)
            current.copy(
                historyState = current.historyState.copy(
                    filter = newFilter,
                    filteredActivities = newFilter.apply(current.historyState.allActivities)
                )
            )
        }
    }

    fun clearHistoryFilters() {
        _state.update { current ->
            val resetFilter = ActivityFilter()
            current.copy(
                historyState = current.historyState.copy(
                    filter = resetFilter,
                    filteredActivities = resetFilter.apply(current.historyState.allActivities)
                )
            )
        }
    }

    fun toggleExpandActivity(activityId: String) {
        _state.update { current ->
            val newExpanded = if (current.historyState.expandedActivityId == activityId) null else activityId
            current.copy(historyState = current.historyState.copy(expandedActivityId = newExpanded))
        }
    }

    fun openQuickActionSheet() {
        _state.update { it.copy(historyState = it.historyState.copy(showQuickActionSheet = true)) }
    }

    fun dismissQuickActionSheet() {
        _state.update { it.copy(historyState = it.historyState.copy(showQuickActionSheet = false)) }
    }

    // --- Anulación Lógica en 2 Pasos (Evolución o Pago) ---
    fun requestVoidActivity(activity: PatientActivity) {
        _state.update {
            it.copy(
                historyState = it.historyState.copy(
                    voidDialogTarget = activity,
                    voidReason = "",
                    voidConfirmationStep = 1,
                    isProcessingVoid = false
                )
            )
        }
    }

    fun onVoidReasonChange(reason: String) {
        _state.update {
            it.copy(historyState = it.historyState.copy(voidReason = reason))
        }
    }

    fun advanceVoidConfirmationStep() {
        val currentReason = _state.value.historyState.voidReason.trim()
        if (currentReason.length < VoidEvolutionUseCase.MIN_REASON_LENGTH) {
            emitError("El motivo de anulación debe tener al menos ${VoidEvolutionUseCase.MIN_REASON_LENGTH} caracteres.")
            return
        }
        _state.update {
            it.copy(historyState = it.historyState.copy(voidConfirmationStep = 2))
        }
    }

    fun dismissVoidDialog() {
        _state.update {
            it.copy(
                historyState = it.historyState.copy(
                    voidDialogTarget = null,
                    voidReason = "",
                    voidConfirmationStep = 1,
                    isProcessingVoid = false
                )
            )
        }
    }

    fun confirmVoidActivity() {
        val hist = _state.value.historyState
        val target = hist.voidDialogTarget ?: return
        val reason = hist.voidReason

        viewModelScope.launch {
            _state.update {
                it.copy(historyState = it.historyState.copy(isProcessingVoid = true), errorMessage = null)
            }

            val result = when (target) {
                is PatientActivity.EvolutionActivity -> voidEvolutionUseCase(target.evolution, reason)
                is PatientActivity.PaymentActivity -> voidPaymentUseCase(target.payment, reason)
                else -> {
                    _state.update { it.copy(historyState = it.historyState.copy(isProcessingVoid = false)) }
                    return@launch
                }
            }

            result.fold(
                ifLeft = { domainError ->
                    _state.update {
                        it.copy(historyState = it.historyState.copy(isProcessingVoid = false))
                    }
                    emitError(domainError.toDisplayMessage())
                },
                ifRight = {
                    _state.update {
                        it.copy(
                            historyState = it.historyState.copy(
                                voidDialogTarget = null,
                                voidReason = "",
                                voidConfirmationStep = 1,
                                isProcessingVoid = false
                            ),
                            successMessage = "Registro anulado con trazabilidad normativa."
                        )
                    }
                    _effectChannel.send(
                        PatientDetailEffect.ShowSnackbar("Registro anulado con trazabilidad normativa.")
                    )
                    loadPatientDetail()
                }
            )
        }
    }

    // --- Nota Aclaratoria de Evolución ---
    fun openClarificationDialog(evolution: Evolution) {
        _state.update {
            it.copy(
                historyState = it.historyState.copy(
                    clarificationTarget = evolution,
                    clarificationText = "",
                    isSavingClarification = false
                )
            )
        }
    }

    fun onClarificationTextChange(text: String) {
        _state.update {
            it.copy(historyState = it.historyState.copy(clarificationText = text))
        }
    }

    fun dismissClarificationDialog() {
        _state.update {
            it.copy(
                historyState = it.historyState.copy(
                    clarificationTarget = null,
                    clarificationText = "",
                    isSavingClarification = false
                )
            )
        }
    }

    fun saveClarification() {
        val hist = _state.value.historyState
        val original = hist.clarificationTarget ?: return
        val text = hist.clarificationText

        viewModelScope.launch {
            _state.update {
                it.copy(historyState = it.historyState.copy(isSavingClarification = true), errorMessage = null)
            }

            addEvolutionClarificationUseCase(
                originalEvolution = original,
                clarificationText = text,
                doctorId = original.doctorId,
                doctorName = original.doctorName ?: "Doctor Tratante"
            ).fold(
                ifLeft = { err ->
                    _state.update {
                        it.copy(historyState = it.historyState.copy(isSavingClarification = false))
                    }
                    emitError(err.toDisplayMessage())
                },
                ifRight = {
                    _state.update {
                        it.copy(
                            historyState = it.historyState.copy(
                                clarificationTarget = null,
                                clarificationText = "",
                                isSavingClarification = false
                            ),
                            successMessage = "Nota aclaratoria vinculada con éxito."
                        )
                    }
                    _effectChannel.send(
                        PatientDetailEffect.ShowSnackbar("Nota aclaratoria vinculada con éxito.")
                    )
                    loadPatientDetail()
                }
            )
        }
    }

    // --- Cambio de Estado de Presupuesto de Tratamiento ---
    fun openTreatmentStatusDialog(treatment: Treatment) {
        val currentStatus = TreatmentStatus.fromString(treatment.status)
        val firstAllowed = currentStatus.availableTransitions().firstOrNull()
        _state.update {
            it.copy(
                historyState = it.historyState.copy(
                    statusDialogTarget = treatment,
                    selectedTargetStatus = firstAllowed,
                    statusChangeReason = "",
                    isUpdatingTreatmentStatus = false
                )
            )
        }
    }

    fun onSelectTargetTreatmentStatus(status: TreatmentStatus) {
        _state.update {
            it.copy(historyState = it.historyState.copy(selectedTargetStatus = status))
        }
    }

    fun onTreatmentStatusReasonChange(reason: String) {
        _state.update {
            it.copy(historyState = it.historyState.copy(statusChangeReason = reason))
        }
    }

    fun dismissTreatmentStatusDialog() {
        _state.update {
            it.copy(
                historyState = it.historyState.copy(
                    statusDialogTarget = null,
                    selectedTargetStatus = null,
                    statusChangeReason = "",
                    isUpdatingTreatmentStatus = false
                )
            )
        }
    }

    fun confirmTreatmentStatusUpdate() {
        val hist = _state.value.historyState
        val treatment = hist.statusDialogTarget ?: return
        val targetStatus = hist.selectedTargetStatus ?: return
        val reason = hist.statusChangeReason

        viewModelScope.launch {
            _state.update {
                it.copy(historyState = it.historyState.copy(isUpdatingTreatmentStatus = true), errorMessage = null)
            }

            updateTreatmentStatusUseCase(
                treatment = treatment,
                targetStatus = targetStatus,
                reason = reason
            ).fold(
                ifLeft = { err ->
                    _state.update {
                        it.copy(historyState = it.historyState.copy(isUpdatingTreatmentStatus = false))
                    }
                    emitError(err.toDisplayMessage())
                },
                ifRight = {
                    _state.update {
                        it.copy(
                            historyState = it.historyState.copy(
                                statusDialogTarget = null,
                                selectedTargetStatus = null,
                                statusChangeReason = "",
                                isUpdatingTreatmentStatus = false
                            ),
                            successMessage = "Estado del presupuesto actualizado a ${targetStatus.displayName}."
                        )
                    }
                    _effectChannel.send(
                        PatientDetailEffect.ShowSnackbar(
                            "Estado del presupuesto actualizado a ${targetStatus.displayName}."
                        )
                    )
                    loadPatientDetail()
                }
            )
        }
    }

    // --- Control de Diálogo de Nuevo Presupuesto de Tratamiento ---
    fun openNewTreatmentDialog() {
        _state.update {
            it.copy(
                historyState = it.historyState.copy(showQuickActionSheet = false),
                showNewTreatmentDialog = true,
                newTreatmentDescription = "",
                newTreatmentTotalCost = "",
                isSavingTreatment = false
            )
        }
    }

    fun dismissNewTreatmentDialog() {
        _state.update { it.copy(showNewTreatmentDialog = false) }
    }

    fun onNewTreatmentDescriptionChange(value: String) {
        _state.update { it.copy(newTreatmentDescription = value) }
    }

    fun onNewTreatmentTotalCostChange(value: String) {
        _state.update { it.copy(newTreatmentTotalCost = value) }
    }

    fun saveTreatment() {
        val s = _state.value
        val desc = s.newTreatmentDescription.trim()
        val costPesos = s.newTreatmentTotalCost.trim().toLongOrNull() ?: 0L

        if (desc.isBlank()) {
            emitError("La descripción del presupuesto de tratamiento es obligatoria.")
            return
        }
        if (costPesos <= 0L) {
            emitError("El costo del presupuesto debe ser mayor a $0.")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSavingTreatment = true, errorMessage = null) }
            val treatment = Treatment(
                patientId = PatientId(patientId),
                description = desc,
                totalCost = CopAmount(costPesos),
                status = TreatmentStatus.PENDIENTE.name
            )
            billingRepository.createTreatment(treatment)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSavingTreatment = false,
                            showNewTreatmentDialog = false,
                            successMessage = "¡Presupuesto de tratamiento creado con éxito!"
                        )
                    }
                    loadPatientDetail()
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isSavingTreatment = false,
                            errorMessage = "Error al crear presupuesto: ${err.toUserFriendlyMessage()}"
                        )
                    }
                }
        }
    }

    // --- Control de Diálogo de Registrar Pago / Abono ---
    fun openNewPaymentDialog(preselectedTreatmentId: String? = null) {
        val activeTreatments = _state.value.treatments.filter {
            TreatmentStatus.fromString(it.status) != TreatmentStatus.CANCELADO
        }
        val targetTreatmentId = preselectedTreatmentId
            ?: activeTreatments.firstOrNull()?.id
            ?: ""

        _state.update {
            it.copy(
                historyState = it.historyState.copy(showQuickActionSheet = false),
                showNewPaymentDialog = true,
                selectedPaymentTreatmentId = targetTreatmentId,
                newPaymentAmount = "",
                newPaymentMethod = "EFECTIVO",
                isSavingPayment = false
            )
        }
    }

    fun dismissNewPaymentDialog() {
        _state.update { it.copy(showNewPaymentDialog = false) }
    }

    fun onSelectPaymentTreatment(treatmentId: String) {
        _state.update { it.copy(selectedPaymentTreatmentId = treatmentId) }
    }

    fun onNewPaymentAmountChange(amount: String) {
        _state.update { it.copy(newPaymentAmount = amount) }
    }

    fun onNewPaymentMethodChange(method: String) {
        _state.update { it.copy(newPaymentMethod = method) }
    }

    fun savePayment() {
        val s = _state.value
        if (s.selectedPaymentTreatmentId.isBlank()) {
            emitError("Debe seleccionar un presupuesto activo para imputar el pago.")
            return
        }
        val amountPesos = s.newPaymentAmount.trim().toLongOrNull() ?: 0L
        if (amountPesos <= 0L) {
            emitError("El monto del abono debe ser mayor a $0.")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSavingPayment = true, errorMessage = null) }
            val payment = Payment(
                treatmentId = s.selectedPaymentTreatmentId,
                amount = CopAmount(amountPesos),
                paymentMethod = s.newPaymentMethod
            )
            billingRepository.recordPayment(payment)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSavingPayment = false,
                            showNewPaymentDialog = false,
                            successMessage = "¡Abono registrado con éxito!"
                        )
                    }
                    loadPatientDetail()
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isSavingPayment = false,
                            errorMessage = "Error al registrar abono: ${err.toUserFriendlyMessage()}"
                        )
                    }
                }
        }
    }

    // --- Control de Diálogo de Nueva Evolución ---
    fun openNewEvolutionDialog() {
        _state.update {
            it.copy(
                historyState = it.historyState.copy(showQuickActionSheet = false),
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
                historyState = it.historyState.copy(showQuickActionSheet = false),
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

    private fun emitError(message: String) {
        _state.update { it.copy(errorMessage = message) }
        viewModelScope.launch {
            _effectChannel.send(PatientDetailEffect.ShowSnackbar(message))
        }
    }

    private fun DomainError.toDisplayMessage(): String = when (this) {
        is ClinicalHistoryError -> userMessage
        is DomainError.NetworkOrPersistenceFailure -> reason
        else -> "No se pudo completar la operación clínica solicitada."
    }

    private object NoOpClinicalAuditRepository : ClinicalAuditRepository {
        override suspend fun getAuditTrail(patientId: String, limit: Int): Result<List<AuditEntry>> =
            Result.success(emptyList())
    }

    companion object {
        fun parseTab(tabName: String): PatientDetailTab = when (tabName.uppercase()) {
            "HISTORY" -> PatientDetailTab.HISTORY
            "SUMMARY" -> PatientDetailTab.SUMMARY
            "ODONTOGRAM" -> PatientDetailTab.ODONTOGRAM
            "EVOLUTIONS" -> PatientDetailTab.EVOLUTIONS
            "TREATMENTS" -> PatientDetailTab.TREATMENTS
            else -> PatientDetailTab.HISTORY
        }
    }
}
