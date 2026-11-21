package com.example.biodontos.presentation.patients

import com.example.biodontos.R

import androidx.compose.ui.res.stringResource

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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.core.designsystem.StatusCancelled
import com.example.biodontos.core.designsystem.StatusConfirmed
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.domain.model.Patient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    state: PatientListState,
    onSearchQueryChange: (String) -> Unit = {},
    onFilterSelected: (PatientQuickFilter) -> Unit = {},
    onRefresh: () -> Unit = {},
    onAddPatientClick: () -> Unit = {},
    onClinicalAdmissionClick: () -> Unit = onAddPatientClick,
    onPatientClick: (Patient) -> Unit,
    onDoctorProfileClick: () -> Unit = {},
    onRetry: () -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.Patients.LIST_ROOT),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ui_patient_list_screen_560),
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isRefreshing,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(TestTags.Patients.LIST_REFRESH_BUTTON)
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BiodontosDark
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.ui_patient_list_screen_561),
                                tint = BiodontosDark
                            )
                        }
                    }
                    IconButton(
                        onClick = onDoctorProfileClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(TestTags.Patients.LIST_DOCTOR_PROFILE_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.ui_doctor_home_screen_238),
                            tint = BiodontosDark
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onClinicalAdmissionClick,
                containerColor = BiodontosDark,
                contentColor = BiodontosWhite,
                modifier = Modifier.testTag(TestTags.Patients.LIST_FAB_ADMISSION)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ui_patient_list_screen_562))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ui_patient_list_screen_562), fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BiodontosScreenGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.ui_doctor_documents_screen_227)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.ui_doctor_documents_screen_228),
                            tint = BiodontosGreyText
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag(TestTags.Patients.LIST_SEARCH_CLEAR_BUTTON)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.ui_signature_pad_172),
                                    tint = BiodontosGreyText
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = biodontosTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag(TestTags.Patients.LIST_SEARCH_FIELD)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PatientQuickFilter.entries.forEach { filter ->
                        val isSelected = state.selectedFilter == filter
                        val labelText = when (filter) {
                            PatientQuickFilter.ALL -> "Todos (${state.totalCount})"
                            PatientQuickFilter.CRITICAL_ALLERGIES -> "Alergias Críticas (${state.criticalAllergiesCount})"
                            PatientQuickFilter.PENDING_CONSENT -> "Firma Pendiente (${state.pendingConsentCount})"
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterSelected(filter) },
                            label = {
                                Text(
                                    text = labelText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BiodontosDark,
                                selectedLabelColor = BiodontosWhite,
                                containerColor = BiodontosWhite.copy(alpha = 0.85f),
                                labelColor = BiodontosDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) BiodontosDark else BiodontosDark.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag(TestTags.Patients.filterChip(filter.name))
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.isLoading && !state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag(TestTags.Patients.LIST_LOADING),
                            color = BiodontosDark
                        )
                    } else if (state.error != null) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                                .testTag(TestTags.Patients.LIST_ERROR),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.testTag(TestTags.Patients.LIST_RETRY_BUTTON)
                            ) {
                                Text(stringResource(R.string.ui_doctor_appointments_screen_175))
                            }
                        }
                    } else if (state.filteredPatients.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(TestTags.Patients.LIST_EMPTY),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyText = if (state.searchQuery.isNotBlank()) {
                                "No se encontraron pacientes para \"${state.searchQuery}\""
                            } else when (state.selectedFilter) {
                                PatientQuickFilter.CRITICAL_ALLERGIES -> "No hay pacientes con alergias críticas registradas."
                                PatientQuickFilter.PENDING_CONSENT -> "No hay pacientes con consentimiento pendiente."
                                PatientQuickFilter.ALL -> "No hay pacientes registrados aún."
                            }
                            Text(
                                text = emptyText,
                                color = BiodontosGreyText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(TestTags.Patients.LIST_LAZY_COLUMN),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.filteredPatients, key = { it.id }) { patient ->
                                PatientItem(
                                    patient = patient,
                                    onClick = { onPatientClick(patient) }
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
fun PatientItem(
    patient: Patient,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.Patients.patientItem(patient.id)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BiodontosDark.copy(alpha = 0.08f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = BiodontosDark
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = BiodontosDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.ui_patient_list_screen_563, patient.dniCedula, if (patient.phone.isNotBlank()) "· Tel: ${patient.phone}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = BiodontosGreyText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (patient.consentSigned) StatusConfirmed.copy(alpha = 0.12f) else StatusCancelled.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (patient.consentSigned) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (patient.consentSigned) StatusConfirmed else StatusCancelled,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (patient.consentSigned) "Consentimiento OK" else "Firma Pendiente",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (patient.consentSigned) StatusConfirmed else StatusCancelled
                            )
                        }
                    }

                    if (patient.hasCriticalAllergies) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = stringResource(R.string.ui_patient_list_screen_564, patient.allergiesDescription.ifBlank { "Crítica" }),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientListLoadingPreview() {
    DentalTheme {
        PatientListScreen(
            state = PatientListState(isLoading = true),
            onAddPatientClick = {},
            onPatientClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientListSuccessPreview() {
    DentalTheme {
        PatientListScreen(
            state = PatientListState(
                isLoading = false,
                patients = listOf(
                    Patient(
                        id = "pat-1",
                        dniCedula = "10203040",
                        firstName = "María",
                        firstLastName = "Gómez",
                        phone = "3001234567",
                        consentSigned = true,
                        hasCriticalAllergies = true,
                        allergiesDescription = "Penicilina"
                    ),
                    Patient(
                        id = "pat-2",
                        dniCedula = "20304050",
                        firstName = "Juan",
                        firstLastName = "Pérez",
                        phone = "3109876543",
                        consentSigned = false,
                        hasCriticalAllergies = false
                    )
                )
            ),
            onAddPatientClick = {},
            onPatientClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientListErrorPreview() {
    DentalTheme {
        PatientListScreen(
            state = PatientListState(
                isLoading = false,
                error = "Error al cargar el listado de pacientes."
            ),
            onAddPatientClick = {},
            onPatientClick = {},
            onRetry = {}
        )
    }
}
