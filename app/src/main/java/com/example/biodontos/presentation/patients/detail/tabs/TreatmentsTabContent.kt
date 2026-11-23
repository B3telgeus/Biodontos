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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.R
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.Esmeralda
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.core.designsystem.components.DateBadge
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.Treatment

@Composable
fun TreatmentsBalanceHeaderCard(
    totalBilled: Double,
    totalPaid: Double,
    balance: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.ui_patient_detail_screen_528), color = BiodontosWhite.copy(alpha = 0.8f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ui_patient_detail_screen_529, balance.toInt()),
                color = BiodontosWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.ui_patient_detail_screen_530, totalBilled.toInt()), color = BiodontosWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(stringResource(R.string.ui_patient_detail_screen_531, totalPaid.toInt()), color = Esmeralda, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TreatmentItemCard(treatment: Treatment, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.Patients.treatmentItem(treatment.id)),
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
                Text(stringResource(R.string.ui_patient_detail_screen_532, treatment.status), fontSize = 12.sp, color = BiodontosGreyText)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.ui_patient_detail_screen_533, treatment.totalCost.pesos), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BiodontosDark)
            }
        }
    }
}

@Composable
fun PatientAppointmentItemCard(appointment: Appointment, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.Patients.appointmentItem(appointment.id)),
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
                    stringResource(R.string.ui_patient_detail_screen_534, appointment.status.name, if (appointment.isConfirmed) "Confirmada" else "Sin confirmar"),
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
            }
        }
    }
}

@Composable
fun NewAppointmentDialog(
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
        modifier = Modifier.testTag(TestTags.Patients.DETAIL_NEW_APPT_DIALOG),
        title = { Text(stringResource(R.string.ui_patient_detail_screen_557), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = onDateChange,
                    label = { Text(stringResource(R.string.ui_patient_detail_screen_558)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.Patients.DETAIL_NEW_APPT_DATE_FIELD),
                    colors = biodontosTextFieldColors()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = onTimeChange,
                    label = { Text(stringResource(R.string.ui_patient_detail_screen_559)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.Patients.DETAIL_NEW_APPT_TIME_FIELD),
                    colors = biodontosTextFieldColors()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_36)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.Patients.DETAIL_NEW_APPT_REASON_FIELD),
                    colors = biodontosTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                modifier = Modifier.testTag(TestTags.Patients.DETAIL_NEW_APPT_SAVE_BUTTON)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BiodontosWhite)
                } else {
                    Text(stringResource(R.string.ui_patient_detail_screen_502))
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
