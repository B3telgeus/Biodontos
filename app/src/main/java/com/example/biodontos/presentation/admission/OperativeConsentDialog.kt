package com.example.biodontos.presentation.admission

import com.example.biodontos.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.biodontos.core.designsystem.*
import com.example.biodontos.domain.model.InformedConsentProcedure
import com.example.biodontos.presentation.components.SignaturePad
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperativeConsentDialog(
    state: ClinicalAdmissionState,
    onDismiss: () -> Unit,
    onUpdateTreatment: (String) -> Unit,
    onUpdateSigner: (signerType: String, repName: String, repId: String) -> Unit,
    onAddProcedure: (toothNumber: Int, date: String) -> Unit,
    onRemoveProcedure: (Int) -> Unit,
    onConfirmSignatures: (patientSig: String, doctorSig: String) -> Unit,
    onSaveConsent: () -> Unit
) {
    var newToothInput by remember { mutableStateOf("16") }
    var newDateInput by remember { mutableStateOf(LocalDate.now().toString()) }

    var patientSignature by remember(state.operativeConsentPatientSignature) {
        mutableStateOf(state.operativeConsentPatientSignature)
    }
    var doctorSignature by remember(state.operativeConsentDoctorSignature) {
        mutableStateOf(state.operativeConsentDoctorSignature)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BiodontosWhite,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
                .testTag(TestTags.Admission.OPERATIVE_CONSENT_DIALOG)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.ui_operative_consent_dialog_86),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Zafiro
                        )
                        Text(
                            text = stringResource(R.string.ui_operative_consent_dialog_87),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = BiodontosDark
                        )
                        Text(
                            text = stringResource(R.string.ui_operative_consent_dialog_88),
                            fontSize = 11.sp,
                            color = BiodontosGreyText
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag(TestTags.Admission.OPERATIVE_CONSENT_CLOSE_BUTTON)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.ui_operative_consent_dialog_89))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SlateLight)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BiodontosInputBg)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.ui_operative_consent_dialog_90),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BiodontosDark
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            onUpdateSigner("PATIENT", "", "")
                                        }
                                    ) {
                                        RadioButton(
                                            selected = state.operativeConsentSignerType == "PATIENT",
                                            onClick = { onUpdateSigner("PATIENT", "", "") },
                                            modifier = Modifier.testTag(TestTags.Admission.OPERATIVE_CONSENT_SIGNER_PATIENT)
                                        )
                                        Text(stringResource(R.string.ui_operative_consent_dialog_91, state.firstName, state.firstLastName), fontSize = 13.sp)
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            val defaultRep = if (state.guardianFirstName.isNotBlank()) {
                                                "${state.guardianFirstName} ${state.guardianLastName}".trim()
                                            } else ""
                                            onUpdateSigner("GUARDIAN", defaultRep, state.operativeConsentRepresentativeId)
                                        }
                                    ) {
                                        RadioButton(
                                            selected = state.operativeConsentSignerType == "GUARDIAN",
                                            onClick = {
                                                val defaultRep = if (state.guardianFirstName.isNotBlank()) {
                                                    "${state.guardianFirstName} ${state.guardianLastName}".trim()
                                                } else ""
                                                onUpdateSigner("GUARDIAN", defaultRep, state.operativeConsentRepresentativeId)
                                            },
                                            modifier = Modifier.testTag(TestTags.Admission.OPERATIVE_CONSENT_SIGNER_GUARDIAN)
                                        )
                                        Text(stringResource(R.string.ui_operative_consent_dialog_92), fontSize = 13.sp)
                                    }
                                }

                                if (state.operativeConsentSignerType == "GUARDIAN") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = state.operativeConsentRepresentativeName,
                                            onValueChange = { onUpdateSigner("GUARDIAN", it, state.operativeConsentRepresentativeId) },
                                            label = { Text(stringResource(R.string.ui_operative_consent_dialog_93)) },
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .testTag(TestTags.Admission.OPERATIVE_CONSENT_REP_NAME_FIELD),
                                            colors = biodontosTextFieldColors(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        OutlinedTextField(
                                            value = state.operativeConsentRepresentativeId,
                                            onValueChange = { onUpdateSigner("GUARDIAN", state.operativeConsentRepresentativeName, it) },
                                            label = { Text(stringResource(R.string.ui_operative_consent_dialog_94)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag(TestTags.Admission.OPERATIVE_CONSENT_REP_ID_FIELD),
                                            colors = biodontosTextFieldColors(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = stringResource(R.string.ui_operative_consent_dialog_95, if (state.operativeConsentSignerType == "GUARDIAN") state.operativeConsentRepresentativeName.ifBlank { "el acudiente" } else "${state.firstName} ${state.firstLastName}", if (state.operativeConsentSignerType == "GUARDIAN") state.operativeConsentRepresentativeId.ifBlank { "N/D" } else state.dniCedula, if (state.operativeConsentSignerType == "GUARDIAN") "representante legal de ${state.firstName} ${state.firstLastName}" else "paciente"),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = BiodontosGreyText
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = state.operativeConsentProposedTreatment,
                            onValueChange = onUpdateTreatment,
                            label = { Text(stringResource(R.string.ui_operative_consent_dialog_96)) },
                            placeholder = { Text(stringResource(R.string.ui_operative_consent_dialog_97)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.Admission.OPERATIVE_CONSENT_TREATMENT_FIELD),
                            minLines = 2,
                            colors = biodontosTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SlateLight.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.ui_operative_consent_dialog_98),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BiodontosDark
                                )
                                Text(
                                    text = stringResource(R.string.ui_operative_consent_dialog_99) +
                                            "2. Fractura de obturaciones por falta de cuidado o fuerzas masticatorias.\n" +
                                            "3. Variaciones sutiles de color respecto al diente natural.\n" +
                                            "4. Posible exposición pulpar que requiera tratamiento de conducto, núcleo y corona.\n" +
                                            "5. Aplicación de anestesia local con riesgos de parestesia, hematoma o inflamación.\n" +
                                            "6. Molestia o dolor transitorio post-operatorio.",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = BiodontosGreyText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.ui_operative_consent_dialog_100),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = Zafiro
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateLight)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.ui_operative_consent_dialog_101),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = BiodontosDark
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newToothInput,
                                            onValueChange = { newToothInput = it },
                                            label = { Text(stringResource(R.string.ui_operative_consent_dialog_102)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .width(90.dp)
                                                .testTag(TestTags.Admission.OPERATIVE_CONSENT_TOOTH_INPUT),
                                            colors = biodontosTextFieldColors(),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        Button(
                                            onClick = {
                                                val tooth = newToothInput.toIntOrNull()
                                                if (tooth != null) {
                                                    onAddProcedure(tooth, newDateInput)
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                                            modifier = Modifier.testTag(TestTags.Admission.OPERATIVE_CONSENT_ADD_PROCEDURE_BUTTON)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(stringResource(R.string.ui_operative_consent_dialog_103), fontSize = 12.sp)
                                        }
                                    }
                                }

                                if (state.operativeConsentProcedures.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.ui_operative_consent_dialog_104),
                                        fontSize = 12.sp,
                                        color = BiodontosGreyText,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    state.operativeConsentProcedures.forEachIndexed { index, proc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(BiodontosInputBg, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .testTag(TestTags.Admission.operativeProcedureItem(index)),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.ui_operative_consent_dialog_105, proc.toothNumber, proc.procedureDate),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = BiodontosDark
                                            )
                                            IconButton(
                                                onClick = { onRemoveProcedure(index) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.ui_clinical_admission_screen_76),
                                                    tint = BiodontosErrorRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.ui_operative_consent_dialog_106),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = BiodontosGreyText
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                SignaturePad(
                                    title = if (state.operativeConsentSignerType == "GUARDIAN") "Firma del Acudiente / Tutor" else "Firma del Paciente",
                                    subtitle = "DNI: ${if (state.operativeConsentSignerType == "GUARDIAN") state.operativeConsentRepresentativeId else state.dniCedula} — ${LocalDate.now()}",
                                    currentSignature = patientSignature,
                                    onSignatureConfirmed = { sig ->
                                        patientSignature = sig
                                        onConfirmSignatures(sig, doctorSignature)
                                    }
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                SignaturePad(
                                    title = "Firma y Sello del Profesional",
                                    subtitle = "${state.doctorName} — Reg: ${state.doctorLicense}",
                                    currentSignature = doctorSignature,
                                    onSignatureConfirmed = { sig ->
                                        doctorSignature = sig
                                        onConfirmSignatures(patientSignature, sig)
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SlateLight)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag(TestTags.Admission.OPERATIVE_CONSENT_DISMISS_BUTTON),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(stringResource(R.string.ui_operative_consent_dialog_107), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSaveConsent,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag(TestTags.Admission.OPERATIVE_CONSENT_SAVE_BUTTON),
                        colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BiodontosWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.ui_operative_consent_dialog_108), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
