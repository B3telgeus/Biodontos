package com.example.biodontos.presentation.patients.detail.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.R
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorBg
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.Carmesi
import com.example.biodontos.core.designsystem.Esmeralda
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.domain.model.Anamnesis
import com.example.biodontos.domain.model.AnamnesisCondition
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.Patient

@Composable
fun PatientDemographicsCard(patient: Patient?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.ui_patient_detail_screen_509),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.ui_patient_detail_screen_510), fontSize = 11.sp, color = BiodontosGreyText)
                    Text(patient?.phone?.ifBlank { "No registrado" } ?: "—", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column {
                    Text(stringResource(R.string.ui_patient_detail_screen_511), fontSize = 11.sp, color = BiodontosGreyText)
                    Text(stringResource(R.string.ui_patient_detail_screen_512, patient?.bloodGroup ?: "O", patient?.rhFactor ?: "+"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column {
                    Text(stringResource(R.string.ui_patient_detail_screen_513), fontSize = 11.sp, color = BiodontosGreyText)
                    Text(patient?.maritalStatus ?: "Soltero(a)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (!patient?.email.isNullOrBlank()) {
                Text(stringResource(R.string.ui_clinical_admission_screen_20), fontSize = 11.sp, color = BiodontosGreyText)
                Text(patient?.email ?: "", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (!patient?.address.isNullOrBlank() || !patient?.locality.isNullOrBlank()) {
                Text(stringResource(R.string.ui_clinical_admission_screen_21), fontSize = 11.sp, color = BiodontosGreyText)
                val fullAddress = listOfNotNull(
                    patient?.address?.takeIf { it.isNotBlank() },
                    patient?.locality?.takeIf { it.isNotBlank() }?.let { "Localidad: $it" }
                ).joinToString(" · ")
                Text(fullAddress, fontSize = 13.sp)
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
                            text = stringResource(R.string.ui_patient_detail_screen_514, patient.allergiesDescription),
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
fun AnamnesisSummaryCard(anamnesis: Anamnesis?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.ui_patient_detail_screen_515),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (anamnesis == null) {
                Text(stringResource(R.string.ui_patient_detail_screen_516), color = BiodontosGreyText, fontSize = 13.sp)
            } else {
                val positives = AnamnesisCondition.entries
                    .filter { anamnesis.hasCondition(it) }
                    .map { cond ->
                        val note = anamnesis.optionNotes[cond.key]?.trim()
                        if (!note.isNullOrBlank()) "${cond.shortLabel} ($note)" else cond.shortLabel
                    }

                if (positives.isEmpty()) {
                    Text(stringResource(R.string.ui_patient_detail_screen_517), color = Esmeralda, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(stringResource(R.string.ui_patient_detail_screen_518), color = Carmesi, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(positives.joinToString(" · "), fontSize = 12.sp, color = BiodontosDark)
                }

                if (!anamnesis.observaciones.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.ui_patient_detail_screen_519, anamnesis.observaciones), fontSize = 12.sp, color = BiodontosGreyText)
                }
            }
        }
    }
}

@Composable
fun ConsentSummaryCard(consent: InformedConsent?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                Text(stringResource(R.string.ui_patient_detail_screen_520), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = BiodontosDark)
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
fun OdontogramHubCard(
    dentitionType: String,
    mainDiagnosis: String,
    onOpenOdontogram: () -> Unit,
    onOpenPeriodontogram: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.ui_patient_detail_screen_521),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ui_patient_detail_screen_522, dentitionType, mainDiagnosis),
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
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.Patients.DETAIL_OPEN_ODONTOGRAM_BUTTON)
                ) {
                    Text(stringResource(R.string.ui_patient_detail_screen_523))
                }
                OutlinedButton(
                    onClick = onOpenPeriodontogram,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.Patients.DETAIL_OPEN_ADMISSION_BUTTON)
                ) {
                    Text("Periodontograma")
                }
            }
        }
    }
}
