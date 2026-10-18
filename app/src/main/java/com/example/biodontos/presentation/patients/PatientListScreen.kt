package com.example.biodontos.presentation.patients

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    state: PatientListState,
    onAddPatientClick: () -> Unit,
    onClinicalAdmissionClick: () -> Unit = onAddPatientClick,
    onPatientClick: (Patient) -> Unit,
    onDoctorProfileClick: () -> Unit = {},
    onRetry: () -> Unit
) {
    Scaffold(
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.patients, key = { it.id }) { patient ->
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

@Composable
fun PatientItem(
    patient: Patient,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = patient.fullName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "DNI: ${patient.dniCedula}",
                style = MaterialTheme.typography.bodySmall
            )
            if (patient.hasCriticalAllergies) {
                Text(
                    text = "⚠ Alergias Críticas",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
