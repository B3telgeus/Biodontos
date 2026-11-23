package com.example.biodontos.presentation.patients.detail

import com.example.biodontos.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.Ambar
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorBg
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosLightGrey
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.Carmesi
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.core.designsystem.Esmeralda
import com.example.biodontos.core.designsystem.SlateLight
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.core.designsystem.Zafiro
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.core.designsystem.components.DateBadge
import com.example.biodontos.domain.model.Anamnesis
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.Cie10Code
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.ConsentType
import com.example.biodontos.domain.model.CupsCode
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.PeriodontalSiteLocation
import com.example.biodontos.domain.model.PeriodontalSiteMeasurement
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import com.example.biodontos.domain.model.history.ActivityType
import com.example.biodontos.domain.model.history.PatientActivity
import com.example.biodontos.domain.usecase.clinical.ConsentRequirementStatus
import com.example.biodontos.presentation.components.Cie10CodeDropdown
import com.example.biodontos.presentation.components.CupsCodeDropdown
import com.example.biodontos.presentation.patients.detail.history.ClarificationNoteDialog
import com.example.biodontos.presentation.patients.detail.history.ClinicalConsentDialog
import com.example.biodontos.presentation.patients.detail.history.EvolutionMaterialsSnapshotCard
import com.example.biodontos.presentation.patients.detail.history.NewPaymentDialog
import com.example.biodontos.presentation.patients.detail.history.NewTreatmentBudgetDialog
import com.example.biodontos.presentation.patients.detail.history.PatientHistoryTab
import com.example.biodontos.presentation.patients.detail.history.QuickCreateActivityBottomSheet
import com.example.biodontos.presentation.patients.detail.history.TreatmentStatusUpdateDialog
import com.example.biodontos.presentation.patients.detail.history.VoidActivityDialog
import com.example.biodontos.presentation.periodontogram.InteractivePeriodontogram
import com.example.biodontos.presentation.patients.detail.tabs.*
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    state: PatientDetailState,
    onTabSelected: (PatientDetailTab) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToOdontogram: () -> Unit = {},
    onNavigateToAdmission: () -> Unit = {},
    onOpenNewEvolutionDialog: () -> Unit = {},
    onDismissNewEvolutionDialog: () -> Unit = {},
    onEvolutionCie10Change: (Cie10Code?) -> Unit = {},
    onEvolutionCupsChange: (CupsCode?) -> Unit = {},
    onEvolutionDescriptionChange: (String) -> Unit = {},
    onEvolutionToothChange: (Int?) -> Unit = {},
    onEvolutionCostChange: (String) -> Unit = {},
    onEvolutionPaymentChange: (String) -> Unit = {},
    onSelectCatalogMaterial: (ClinicalMaterial) -> Unit = {},
    onAddMaterial: (Double) -> Unit = {},
    onRemoveMaterial: (String) -> Unit = {},
    onSaveEvolution: () -> Unit = {},
    onOpenNewAppointmentDialog: () -> Unit = {},
    onDismissNewAppointmentDialog: () -> Unit = {},
    onAppointmentDateChange: (String) -> Unit = {},
    onAppointmentTimeChange: (String) -> Unit = {},
    onAppointmentReasonChange: (String) -> Unit = {},
    onSaveAppointment: () -> Unit = {},
    onClearMessages: () -> Unit = {},
    onSelectMonthFilter: (YearMonth?) -> Unit = {},
    onToggleActivityTypeFilter: (ActivityType) -> Unit = {},
    onClearHistoryFilters: () -> Unit = {},
    onToggleExpandActivity: (String) -> Unit = {},
    onOpenQuickActionSheet: () -> Unit = {},
    onDismissQuickActionSheet: () -> Unit = {},
    onRequestVoidActivity: (PatientActivity) -> Unit = {},
    onVoidReasonChange: (String) -> Unit = {},
    onAdvanceVoidStep: () -> Unit = {},
    onConfirmVoidActivity: () -> Unit = {},
    onDismissVoidDialog: () -> Unit = {},
    onOpenClarificationDialog: (Evolution) -> Unit = {},
    onClarificationTextChange: (String) -> Unit = {},
    onSaveClarification: () -> Unit = {},
    onDismissClarificationDialog: () -> Unit = {},
    onOpenTreatmentStatusDialog: (Treatment) -> Unit = {},
    onSelectTargetTreatmentStatus: (TreatmentStatus) -> Unit = {},
    onTreatmentStatusReasonChange: (String) -> Unit = {},
    onConfirmTreatmentStatusUpdate: () -> Unit = {},
    onDismissTreatmentStatusDialog: () -> Unit = {},
    onOpenNewTreatmentDialog: () -> Unit = {},
    onDismissNewTreatmentDialog: () -> Unit = {},
    onNewTreatmentDescriptionChange: (String) -> Unit = {},
    onNewTreatmentTotalCostChange: (String) -> Unit = {},
    onSaveTreatment: () -> Unit = {},
    onOpenNewPaymentDialog: (String?) -> Unit = {},
    onDismissNewPaymentDialog: () -> Unit = {},
    onSelectPaymentTreatment: (String) -> Unit = {},
    onNewPaymentAmountChange: (String) -> Unit = {},
    onNewPaymentMethodChange: (String) -> Unit = {},
    onSavePayment: () -> Unit = {},
    onRefreshTimeline: () -> Unit = {},
    onOpenClinicalConsentDialog: (ConsentType, Int?) -> Unit = { _, _ -> },
    onDismissClinicalConsentDialog: () -> Unit = {},
    onSaveClinicalConsent: (InformedConsent) -> Unit = {},
    onSelectPeriodontalTooth: (Int) -> Unit = {},
    onUpdatePeriodontalMeasurement: (PeriodontalSiteMeasurement) -> Unit = {},
    onPeriodontalObservationsChange: (String) -> Unit = {},
    onSavePeriodontogram: () -> Unit = {}
) {
    val patient = state.patient
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }

    Scaffold(
        modifier = Modifier.testTag(TestTags.Patients.DETAIL_ROOT),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = patient?.fullName ?: "Ficha Clínica del Paciente",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BiodontosDark
                        )
                        Text(
                            text = stringResource(R.string.ui_doctor_documents_screen_231, patient?.dniCedula ?: state.patientId, patient?.eps ?: "Particular"),
                            style = MaterialTheme.typography.bodySmall,
                            color = BiodontosGreyText
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag(TestTags.Patients.DETAIL_BACK_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ui_doctor_registration_screen_110),
                            tint = BiodontosDark
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onNavigateToAdmission,
                        modifier = Modifier.testTag(TestTags.Patients.DETAIL_ADMISSION_BUTTON)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.ui_doctor_documents_screen_237), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BiodontosWhite)
            )
        },
        floatingActionButton = {
            when (state.selectedTab) {
                PatientDetailTab.HISTORY -> {
                    ExtendedFloatingActionButton(
                        onClick = onOpenQuickActionSheet,
                        containerColor = BiodontosDark,
                        contentColor = BiodontosWhite,
                        modifier = Modifier.testTag(TestTags.Patients.DETAIL_FAB)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ui_patient_detail_screen_500))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.ui_patient_detail_screen_500), fontWeight = FontWeight.Bold)
                    }
                }
                PatientDetailTab.EVOLUTIONS -> {
                    ExtendedFloatingActionButton(
                        onClick = onOpenNewEvolutionDialog,
                        containerColor = BiodontosDark,
                        contentColor = BiodontosWhite,
                        modifier = Modifier.testTag(TestTags.Patients.DETAIL_FAB)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ui_patient_detail_screen_501))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.ui_patient_detail_screen_501), fontWeight = FontWeight.Bold)
                    }
                }
                PatientDetailTab.TREATMENTS -> {
                    ExtendedFloatingActionButton(
                        onClick = onOpenNewAppointmentDialog,
                        containerColor = BiodontosDark,
                        contentColor = BiodontosWhite,
                        modifier = Modifier.testTag(TestTags.Patients.DETAIL_FAB)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.ui_patient_detail_screen_502))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.ui_patient_detail_screen_502), fontWeight = FontWeight.Bold)
                    }
                }
                else -> {}
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .background(BiodontosLightGrey)
        ) {
            ClinicalStatusBar(
                patient = patient,
                anamnesis = state.anamnesis,
                consent = state.consent,
                modifier = Modifier.testTag(TestTags.Patients.DETAIL_STATUS_BAR)
            )

            ScrollableTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = BiodontosWhite,
                contentColor = BiodontosDark,
                edgePadding = 12.dp,
                modifier = Modifier.testTag(TestTags.Patients.DETAIL_TAB_ROW)
            ) {
                PatientDetailTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.testTag(TestTags.Patients.detailTab(tab.name)),
                        text = {
                            Text(
                                text = tab.label,
                                fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = BiodontosDark,
                        modifier = Modifier.testTag(TestTags.Patients.DETAIL_LOADING)
                    )
                }
            } else if (state.selectedTab == PatientDetailTab.HISTORY) {
                PatientHistoryTab(
                    historyState = state.historyState,
                    onSelectMonth = onSelectMonthFilter,
                    onToggleType = onToggleActivityTypeFilter,
                    onClearFilters = onClearHistoryFilters,
                    onToggleExpandActivity = onToggleExpandActivity,
                    onRequestVoidActivity = onRequestVoidActivity,
                    onOpenClarificationDialog = onOpenClarificationDialog,
                    onOpenTreatmentStatusDialog = onOpenTreatmentStatusDialog,
                    onOpenNewPaymentForTreatment = { treatId -> onOpenNewPaymentDialog(treatId) },
                    onRetry = onRefreshTimeline
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TestTags.Patients.DETAIL_LIST),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (state.selectedTab) {
                        PatientDetailTab.HISTORY -> {}
                        PatientDetailTab.SUMMARY -> {
                            item(key = "demographics") { PatientDemographicsCard(patient = patient) }
                            item(key = "anamnesis") { AnamnesisSummaryCard(anamnesis = state.anamnesis) }
                            item(key = "consent") { ConsentSummaryCard(consent = state.consent) }
                        }
                        PatientDetailTab.ODONTOGRAM -> {
                            item(key = "odontogram-hub") {
                                OdontogramHubCard(
                                    dentitionType = state.clinicalRecord?.dentitionType?.name ?: "ADULT",
                                    mainDiagnosis = state.clinicalRecord?.mainDiagnosis ?: "Sin diagnóstico registrado",
                                    onOpenOdontogram = onNavigateToOdontogram,
                                    onOpenPeriodontogram = { onTabSelected(PatientDetailTab.PERIODONTOGRAM) }
                                )
                            }
                        }
                        PatientDetailTab.PERIODONTOGRAM -> {
                            item(key = "periodontogram-content") {
                                PeriodontogramTabContent(
                                    selectedTooth = state.periodontalSelectedTooth,
                                    availableTeeth = state.availableFdiTeeth,
                                    siteMeasurements = state.periodontalSiteMeasurements,
                                    observations = state.periodontalObservations,
                                    isSaving = state.isSavingPeriodontogram,
                                    onToothSelected = onSelectPeriodontalTooth,
                                    onMeasurementChanged = onUpdatePeriodontalMeasurement,
                                    onObservationsChanged = onPeriodontalObservationsChange,
                                    onSave = onSavePeriodontogram
                                )
                            }
                        }
                        PatientDetailTab.EVOLUTIONS -> {
                            if (state.evolutions.isEmpty()) {
                                item(key = "evolutions-empty") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = stringResource(R.string.ui_patient_detail_screen_503),
                                                color = BiodontosGreyText,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = onOpenNewEvolutionDialog,
                                                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                                            ) {
                                                Text(stringResource(R.string.ui_patient_detail_screen_504))
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(state.evolutions, key = { it.id }) { evo ->
                                    EvolutionItemCard(evolution = evo)
                                }
                            }
                        }
                        PatientDetailTab.TREATMENTS -> {
                            item(key = "treatments-balance") {
                                TreatmentsBalanceHeaderCard(
                                    totalBilled = state.totalBilled,
                                    totalPaid = state.totalPaid,
                                    balance = state.pendingBalance
                                )
                            }
                            item(key = "treatments-header") {
                                Text(
                                    text = stringResource(R.string.ui_patient_detail_screen_505),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BiodontosDark
                                )
                            }
                            if (state.treatments.isEmpty()) {
                                item(key = "treatments-empty") {
                                    Text(
                                        text = stringResource(R.string.ui_patient_detail_screen_506),
                                        color = BiodontosGreyText,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                items(state.treatments, key = { it.id }) { treat ->
                                    TreatmentItemCard(treatment = treat)
                                }
                            }
                            item(key = "appointments-header") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.ui_patient_detail_screen_507),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BiodontosDark
                                )
                            }
                            if (state.appointments.isEmpty()) {
                                item(key = "appointments-empty") {
                                    Text(
                                        text = stringResource(R.string.ui_patient_detail_screen_508),
                                        color = BiodontosGreyText,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                items(state.appointments, key = { it.id }) { appt ->
                                    PatientAppointmentItemCard(appointment = appt)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.historyState.showQuickActionSheet) {
        QuickCreateActivityBottomSheet(
            onCreateEvolution = onOpenNewEvolutionDialog,
            onCreateAppointment = onOpenNewAppointmentDialog,
            onCreateTreatmentBudget = onOpenNewTreatmentDialog,
            onCreatePayment = { onOpenNewPaymentDialog(null) },
            onCreateConsent = { onOpenClinicalConsentDialog(ConsentType.GENERAL_ODONTOLOGY, null) },
            onDismiss = onDismissQuickActionSheet
        )
    }

    val voidTarget = state.historyState.voidDialogTarget
    if (voidTarget != null) {
        VoidActivityDialog(
            target = voidTarget,
            reason = state.historyState.voidReason,
            step = state.historyState.voidConfirmationStep,
            isProcessing = state.historyState.isProcessingVoid,
            onReasonChange = onVoidReasonChange,
            onAdvanceStep = onAdvanceVoidStep,
            onConfirmVoid = onConfirmVoidActivity,
            onDismiss = onDismissVoidDialog
        )
    }

    val clarificationTarget = state.historyState.clarificationTarget
    if (clarificationTarget != null) {
        ClarificationNoteDialog(
            originalEvolution = clarificationTarget,
            clarificationText = state.historyState.clarificationText,
            isSaving = state.historyState.isSavingClarification,
            onTextChange = onClarificationTextChange,
            onSave = onSaveClarification,
            onDismiss = onDismissClarificationDialog
        )
    }

    val statusTarget = state.historyState.statusDialogTarget
    if (statusTarget != null) {
        TreatmentStatusUpdateDialog(
            treatment = statusTarget,
            selectedTargetStatus = state.historyState.selectedTargetStatus,
            reason = state.historyState.statusChangeReason,
            isUpdating = state.historyState.isUpdatingTreatmentStatus,
            onSelectStatus = onSelectTargetTreatmentStatus,
            onReasonChange = onTreatmentStatusReasonChange,
            onConfirm = onConfirmTreatmentStatusUpdate,
            onDismiss = onDismissTreatmentStatusDialog
        )
    }

    if (state.showNewTreatmentDialog) {
        NewTreatmentBudgetDialog(
            description = state.newTreatmentDescription,
            totalCost = state.newTreatmentTotalCost,
            isSaving = state.isSavingTreatment,
            onDescriptionChange = onNewTreatmentDescriptionChange,
            onTotalCostChange = onNewTreatmentTotalCostChange,
            onSave = onSaveTreatment,
            onDismiss = onDismissNewTreatmentDialog
        )
    }

    if (state.showNewPaymentDialog) {
        NewPaymentDialog(
            treatments = state.treatments,
            selectedTreatmentId = state.selectedPaymentTreatmentId,
            amount = state.newPaymentAmount,
            method = state.newPaymentMethod,
            isSaving = state.isSavingPayment,
            onSelectTreatment = onSelectPaymentTreatment,
            onAmountChange = onNewPaymentAmountChange,
            onMethodChange = onNewPaymentMethodChange,
            onSave = onSavePayment,
            onDismiss = onDismissNewPaymentDialog
        )
    }

    if (state.showNewEvolutionDialog) {
        NewEvolutionDialog(
            selectedCie10 = state.newEvolutionCie10,
            selectedCups = state.newEvolutionCups,
            cie10Error = state.newEvolutionCie10Error,
            cupsError = state.newEvolutionCupsError,
            description = state.newEvolutionDescription,
            tooth = state.newEvolutionToothTreated,
            cost = state.newEvolutionCost,
            payment = state.newEvolutionPayment,
            materials = state.newEvolutionMaterials,
            catalog = state.materialsCatalog,
            selectedMaterial = state.selectedCatalogMaterial,
            isSaving = state.isSavingEvolution,
            consentStatus = state.evolutionConsentStatus,
            onOpenConsentForm = { type, tooth -> onOpenClinicalConsentDialog(type, tooth) },
            onCie10Change = onEvolutionCie10Change,
            onCupsChange = onEvolutionCupsChange,
            onDescriptionChange = onEvolutionDescriptionChange,
            onToothChange = onEvolutionToothChange,
            onCostChange = onEvolutionCostChange,
            onPaymentChange = onEvolutionPaymentChange,
            onSelectCatalogMaterial = onSelectCatalogMaterial,
            onAddMaterial = onAddMaterial,
            onRemoveMaterial = onRemoveMaterial,
            onSave = onSaveEvolution,
            onDismiss = onDismissNewEvolutionDialog
        )
    }

    if (state.showClinicalConsentDialog) {
        ClinicalConsentDialog(
            patient = state.patient,
            initialConsentType = state.activeConsentType,
            preselectedTooth = state.preselectedToothForConsent,
            onDismiss = onDismissClinicalConsentDialog,
            onSaveConsent = onSaveClinicalConsent
        )
    }

    if (state.showNewAppointmentDialog) {
        NewAppointmentDialog(
            date = state.newAppointmentDate,
            time = state.newAppointmentTime,
            reason = state.newAppointmentReason,
            isSaving = state.isSavingAppointment,
            onDateChange = onAppointmentDateChange,
            onTimeChange = onAppointmentTimeChange,
            onReasonChange = onAppointmentReasonChange,
            onSave = onSaveAppointment,
            onDismiss = onDismissNewAppointmentDialog
        )
    }
}

@Composable
private fun ClinicalStatusBar(
    patient: Patient?,
    anamnesis: Anamnesis?,
    consent: InformedConsent?,
    modifier: Modifier = Modifier
) {
    val isConsentSigned = consent?.status.equals("SIGNED", ignoreCase = true) ||
        consent?.signedAt != null ||
        patient?.consentSigned == true
    val hasAllergies = patient?.hasCriticalAllergies == true || anamnesis?.alergias == true
    val allergyText = when {
        anamnesis?.alergiasDescripcion?.isNotBlank() == true -> anamnesis.alergiasDescripcion
        patient?.hasCriticalAllergies == true && patient.allergiesDescription.isNotBlank() ->
            patient.allergiesDescription
        hasAllergies -> "Alergias presentes"
        else -> "Sin alergias"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BiodontosWhite,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isConsentSigned) Esmeralda.copy(alpha = 0.12f) else Carmesi.copy(alpha = 0.12f),
                border = BorderStroke(
                    1.dp,
                    if (isConsentSigned) Esmeralda.copy(alpha = 0.4f) else Carmesi.copy(alpha = 0.4f)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isConsentSigned) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isConsentSigned) Esmeralda else Carmesi,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isConsentSigned) "Consentimiento OK" else "Firma Pendiente",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConsentSigned) Esmeralda else Carmesi,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (hasAllergies) Carmesi.copy(alpha = 0.12f) else BiodontosInputBg,
                border = BorderStroke(
                    1.dp,
                    if (hasAllergies) Carmesi.copy(alpha = 0.4f) else Color.Transparent
                ),
                modifier = Modifier.weight(1.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (hasAllergies) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (hasAllergies) Carmesi else BiodontosGreyText,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasAllergies) "Alergia: $allergyText" else "Sin alergias conocidas",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasAllergies) Carmesi else BiodontosGreyText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientDetailLoadingPreview() {
    DentalTheme {
        PatientDetailScreen(
            state = PatientDetailState(isLoading = true),
            onTabSelected = {},
            onNavigateBack = {},
            onNavigateToOdontogram = {},
            onNavigateToAdmission = {},
            onOpenNewEvolutionDialog = {},
            onDismissNewEvolutionDialog = {},
            onEvolutionDescriptionChange = {},
            onEvolutionToothChange = {},
            onEvolutionCostChange = {},
            onEvolutionPaymentChange = {},
            onSelectCatalogMaterial = {},
            onAddMaterial = {},
            onRemoveMaterial = {},
            onSaveEvolution = {},
            onOpenNewAppointmentDialog = {},
            onDismissNewAppointmentDialog = {},
            onAppointmentDateChange = {},
            onAppointmentTimeChange = {},
            onAppointmentReasonChange = {},
            onSaveAppointment = {},
            onClearMessages = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientDetailSuccessPreview() {
    DentalTheme {
        PatientDetailScreen(
            state = PatientDetailState(
                isLoading = false,
                patientId = "pat-1",
                patient = Patient(
                    id = "pat-1",
                    dniCedula = "10203040",
                    firstName = "María",
                    firstLastName = "Gómez",
                    birthDate = "1992-08-15",
                    eps = "Sura",
                    phone = "3001234567",
                    consentSigned = true
                )
            ),
            onTabSelected = {},
            onNavigateBack = {},
            onNavigateToOdontogram = {},
            onNavigateToAdmission = {},
            onOpenNewEvolutionDialog = {},
            onDismissNewEvolutionDialog = {},
            onEvolutionDescriptionChange = {},
            onEvolutionToothChange = {},
            onEvolutionCostChange = {},
            onEvolutionPaymentChange = {},
            onSelectCatalogMaterial = {},
            onAddMaterial = {},
            onRemoveMaterial = {},
            onSaveEvolution = {},
            onOpenNewAppointmentDialog = {},
            onDismissNewAppointmentDialog = {},
            onAppointmentDateChange = {},
            onAppointmentTimeChange = {},
            onAppointmentReasonChange = {},
            onSaveAppointment = {},
            onClearMessages = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientDetailErrorPreview() {
    DentalTheme {
        PatientDetailScreen(
            state = PatientDetailState(
                isLoading = false,
                patientId = "pat-1",
                errorMessage = "No se pudo cargar la historia clínica del paciente."
            ),
            onTabSelected = {},
            onNavigateBack = {},
            onNavigateToOdontogram = {},
            onNavigateToAdmission = {},
            onOpenNewEvolutionDialog = {},
            onDismissNewEvolutionDialog = {},
            onEvolutionDescriptionChange = {},
            onEvolutionToothChange = {},
            onEvolutionCostChange = {},
            onEvolutionPaymentChange = {},
            onSelectCatalogMaterial = {},
            onAddMaterial = {},
            onRemoveMaterial = {},
            onSaveEvolution = {},
            onOpenNewAppointmentDialog = {},
            onDismissNewAppointmentDialog = {},
            onAppointmentDateChange = {},
            onAppointmentTimeChange = {},
            onAppointmentReasonChange = {},
            onSaveAppointment = {},
            onClearMessages = {}
        )
    }
}
