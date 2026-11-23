package com.example.biodontos.presentation.patients.detail.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.Esmeralda
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.domain.model.PeriodontalSiteLocation
import com.example.biodontos.domain.model.PeriodontalSiteMeasurement
import com.example.biodontos.presentation.periodontogram.InteractivePeriodontogram

@Composable
fun PeriodontogramTabContent(
    selectedTooth: Int,
    availableTeeth: List<Int>,
    siteMeasurements: Map<Pair<Int, PeriodontalSiteLocation>, PeriodontalSiteMeasurement>,
    observations: String,
    isSaving: Boolean,
    onToothSelected: (Int) -> Unit,
    onMeasurementChanged: (PeriodontalSiteMeasurement) -> Unit,
    onObservationsChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.Patients.PERIODONTOGRAM_TAB_CONTENT),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Periodontograma Clínico",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Mediciones periodontales por sitio (Palatino en maxilar superior / Lingual en mandibular inferior)",
                fontSize = 12.sp,
                color = BiodontosGreyText
            )
            Spacer(modifier = Modifier.height(16.dp))

            InteractivePeriodontogram(
                selectedToothNumber = selectedTooth,
                availableTeeth = availableTeeth,
                siteMeasurements = siteMeasurements,
                onToothSelected = onToothSelected,
                onMeasurementChanged = onMeasurementChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = observations,
                onValueChange = onObservationsChanged,
                label = { Text("Observaciones Periodontales") },
                placeholder = { Text("Hallazgos clínicos, cálculo supra/subgingival, movilidad...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Patients.PERIODONTOGRAM_OBSERVATIONS_INPUT),
                minLines = 3,
                colors = biodontosTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSave,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Esmeralda),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Patients.PERIODONTOGRAM_SAVE_BUTTON)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = BiodontosWhite,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = BiodontosWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Periodontograma", color = BiodontosWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
