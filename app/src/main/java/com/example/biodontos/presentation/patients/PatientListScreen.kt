package com.example.biodontos.presentation.patients

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.biodontos.domain.model.Patient

import androidx.compose.ui.text.font.FontWeight
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosWhite

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.tooling.preview.Preview
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.core.designsystem.biodontosTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@VisibleForTesting
@Composable
fun PatientListScreen(
    state: PatientListState,
    onSearchQueryChange: (String) -> Unit = {},
    onAddPatientClick: () -> Unit,
    onClinicalAdmissionClick: () -> Unit = onAddPatientClick,
    onPatientClick: (Patient) -> Unit,
    onDoctorProfileClick: () -> Unit = {},
    onRetry: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pacientes - Biodontos", fontWeight = FontWeight.Bold, color = BiodontosDark) },
                actions = {
                    IconButton(onClick = onDoctorProfileClick) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Person,
                            contentDescription = "Mi Perfil & Citas",
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
                contentColor = BiodontosWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ingreso Clínico")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ingreso Clínico", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            // Barra de Búsqueda
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Buscar paciente por nombre o DNI...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = BiodontosGreyText
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar",
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (state.error != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.error, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetry) {
                            Text("Reintentar")
                        }
                    }
                } else if (state.filteredPatients.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isBlank()) "No hay pacientes registrados aún."
                            else "No se encontraron pacientes para \"${state.searchQuery}\"",
                            color = BiodontosGreyText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
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

@Composable
fun PatientItem(
    patient: Patient,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
                    text = "DNI: ${patient.dniCedula} ${if (patient.phone.isNotBlank()) "· Tel: ${patient.phone}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BiodontosGreyText
                )
                if (patient.hasCriticalAllergies) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠ Alergias: ${patient.allergiesDescription.ifBlank { "Críticas" }}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
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
                        hasCriticalAllergies = true,
                        allergiesDescription = "Penicilina"
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
