package com.example.biodontos.presentation.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRegistrationScreen(
    state: PatientRegistrationState,
    onDniChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onAllergiesChange: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Paciente") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.dniCedula,
                onValueChange = onDniChange,
                label = { Text("DNI / Cédula") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.dniError != null,
                supportingText = state.dniError?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = state.firstName,
                onValueChange = onFirstNameChange,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.firstNameError != null,
                supportingText = state.firstNameError?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = state.lastName,
                onValueChange = onLastNameChange,
                label = { Text("Apellidos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.lastNameError != null,
                supportingText = state.lastNameError?.let { { Text(it) } }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.consentSigned,
                        onValueChange = onConsentChange,
                        role = Role.Checkbox
                    )
            ) {
                Checkbox(
                    checked = state.consentSigned,
                    onCheckedChange = null
                )
                Text("Consentimiento firmado", modifier = Modifier.padding(start = 8.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.hasCriticalAllergies,
                        onValueChange = onAllergiesChange,
                        role = Role.Checkbox
                    )
            ) {
                Checkbox(
                    checked = state.hasCriticalAllergies,
                    onCheckedChange = null
                )
                Text("¿Tiene alergias críticas?", modifier = Modifier.padding(start = 8.dp))
            }

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar Paciente")
                }
            }
        }
    }
}
