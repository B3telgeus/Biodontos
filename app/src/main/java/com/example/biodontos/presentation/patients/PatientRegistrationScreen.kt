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

import com.example.biodontos.core.designsystem.biodontosTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRegistrationScreen(
    state: PatientRegistrationState,
    onDniChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onAllergiesChange: (Boolean) -> Unit,
    onAllergiesDescriptionChange: (String) -> Unit,
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
                supportingText = state.dniError?.let { { Text(it) } },
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.firstName,
                onValueChange = onFirstNameChange,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.firstNameError != null,
                supportingText = state.firstNameError?.let { { Text(it) } },
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.lastName,
                onValueChange = onLastNameChange,
                label = { Text("Apellidos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.lastNameError != null,
                supportingText = state.lastNameError?.let { { Text(it) } },
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.phone,
                onValueChange = onPhoneChange,
                label = { Text("Teléfono de Contacto (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = biodontosTextFieldColors()
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

            if (state.hasCriticalAllergies) {
                OutlinedTextField(
                    value = state.allergiesDescription,
                    onValueChange = onAllergiesDescriptionChange,
                    label = { Text("Especificar Alergias *") },
                    placeholder = { Text("Ej: Penicilina, Látex, AINEs...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = biodontosTextFieldColors()
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.biodontos.core.designsystem.BiodontosDark),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Atrás",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                Button(
                    onClick = onSaveClick,
                    enabled = !state.isLoading,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.biodontos.core.designsystem.BiodontosDark,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Guardar Paciente",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
