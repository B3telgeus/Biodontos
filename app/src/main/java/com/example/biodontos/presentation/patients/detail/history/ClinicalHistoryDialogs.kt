package com.example.biodontos.presentation.patients.detail.history

import com.example.biodontos.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorBg
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.Carmesi
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import com.example.biodontos.domain.model.history.PatientActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCreateActivityBottomSheet(
    onCreateEvolution: () -> Unit,
    onCreateAppointment: () -> Unit,
    onCreateTreatmentBudget: () -> Unit,
    onCreatePayment: () -> Unit,
    onCreateConsent: () -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BiodontosWhite,
        modifier = Modifier.testTag(TestTags.Patients.QUICK_SHEET)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.ui_clinical_history_dialogs_429),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Text(
                text = stringResource(R.string.ui_clinical_history_dialogs_430),
                fontSize = 12.sp,
                color = BiodontosGreyText
            )
            Spacer(modifier = Modifier.height(4.dp))

            QuickActionOptionRow(
                title = "Nueva Evolución Clínica",
                subtitle = "Registrar procedimiento, pieza FDI y materiales INVIMA",
                icon = Icons.Default.Edit,
                onClick = onCreateEvolution,
                modifier = Modifier.testTag(TestTags.Patients.QUICK_SHEET_EVOLUTION)
            )
            QuickActionOptionRow(
                title = "Emitir Consentimiento Informado",
                subtitle = "Diligenciar y firmar formulario legal (Blanqueamiento, Operatoria o Rehabilitación)",
                icon = Icons.Default.Check,
                onClick = onCreateConsent,
                modifier = Modifier.testTag(TestTags.Patients.QUICK_SHEET_CONSENT)
            )
            QuickActionOptionRow(
                title = "Agendar Cita",
                subtitle = "Programar control o sesión odontológica",
                icon = Icons.Default.DateRange,
                onClick = onCreateAppointment,
                modifier = Modifier.testTag(TestTags.Patients.QUICK_SHEET_APPOINTMENT)
            )
            QuickActionOptionRow(
                title = "Nuevo Presupuesto de Tratamiento",
                subtitle = "Crear plan de tratamiento valorizado en COP",
                icon = Icons.Default.Add,
                onClick = onCreateTreatmentBudget,
                modifier = Modifier.testTag(TestTags.Patients.QUICK_SHEET_TREATMENT)
            )
            QuickActionOptionRow(
                title = "Registrar Pago / Abono",
                subtitle = "Imputar abono a un presupuesto activo del paciente",
                icon = Icons.Default.Add,
                onClick = onCreatePayment,
                modifier = Modifier.testTag(TestTags.Patients.QUICK_SHEET_PAYMENT)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickActionOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = BiodontosInputBg
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = BiodontosDark,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BiodontosDark)
                Text(text = subtitle, fontSize = 12.sp, color = BiodontosGreyText)
            }
        }
    }
}

@Composable
fun VoidActivityDialog(
    target: PatientActivity,
    reason: String,
    step: Int,
    isProcessing: Boolean,
    onReasonChange: (String) -> Unit,
    onAdvanceStep: () -> Unit,
    onConfirmVoid: () -> Unit,
    onDismiss: () -> Unit
) {
    val itemSummary = when (target) {
        is PatientActivity.EvolutionActivity ->
            "Evolución del ${target.evolution.date}: ${target.evolution.description}"
        is PatientActivity.PaymentActivity ->
            "Abono de ${target.payment.amount.formatCop()} (${target.treatmentDescription})"
        else -> "Registro clínico"
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        modifier = Modifier.testTag(TestTags.Patients.VOID_DIALOG),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = stringResource(R.string.ui_clinical_history_dialogs_431),
                    tint = Carmesi,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (step == 1) "Anular Registro (Paso 1 de 2)" else "Confirmar Anulación Definitiva (Paso 2 de 2)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (step == 1) {
                    Text(
                        text = stringResource(R.string.ui_clinical_history_dialogs_432),
                        fontSize = 12.sp,
                        color = BiodontosGreyText
                    )
                    Surface(
                        color = BiodontosInputBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = itemSummary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BiodontosDark,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = onReasonChange,
                        label = { Text(stringResource(R.string.ui_clinical_history_dialogs_433)) },
                        placeholder = { Text(stringResource(R.string.ui_clinical_history_dialogs_434)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.Patients.VOID_REASON_FIELD),
                        colors = biodontosTextFieldColors()
                    )
                } else {
                    Surface(
                        color = BiodontosErrorBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.ui_clinical_history_dialogs_435),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Carmesi
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.ui_clinical_history_dialogs_436, itemSummary),
                                fontSize = 12.sp,
                                color = BiodontosDark
                            )
                            Text(
                                text = stringResource(R.string.ui_clinical_history_dialogs_437, reason.trim()),
                                fontSize = 12.sp,
                                color = BiodontosDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.ui_clinical_history_dialogs_438),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Carmesi
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(
                    onClick = onAdvanceStep,
                    enabled = reason.trim().length >= 10,
                    colors = ButtonDefaults.buttonColors(containerColor = Carmesi),
                    modifier = Modifier.testTag(TestTags.Patients.VOID_CONFIRM_BUTTON)
                ) {
                    Text(stringResource(R.string.ui_clinical_history_dialogs_439))
                }
            } else {
                Button(
                    onClick = onConfirmVoid,
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Carmesi),
                    modifier = Modifier.testTag(TestTags.Patients.VOID_CONFIRM_BUTTON)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                    } else {
                        Text(stringResource(R.string.ui_clinical_history_dialogs_440))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing,
                modifier = Modifier.testTag(TestTags.Patients.VOID_DISMISS_BUTTON)
            ) {
                Text(stringResource(R.string.ui_doctor_appointments_screen_191))
            }
        }
    )
}

@Composable
fun ClarificationNoteDialog(
    originalEvolution: Evolution,
    clarificationText: String,
    isSaving: Boolean,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        modifier = Modifier.testTag(TestTags.Patients.CLARIFICATION_DIALOG),
        title = { Text(stringResource(R.string.ui_clinical_history_dialogs_441), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.ui_clinical_history_dialogs_442, originalEvolution.date),
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
                Surface(
                    color = BiodontosInputBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.ui_clinical_history_dialogs_443, originalEvolution.description),
                        fontSize = 12.sp,
                        color = BiodontosDark,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                OutlinedTextField(
                    value = clarificationText,
                    onValueChange = onTextChange,
                    label = { Text(stringResource(R.string.ui_clinical_history_dialogs_444)) },
                    placeholder = { Text(stringResource(R.string.ui_clinical_history_dialogs_445)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.Patients.CLARIFICATION_TEXT_FIELD),
                    colors = biodontosTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving && clarificationText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                modifier = Modifier.testTag(TestTags.Patients.CLARIFICATION_SAVE_BUTTON)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                } else {
                    Text(stringResource(R.string.ui_clinical_history_dialogs_446))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.ui_doctor_appointments_screen_191))
            }
        }
    )
}

@Composable
fun TreatmentStatusUpdateDialog(
    treatment: Treatment,
    selectedTargetStatus: TreatmentStatus?,
    reason: String,
    isUpdating: Boolean,
    onSelectStatus: (TreatmentStatus) -> Unit,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentStatus = TreatmentStatus.fromString(treatment.status)
    val allowedTransitions = currentStatus.availableTransitions()

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        modifier = Modifier.testTag(TestTags.Patients.STATUS_DIALOG),
        title = { Text(stringResource(R.string.ui_clinical_history_dialogs_447), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.ui_doctor_documents_screen_229, treatment.description, treatment.totalCost.formatCop()),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = BiodontosDark
                )
                Text(
                    text = stringResource(R.string.ui_clinical_history_dialogs_448, currentStatus.displayName),
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )

                if (allowedTransitions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ui_clinical_history_dialogs_449),
                        fontSize = 12.sp,
                        color = Carmesi
                    )
                } else {
                    allowedTransitions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectStatus(option) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTargetStatus == option,
                                onClick = { onSelectStatus(option) },
                                modifier = Modifier.testTag(TestTags.Patients.statusOption(option.name))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = option.displayName, fontSize = 13.sp, color = BiodontosDark)
                        }
                    }

                    if (selectedTargetStatus == TreatmentStatus.CANCELADO) {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = onReasonChange,
                            label = { Text(stringResource(R.string.ui_clinical_history_dialogs_450)) },
                            placeholder = { Text(stringResource(R.string.ui_clinical_history_dialogs_451)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.Patients.STATUS_REASON_FIELD),
                            colors = biodontosTextFieldColors()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (allowedTransitions.isNotEmpty()) {
                Button(
                    onClick = onConfirm,
                    enabled = !isUpdating && selectedTargetStatus != null,
                    colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                    modifier = Modifier.testTag(TestTags.Patients.STATUS_CONFIRM_BUTTON)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                    } else {
                        Text(stringResource(R.string.ui_clinical_history_dialogs_452))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUpdating) {
                Text(stringResource(R.string.ui_operative_consent_dialog_89))
            }
        }
    )
}

@Composable
fun NewTreatmentBudgetDialog(
    description: String,
    totalCost: String,
    isSaving: Boolean,
    onDescriptionChange: (String) -> Unit,
    onTotalCostChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        modifier = Modifier.testTag(TestTags.Patients.NEW_TREATMENT_DIALOG),
        title = { Text(stringResource(R.string.ui_clinical_history_dialogs_453), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.ui_clinical_history_dialogs_454)) },
                    placeholder = { Text(stringResource(R.string.ui_clinical_history_dialogs_455)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.Patients.NEW_TREATMENT_DESC_FIELD),
                    colors = biodontosTextFieldColors()
                )
                OutlinedTextField(
                    value = totalCost,
                    onValueChange = onTotalCostChange,
                    label = { Text(stringResource(R.string.ui_clinical_history_dialogs_456)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.Patients.NEW_TREATMENT_COST_FIELD),
                    colors = biodontosTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                modifier = Modifier.testTag(TestTags.Patients.NEW_TREATMENT_SAVE_BUTTON)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                } else {
                    Text(stringResource(R.string.ui_clinical_history_dialogs_457))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.ui_doctor_appointments_screen_191))
            }
        }
    )
}

@Composable
fun NewPaymentDialog(
    treatments: List<Treatment>,
    selectedTreatmentId: String,
    amount: String,
    method: String,
    isSaving: Boolean,
    onSelectTreatment: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onMethodChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeTreatments = treatments.filter {
        TreatmentStatus.fromString(it.status) != TreatmentStatus.CANCELADO
    }
    val methods = listOf("EFECTIVO", "TARJETA", "TRANSFERENCIA")

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        modifier = Modifier.testTag(TestTags.Patients.NEW_PAYMENT_DIALOG),
        title = { Text(stringResource(R.string.ui_clinical_history_dialogs_458), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeTreatments.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ui_clinical_history_dialogs_459),
                        fontSize = 12.sp,
                        color = Carmesi
                    )
                } else {
                    Text(stringResource(R.string.ui_clinical_history_dialogs_460), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    activeTreatments.forEach { tr ->
                        OutlinedButton(
                            onClick = { onSelectTreatment(tr.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ui_clinical_history_dialogs_461, if (tr.id == selectedTreatmentId) "✓ " else "", tr.description, tr.totalCost.formatCop()),
                                fontSize = 12.sp,
                                fontWeight = if (tr.id == selectedTreatmentId) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amount,
                        onValueChange = onAmountChange,
                        label = { Text(stringResource(R.string.ui_clinical_history_dialogs_462)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.Patients.NEW_PAYMENT_AMOUNT_FIELD),
                        colors = biodontosTextFieldColors()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        methods.forEach { m ->
                            FilterChip(
                                selected = method == m,
                                onClick = { onMethodChange(m) },
                                label = { Text(m, fontSize = 11.sp) },
                                modifier = Modifier.testTag(TestTags.Patients.paymentMethodChip(m))
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (activeTreatments.isNotEmpty()) {
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                    modifier = Modifier.testTag(TestTags.Patients.NEW_PAYMENT_SAVE_BUTTON)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                    } else {
                        Text(stringResource(R.string.ui_clinical_history_dialogs_463))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.ui_doctor_appointments_screen_191))
            }
        }
    )
}
