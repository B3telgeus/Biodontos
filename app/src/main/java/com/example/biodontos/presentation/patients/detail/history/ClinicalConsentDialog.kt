package com.example.biodontos.presentation.patients.detail.history

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.biodontos.core.designsystem.*
import com.example.biodontos.domain.model.ConsentType
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.InformedConsentProcedure
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.presentation.components.SignaturePad
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalConsentDialog(
    patient: Patient?,
    doctorLicense: String = "1995-ODONT-COL",
    doctorName: String = "Dra. Erika Liney Bejarano Beltrán",
    initialConsentType: ConsentType = ConsentType.GENERAL_ODONTOLOGY,
    preselectedTooth: Int? = null,
    onDismiss: () -> Unit,
    onSaveConsent: (InformedConsent) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialConsentType) }
    var signerType by remember { mutableStateOf("PATIENT") }
    var representativeName by remember { mutableStateOf("") }
    var representativeId by remember { mutableStateOf("") }
    var patientCity by remember {
        mutableStateOf(
            patient?.locality?.takeIf { it.isNotBlank() }
                ?: patient?.birthPlace?.takeIf { it.isNotBlank() }
                ?: "Bogotá D.C."
        )
    }

    var treatmentSummary by remember {
        mutableStateOf(
            when (initialConsentType) {
                ConsentType.BLEACHING -> "Aclaramiento dental en consultorio con peróxido de hidrógeno y pauta ambulatoria."
                ConsentType.ORAL_REHABILITATION -> "Procedimiento de rehabilitación protésica y oclusal."
                else -> "Tratamiento restaurador y operatoria dental preventiva y correctiva."
            }
        )
    }

    var relevantMedicalConditions by remember { mutableStateOf("") }
    var initialColor by remember { mutableStateOf("A3") }
    var finalColor by remember { mutableStateOf("A1") }

    val generalProceduresOptions = listOf(
        "Fase higiénica",
        "Ionómero de vidrio",
        "Resinas",
        "Sellantes",
        "Urgencias"
    )
    val selectedGeneralProcedures = remember { mutableStateListOf("Resinas", "Fase higiénica") }

    val rehabProceduresOptions = listOf(
        "Rebase de prótesis",
        "Acondicionador de tejido",
        "Ajuste de oclusión o tallado selectivo",
        "Prótesis fija / Corona metal porcelana",
        "Corona cerámica",
        "Corona metal porcelana sobre implante",
        "Domos",
        "Incrustación cerámica",
        "Incrustación cerómero",
        "Puente Meryland",
        "Núcleos intrarradiculares",
        "Placa neuromiorrelajante programada",
        "Postes intrarradiculares",
        "Prótesis inmediata/ transicional",
        "Prótesis Total",
        "Prótesis Parcial Removible",
        "Prótesis Parcial Removible con Ajustes",
        "Provisional",
        "Puente california"
    )
    val selectedRehabProcedures = remember { mutableStateListOf("Prótesis fija / Corona metal porcelana") }

    var occlusionTestAccepted by remember { mutableStateOf(false) }

    val fdiProcedures = remember {
        mutableStateListOf<InformedConsentProcedure>().apply {
            if (preselectedTooth != null) {
                add(
                    InformedConsentProcedure(
                        toothNumber = preselectedTooth,
                        procedureDate = LocalDate.now().toString(),
                        procedureName = "Restauración pieza dental"
                    )
                )
            }
        }
    }
    var newToothInput by remember { mutableStateOf(preselectedTooth?.toString() ?: "16") }
    var newProcedureNameInput by remember { mutableStateOf("Resina oclusal") }

    var patientSignature by remember { mutableStateOf("") }
    var doctorSignature by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BiodontosWhite,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
                .padding(8.dp)
                .testTag(TestTags.Patients.CLINICAL_CONSENT_DIALOG)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONSULTORIO $doctorName".uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Zafiro
                        )
                        Text(
                            text = when (selectedType) {
                                ConsentType.BLEACHING -> "CONSENTIMIENTO DE BLANQUEAMIENTO DENTAL"
                                ConsentType.ORAL_REHABILITATION -> "CONSENTIMIENTO DE REHABILITACIÓN ORAL"
                                else -> "CONSENTIMIENTO DE ODONTOLOGÍA GENERAL"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = BiodontosDark
                        )
                        Text(
                            text = "Versión: 2 | JUNIO DE 2019 (Res. 1995/1999 MinSalud Colombia)",
                            fontSize = 11.sp,
                            color = BiodontosGreyText
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag(TestTags.Patients.CLINICAL_CONSENT_DISMISS_BUTTON)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = BiodontosDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = when (selectedType) {
                        ConsentType.GENERAL_ODONTOLOGY -> 0
                        ConsentType.BLEACHING -> 1
                        ConsentType.ORAL_REHABILITATION -> 2
                        else -> 0
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.Patients.CLINICAL_CONSENT_TYPE_SELECTOR)
                ) {
                    Tab(
                        selected = selectedType == ConsentType.GENERAL_ODONTOLOGY,
                        onClick = {
                            selectedType = ConsentType.GENERAL_ODONTOLOGY
                            treatmentSummary = "Tratamiento restaurador y operatoria dental preventiva y correctiva."
                        },
                        text = { Text("Odontología General", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag(TestTags.Patients.CLINICAL_CONSENT_GENERAL_TAB)
                    )
                    Tab(
                        selected = selectedType == ConsentType.BLEACHING,
                        onClick = {
                            selectedType = ConsentType.BLEACHING
                            treatmentSummary = "Aclaramiento dental en consultorio con peróxido de hidrógeno y pauta ambulatoria."
                        },
                        text = { Text("Blanqueamiento", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag(TestTags.Patients.CLINICAL_CONSENT_BLEACH_TAB)
                    )
                    Tab(
                        selected = selectedType == ConsentType.ORAL_REHABILITATION,
                        onClick = {
                            selectedType = ConsentType.ORAL_REHABILITATION
                            treatmentSummary = "Procedimiento de rehabilitación protésica y oclusal."
                        },
                        text = { Text("Rehabilitación Oral", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag(TestTags.Patients.CLINICAL_CONSENT_REHAB_TAB)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (validationError != null) {
                    Surface(
                        color = Carmesi.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Carmesi, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = validationError ?: "", fontSize = 12.sp, color = Carmesi, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BiodontosLightGrey),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "¿Quién declara y firma el consentimiento legal?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BiodontosDark
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { signerType = "PATIENT" }
                                    ) {
                                        RadioButton(
                                            selected = signerType == "PATIENT",
                                            onClick = { signerType = "PATIENT" }
                                        )
                                        Text(
                                            text = "Paciente Titular (${patient?.fullName ?: "Paciente"})",
                                            fontSize = 12.sp,
                                            color = BiodontosDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { signerType = "REPRESENTATIVE" }
                                    ) {
                                        RadioButton(
                                            selected = signerType == "REPRESENTATIVE",
                                            onClick = { signerType = "REPRESENTATIVE" }
                                        )
                                        Text(
                                            text = "Acudiente / Representante Legal",
                                            fontSize = 12.sp,
                                            color = BiodontosDark
                                        )
                                    }
                                }

                                if (signerType == "REPRESENTATIVE") {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = representativeName,
                                            onValueChange = { representativeName = it },
                                            label = { Text("Nombre del Acudiente") },
                                            modifier = Modifier.weight(1.2f),
                                            colors = biodontosTextFieldColors()
                                        )
                                        OutlinedTextField(
                                            value = representativeId,
                                            onValueChange = { representativeId = it },
                                            label = { Text("C.C. Acudiente") },
                                            modifier = Modifier.weight(0.8f),
                                            colors = biodontosTextFieldColors()
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = patientCity,
                                    onValueChange = { patientCity = it },
                                    label = { Text("Ciudad de Expedición") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = biodontosTextFieldColors()
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = treatmentSummary,
                            onValueChange = { treatmentSummary = it },
                            label = { Text("Tratamiento propuesto que consiste en: *") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            colors = biodontosTextFieldColors()
                        )
                    }

                    when (selectedType) {
                        ConsentType.GENERAL_ODONTOLOGY -> {
                            item {
                                Text("Procedimientos de Odontología General autorizados:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                generalProceduresOptions.forEach { proc ->
                                    val isChecked = selectedGeneralProcedures.contains(proc)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) selectedGeneralProcedures.remove(proc)
                                                else selectedGeneralProcedures.add(proc)
                                            }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                if (it) selectedGeneralProcedures.add(proc)
                                                else selectedGeneralProcedures.remove(proc)
                                            }
                                        )
                                        Text(text = proc, fontSize = 13.sp, color = BiodontosDark)
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = BiodontosLightGrey),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Riesgos informados al paciente:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BiodontosDark)
                                        Text("• Hipersensibilidad dental transitoria y posterior al tratamiento.", fontSize = 11.sp)
                                        Text("• Laceración reversible de tejidos blandos.", fontSize = 11.sp)
                                        Text("• Recesión gingival y sensibilidad.", fontSize = 11.sp)
                                        Text("• Fractura de restauración o remanente dental.", fontSize = 11.sp)
                                        Text("• Molestia alrededor del diente por uso de porta matriz.", fontSize = 11.sp)
                                        Text("• Exposición pulpar que pueda requerir endodoncia, perno o corona.", fontSize = 11.sp)
                                        Text("• Posibles efectos de anestesia local (parestesia, hematomas, inflamación).", fontSize = 11.sp)
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Zafiro.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.border(1.dp, Zafiro.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Recomendaciones de Odontología General:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Zafiro)
                                        Text("1. Si presenta obturación no ingiera alimentos durante al menos 1 hora.", fontSize = 11.sp)
                                        Text("2. No consuma bebidas oscuras (café, té, gaseosas) durante las primeras 24 horas si es resina estética.", fontSize = 11.sp)
                                        Text("3. Puede presentar sensibilidad los primeros 3 días; si persiste, consulte.", fontSize = 11.sp)
                                        Text("4. No consumir alimentos crocantes o excesivamente duros.", fontSize = 11.sp)
                                        Text("5. Mantenga una estricta higiene oral con cepillo y seda dental.", fontSize = 11.sp)
                                        Text("6. Asista a controles clínicos y radiográficos periódicos cada 6 meses.", fontSize = 11.sp)
                                    }
                                }
                            }

                            item {
                                Text("Intervenciones por Diente (FDI):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = newToothInput,
                                        onValueChange = { newToothInput = it },
                                        label = { Text("Pieza FDI") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(100.dp),
                                        colors = biodontosTextFieldColors()
                                    )
                                    OutlinedTextField(
                                        value = newProcedureNameInput,
                                        onValueChange = { newProcedureNameInput = it },
                                        label = { Text("Procedimiento") },
                                        modifier = Modifier.weight(1f),
                                        colors = biodontosTextFieldColors()
                                    )
                                    Button(
                                        onClick = {
                                            val tNum = newToothInput.toIntOrNull()
                                            if (tNum != null && fdiProcedures.size < 6) {
                                                fdiProcedures.add(
                                                    InformedConsentProcedure(
                                                        toothNumber = tNum,
                                                        procedureDate = LocalDate.now().toString(),
                                                        procedureName = newProcedureNameInput.ifBlank { "Restauración" }
                                                    )
                                                )
                                                newProcedureNameInput = "Resina oclusal"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Zafiro)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Añadir", fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                fdiProcedures.forEachIndexed { idx, proc ->
                                    Surface(
                                        color = BiodontosLightGrey,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Diente FDI #${proc.toothNumber} — ${proc.procedureName} (${proc.procedureDate})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = BiodontosDark
                                            )
                                            IconButton(
                                                onClick = { fdiProcedures.removeAt(idx) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Carmesi, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ConsentType.BLEACHING -> {
                            item {
                                OutlinedTextField(
                                    value = relevantMedicalConditions,
                                    onValueChange = { relevantMedicalConditions = it },
                                    label = { Text("Antecedentes, alergias a peróxidos o medicación relevante:") },
                                    placeholder = { Text("Ej: Hipersensibilidad previa, sin alergia a peróxidos.") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = biodontosTextFieldColors()
                                )
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = BiodontosLightGrey),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Información sobre el Aclaramiento Dental:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BiodontosDark)
                                        Text("• El gel actúa sobre tejidos duros liberando oxígeno para aclaramiento progresivo.", fontSize = 11.sp)
                                        Text("• Solo actúa en dientes naturales; coronas y resinas previas no modifican color.", fontSize = 11.sp)
                                        Text("• La zona cervical y caninos son de más difícil aclaramiento por su anatomía.", fontSize = 11.sp)
                                        Text("• Riesgo de sensibilidad temporal durante y tras la aplicación.", fontSize = 11.sp)
                                        Text("• No hay certeza del grado final exacto; la estabilidad depende de no consumir bebidas oscuras ni tabaco.", fontSize = 11.sp)
                                    }
                                }
                            }

                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = initialColor,
                                        onValueChange = { initialColor = it },
                                        label = { Text("Color Inicial (Guía Vita)") },
                                        modifier = Modifier.weight(1f),
                                        colors = biodontosTextFieldColors()
                                    )
                                    OutlinedTextField(
                                        value = finalColor,
                                        onValueChange = { finalColor = it },
                                        label = { Text("Color Proyectado") },
                                        modifier = Modifier.weight(1f),
                                        colors = biodontosTextFieldColors()
                                    )
                                }
                            }
                        }

                        ConsentType.ORAL_REHABILITATION -> {
                            item {
                                Text("Procedimientos de Rehabilitación Oral:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                rehabProceduresOptions.chunked(2).forEach { pair ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        pair.forEach { proc ->
                                            val isChecked = selectedRehabProcedures.contains(proc)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        if (isChecked) selectedRehabProcedures.remove(proc)
                                                        else selectedRehabProcedures.add(proc)
                                                    }
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = {
                                                        if (it) selectedRehabProcedures.add(proc)
                                                        else selectedRehabProcedures.remove(proc)
                                                    }
                                                )
                                                Text(text = proc, fontSize = 11.sp, color = BiodontosDark)
                                            }
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Ambar.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.border(1.dp, Ambar.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = Ambar, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "CLÁUSULA DE PRUEBA DE OCLUSIÓN Y ESTÉTICA",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = BiodontosDark
                                            )
                                        }
                                        Text(
                                            text = "Para la realización de cualquier tipo de prótesis el paciente debe asistir con un acompañante, ESPECIALMENTE EN LA CITA DONDE SE REALIZA LA PRUEBA DE OCLUSIÓN ya que en esta cita se podrán realizar todas las modificaciones que el paciente desee en cuanto a la posición, forma, tamaño y color de sus nuevos dientes; luego de esta cita el paciente autoriza el acrilado o glaseado (endurecimiento o selle) y ya no se podrán hacer ningún tipo de correcciones, ni repeticiones, ni tampoco devoluciones de dinero.",
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = BiodontosDark
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { occlusionTestAccepted = !occlusionTestAccepted }
                                                .padding(top = 4.dp)
                                        ) {
                                            Checkbox(
                                                checked = occlusionTestAccepted,
                                                onCheckedChange = { occlusionTestAccepted = it },
                                                modifier = Modifier.testTag(TestTags.Patients.CLINICAL_CONSENT_OCCLUSION_CHECKBOX)
                                            )
                                            Text(
                                                text = "El paciente / acudiente declara haber realizado la prueba de oclusión y estética a entera conformidad. *",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (occlusionTestAccepted) Esmeralda else Carmesi
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> Unit
                    }

                    item {
                        Text(
                            text = "Comprendo perfectamente las implicaciones del presente documento y dejo constancia de que los espacios fueron llenados en mi presencia antes de mi firma. Siempre y cuando cumpla las recomendaciones del profesional y asista a controles, exonero de responsabilidad por complicaciones imprevisibles.",
                            fontSize = 11.sp,
                            color = BiodontosGreyText,
                            lineHeight = 14.sp
                        )
                    }

                    item {
                        SignaturePad(
                            title = if (signerType == "PATIENT") "Firma del Paciente Titular *" else "Firma del Acudiente / Representante Legal *",
                            subtitle = "Dibuje su firma dentro del recuadro y presione confirmar.",
                            currentSignature = patientSignature.ifBlank { null },
                            onSignatureConfirmed = { patientSignature = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        SignaturePad(
                            title = "Firma y Sello del Odontólogo Tratante ($doctorName - Reg: $doctorLicense) *",
                            subtitle = "Dibuje su firma profesional dentro del recuadro y presione confirmar.",
                            currentSignature = doctorSignature.ifBlank { null },
                            onSignatureConfirmed = { doctorSignature = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            if (treatmentSummary.isBlank()) {
                                validationError = "Debe describir el tratamiento propuesto."
                                return@Button
                            }
                            if (patientSignature.isBlank()) {
                                validationError = "Es obligatoria la firma del paciente o acudiente."
                                return@Button
                            }
                            if (doctorSignature.isBlank()) {
                                validationError = "Es obligatoria la firma del odontólogo tratante."
                                return@Button
                            }
                            if (selectedType == ConsentType.ORAL_REHABILITATION && !occlusionTestAccepted) {
                                validationError = "Debe aceptar la prueba de oclusión y estética para rehabilitación oral."
                                return@Button
                            }

                            validationError = null
                            val consent = InformedConsent(
                                patientId = patient?.id ?: "",
                                doctorId = doctorLicense,
                                consentType = selectedType,
                                documentVersion = "2",
                                versionDate = "2019-06-01",
                                treatmentSummary = treatmentSummary.trim(),
                                representativeName = if (signerType == "REPRESENTATIVE") representativeName.trim() else null,
                                representativeId = if (signerType == "REPRESENTATIVE") representativeId.trim() else null,
                                patientCity = patientCity.trim(),
                                patientSignatureUrl = if (signerType == "PATIENT") patientSignature else null,
                                guardianSignatureUrl = if (signerType == "REPRESENTATIVE") patientSignature else null,
                                doctorSignatureUrl = doctorSignature,
                                doctorLicenseNumber = doctorLicense,
                                signedAt = LocalDate.now().toString(),
                                status = "SIGNED",
                                procedures = fdiProcedures.toList(),
                                selectedProcedures = when (selectedType) {
                                    ConsentType.GENERAL_ODONTOLOGY -> selectedGeneralProcedures.toList()
                                    ConsentType.ORAL_REHABILITATION -> selectedRehabProcedures.toList()
                                    else -> emptyList()
                                },
                                relevantMedicalConditions = relevantMedicalConditions.trim(),
                                occlusionTestAccepted = occlusionTestAccepted,
                                aestheticColorAccepted = occlusionTestAccepted,
                                initialColorGuide = initialColor.trim(),
                                finalColorGuide = finalColor.trim()
                            )
                            onSaveConsent(consent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Esmeralda),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag(TestTags.Patients.CLINICAL_CONSENT_SAVE_BUTTON)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar y Firmar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
