package com.example.biodontos.presentation.patients.detail

import androidx.annotation.VisibleForTesting
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorBg
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosLightGrey
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.Carmesi
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.core.designsystem.Esmeralda
import com.example.biodontos.core.designsystem.Zafiro
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.core.designsystem.components.DateBadge
import com.example.biodontos.domain.model.Anamnesis
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import com.example.biodontos.domain.model.history.ActivityType
import com.example.biodontos.domain.model.history.PatientActivity
import com.example.biodontos.presentation.patients.detail.history.ClarificationNoteDialog
import com.example.biodontos.presentation.patients.detail.history.NewPaymentDialog
import com.example.biodontos.presentation.patients.detail.history.NewTreatmentBudgetDialog
import com.example.biodontos.presentation.patients.detail.history.PatientHistoryTab
import com.example.biodontos.presentation.patients.detail.history.QuickCreateActivityBottomSheet
import com.example.biodontos.presentation.patients.detail.history.TreatmentStatusUpdateDialog
import com.example.biodontos.presentation.patients.detail.history.VoidActivityDialog
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@VisibleForTesting
@Composable
fun PatientDetailScreen(
    state: PatientDetailState,
    onTabSelected: (PatientDetailTab) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToOdontogram: () -> Unit,
    onNavigateToAdmission: () -> Unit,
    onOpenNewEvolutionDialog: () -> Unit,
    onDismissNewEvolutionDialog: () -> Unit,
    onEvolutionDescriptionChange: (String) -> Unit,
    onEvolutionToothChange: (Int?) -> Unit,
    onEvolutionCostChange: (String) -> Unit,
    onEvolutionPaymentChange: (String) -> Unit,
    onSelectCatalogMaterial: (ClinicalMaterial) -> Unit,
    onAddMaterial: (Double) -> Unit,
    onRemoveMaterial: (String) -> Unit,
    onSaveEvolution: () -> Unit,
    onOpenNewAppointmentDialog: () -> Unit,
    onDismissNewAppointmentDialog: () -> Unit,
    onAppointmentDateChange: (String) -> Unit,
    onAppointmentTimeChange: (String) -> Unit,
    onAppointmentReasonChange: (String) -> Unit,
    onSaveAppointment: () -> Unit,
    onClearMessages: () -> Unit,
    // Callbacks del Módulo de Historial Clínico y Trazabilidad (con valores por defecto compatibles)
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
    onRefreshTimeline: () -> Unit = {}
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
                            text = "DNI: ${patient?.dniCedula ?: state.patientId} · EPS: ${patient?.eps ?: "Particular"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BiodontosGreyText
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = BiodontosDark
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToAdmission) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Admisión", fontWeight = FontWeight.Bold)
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
                        contentColor = BiodontosWhite
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Registrar Actividad")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrar Actividad", fontWeight = FontWeight.Bold)
                    }
                }
                PatientDetailTab.EVOLUTIONS -> {
                    ExtendedFloatingActionButton(
                        onClick = onOpenNewEvolutionDialog,
                        containerColor = BiodontosDark,
                        contentColor = BiodontosWhite
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nueva Evolución")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nueva Evolución", fontWeight = FontWeight.Bold)
                    }
                }
                PatientDetailTab.TREATMENTS -> {
                    ExtendedFloatingActionButton(
                        onClick = onOpenNewAppointmentDialog,
                        containerColor = BiodontosDark,
                        contentColor = BiodontosWhite
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Agendar Cita")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agendar Cita", fontWeight = FontWeight.Bold)
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
            // Tabs de Navegación del Hub Clínico
            ScrollableTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = BiodontosWhite,
                contentColor = BiodontosDark,
                edgePadding = 12.dp
            ) {
                PatientDetailTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
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
                    CircularProgressIndicator(color = BiodontosDark)
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (state.selectedTab) {
                        PatientDetailTab.HISTORY -> {}
                        PatientDetailTab.SUMMARY -> {
                            item { PatientDemographicsCard(patient = patient) }
                            item { AnamnesisSummaryCard(anamnesis = state.anamnesis) }
                            item { ConsentSummaryCard(consent = state.consent) }
                        }
                        PatientDetailTab.ODONTOGRAM -> {
                            item {
                                OdontogramHubCard(
                                    dentitionType = state.clinicalRecord?.dentitionType?.name ?: "ADULT",
                                    mainDiagnosis = state.clinicalRecord?.mainDiagnosis ?: "Sin diagnóstico registrado",
                                    onOpenOdontogram = onNavigateToOdontogram,
                                    onOpenAdmission = onNavigateToAdmission
                                )
                            }
                        }
                        PatientDetailTab.EVOLUTIONS -> {
                            if (state.evolutions.isEmpty()) {
                                item {
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
                                                text = "No hay notas de evolución registradas aún.",
                                                color = BiodontosGreyText,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = onOpenNewEvolutionDialog,
                                                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                                            ) {
                                                Text("Registrar Primera Evolución")
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
                            item {
                                TreatmentsBalanceHeaderCard(
                                    totalBilled = state.totalBilled,
                                    totalPaid = state.totalPaid,
                                    balance = state.pendingBalance
                                )
                            }
                            item {
                                Text(
                                    text = "Tratamientos Registrados",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BiodontosDark
                                )
                            }
                            if (state.treatments.isEmpty()) {
                                item {
                                    Text(
                                        text = "No hay tratamientos activos.",
                                        color = BiodontosGreyText,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                items(state.treatments, key = { it.id }) { treat ->
                                    TreatmentItemCard(treatment = treat)
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Citas del Paciente",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BiodontosDark
                                )
                            }
                            if (state.appointments.isEmpty()) {
                                item {
                                    Text(
                                        text = "No hay citas programadas.",
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

    // BottomSheet: Creación Rápida desde el Historial Clínico
    if (state.historyState.showQuickActionSheet) {
        QuickCreateActivityBottomSheet(
            onCreateEvolution = onOpenNewEvolutionDialog,
            onCreateAppointment = onOpenNewAppointmentDialog,
            onCreateTreatmentBudget = onOpenNewTreatmentDialog,
            onCreatePayment = { onOpenNewPaymentDialog(null) },
            onDismiss = onDismissQuickActionSheet
        )
    }

    // Diálogo: Anulación Lógica en 2 Pasos (Resolución 1995/1999)
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

    // Diálogo: Nota Aclaratoria Vinculada
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

    // Diálogo: Actualizar Estado de Presupuesto de Tratamiento
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

    // Diálogo: Nuevo Presupuesto de Tratamiento
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

    // Diálogo: Registrar Pago / Abono
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

    // Diálogo: Nueva Evolución Clínica con Trazabilidad INVIMA
    if (state.showNewEvolutionDialog) {
        NewEvolutionDialog(
            description = state.newEvolutionDescription,
            tooth = state.newEvolutionToothTreated,
            cost = state.newEvolutionCost,
            payment = state.newEvolutionPayment,
            materials = state.newEvolutionMaterials,
            catalog = state.materialsCatalog,
            selectedMaterial = state.selectedCatalogMaterial,
            isSaving = state.isSavingEvolution,
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

    // Diálogo: Agendar Cita
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
private fun PatientDemographicsCard(patient: Patient?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Datos de Identificación y Contacto",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Teléfono", fontSize = 11.sp, color = BiodontosGreyText)
                    Text(patient?.phone?.ifBlank { "No registrado" } ?: "—", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column {
                    Text("Grupo / RH", fontSize = 11.sp, color = BiodontosGreyText)
                    Text("${patient?.bloodGroup ?: "O"}${patient?.rhFactor ?: "+"}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column {
                    Text("Estado Civil", fontSize = 11.sp, color = BiodontosGreyText)
                    Text(patient?.maritalStatus ?: "Soltero(a)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (!patient?.email.isNullOrBlank()) {
                Text("Correo Electrónico", fontSize = 11.sp, color = BiodontosGreyText)
                Text(patient?.email ?: "", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (!patient?.address.isNullOrBlank()) {
                Text("Dirección de Residencia", fontSize = 11.sp, color = BiodontosGreyText)
                Text(patient?.address ?: "", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (patient?.hasCriticalAllergies == true) {
                Surface(
                    color = BiodontosErrorBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠ ALERGIAS CRÍTICAS: ${patient.allergiesDescription}",
                            color = Carmesi,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnamnesisSummaryCard(anamnesis: Anamnesis?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Anamnesis (Cuestionario Médico de 20 Preguntas)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (anamnesis == null) {
                Text("Anamnesis pendiente de registro.", color = BiodontosGreyText, fontSize = 13.sp)
            } else {
                val positives = mutableListOf<String>()
                if (anamnesis.tratamientoMedicoActual) positives.add("Tratamiento médico actual")
                if (anamnesis.tomaMedicamentos) positives.add("Toma medicamentos")
                if (anamnesis.alergias) positives.add("Alergias")
                if (anamnesis.cardiopatias) positives.add("Cardiopatías")
                if (anamnesis.alteracionPresionArterial) positives.add("Presión arterial")
                if (anamnesis.diabetes) positives.add("Diabetes")
                if (anamnesis.embarazo) positives.add("Embarazo")
                if (anamnesis.epilepsia) positives.add("Epilepsia")
                if (anamnesis.enfermedadesRenales) positives.add("Enf. Renales")
                if (anamnesis.inmunosupresion) positives.add("Inmunosupresión")

                if (positives.isEmpty()) {
                    Text("✓ Sin hallazgos patológicos reportados en el cuestionario de salud.", color = Esmeralda, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Condiciones afirmativas registradas:", color = Carmesi, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(positives.joinToString(" · "), fontSize = 12.sp, color = BiodontosDark)
                }

                if (!anamnesis.observaciones.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Observaciones: ${anamnesis.observaciones}", fontSize = 12.sp, color = BiodontosGreyText)
                }
            }
        }
    }
}

@Composable
private fun ConsentSummaryCard(consent: InformedConsent?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Consentimiento Informado", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = BiodontosDark)
                Spacer(modifier = Modifier.height(4.dp))
                val isSigned = consent?.status == "SIGNED" || !consent?.patientSignatureUrl.isNullOrBlank()
                Text(
                    text = if (isSigned) "Firmado legalmente por el paciente" else "Pendiente de firma digital",
                    fontSize = 12.sp,
                    color = if (isSigned) Esmeralda else Carmesi,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (consent?.status == "SIGNED") Esmeralda.copy(alpha = 0.15f) else BiodontosInputBg)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (consent?.status == "SIGNED") Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (consent?.status == "SIGNED") Esmeralda else BiodontosGreyText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun OdontogramHubCard(
    dentitionType: String,
    mainDiagnosis: String,
    onOpenOdontogram: () -> Unit,
    onOpenAdmission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Odontograma y Periodontometría",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tipo de dentición: $dentitionType · Diagnóstico: $mainDiagnosis",
                fontSize = 12.sp,
                color = BiodontosGreyText
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenOdontogram,
                    colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Abrir Odontograma FDI")
                }
                OutlinedButton(
                    onClick = onOpenAdmission,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Periodontograma / Admisión")
                }
            }
        }
    }
}

@Composable
private fun EvolutionItemCard(evolution: Evolution) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = evolution.date,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BiodontosDark
                )
                if (evolution.toothTreated != null) {
                    Surface(
                        color = BiodontosDark,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Pieza FDI: ${evolution.toothTreated}",
                            color = BiodontosWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = evolution.description,
                fontSize = 14.sp,
                color = BiodontosDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Costo: $${evolution.cost.toInt()} · Abono: $${evolution.paymentAmount.toInt()}",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
                Text(
                    text = "Saldo: $${evolution.balance.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (evolution.balance > 0) Carmesi else Esmeralda
                )
            }
            if (evolution.materialsUsed.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Materiales INVIMA trazados:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Zafiro
                )
                evolution.materialsUsed.forEach { mat ->
                    Text(
                        text = "• ${mat.materialName.ifBlank { "Material" }} (Lote: ${mat.lotNumberSnapshot} - Reg: ${mat.invimaSnapshot})",
                        fontSize = 11.sp,
                        color = BiodontosGreyText
                    )
                }
            }
        }
    }
}

@Composable
private fun TreatmentsBalanceHeaderCard(
    totalBilled: Double,
    totalPaid: Double,
    balance: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Balance de Tratamientos", color = BiodontosWhite.copy(alpha = 0.8f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Saldo Pendiente: $${balance.toInt()}",
                color = BiodontosWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Presupuestado: $${totalBilled.toInt()}", color = BiodontosWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                Text("Total Abonado: $${totalPaid.toInt()}", color = Esmeralda, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TreatmentItemCard(treatment: Treatment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(treatment.description, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BiodontosDark)
                Text("Estado: ${treatment.status}", fontSize = 12.sp, color = BiodontosGreyText)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Costo: $${treatment.totalCost.pesos}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BiodontosDark)
            }
        }
    }
}

@Composable
private fun PatientAppointmentItemCard(appointment: Appointment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateBadge(dateTime = appointment.startTime)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(appointment.reason ?: "Cita Odontológica", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BiodontosDark)
                Text(
                    "Estado: ${appointment.status.name} · ${if (appointment.isConfirmed) "Confirmada" else "Sin confirmar"}",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewEvolutionDialog(
    description: String,
    tooth: Int?,
    cost: String,
    payment: String,
    materials: List<EvolutionMaterialUsed>,
    catalog: List<ClinicalMaterial>,
    selectedMaterial: ClinicalMaterial?,
    isSaving: Boolean,
    onDescriptionChange: (String) -> Unit,
    onToothChange: (Int?) -> Unit,
    onCostChange: (String) -> Unit,
    onPaymentChange: (String) -> Unit,
    onSelectCatalogMaterial: (ClinicalMaterial) -> Unit,
    onAddMaterial: (Double) -> Unit,
    onRemoveMaterial: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var materialQty by remember { mutableStateOf("1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Nota de Evolución Clínica", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        label = { Text("Descripción del Procedimiento *") },
                        placeholder = { Text("Ej: Obturación en resina compuesta clase II...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = biodontosTextFieldColors()
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tooth?.toString() ?: "",
                            onValueChange = { onToothChange(it.toIntOrNull()) },
                            label = { Text("Pieza FDI (opc)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = biodontosTextFieldColors()
                        )
                        OutlinedTextField(
                            value = cost,
                            onValueChange = onCostChange,
                            label = { Text("Costo ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = biodontosTextFieldColors()
                        )
                        OutlinedTextField(
                            value = payment,
                            onValueChange = onPaymentChange,
                            label = { Text("Abono ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = biodontosTextFieldColors()
                        )
                    }
                }
                item {
                    Text("Materiales INVIMA Usados:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (catalog.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = selectedMaterial?.name ?: "Seleccione material",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { onAddMaterial(materialQty.toDoubleOrNull() ?: 1.0) },
                                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                            ) {
                                Text("Agregar")
                            }
                        }
                    }
                }
                items(materials) { mat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("• ${mat.materialName} (Lote: ${mat.lotNumberSnapshot})", fontSize = 11.sp, color = BiodontosGreyText)
                        IconButton(onClick = { onRemoveMaterial(mat.materialId) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Carmesi, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                } else {
                    Text("Guardar Evolución")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun NewAppointmentDialog(
    date: String,
    time: String,
    reason: String,
    isSaving: Boolean,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agendar Próxima Cita", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = onDateChange,
                    label = { Text("Fecha (YYYY-MM-DD) *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = biodontosTextFieldColors()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = onTimeChange,
                    label = { Text("Hora (HH:mm) *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = biodontosTextFieldColors()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Motivo de Consulta") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = biodontosTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                } else {
                    Text("Agendar Cita")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        }
    )
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
