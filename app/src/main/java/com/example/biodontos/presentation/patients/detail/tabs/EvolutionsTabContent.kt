package com.example.biodontos.presentation.patients.detail.tabs

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.R
import com.example.biodontos.core.designsystem.Ambar
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.Carmesi
import com.example.biodontos.core.designsystem.Esmeralda
import com.example.biodontos.core.designsystem.SlateLight
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.core.designsystem.Zafiro
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.domain.model.Cie10Code
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.ConsentType
import com.example.biodontos.domain.model.CupsCode
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.usecase.clinical.ConsentRequirementStatus
import com.example.biodontos.presentation.components.Cie10CodeDropdown
import com.example.biodontos.presentation.components.CupsCodeDropdown

@Composable
fun EvolutionItemCard(evolution: Evolution, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.Patients.evolutionItem(evolution.id)),
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
                    color = BiodontosDark
                )
                if (evolution.isClarification) {
                    Surface(
                        color = Ambar.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Aclarada",
                            color = Ambar,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (evolution.cie10Code != null) {
                Text(
                    text = "Diagnóstico: ${evolution.cie10Code} - ${evolution.cie10Description.orEmpty()}",
                    fontSize = 12.sp,
                    color = Zafiro,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (evolution.cupsCode != null) {
                Text(
                    text = "Procedimiento CUPS: ${evolution.cupsCode} - ${evolution.cupsDescription.orEmpty()}",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = evolution.description,
                fontSize = 13.sp,
                color = BiodontosDark
            )
            if (evolution.toothTreated != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Diente tratado: ${evolution.toothTreated}",
                    fontSize = 11.sp,
                    color = BiodontosGreyText
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEvolutionDialog(
    selectedCie10: Cie10Code?,
    selectedCups: CupsCode?,
    cie10Error: String?,
    cupsError: String?,
    description: String,
    tooth: Int?,
    cost: String,
    payment: String,
    materials: List<EvolutionMaterialUsed>,
    catalog: List<ClinicalMaterial>,
    selectedMaterial: ClinicalMaterial?,
    isSaving: Boolean,
    consentStatus: ConsentRequirementStatus = ConsentRequirementStatus.NotRequired,
    onOpenConsentForm: (ConsentType, Int?) -> Unit = { _, _ -> },
    onCie10Change: (Cie10Code?) -> Unit,
    onCupsChange: (CupsCode?) -> Unit,
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
    var isDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.Patients.DETAIL_NEW_EVOLUTION_DIALOG),
        title = { Text(stringResource(R.string.ui_patient_detail_screen_535), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    when (consentStatus) {
                        is ConsentRequirementStatus.Missing -> {
                            Surface(
                                color = Ambar.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.Patients.EVOLUTION_CONSENT_BANNER)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Ambar,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Consentimiento requerido (${consentStatus.requiredType.label})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Ambar
                                        )
                                    }
                                    Text(
                                        text = "Este procedimiento requiere consentimiento informado firmado antes de registrar la evolución.",
                                        fontSize = 11.sp,
                                        color = BiodontosDark
                                    )
                                    Button(
                                        onClick = { onOpenConsentForm(consentStatus.requiredType, tooth) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Ambar),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag(TestTags.Patients.EVOLUTION_CONSENT_ACTION_BUTTON)
                                    ) {
                                        Text("Diligenciar y Firmar Consentimiento", fontSize = 11.sp, color = BiodontosWhite)
                                    }
                                }
                            }
                        }
                        is ConsentRequirementStatus.Satisfied -> {
                            Surface(
                                color = Esmeralda.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.Patients.EVOLUTION_CONSENT_BANNER)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Esmeralda,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Consentimiento firmado y vigente (${consentStatus.type.label})",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Esmeralda
                                    )
                                }
                            }
                        }
                        ConsentRequirementStatus.NotRequired -> Unit
                    }
                }
                item {
                    Cie10CodeDropdown(
                        selectedCode = selectedCie10,
                        onSelect = onCie10Change,
                        isError = cie10Error != null,
                        supportingText = cie10Error,
                        testTag = TestTags.Patients.DETAIL_NEW_EVOLUTION_CIE10_FIELD,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    CupsCodeDropdown(
                        selectedCode = selectedCups,
                        onSelect = onCupsChange,
                        isError = cupsError != null,
                        supportingText = cupsError,
                        testTag = TestTags.Patients.DETAIL_NEW_EVOLUTION_CUPS_FIELD,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        label = { Text(stringResource(R.string.ui_patient_detail_screen_536)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.Patients.DETAIL_NEW_EVOLUTION_DESC_FIELD),
                        colors = biodontosTextFieldColors()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tooth?.toString().orEmpty(),
                            onValueChange = { onToothChange(it.toIntOrNull()) },
                            label = { Text(stringResource(R.string.ui_patient_detail_screen_537)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag(TestTags.Patients.DETAIL_NEW_EVOLUTION_TOOTH_FIELD),
                            colors = biodontosTextFieldColors()
                        )
                        OutlinedTextField(
                            value = cost,
                            onValueChange = onCostChange,
                            label = { Text(stringResource(R.string.ui_patient_detail_screen_538)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag(TestTags.Patients.DETAIL_NEW_EVOLUTION_COST_FIELD),
                            colors = biodontosTextFieldColors()
                        )
                        OutlinedTextField(
                            value = payment,
                            onValueChange = onPaymentChange,
                            label = { Text(stringResource(R.string.ui_patient_detail_screen_539)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag(TestTags.Patients.DETAIL_NEW_EVOLUTION_PAYMENT_FIELD),
                            colors = biodontosTextFieldColors()
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ui_patient_detail_screen_540),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BiodontosDark
                        )
                        Text(
                            text = stringResource(R.string.ui_patient_detail_screen_541),
                            fontSize = 11.sp,
                            color = BiodontosGreyText
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedMaterial?.name ?: stringResource(R.string.ui_patient_detail_screen_542),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                colors = biodontosTextFieldColors(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                catalog.forEach { mat ->
                                    val expired = mat.isExpired()
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = mat.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text(text = "Lote: ${mat.batchLotNumber} · Vence: ${mat.expirationDate}", fontSize = 10.sp, color = BiodontosGreyText)
                                                }
                                                if (expired) {
                                                    Text(text = "VENCIDO", fontSize = 9.sp, color = Carmesi, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                        onClick = {
                                            if (!expired) {
                                                onSelectCatalogMaterial(mat)
                                                isDropdownExpanded = false
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        selectedMaterial?.let { selMat ->
                            val isExpired = selMat.isExpired()
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateLight),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = selMat.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BiodontosDark)
                                    Text(text = "INVIMA: ${selMat.invimaRegistration}", fontSize = 11.sp, color = BiodontosGreyText)
                                    Text(text = "Lote: ${selMat.batchLotNumber} · Vence: ${selMat.expirationDate}", fontSize = 11.sp, color = if (isExpired) Carmesi else BiodontosGreyText)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = materialQty,
                                            onValueChange = { input ->
                                                if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                                    materialQty = input
                                                }
                                            },
                                            label = { Text("Cantidad") },
                                            enabled = !isExpired,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            colors = biodontosTextFieldColors()
                                        )

                                        val qtyDouble = materialQty.toDoubleOrNull() ?: 0.0
                                        val canAdd = !isExpired && qtyDouble > 0.0
                                        Button(
                                            onClick = {
                                                if (canAdd) {
                                                    onAddMaterial(qtyDouble)
                                                    materialQty = "1.0"
                                                }
                                            },
                                            enabled = canAdd,
                                            colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Añadir")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (materials.isNotEmpty()) {
                    item {
                        Text(
                            text = "Materiales asignados (${materials.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Zafiro
                        )
                    }
                    items(materials, key = { it.materialId }) { mat ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = BiodontosInputBg.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${mat.quantityUsed}x ${mat.materialName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = BiodontosDark
                                    )
                                    Text(
                                        text = "INVIMA: ${mat.invimaSnapshot} | Lote: ${mat.lotNumberSnapshot} | Vence: ${mat.expirationDateSnapshot}",
                                        fontSize = 10.sp,
                                        color = BiodontosGreyText
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveMaterial(mat.materialId) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar material",
                                        tint = Carmesi,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                modifier = Modifier.testTag(TestTags.Patients.DETAIL_NEW_EVOLUTION_SAVE_BUTTON)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                } else {
                    Text(stringResource(R.string.ui_patient_detail_screen_556))
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
