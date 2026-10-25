package com.example.biodontos.presentation.patients.detail.history

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
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BiodontosWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Registrar Actividad en Historia Clínica",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Text(
                text = "Toda acción queda registrada con sello de tiempo y profesional tratante.",
                fontSize = 12.sp,
                color = BiodontosGreyText
            )
            Spacer(modifier = Modifier.height(4.dp))

            QuickActionOptionRow(
                title = "Nueva Evolución Clínica",
                subtitle = "Registrar procedimiento, pieza FDI y materiales INVIMA",
                icon = Icons.Default.Edit,
                onClick = onCreateEvolution
            )
            QuickActionOptionRow(
                title = "Agendar Cita",
                subtitle = "Programar control o sesión odontológica",
                icon = Icons.Default.DateRange,
                onClick = onCreateAppointment
            )
            QuickActionOptionRow(
                title = "Nuevo Presupuesto de Tratamiento",
                subtitle = "Crear plan de tratamiento valorizado en COP",
                icon = Icons.Default.Add,
                onClick = onCreateTreatmentBudget
            )
            QuickActionOptionRow(
                title = "Registrar Pago / Abono",
                subtitle = "Imputar abono a un presupuesto activo del paciente",
                icon = Icons.Default.Add,
                onClick = onCreatePayment
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
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
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
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Advertencia clínica",
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
                        text = "Conforme a la Resolución 1995 de 1999, el registro no se elimina físicamente: quedará marcado como ANULADO en la línea de tiempo con su justificación.",
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
                        label = { Text("Motivo de anulación (mín. 10 caracteres) *") },
                        placeholder = { Text("Ej: Error de digitación en monto o pieza dental...") },
                        modifier = Modifier.fillMaxWidth(),
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
                                text = "RESUMEN DE IMPACTO CLÍNICO Y CONTABLE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Carmesi
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Registro: $itemSummary",
                                fontSize = 12.sp,
                                color = BiodontosDark
                            )
                            Text(
                                text = "• Justificación: ${reason.trim()}",
                                fontSize = 12.sp,
                                color = BiodontosDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Esta acción es irreversible y quedará auditada a su nombre.",
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
                    colors = ButtonDefaults.buttonColors(containerColor = Carmesi)
                ) {
                    Text("Continuar a Confirmación")
                }
            } else {
                Button(
                    onClick = onConfirmVoid,
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Carmesi)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                    } else {
                        Text("Confirmar Anulación")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("Cancelar")
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
        title = { Text("Agregar Nota Aclaratoria", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Se creará una nueva evolución vinculada al registro del ${originalEvolution.date} sin alterar el texto original.",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
                Surface(
                    color = BiodontosInputBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Original: ${originalEvolution.description}",
                        fontSize = 12.sp,
                        color = BiodontosDark,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                OutlinedTextField(
                    value = clarificationText,
                    onValueChange = onTextChange,
                    label = { Text("Texto de la Nota Aclaratoria *") },
                    placeholder = { Text("Aclaración clínica sobre hallazgo o procedimiento...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = biodontosTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving && clarificationText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                } else {
                    Text("Guardar Aclaración")
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
        title = { Text("Actualizar Estado de Presupuesto", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${treatment.description} (${treatment.totalCost.formatCop()})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = BiodontosDark
                )
                Text(
                    text = "Estado actual: ${currentStatus.displayName}",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )

                if (allowedTransitions.isEmpty()) {
                    Text(
                        text = "Este presupuesto se encuentra en un estado final y no admite más cambios.",
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
                                onClick = { onSelectStatus(option) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = option.displayName, fontSize = 13.sp, color = BiodontosDark)
                        }
                    }

                    if (selectedTargetStatus == TreatmentStatus.CANCELADO) {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = onReasonChange,
                            label = { Text("Motivo de cancelación *") },
                            placeholder = { Text("Ej: Paciente desiste del plan protésico...") },
                            modifier = Modifier.fillMaxWidth(),
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
                    colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                    } else {
                        Text("Guardar Estado")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUpdating) {
                Text("Cerrar")
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
        title = { Text("Nuevo Presupuesto de Tratamiento", fontWeight = FontWeight.Bold) },
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
                    label = { Text("Descripción del Plan / Tratamiento *") },
                    placeholder = { Text("Ej: Rehabilitación corona circonio diente 21") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = biodontosTextFieldColors()
                )
                OutlinedTextField(
                    value = totalCost,
                    onValueChange = onTotalCostChange,
                    label = { Text("Costo Total Presupuestado (COP) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    Text("Crear Presupuesto")
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
        title = { Text("Registrar Pago / Abono", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeTreatments.isEmpty()) {
                    Text(
                        text = "No hay presupuestos activos para este paciente. Cree primero un presupuesto de tratamiento.",
                        fontSize = 12.sp,
                        color = Carmesi
                    )
                } else {
                    Text("Presupuesto a abonar:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    activeTreatments.forEach { tr ->
                        OutlinedButton(
                            onClick = { onSelectTreatment(tr.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${if (tr.id == selectedTreatmentId) "✓ " else ""}${tr.description} (${tr.totalCost.formatCop()})",
                                fontSize = 12.sp,
                                fontWeight = if (tr.id == selectedTreatmentId) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amount,
                        onValueChange = onAmountChange,
                        label = { Text("Monto del Abono (COP) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = biodontosTextFieldColors()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        methods.forEach { m ->
                            FilterChip(
                                selected = method == m,
                                onClick = { onMethodChange(m) },
                                label = { Text(m, fontSize = 11.sp) }
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
                    colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                    } else {
                        Text("Registrar Abono")
                    }
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
