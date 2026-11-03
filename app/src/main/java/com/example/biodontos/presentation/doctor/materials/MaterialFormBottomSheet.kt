package com.example.biodontos.presentation.doctor.materials

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.Ambar
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorBg
import com.example.biodontos.core.designsystem.BiodontosErrorRed
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.core.designsystem.insets.BiodontosBottomActionBar
import com.example.biodontos.core.designsystem.insets.BiodontosFormColumn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialFormBottomSheet(
    formState: MaterialFormState,
    onNameChange: (String) -> Unit,
    onInvimaChange: (String) -> Unit,
    onLotChange: (String) -> Unit,
    onManufacturingDateChange: (LocalDate) -> Unit,
    onExpirationDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    var showMfgDatePicker by remember { mutableStateOf(false) }
    var showExpDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BiodontosWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (formState.isEditing) "Editar Insumo Clínico" else "Nuevo Insumo Clínico",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                    Text(
                        text = "Trazabilidad INVIMA y control de lotes",
                        fontSize = 12.sp,
                        color = BiodontosGreyText
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = BiodontosDark
                    )
                }
            }

            HorizontalDivider(color = BiodontosInputBg)

            // Form Fields
            BiodontosFormColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nombre Comercial
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = onNameChange,
                    label = { Text("Nombre Comercial *") },
                    placeholder = { Text("Ej: Resina Filtek Z350 XT") },
                    isError = formState.nameError != null,
                    supportingText = formState.nameError?.let { { Text(it, color = BiodontosErrorRed) } },
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Registro INVIMA
                OutlinedTextField(
                    value = formState.invimaRegistration,
                    onValueChange = onInvimaChange,
                    label = { Text("Registro Sanitario INVIMA *") },
                    placeholder = { Text("Ej: INVIMA 2020DM-0012345") },
                    isError = formState.invimaError != null,
                    supportingText = formState.invimaError?.let { { Text(it, color = BiodontosErrorRed) } },
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Número de Lote
                OutlinedTextField(
                    value = formState.batchLotNumber,
                    onValueChange = onLotChange,
                    label = { Text("Número de Lote *") },
                    placeholder = { Text("Ej: LOT-2024-X89") },
                    isError = formState.lotError != null,
                    supportingText = formState.lotError?.let { { Text(it, color = BiodontosErrorRed) } },
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Fechas: Fabricación y Vencimiento
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Fecha de Elaboración
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = formState.manufacturingDate.format(dateFormatter),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Elaboración *") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Seleccionar fecha de elaboración",
                                    tint = BiodontosDark
                                )
                            },
                            colors = biodontosTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMfgDatePicker = true },
                            enabled = false
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showMfgDatePicker = true }
                        )
                    }

                    // Fecha de Vencimiento
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = formState.expirationDate.format(dateFormatter),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vencimiento *") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Seleccionar fecha de vencimiento",
                                    tint = BiodontosDark
                                )
                            },
                            colors = biodontosTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showExpDatePicker = true },
                            enabled = false
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showExpDatePicker = true }
                        )
                    }
                }

                // Error general de fechas si existe
                if (formState.dateError != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BiodontosErrorBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error de fechas",
                            tint = BiodontosErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formState.dateError,
                            fontSize = 12.sp,
                            color = BiodontosErrorRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Regulatory clarification notice
                if (formState.isEditing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Aviso regulatorio",
                            tint = Ambar,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "La edición de este catálogo no altera las notas de evoluciones clínicas firmadas anteriormente.",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E),
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Bottom Action Bar with Insets
            BiodontosBottomActionBar(
                containerColor = BiodontosWhite,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !formState.isSaving
                    ) {
                        Text("Cancelar", color = BiodontosDark)
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                        enabled = !formState.isSaving
                    ) {
                        if (formState.isSaving) {
                            CircularProgressIndicator(
                                color = BiodontosWhite,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (formState.isEditing) "Actualizar Insumo" else "Guardar Insumo",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Material 3 DatePickerDialog para Fecha de Elaboración
    if (showMfgDatePicker) {
        val initialEpochMillis = remember(formState.manufacturingDate) {
            formState.manufacturingDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochMillis
        )

        DatePickerDialog(
            onDismissRequest = { showMfgDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            onManufacturingDateChange(selectedDate)
                        }
                        showMfgDatePicker = false
                    }
                ) {
                    Text("Seleccionar", color = BiodontosDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMfgDatePicker = false }) {
                    Text("Cancelar", color = BiodontosGreyText)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = BiodontosWhite)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = BiodontosDark,
                    headlineContentColor = BiodontosDark
                )
            )
        }
    }

    // Material 3 DatePickerDialog para Fecha de Vencimiento
    if (showExpDatePicker) {
        val initialEpochMillis = remember(formState.expirationDate) {
            formState.expirationDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochMillis
        )

        DatePickerDialog(
            onDismissRequest = { showExpDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            onExpirationDateChange(selectedDate)
                        }
                        showExpDatePicker = false
                    }
                ) {
                    Text("Seleccionar", color = BiodontosDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpDatePicker = false }) {
                    Text("Cancelar", color = BiodontosGreyText)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = BiodontosWhite)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = BiodontosDark,
                    headlineContentColor = BiodontosDark
                )
            )
        }
    }
}
