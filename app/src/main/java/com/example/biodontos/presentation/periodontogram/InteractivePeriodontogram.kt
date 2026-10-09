package com.example.biodontos.presentation.periodontogram

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.*
import com.example.biodontos.domain.model.PeriodontalSiteLocation
import com.example.biodontos.domain.model.PeriodontalSiteMeasurement

@Composable
fun InteractivePeriodontogram(
    selectedToothNumber: Int,
    availableTeeth: List<Int>,
    siteMeasurements: Map<Pair<Int, PeriodontalSiteLocation>, PeriodontalSiteMeasurement>,
    onToothSelected: (Int) -> Unit,
    onMeasurementChanged: (PeriodontalSiteMeasurement) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentToothSites = PeriodontalSiteLocation.entries.associateWith { location ->
        siteMeasurements[selectedToothNumber to location] ?: PeriodontalSiteMeasurement(
            toothNumber = selectedToothNumber,
            siteLocation = location,
            pocketDepthMm = 2.0f,
            gingivalRecessionMm = 0.0f
        )
    }

    val vestibularSites = listOf(
        PeriodontalSiteLocation.MESIO_VESTIBULAR,
        PeriodontalSiteLocation.MID_VESTIBULAR,
        PeriodontalSiteLocation.DISTO_VESTIBULAR
    )

    val lingualSites = listOf(
        PeriodontalSiteLocation.MESIO_LINGUAL,
        PeriodontalSiteLocation.MID_LINGUAL,
        PeriodontalSiteLocation.DISTO_LINGUAL
    )

    // Compute tooth summary
    val deepPocketsCount = currentToothSites.values.count { it.isDeepPocket }
    val bopCount = currentToothSites.values.count { it.bleedingOnProbing }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BiodontosWhite)
            .border(1.dp, BiodontosBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // --- Header: Selector de Dientes FDI ---
        Text(
            text = "Periodontometría de 6 Sitios (FDI)",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )
        )
        Text(
            text = "Seleccione la pieza dental para registrar bolsas y recesión gingival.",
            style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(availableTeeth) { tooth ->
                val isSelected = tooth == selectedToothNumber
                val hasDeepPocketInTooth = PeriodontalSiteLocation.entries.any { loc ->
                    siteMeasurements[tooth to loc]?.isDeepPocket == true
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isSelected -> BiodontosDark
                        hasDeepPocketInTooth -> BiodontosErrorBg
                        else -> BiodontosInputBg
                    },
                    modifier = Modifier.clickable { onToothSelected(tooth) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tooth.toString(),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isSelected -> BiodontosWhite
                                    hasDeepPocketInTooth -> Carmesi
                                    else -> BiodontosDark
                                },
                                fontSize = 15.sp
                            )
                            if (hasDeepPocketInTooth) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Carmesi)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Resumen Clínico del Diente Seleccionado ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (deepPocketsCount > 0) BiodontosErrorBg else SlateLight)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Pieza FDI: $selectedToothNumber",
                fontWeight = FontWeight.Bold,
                color = BiodontosDark,
                style = MaterialTheme.typography.titleSmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (deepPocketsCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta Bolsa",
                            tint = Carmesi,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$deepPocketsCount bolsa(s) ≥ 4mm",
                            color = Carmesi,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (bopCount > 0) {
                    Text(
                        text = "🩸 Sangrado: $bopCount sitio(s)",
                        color = Carmesi,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECCIÓN 1: VESTIBULAR (MV, V, DV) ---
        Text(
            text = "CARA VESTIBULAR",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Zafiro,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            vestibularSites.forEach { loc ->
                val measurement = currentToothSites[loc] ?: PeriodontalSiteMeasurement(
                    toothNumber = selectedToothNumber,
                    siteLocation = loc
                )
                SiteMeasurementCard(
                    title = when (loc) {
                        PeriodontalSiteLocation.MESIO_VESTIBULAR -> "MV"
                        PeriodontalSiteLocation.MID_VESTIBULAR -> "V"
                        PeriodontalSiteLocation.DISTO_VESTIBULAR -> "DV"
                        else -> ""
                    },
                    measurement = measurement,
                    onChanged = onMeasurementChanged,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECCIÓN 2: LINGUAL / PALATINO (ML, L, DL) ---
        Text(
            text = "CARA LINGUAL / PALATINA",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Zafiro,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            lingualSites.forEach { loc ->
                val measurement = currentToothSites[loc] ?: PeriodontalSiteMeasurement(
                    toothNumber = selectedToothNumber,
                    siteLocation = loc
                )
                SiteMeasurementCard(
                    title = when (loc) {
                        PeriodontalSiteLocation.MESIO_LINGUAL -> "ML"
                        PeriodontalSiteLocation.MID_LINGUAL -> "L/P"
                        PeriodontalSiteLocation.DISTO_LINGUAL -> "DL"
                        else -> ""
                    },
                    measurement = measurement,
                    onChanged = onMeasurementChanged,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Movilidad y Furca del Diente ---
        val sampleMeasurement = currentToothSites.values.firstOrNull() ?: PeriodontalSiteMeasurement(
            toothNumber = selectedToothNumber,
            siteLocation = PeriodontalSiteLocation.MID_VESTIBULAR
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Movilidad (0..3)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Movilidad Dental (Grado 0-3):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..3).forEach { grade ->
                        val selected = sampleMeasurement.mobilityGrade == grade
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) BiodontosDark else BiodontosInputBg,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    // Update mobility on all sites of current tooth
                                    currentToothSites.values.forEach { site ->
                                        onMeasurementChanged(site.copy(mobilityGrade = grade))
                                    }
                                }
                        ) {
                            Text(
                                text = grade.toString(),
                                textAlign = TextAlign.Center,
                                color = if (selected) BiodontosWhite else BiodontosDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Furca (0..4)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Afectación Furca (0-4):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..4).forEach { grade ->
                        val selected = sampleMeasurement.furcationGrade == grade
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) BiodontosDark else BiodontosInputBg,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    currentToothSites.values.forEach { site ->
                                        onMeasurementChanged(site.copy(furcationGrade = grade))
                                    }
                                }
                        ) {
                            Text(
                                text = grade.toString(),
                                textAlign = TextAlign.Center,
                                color = if (selected) BiodontosWhite else BiodontosDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteMeasurementCard(
    title: String,
    measurement: PeriodontalSiteMeasurement,
    onChanged: (PeriodontalSiteMeasurement) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDeep = measurement.isDeepPocket
    val containerBg by animateColorAsState(
        targetValue = if (isDeep) BiodontosErrorBg else BiodontosInputBg,
        label = "siteBg"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(
                width = if (isDeep) 1.5.dp else 1.dp,
                color = if (isDeep) Carmesi else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sitio
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = if (isDeep) Carmesi else BiodontosDark,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Profundidad de Bolsa (PD)
        Text(
            text = "PB: ${measurement.pocketDepthMm.toInt()} mm",
            fontWeight = FontWeight.ExtraBold,
            color = if (isDeep) Carmesi else BiodontosDark,
            fontSize = 15.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            SmallStepperButton(
                text = "-",
                enabled = measurement.pocketDepthMm > 0f,
                onClick = {
                    onChanged(measurement.copy(pocketDepthMm = (measurement.pocketDepthMm - 1f).coerceAtLeast(0f)))
                }
            )
            Spacer(modifier = Modifier.width(6.dp))
            SmallStepperButton(
                text = "+",
                enabled = measurement.pocketDepthMm < 15f,
                onClick = {
                    onChanged(measurement.copy(pocketDepthMm = measurement.pocketDepthMm + 1f))
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Recesión Gingival (GR)
        Text(
            text = "RG: ${measurement.gingivalRecessionMm.toInt()} mm",
            color = BiodontosGreyText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            SmallStepperButton(
                text = "-",
                enabled = measurement.gingivalRecessionMm > 0f,
                onClick = {
                    onChanged(measurement.copy(gingivalRecessionMm = (measurement.gingivalRecessionMm - 1f).coerceAtLeast(0f)))
                }
            )
            Spacer(modifier = Modifier.width(6.dp))
            SmallStepperButton(
                text = "+",
                enabled = measurement.gingivalRecessionMm < 10f,
                onClick = {
                    onChanged(measurement.copy(gingivalRecessionMm = measurement.gingivalRecessionMm + 1f))
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // CAL Calculado
        Text(
            text = "CAL: ${measurement.clinicalAttachmentLossMm.toInt()} mm",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (measurement.clinicalAttachmentLossMm >= 4f) Carmesi else Zafiro
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Botón BOP (Sangrado)
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (measurement.bleedingOnProbing) Carmesi else BiodontosWhite,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onChanged(measurement.copy(bleedingOnProbing = !measurement.bleedingOnProbing))
                }
        ) {
            Text(
                text = if (measurement.bleedingOnProbing) "🩸 BOP" else "Sin Sangrado",
                color = if (measurement.bleedingOnProbing) BiodontosWhite else BiodontosGreyText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SmallStepperButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (enabled) BiodontosWhite else BiodontosInputBg,
        shadowElevation = if (enabled) 1.dp else 0.dp,
        modifier = Modifier
            .size(24.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (enabled) BiodontosDark else BiodontosGreyText
            )
        }
    }
}
