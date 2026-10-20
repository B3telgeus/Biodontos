package com.example.biodontos.presentation.admission

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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(20.dp)
            ) {
                // Cabecera Institucional
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CONSULTORIO DRA. ERIKA LINEY BEJARANO BELTRÁN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Zafiro
                        )
                        Text(
                            text = "CONSENTIMIENTO INFORMADO DE OPERATORIA DENTAL",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = BiodontosDark
                        )
                        Text(
                            text = "Versión: 2 | Fecha: JUNIO DE 2019 (Res. 1995/1999 MinSalud)",
                            fontSize = 11.sp,
                            color = BiodontosGreyText
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SlateLight)

                // Cuerpo del documento con Scroll
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Selector de Declarante
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BiodontosInputBg)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "¿Quién declara y firma el consentimiento legal?",
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
                                            onClick = { onUpdateSigner("PATIENT", "", "") }
                                        )
                                        Text("Paciente Titular (${state.firstName} ${state.firstLastName})", fontSize = 13.sp)
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
                                            }
                                        )
                                        Text("Acudiente / Representante Legal", fontSize = 13.sp)
                                    }
                                }

                                if (state.operativeConsentSignerType == "GUARDIAN") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = state.operativeConsentRepresentativeName,
                                            onValueChange = { onUpdateSigner("GUARDIAN", it, state.operativeConsentRepresentativeId) },
                                            label = { Text("Nombre Representante") },
                                            modifier = Modifier.weight(1.3f),
                                            colors = biodontosTextFieldColors(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        OutlinedTextField(
                                            value = state.operativeConsentRepresentativeId,
                                            onValueChange = { onUpdateSigner("GUARDIAN", state.operativeConsentRepresentativeName, it) },
                                            label = { Text("C.C. Representante") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            colors = biodontosTextFieldColors(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Yo, ${if (state.operativeConsentSignerType == "GUARDIAN") state.operativeConsentRepresentativeName.ifBlank { "el acudiente" } else "${state.firstName} ${state.firstLastName}"} mayor de edad, identificado(a) con C.C ${if (state.operativeConsentSignerType == "GUARDIAN") state.operativeConsentRepresentativeId.ifBlank { "N/D" } else state.dniCedula} actuando en calidad de ${if (state.operativeConsentSignerType == "GUARDIAN") "representante legal de ${state.firstName} ${state.firstLastName}" else "paciente"}, en pleno uso de mis facultades mentales, libre y conscientemente autorizo al profesional ERIKA LINEY BEJARANO BELTRAN en calidad de Odontólogo y a profesionales y/o procedimientos adicionales que considere necesarios para el tratamiento propuesto:",
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = BiodontosGreyText
                                )
                            }
                        }
                    }

                    // 2. Descripción del Tratamiento Propuesto
                    item {
                        OutlinedTextField(
                            value = state.operativeConsentProposedTreatment,
                            onValueChange = onUpdateTreatment,
                            label = { Text("Tratamiento propuesto que consiste en: *") },
                            placeholder = { Text("Ej: Restauraciones con resina compuesta en piezas dentales 16, 26 y profilaxis general.") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            colors = biodontosTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 3. Resumen Normativo y Riesgos de Operatoria
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SlateLight.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "RESTAURACIONES Y RIESGOS INFORMADOS:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BiodontosDark
                                )
                                Text(
                                    text = "1. Hipersensibilidad después de realizadas las obturaciones.\n" +
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
                                    text = "RECOMENDACIONES: No ingerir alimentos 1 hora post-obturación; evitar bebidas oscuras durante 24h si es resina estética; evitar alimentos duros/crocantes; mantener higiene estricta.",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = Zafiro
                                )
                            }
                        }
                    }

                    // 4. Tabla Dinámica de Dientes a Tratar
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
                                        text = "Intervenciones por Diente (FDI) y Fechas",
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
                                            label = { Text("Pieza FDI") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(90.dp),
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
                                            colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Añadir", fontSize = 12.sp)
                                        }
                                    }
                                }

                                if (state.operativeConsentProcedures.isEmpty()) {
                                    Text(
                                        text = "No se han agregado piezas dentales específicas. Añada los dientes FDI a intervenir.",
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
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Diente FDI #${proc.toothNumber} — Fecha: ${proc.procedureDate}",
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
                                                    contentDescription = "Eliminar",
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

                    // 5. Exoneración y Cierre Legal
                    item {
                        Text(
                            text = "Comprendo perfectamente las implicaciones del presente documento y dejo constancia de que los espacios en blanco fueron llenados en mi presencia antes de mi firma. Siempre y cuando cumpla las recomendaciones dadas por el profesional y asista a control periódico, libero de responsabilidad por riesgos imprevisibles.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = BiodontosGreyText
                        )
                    }

                    // 6. Firmas Digitales
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Firma del Paciente o Acudiente
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

                            // Firma del Profesional
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

                // Botones de Acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Cancelar / Salir", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSaveConsent,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
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
                            Text("Guardar y Firmar Consentimiento", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
