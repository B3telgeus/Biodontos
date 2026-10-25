package com.example.biodontos.presentation.admission

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biodontos.core.designsystem.*
import com.example.biodontos.domain.model.*
import com.example.biodontos.presentation.components.SignaturePad
import com.example.biodontos.presentation.odontogram.InteractiveTooth
import com.example.biodontos.presentation.odontogram.color
import com.example.biodontos.presentation.periodontogram.InteractivePeriodontogram

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalAdmissionScreen(
    viewModel: ClinicalAdmissionViewModel,
    onNavigateBack: () -> Unit,
    onAdmissionFinished: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            onAdmissionFinished()
        }
    }

    ClinicalAdmissionContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@VisibleForTesting
@Composable
fun ClinicalAdmissionContent(
    state: ClinicalAdmissionState,
    onEvent: (ClinicalAdmissionEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    if (state.showOperativeConsentDialog) {
        OperativeConsentDialog(
            state = state,
            onDismiss = { onEvent(ClinicalAdmissionEvent.ToggleOperativeConsentDialog(false)) },
            onUpdateTreatment = { onEvent(ClinicalAdmissionEvent.UpdateOperativeConsentTreatment(it)) },
            onUpdateSigner = { signerType, repName, repId ->
                onEvent(ClinicalAdmissionEvent.UpdateOperativeConsentSigner(signerType, repName, repId))
            },
            onAddProcedure = { tooth, date ->
                onEvent(ClinicalAdmissionEvent.AddOperativeConsentProcedure(tooth, date))
            },
            onRemoveProcedure = { index ->
                onEvent(ClinicalAdmissionEvent.RemoveOperativeConsentProcedure(index))
            },
            onConfirmSignatures = { pSig, dSig ->
                onEvent(ClinicalAdmissionEvent.ConfirmOperativeConsentSignatures(pSig, dSig))
            },
            onSaveConsent = {
                onEvent(ClinicalAdmissionEvent.SaveOperativeConsent)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ingreso Clínico Odontológico",
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Paso ${state.currentStep} de 7: ${getStepTitle(state.currentStep)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BiodontosGreyText
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = BiodontosDark
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { onEvent(ClinicalAdmissionEvent.ToggleOperativeConsentDialog(true)) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Zafiro.copy(alpha = 0.12f),
                            contentColor = Zafiro
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Consentimiento Operatoria", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BiodontosWhite)
            )
        },
        bottomBar = {
            // Barra inferior de navegación de pasos consciente de insets
            com.example.biodontos.core.designsystem.insets.BiodontosBottomActionBar(
                containerColor = BiodontosWhite,
                shadowElevation = 8.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.currentStep > 1) {
                        OutlinedButton(
                            onClick = { onEvent(ClinicalAdmissionEvent.PreviousStep) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Anterior", color = BiodontosDark)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = { onEvent(ClinicalAdmissionEvent.NextStep) },
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.currentStep == 7) Esmeralda else BiodontosDark
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = BiodontosWhite,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (state.currentStep == 7) "Finalizar Admisión" else "Guardar y Continuar",
                                color = BiodontosWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .background(BiodontosLightGrey)
        ) {
            // Indicador Horizontal de Pasos
            StepProgressHeader(
                currentStep = state.currentStep,
                onStepClicked = { step -> onEvent(ClinicalAdmissionEvent.JumpToStep(step)) }
            )

            // Mensaje de Error
            state.errorMessage?.let { errorMsg ->
                Surface(
                    color = BiodontosErrorBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚠ $errorMsg",
                        color = BiodontosErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Contenido Dinámico por Paso
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                when (state.currentStep) {
                    1 -> item { Step1IdentificationContent(state, onEvent) }
                    2 -> item { Step2AnamnesisContent(state, onEvent) }
                    3 -> item { Step3OdontogramContent(state, onEvent) }
                    4 -> item { Step4PeriodontogramContent(state, onEvent) }
                    5 -> item { Step5TreatmentConsentContent(state, onEvent) }
                    6 -> item { Step6EvolutionMaterialsContent(state, onEvent) }
                    7 -> item { Step7NextAppointmentContent(state, onEvent) }
                }
            }
        }
    }
}

private fun getStepTitle(step: Int): String = when (step) {
    1 -> "Identificación del Paciente"
    2 -> "Anamnesis (20 Preguntas)"
    3 -> "Odontograma FDI"
    4 -> "Periodontograma de 6 Sitios"
    5 -> "Plan de Tratamiento & Consentimiento"
    6 -> "Evolución, Costos & INVIMA"
    7 -> "Agendar Próxima Cita"
    else -> ""
}

@Composable
private fun StepProgressHeader(
    currentStep: Int,
    onStepClicked: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(BiodontosWhite)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items((1..7).toList()) { step ->
            val isActive = step == currentStep
            val isCompleted = step < currentStep

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                    isActive -> BiodontosDark
                    isCompleted -> Esmeralda.copy(alpha = 0.15f)
                    else -> BiodontosInputBg
                },
                modifier = Modifier.clickable { onStepClicked(step) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> BiodontosWhite
                                    isCompleted -> Esmeralda
                                    else -> BiodontosBorder
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = BiodontosWhite,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Text(
                                text = step.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) BiodontosDark else BiodontosWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = when (step) {
                            1 -> "Datos"
                            2 -> "Anamnesis"
                            3 -> "Odonto"
                            4 -> "Periodonto"
                            5 -> "Plan/Firma"
                            6 -> "Evolución"
                            7 -> "Cita"
                            else -> ""
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isActive -> BiodontosWhite
                            isCompleted -> Esmeralda
                            else -> BiodontosGreyText
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// PASO 1: IDENTIFICACIÓN DEL PACIENTE
// ============================================================================
@Composable
private fun Step1IdentificationContent(
    state: ClinicalAdmissionState,
    onEvent: (ClinicalAdmissionEvent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Información Personal y Demográfica",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )

            // DNI
            OutlinedTextField(
                value = state.dniCedula,
                onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateDni(it)) },
                label = { Text("Número de Documento (DNI/Cédula) *") },
                isError = state.dniError != null,
                supportingText = state.dniError?.let { { Text(it, color = BiodontosErrorRed) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // Nombres y Apellidos
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateFirstName(it)) },
                    label = { Text("Nombres *") },
                    isError = state.nameError != null,
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.firstLastName,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateFirstLastName(it)) },
                    label = { Text("1° Apellido *") },
                    isError = state.nameError != null,
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.secondLastName,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateSecondLastName(it)) },
                    label = { Text("2° Apellido") },
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.birthDate,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateBirthDate(it)) },
                    label = { Text("Fecha Nac. (AAAA-MM-DD)") },
                    placeholder = { Text("2000-01-01") },
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Indicador de Dentición calculada
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SlateLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Dentición sugerida por edad: ${state.dentitionType.name}",
                    fontSize = 12.sp,
                    color = Zafiro,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.birthPlace,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateBirthPlace(it)) },
                    label = { Text("Lugar de Nacimiento") },
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.eps,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateEps(it)) },
                    label = { Text("EPS / Aseguradora") },
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Grupo Sanguíneo y RH
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.bloodGroup,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateBloodGroup(it, state.rhFactor)) },
                    label = { Text("Grupo Sang.") },
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.rhFactor,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateBloodGroup(state.bloodGroup, it)) },
                    label = { Text("Factor RH") },
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdatePhone(it)) },
                    label = { Text("Teléfono *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(2f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = state.email,
                onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateEmail(it)) },
                label = { Text("Correo Electrónico") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.address,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateAddress(it)) },
                    label = { Text("Dirección de Residencia") },
                    modifier = Modifier.weight(1.3f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.locality,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateLocality(it)) },
                    label = { Text("Localidad / Barrio") },
                    placeholder = { Text("Ej: Suba, Kennedy...") },
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Alerta visual médica si el paciente es menor de 18 años
            if (state.isMinor) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Paciente menor de edad: se sugiere registrar acudiente o representante legal para fines médico-legales y consentimiento informado.",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            // Tarjeta de Acudiente / Acompañante
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateLight.copy(alpha = 0.45f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = BiodontosDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Acudiente o Acompañante",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BiodontosDark
                            )
                        }
                        Switch(
                            checked = state.hasGuardian,
                            onCheckedChange = { onEvent(ClinicalAdmissionEvent.ToggleHasGuardian(it)) }
                        )
                    }

                    AnimatedVisibility(visible = state.hasGuardian) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = state.guardianRelationship,
                                onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateGuardianRelationship(it)) },
                                label = { Text("Parentesco o Relación *") },
                                placeholder = { Text("Ej: Madre, Padre, Tutor Legal...") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = biodontosTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = state.guardianFirstName,
                                    onValueChange = { firstName ->
                                        onEvent(
                                            ClinicalAdmissionEvent.UpdateGuardian(
                                                relationship = state.guardianRelationship,
                                                firstName = firstName,
                                                lastName = state.guardianLastName,
                                                phone = state.guardianPhone,
                                                address = state.guardianAddress,
                                                locality = state.guardianLocality
                                            )
                                        )
                                    },
                                    label = { Text("Nombres Acudiente *") },
                                    modifier = Modifier.weight(1f),
                                    colors = biodontosTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = state.guardianLastName,
                                    onValueChange = { lastName ->
                                        onEvent(
                                            ClinicalAdmissionEvent.UpdateGuardian(
                                                relationship = state.guardianRelationship,
                                                firstName = state.guardianFirstName,
                                                lastName = lastName,
                                                phone = state.guardianPhone,
                                                address = state.guardianAddress,
                                                locality = state.guardianLocality
                                            )
                                        )
                                    },
                                    label = { Text("Apellidos Acudiente") },
                                    modifier = Modifier.weight(1f),
                                    colors = biodontosTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            OutlinedTextField(
                                value = state.guardianPhone,
                                onValueChange = { phone ->
                                    onEvent(
                                        ClinicalAdmissionEvent.UpdateGuardian(
                                            relationship = state.guardianRelationship,
                                            firstName = state.guardianFirstName,
                                            lastName = state.guardianLastName,
                                            phone = phone,
                                            address = state.guardianAddress,
                                            locality = state.guardianLocality
                                        )
                                    )
                                },
                                label = { Text("Teléfono de Contacto del Acudiente") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                colors = biodontosTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Dirección de residencia del acudiente",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BiodontosGreyText
                                )
                                TextButton(onClick = { onEvent(ClinicalAdmissionEvent.CopyAddressToGuardian) }) {
                                    Text("📋 Misma del paciente", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = state.guardianAddress,
                                    onValueChange = { addr ->
                                        onEvent(
                                            ClinicalAdmissionEvent.UpdateGuardian(
                                                relationship = state.guardianRelationship,
                                                firstName = state.guardianFirstName,
                                                lastName = state.guardianLastName,
                                                phone = state.guardianPhone,
                                                address = addr,
                                                locality = state.guardianLocality
                                            )
                                        )
                                    },
                                    label = { Text("Dirección Acudiente") },
                                    modifier = Modifier.weight(1.3f),
                                    colors = biodontosTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = state.guardianLocality,
                                    onValueChange = { loc ->
                                        onEvent(ClinicalAdmissionEvent.UpdateGuardianLocality(loc))
                                    },
                                    label = { Text("Localidad Acudiente") },
                                    modifier = Modifier.weight(1f),
                                    colors = biodontosTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Motivo de Consulta
            Text(
                text = "Motivo de Consulta y Antecedentes",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = BiodontosDark
            )

            OutlinedTextField(
                value = state.consultationReason,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateAntecedents(
                            lastVisit = state.lastDentalVisit,
                            reason = it,
                            currentReason = state.currentConsultationReason,
                            illnessHistory = state.illnessHistory
                        )
                    )
                },
                label = { Text("Motivo de Consulta") },
                placeholder = { Text("Ej: Dolor en molar superior, limpieza dental...") },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.illnessHistory,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateAntecedents(
                            lastVisit = state.lastDentalVisit,
                            reason = state.consultationReason,
                            currentReason = state.currentConsultationReason,
                            illnessHistory = it
                        )
                    )
                },
                label = { Text("Historia de la Enfermedad Actual") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // Alergias Críticas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "¿Presenta Alergias Medicamentosas / Críticas?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Switch(
                    checked = state.hasCriticalAllergies,
                    onCheckedChange = {
                        onEvent(
                            ClinicalAdmissionEvent.UpdateAllergies(it, state.allergiesDescription)
                        )
                    }
                )
            }

            if (state.hasCriticalAllergies) {
                OutlinedTextField(
                    value = state.allergiesDescription,
                    onValueChange = {
                        onEvent(ClinicalAdmissionEvent.UpdateAllergies(true, it))
                    },
                    label = { Text("Detalle de Alergias (Penicilina, Látex, AINEs...)") },
                    isError = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

// ============================================================================
// PASO 2: ANAMNESIS (20 PREGUNTAS)
// ============================================================================
@Composable
private fun Step2AnamnesisContent(
    state: ClinicalAdmissionState,
    onEvent: (ClinicalAdmissionEvent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Cuestionario de Salud General (20 Preguntas)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Text(
                text = "Marque Sí / No según las respuestas del paciente para seguridad clínica.",
                fontSize = 12.sp,
                color = BiodontosGreyText
            )

            Divider(color = SlateLight)

            val questions = listOf(
                "tratamientoMedicoActual" to "¿Está bajo tratamiento médico actual?" to state.tratamientoMedicoActual,
                "tomaMedicamentos" to "¿Toma medicamentos habitualmente?" to state.tomaMedicamentos,
                "alergias" to "¿Tiene o ha tenido alergias?" to state.alergias,
                "cardiopatias" to "¿Sufre del corazón o cardiopatías?" to state.cardiopatias,
                "alteracionPresionArterial" to "¿Alteración en presión arterial (Hipo/Hipertensión)?" to state.alteracionPresionArterial,
                "embarazo" to "¿Embarazo o sospecha de embarazo?" to state.embarazo,
                "diabetes" to "¿Padece de diabetes?" to state.diabetes,
                "hepatitis" to "¿Ha tenido hepatitis?" to state.hepatitis,
                "irradiaciones" to "¿Ha recibido radiaciones o quimioterapia?" to state.irradiaciones,
                "discrasiasSanguineas" to "¿Trastornos de coagulación / discrasias?" to state.discrasiasSanguineas,
                "fiebreReumatica" to "¿Ha padecido fiebre reumática?" to state.fiebreReumatica,
                "enfermedadesRenales" to "¿Enfermedades renales?" to state.enfermedadesRenales,
                "inmunosupresion" to "¿Inmunosupresión o VIH?" to state.inmunosupresion,
                "trastornosEmocionales" to "¿Trastornos emocionales o depresión?" to state.trastornosEmocionales,
                "patologiaRespiratoria" to "¿Patología respiratoria (Asma / EPOC)?" to state.patologiaRespiratoria,
                "trastornosGastricos" to "¿Trastornos gástricos (Úlceras, reflujo)?" to state.trastornosGastricos,
                "epilepsia" to "¿Epilepsia o convulsiones?" to state.epilepsia,
                "cirugias" to "¿Ha sido intervenido quirúrgicamente?" to state.cirugias,
                "enfermedadesOrales" to "¿Enfermedades orales previas?" to state.enfermedadesOrales,
                "otrasAlteraciones" to "¿Otras alteraciones de salud?" to state.otrasAlteraciones
            )

            questions.forEach { (pair, isChecked) ->
                val (key, label) = pair
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isChecked) BiodontosErrorBg.copy(alpha = 0.5f) else BiodontosInputBg.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                        color = if (isChecked) Carmesi else BiodontosDark,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isChecked,
                        onCheckedChange = {
                            onEvent(ClinicalAdmissionEvent.ToggleAnamnesisQuestion(key, it))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = state.medicamentosActuales,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateAnamnesisObservations(
                            state.anamnesisObservaciones,
                            state.antecedentesFamiliares,
                            it
                        )
                    )
                },
                label = { Text("Medicamentos Actuales y Dosis") },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.antecedentesFamiliares,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateAnamnesisObservations(
                            state.anamnesisObservaciones,
                            it,
                            state.medicamentosActuales
                        )
                    )
                },
                label = { Text("Antecedentes Familiares Médicos") },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.anamnesisObservaciones,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateAnamnesisObservations(
                            it,
                            state.antecedentesFamiliares,
                            state.medicamentosActuales
                        )
                    )
                },
                label = { Text("Observaciones de Anamnesis") },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

// ============================================================================
// PASO 3: ODONTOGRAMA
// ============================================================================
@Composable
private fun Step3OdontogramContent(
    state: ClinicalAdmissionState,
    onEvent: (ClinicalAdmissionEvent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Odontograma Reactivo FDI",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = BiodontosDark
                )

                // Selector de Dentición
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DentitionType.entries.forEach { type ->
                        val selected = state.dentitionType == type
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) BiodontosDark else BiodontosInputBg,
                            modifier = Modifier.clickable {
                                onEvent(ClinicalAdmissionEvent.ChangeDentitionType(type))
                            }
                        ) {
                            Text(
                                text = when (type) {
                                    DentitionType.ADULT -> "Adulto"
                                    DentitionType.CHILD -> "Niño"
                                    DentitionType.MIXED -> "Mixta"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) BiodontosWhite else BiodontosDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "1. Elija la patología. 2. Toque las superficies (Vestibular, Oclusal, Mesial, Distal, Lingual).",
                fontSize = 12.sp,
                color = BiodontosGreyText
            )

            // Selector de Condición a Aplicar
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ToothCondition.entries) { condition ->
                    val isSelected = state.selectedConditionToApply == condition
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) condition.color.copy(alpha = 0.2f) else BiodontosInputBg,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, condition.color) else null,
                        modifier = Modifier.clickable {
                            onEvent(ClinicalAdmissionEvent.SelectConditionToApply(condition))
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(condition.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = condition.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rejilla de Dientes
            Text(
                text = "Arco Dental (Pieza seleccionada para detalle: ${state.selectedOdontogramTooth})",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Zafiro
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.availableFdiTeeth) { toothNum ->
                    val surfaces = state.teethSurfaces[toothNum] ?: emptyMap()
                    val isSelected = state.selectedOdontogramTooth == toothNum

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SlateLight else Color.Transparent)
                            .padding(4.dp)
                            .clickable {
                                onEvent(ClinicalAdmissionEvent.SelectOdontogramTooth(toothNum))
                            }
                    ) {
                        Text(
                            text = toothNum.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) Zafiro else BiodontosDark
                        )
                        InteractiveTooth(
                            toothNumber = toothNum,
                            surfaceStates = surfaces,
                            onSurfaceClicked = { surf ->
                                onEvent(ClinicalAdmissionEvent.ApplySurfaceCondition(toothNum, surf))
                            },
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// PASO 4: PERIODONTOGRAMA
// ============================================================================
@Composable
private fun Step4PeriodontogramContent(
    state: ClinicalAdmissionState,
    onEvent: (ClinicalAdmissionEvent) -> Unit
) {
    InteractivePeriodontogram(
        selectedToothNumber = state.selectedPeriodontalTooth,
        availableTeeth = state.availableFdiTeeth,
        siteMeasurements = state.siteMeasurements,
        onToothSelected = { tooth ->
            onEvent(ClinicalAdmissionEvent.SelectPeriodontalTooth(tooth))
        },
        onMeasurementChanged = { meas ->
            onEvent(ClinicalAdmissionEvent.UpdatePeriodontalMeasurement(meas))
        }
    )
}

// ============================================================================
// PASO 5: PLAN DE TRATAMIENTO & CONSENTIMIENTO INFORMADO
// ============================================================================
@Composable
private fun Step5TreatmentConsentContent(
    state: ClinicalAdmissionState,
    onEvent: (ClinicalAdmissionEvent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Diagnósticos y Plan de Tratamiento",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )

            OutlinedTextField(
                value = state.mainDiagnosis,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateDiagnoses(
                            main = it,
                            related = state.relatedDiagnoses,
                            exams = state.complementaryExams,
                            plan = state.treatmentPlanDescription
                        )
                    )
                },
                label = { Text("Diagnóstico Principal (DP - CIE-10) *") },
                placeholder = { Text("Ej: K02.1 Caries de la dentina") },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.relatedDiagnoses,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateDiagnoses(
                            main = state.mainDiagnosis,
                            related = it,
                            exams = state.complementaryExams,
                            plan = state.treatmentPlanDescription
                        )
                    )
                },
                label = { Text("Diagnósticos Relacionados (DR)") },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.treatmentPlanDescription,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateDiagnoses(
                            main = state.mainDiagnosis,
                            related = state.relatedDiagnoses,
                            exams = state.complementaryExams,
                            plan = it
                        )
                    )
                },
                label = { Text("Plan de Tratamiento Proyectado *") },
                placeholder = { Text("Operatoria en piezas 16, 26; profilaxis periodontal profunda...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            Divider(color = SlateLight)

            // Texto de Consentimiento Legal
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SlateLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Consentimiento Informado de Operatoria Dental (Res. 1995/1999)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BiodontosDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Declaro que se me han explicado los procedimientos odontológicos a realizar, los beneficios, riesgos previstos, alternativas de tratamiento y secuelas potenciales. Autorizo la realización del tratamiento y la administración de anestesia local si fuese requerida.",
                        fontSize = 11.sp,
                        color = BiodontosGreyText,
                        lineHeight = 16.sp
                    )
                }
            }

            OutlinedButton(
                onClick = { onEvent(ClinicalAdmissionEvent.ToggleOperativeConsentDialog(true)) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Zafiro),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Emitir Consentimiento Detallado de Operatoria (Por Diente / Recomendaciones)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Pad de Firma
            SignaturePad(
                title = "Firma del Paciente / Acudiente",
                subtitle = "Firme en el recuadro para validar el consentimiento legal.",
                currentSignature = state.consentPatientSignature,
                onSignatureConfirmed = { sig ->
                    onEvent(ClinicalAdmissionEvent.ConfirmConsentSignature(sig))
                }
            )
        }
    }
}

// ============================================================================
// PASO 6: EVOLUCIÓN, COSTOS & INVIMA
// ============================================================================
@Composable
private fun Step6EvolutionMaterialsContent(
    state: ClinicalAdmissionState,
    onEvent: (ClinicalAdmissionEvent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Evolución Clínica y Honorarios",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )

            OutlinedTextField(
                value = state.evolutionDescription,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateEvolution(
                            description = it,
                            cost = state.evolutionCost,
                            payment = state.evolutionPayment
                        )
                    )
                },
                label = { Text("Nota de Evolución del Procedimiento Realizado *") },
                placeholder = { Text("Se realiza apertura de cavidad, desinfección y obturación...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // Costos y Pagos
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.evolutionCost,
                    onValueChange = {
                        onEvent(
                            ClinicalAdmissionEvent.UpdateEvolution(
                                description = state.evolutionDescription,
                                cost = it,
                                payment = state.evolutionPayment
                            )
                        )
                    },
                    label = { Text("Costo Total ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = state.evolutionPayment,
                    onValueChange = {
                        onEvent(
                            ClinicalAdmissionEvent.UpdateEvolution(
                                description = state.evolutionDescription,
                                cost = state.evolutionCost,
                                payment = it
                            )
                        )
                    },
                    label = { Text("Abono / Pago ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Cálculo Automático de Saldo
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (state.evolutionBalance > 0.0) BiodontosErrorBg else Esmeralda.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saldo Pendiente por Pagar:",
                        fontWeight = FontWeight.Bold,
                        color = if (state.evolutionBalance > 0.0) Carmesi else Esmeralda
                    )
                    Text(
                        text = "$ ${String.format("%,.2f", state.evolutionBalance)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (state.evolutionBalance > 0.0) Carmesi else Esmeralda
                    )
                }
            }

            Divider(color = SlateLight)

            // Trazabilidad de Materiales INVIMA
            Text(
                text = "Trazabilidad de Materiales Utilizados (INVIMA)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = BiodontosDark
            )

            if (state.materialsCatalog.isNotEmpty()) {
                Text(
                    text = "Seleccione del inventario de clínica:",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.materialsCatalog) { mat ->
                        val isSelected = state.selectedCatalogMaterial?.id == mat.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BiodontosDark else BiodontosInputBg,
                            modifier = Modifier.clickable {
                                onEvent(ClinicalAdmissionEvent.SelectMaterialFromCatalog(mat))
                            }
                        ) {
                            Text(
                                text = mat.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BiodontosWhite else BiodontosDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                state.selectedCatalogMaterial?.let { selectedMat ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SlateLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Material: ${selectedMat.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = "Registro INVIMA: ${selectedMat.invimaRegistration}", fontSize = 11.sp)
                            Text(text = "Lote: ${selectedMat.batchLotNumber} | Vence: ${selectedMat.expirationDate}", fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { onEvent(ClinicalAdmissionEvent.AddMaterialUsed(1.0)) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Zafiro),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Registrar Uso en Evolución", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Lista de Materiales Anexados a la Evolución
            if (state.usedMaterials.isNotEmpty()) {
                Text(
                    text = "Materiales Anexados a esta Consulta:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Zafiro
                )
                state.usedMaterials.forEach { used ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BiodontosInputBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = used.materialName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = "INVIMA: ${used.invimaSnapshot} | Lote: ${used.lotNumberSnapshot}", fontSize = 10.sp, color = BiodontosGreyText)
                        }
                        IconButton(onClick = { onEvent(ClinicalAdmissionEvent.RemoveMaterialUsed(used.materialId)) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Carmesi, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Firma de Evolución
            SignaturePad(
                title = "Firma del Paciente en Evolución",
                subtitle = "Conformidad con el procedimiento realizado.",
                currentSignature = state.evolutionPatientSignature,
                onSignatureConfirmed = { sig ->
                    onEvent(ClinicalAdmissionEvent.ConfirmEvolutionSignature(sig))
                }
            )

            // Cierre Médico-Legal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "¿Cerrar Historia Clínica Médicamente al terminar?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = state.closeRecordOnFinish,
                    onCheckedChange = {
                        onEvent(ClinicalAdmissionEvent.ToggleCloseRecord(it, state.closureSummary))
                    }
                )
            }
        }
    }
}

// ============================================================================
// PASO 7: AGENDAR PRÓXIMA CITA
// ============================================================================
@Composable
private fun Step7NextAppointmentContent(
    state: ClinicalAdmissionState,
    onEvent: (ClinicalAdmissionEvent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Agendamiento de Próxima Cita de Control",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Text(
                text = "Vincule la siguiente cita de seguimiento a esta evolución clínica.",
                fontSize = 12.sp,
                color = BiodontosGreyText
            )

            OutlinedTextField(
                value = state.nextAppointmentDate,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateNextAppointment(
                            date = it,
                            time = state.nextAppointmentTime,
                            reason = state.nextAppointmentReason
                        )
                    )
                },
                label = { Text("Fecha de Próxima Cita (AAAA-MM-DD)") },
                placeholder = { Text("2026-10-25") },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.nextAppointmentTime,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateNextAppointment(
                            date = state.nextAppointmentDate,
                            time = it,
                            reason = state.nextAppointmentReason
                        )
                    )
                },
                label = { Text("Hora de la Cita (HH:MM)") },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.nextAppointmentReason,
                onValueChange = {
                    onEvent(
                        ClinicalAdmissionEvent.UpdateNextAppointment(
                            date = state.nextAppointmentDate,
                            time = state.nextAppointmentTime,
                            reason = it
                        )
                    )
                },
                label = { Text("Motivo de Control o Procedimiento Programado") },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Esmeralda.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "✓ Resumen del Ingreso Clínico Completo",
                        fontWeight = FontWeight.Bold,
                        color = Esmeralda,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paciente: ${state.firstName} ${state.firstLastName} (DNI: ${state.dniCedula})\n" +
                                "Dentición: ${state.dentitionType.name} | Anamnesis: 20 puntos diligenciados\n" +
                                "Odontograma y Periodontograma registrados.\n" +
                                "Evolución y firmas anexadas correctamente.",
                        fontSize = 12.sp,
                        color = BiodontosDark,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClinicalAdmissionLoadingPreview() {
    DentalTheme {
        ClinicalAdmissionContent(
            state = ClinicalAdmissionState(
                currentStep = 1,
                isLoading = true
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClinicalAdmissionSuccessPreview() {
    DentalTheme {
        ClinicalAdmissionContent(
            state = ClinicalAdmissionState(
                currentStep = 1,
                dniCedula = "1020304050",
                firstName = "María",
                firstLastName = "Gómez",
                secondLastName = "Pérez",
                birthDate = "1995-04-12",
                phone = "3001234567",
                email = "maria.gomez@email.com",
                consultationReason = "Valoración general y profilaxis"
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClinicalAdmissionErrorPreview() {
    DentalTheme {
        ClinicalAdmissionContent(
            state = ClinicalAdmissionState(
                currentStep = 1,
                dniError = "El documento es obligatorio",
                errorMessage = "Complete los campos obligatorios antes de continuar."
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
