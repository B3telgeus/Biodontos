package com.example.biodontos.presentation.doctor.documents

import com.example.biodontos.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biodontos.core.designsystem.Ambar
import com.example.biodontos.core.designsystem.BiodontosBorder
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorRed
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosLightGrey
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.Carmesi
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.core.designsystem.Esmeralda
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.domain.model.Patient

@Composable
fun DoctorDocumentsRoute(
    viewModel: DoctorDocumentsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPatientDetail: (patientId: String, initialTab: String) -> Unit,
    onNavigateToAdmission: (patientId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DoctorDocumentsScreen(
        state = state,
        onSearchQueryChange = { viewModel.onEvent(DoctorDocumentsUiEvent.SearchQueryChanged(it)) },
        onFilterSelect = { viewModel.onEvent(DoctorDocumentsUiEvent.FilterSelected(it)) },
        onRefresh = { viewModel.onEvent(DoctorDocumentsUiEvent.Refresh) },
        onNavigateBack = onNavigateBack,
        onNavigateToPatientDetail = onNavigateToPatientDetail,
        onNavigateToAdmission = onNavigateToAdmission,
        modifier = modifier
    )
}

@Composable
fun DoctorDocumentsScreen(
    state: DoctorDocumentsState,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (DocumentFilter) -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPatientDetail: (patientId: String, initialTab: String) -> Unit,
    onNavigateToAdmission: (patientId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    DoctorDocumentsContent(
        state = state,
        onSearchQueryChange = onSearchQueryChange,
        onFilterSelect = onFilterSelect,
        onRefresh = onRefresh,
        onNavigateBack = onNavigateBack,
        onNavigateToPatientDetail = onNavigateToPatientDetail,
        onNavigateToAdmission = onNavigateToAdmission,
        modifier = modifier
    )
}

@Composable
fun DoctorDocumentsContent(
    state: DoctorDocumentsState,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (DocumentFilter) -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPatientDetail: (patientId: String, initialTab: String) -> Unit,
    onNavigateToAdmission: (patientId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BiodontosScreenGradient)
                .padding(padding)
                .consumeWindowInsets(padding)
                .testTag(TestTags.Doctor.DOCS_ROOT)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag(TestTags.Doctor.DOCS_BACK_BUTTON)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.ui_doctor_appointments_screen_182),
                                tint = BiodontosDark
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.ui_doctor_documents_screen_225),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosGreyText,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = stringResource(R.string.ui_doctor_documents_screen_226),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRefresh()
                        },
                        modifier = Modifier.testTag(TestTags.Doctor.DOCS_REFRESH_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.ui_doctor_appointments_screen_186),
                            tint = BiodontosDark
                        )
                    }
                }

                DocumentsKpiSummaryCard(
                    totalRecords = state.totalRecords,
                    consentedCount = state.consentedCount,
                    pendingConsentCount = state.pendingConsentCount,
                    criticalAllergiesCount = state.criticalAllergiesCount,
                    modifier = Modifier.testTag(TestTags.Doctor.DOCS_KPI_CARD)
                )

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.Doctor.DOCS_SEARCH_FIELD),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.ui_doctor_documents_screen_227),
                            color = BiodontosGreyText,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.ui_doctor_documents_screen_228),
                            tint = BiodontosDark
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag(TestTags.Doctor.DOCS_SEARCH_CLEAR_BUTTON)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.ui_doctor_appointments_screen_213),
                                    tint = BiodontosGreyText
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = biodontosTextFieldColors(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DocumentFilter.entries.forEach { filter ->
                        val isSelected = state.activeFilter == filter
                        val count = when (filter) {
                            DocumentFilter.ALL -> state.totalRecords
                            DocumentFilter.CONSENT_SIGNED -> state.consentedCount
                            DocumentFilter.CONSENT_PENDING -> state.pendingConsentCount
                            DocumentFilter.CRITICAL_ALLERGIES -> state.criticalAllergiesCount
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onFilterSelect(filter)
                            },
                            modifier = Modifier.testTag(TestTags.Doctor.documentFilterChip(filter.name)),
                            label = {
                                Text(
                                    text = stringResource(R.string.ui_doctor_documents_screen_229, filter.label, count),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BiodontosDark,
                                selectedLabelColor = BiodontosWhite,
                                containerColor = BiodontosWhite,
                                labelColor = BiodontosDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .testTag(TestTags.Doctor.DOCS_LOADING),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BiodontosDark)
                        }
                    }
                    state.errorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .testTag(TestTags.Doctor.DOCS_ERROR),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = state.errorMessage,
                                    color = BiodontosErrorRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Button(
                                    onClick = onRefresh,
                                    colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                                    modifier = Modifier.testTag(TestTags.Doctor.DOCS_RETRY_BUTTON)
                                ) {
                                    Text(stringResource(R.string.ui_doctor_appointments_screen_175))
                                }
                            }
                        }
                    }
                    state.filteredPatients.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .testTag(TestTags.Doctor.DOCS_EMPTY),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.searchQuery.isNotBlank() || state.activeFilter != DocumentFilter.ALL) {
                                    "No se encontraron expedientes con los criterios seleccionados."
                                } else {
                                    "No hay historias clínicas registradas aún."
                                },
                                color = BiodontosGreyText,
                                fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .testTag(TestTags.Doctor.DOCS_LIST),
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.filteredPatients, key = { it.id }) { patient ->
                                DocumentItemCard(
                                    patient = patient,
                                    onNavigateToPatientDetail = onNavigateToPatientDetail,
                                    onNavigateToAdmission = onNavigateToAdmission,
                                    modifier = Modifier.testTag(TestTags.Doctor.documentItem(patient.id))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentsKpiSummaryCard(
    totalRecords: Int,
    consentedCount: Int,
    pendingConsentCount: Int,
    criticalAllergiesCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.ui_doctor_documents_screen_230),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosGreyText,
                letterSpacing = 1.1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KpiMetricItem("Expedientes", totalRecords.toString(), BiodontosDark)
                KpiMetricItem("Consentidos", consentedCount.toString(), Esmeralda)
                KpiMetricItem("Pendientes", pendingConsentCount.toString(), Carmesi)
                KpiMetricItem("Alergias", criticalAllergiesCount.toString(), Ambar)
            }
        }
    }
}

@Composable
private fun KpiMetricItem(label: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = BiodontosGreyText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DocumentItemCard(
    patient: Patient,
    onNavigateToPatientDetail: (patientId: String, initialTab: String) -> Unit,
    onNavigateToAdmission: (patientId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BiodontosDark),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = (patient.firstName.take(1) + patient.lastName.take(1)).uppercase()
                    Text(
                        text = initials.ifBlank { "PX" },
                        color = BiodontosWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.fullName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                    Text(
                        text = stringResource(R.string.ui_doctor_documents_screen_231, patient.dniCedula, patient.eps),
                        fontSize = 12.sp,
                        color = BiodontosGreyText
                    )
                    if (patient.birthDate.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.ui_doctor_documents_screen_232, patient.birthDate),
                            fontSize = 11.sp,
                            color = BiodontosGreyText
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (patient.consentSigned) {
                    DocumentStatusBadge(
                        text = stringResource(R.string.ui_doctor_documents_screen_233),
                        containerColor = Esmeralda.copy(alpha = 0.12f),
                        textColor = Esmeralda,
                        icon = Icons.Default.Check
                    )
                } else {
                    DocumentStatusBadge(
                        text = stringResource(R.string.ui_doctor_documents_screen_234),
                        containerColor = Carmesi.copy(alpha = 0.12f),
                        textColor = Carmesi,
                        icon = Icons.Default.Warning
                    )
                }

                if (patient.hasCriticalAllergies) {
                    val allergyLabel = if (patient.allergiesDescription.isNotBlank()) {
                        "Alergia: ${patient.allergiesDescription}"
                    } else {
                        "Alergia Crítica"
                    }
                    DocumentStatusBadge(
                        text = allergyLabel,
                        containerColor = Carmesi.copy(alpha = 0.12f),
                        textColor = Carmesi,
                        icon = Icons.Default.Warning
                    )
                }
            }

            HorizontalDivider(color = BiodontosLightGrey, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToPatientDetail(patient.id, "HISTORY")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .testTag(TestTags.Doctor.documentHistoryButton(patient.id)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BiodontosDark),
                    border = BorderStroke(1.dp, BiodontosBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.ui_doctor_documents_screen_235),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToPatientDetail(patient.id, "ODONTOGRAM")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .testTag(TestTags.Doctor.documentOdontogramButton(patient.id)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BiodontosDark),
                    border = BorderStroke(1.dp, BiodontosBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.ui_doctor_documents_screen_236),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToAdmission(patient.id)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .testTag(TestTags.Doctor.documentAdmissionButton(patient.id)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BiodontosDark,
                        contentColor = BiodontosWhite
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.ui_doctor_documents_screen_237),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentStatusBadge(
    text: String,
    containerColor: Color,
    textColor: Color,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorDocumentsLoadingPreview() {
    DentalTheme {
        DoctorDocumentsContent(
            state = DoctorDocumentsState(isLoading = true),
            onSearchQueryChange = {},
            onFilterSelect = {},
            onRefresh = {},
            onNavigateBack = {},
            onNavigateToPatientDetail = { _, _ -> },
            onNavigateToAdmission = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorDocumentsSuccessPreview() {
    DentalTheme {
        DoctorDocumentsContent(
            state = DoctorDocumentsState(
                patients = listOf(
                    Patient(
                        id = "pat-1",
                        dniCedula = "10203040",
                        firstName = "Ana",
                        firstLastName = "Gómez",
                        birthDate = "1990-05-14",
                        consentSigned = true,
                        hasCriticalAllergies = false
                    ),
                    Patient(
                        id = "pat-2",
                        dniCedula = "98765432",
                        firstName = "Carlos",
                        firstLastName = "Pérez",
                        birthDate = "1985-11-20",
                        consentSigned = false,
                        hasCriticalAllergies = true,
                        allergiesDescription = "Penicilina"
                    )
                ),
                isLoading = false
            ),
            onSearchQueryChange = {},
            onFilterSelect = {},
            onRefresh = {},
            onNavigateBack = {},
            onNavigateToPatientDetail = { _, _ -> },
            onNavigateToAdmission = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorDocumentsErrorPreview() {
    DentalTheme {
        DoctorDocumentsContent(
            state = DoctorDocumentsState(
                errorMessage = "No se pudieron cargar los expedientes clínicos.",
                isLoading = false
            ),
            onSearchQueryChange = {},
            onFilterSelect = {},
            onRefresh = {},
            onNavigateBack = {},
            onNavigateToPatientDetail = { _, _ -> },
            onNavigateToAdmission = {}
        )
    }
}
