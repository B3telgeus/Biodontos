package com.example.biodontos.presentation.admission

import com.example.biodontos.R

import androidx.compose.ui.res.stringResource

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biodontos.core.designsystem.*
import com.example.biodontos.domain.model.*
import com.example.biodontos.presentation.components.LocalityDropdownField
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalAdmissionScreen(
    viewModel: ClinicalAdmissionViewModel,
    onNavigateBack: () -> Unit,
    onAdmissionFinished: (patientId: String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    ClinicalAdmissionContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onGoToDetail = onAdmissionFinished,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalAdmissionContent(
    state: ClinicalAdmissionState,
    onEvent: (ClinicalAdmissionEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onGoToDetail: (patientId: String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    if (state.completionResult != null) {
        AdmissionSuccessDialog(
            completion = state.completionResult,
            onGoToDetail = onGoToDetail
        )
    }

    Scaffold(
        modifier = Modifier.testTag(TestTags.Admission.ROOT),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.ui_clinical_admission_screen_1),
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark,
                            fontSize = 18.sp,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = getStepTitle(state.currentStep),
                            style = MaterialTheme.typography.bodySmall,
                            color = BiodontosGreyText
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag(TestTags.Admission.BACK_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ui_clinical_admission_screen_3),
                            tint = BiodontosDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BiodontosWhite)
            )
        },
        bottomBar = {
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag(TestTags.Admission.PREV_BUTTON)
                        ) {
                            Text(stringResource(R.string.ui_clinical_admission_screen_5), color = BiodontosDark)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = { onEvent(ClinicalAdmissionEvent.NextStep) },
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.currentStep == 2) Esmeralda else BiodontosDark
                        ),
                        modifier = Modifier.testTag(TestTags.Admission.NEXT_BUTTON)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = BiodontosWhite,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (state.currentStep == 2) "Finalizar Registro" else "Guardar y Continuar",
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
            StepProgressHeader(
                currentStep = state.currentStep,
                onStepClicked = { step -> onEvent(ClinicalAdmissionEvent.JumpToStep(step)) }
            )

            state.errorMessage?.let { errorMsg ->
                Surface(
                    color = BiodontosErrorBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag(TestTags.Admission.ERROR_BANNER)
                ) {
                    Text(
                        text = stringResource(R.string.ui_clinical_admission_screen_6, errorMsg),
                        color = BiodontosErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag(TestTags.Admission.STEP_LIST),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                when (state.currentStep) {
                    1 -> item(key = "step_1") { Step1IdentificationContent(state, onEvent) }
                    2 -> item(key = "step_2") { Step2AnamnesisContent(state, onEvent) }
                }
            }
        }
    }
}

private fun getStepTitle(step: Int): String = when (step) {
    1 -> "Identificación del Paciente"
    2 -> "Anamnesis Médica"
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
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .testTag(TestTags.Admission.STEP_HEADER_ROW),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items((1..AdmissionStep.MAX_STEP).toList(), key = { it }) { step ->
            val isActive = step == currentStep
            val isCompleted = step < currentStep

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                    isActive -> BiodontosDark
                    isCompleted -> Esmeralda.copy(alpha = 0.15f)
                    else -> BiodontosInputBg
                },
                modifier = Modifier
                    .clickable { onStepClicked(step) }
                    .testTag(TestTags.Admission.stepChip(step))
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
                            1 -> "Datos del Paciente"
                            2 -> "Anamnesis"
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
                text = stringResource(R.string.ui_clinical_admission_screen_7),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )

            OutlinedTextField(
                value = state.dniCedula,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 10) {
                        onEvent(ClinicalAdmissionEvent.UpdateDni(value = input))
                    }
                },
                label = { Text(stringResource(R.string.ui_clinical_admission_screen_8)) },
                isError = state.dniError != null,
                supportingText = state.dniError?.let { { Text(it, color = BiodontosErrorRed) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Admission.DNI_FIELD),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateFirstName(it)) },
                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_9)) },
                    isError = state.nameError != null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.Admission.FIRST_NAME_FIELD),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.firstLastName,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateFirstLastName(it)) },
                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_10)) },
                    isError = state.nameError != null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.Admission.FIRST_LAST_NAME_FIELD),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.secondLastName,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateSecondLastName(it)) },
                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_11)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.Admission.SECOND_LAST_NAME_FIELD),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.birthDate,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateBirthDate(it)) },
                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_12)) },
                    placeholder = { Text(stringResource(R.string.ui_clinical_admission_screen_13)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.Admission.BIRTH_DATE_FIELD),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SlateLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.ui_clinical_admission_screen_14, state.dentitionType.name),
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
                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_15)) },
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.eps,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateEps(it)) },
                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_16)) },
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.bloodGroup,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateBloodGroup(it, state.rhFactor)) },
                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_17)) },
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.rhFactor,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateBloodGroup(state.bloodGroup, it)) },
                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_18)) },
                    modifier = Modifier.weight(1f),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { onEvent(ClinicalAdmissionEvent.UpdatePhone(it)) },
                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_19)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .weight(2f)
                        .testTag(TestTags.Admission.PHONE_FIELD),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = state.email,
                onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateEmail(it)) },
                label = { Text(stringResource(R.string.ui_clinical_admission_screen_20)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Admission.EMAIL_FIELD),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = state.address,
                onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateAddress(it)) },
                label = { Text(stringResource(R.string.ui_clinical_admission_screen_21)) },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            LocalityDropdownField(
                selectedLocality = state.locality,
                onLocalityChange = { onEvent(ClinicalAdmissionEvent.UpdateLocality(it)) },
                label = stringResource(R.string.ui_clinical_admission_screen_22),
                dropdownTestTag = TestTags.Admission.LOCALITY_DROPDOWN,
                customInputTestTag = TestTags.Admission.LOCALITY_CUSTOM_FIELD,
                modifier = Modifier.fillMaxWidth()
            )

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
                            text = stringResource(R.string.ui_clinical_admission_screen_24),
                            fontSize = 12.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

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
                                text = stringResource(R.string.ui_clinical_admission_screen_25),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BiodontosDark
                            )
                        }
                        Switch(
                            checked = state.hasGuardian,
                            onCheckedChange = { onEvent(ClinicalAdmissionEvent.ToggleHasGuardian(it)) },
                            modifier = Modifier.testTag(TestTags.Admission.GUARDIAN_SWITCH)
                        )
                    }

                    AnimatedVisibility(visible = state.hasGuardian) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = state.guardianRelationship,
                                onValueChange = { onEvent(ClinicalAdmissionEvent.UpdateGuardianRelationship(it)) },
                                label = { Text(stringResource(R.string.ui_clinical_admission_screen_26)) },
                                placeholder = { Text(stringResource(R.string.ui_clinical_admission_screen_27)) },
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
                                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_28)) },
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
                                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_29)) },
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
                                label = { Text(stringResource(R.string.ui_clinical_admission_screen_30)) },
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
                                    text = stringResource(R.string.ui_clinical_admission_screen_31),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BiodontosGreyText
                                )
                                TextButton(onClick = { onEvent(ClinicalAdmissionEvent.CopyAddressToGuardian) }) {
                                    Text(stringResource(R.string.ui_clinical_admission_screen_32), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

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
                                label = { Text(stringResource(R.string.ui_clinical_admission_screen_33)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = biodontosTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            LocalityDropdownField(
                                selectedLocality = state.guardianLocality,
                                onLocalityChange = { loc ->
                                    onEvent(ClinicalAdmissionEvent.UpdateGuardianLocality(loc))
                                },
                                label = stringResource(R.string.ui_clinical_admission_screen_34),
                                dropdownTestTag = TestTags.Admission.GUARDIAN_LOCALITY_DROPDOWN,
                                customInputTestTag = TestTags.Admission.GUARDIAN_LOCALITY_CUSTOM_FIELD,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.ui_clinical_admission_screen_35),
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
                label = { Text(stringResource(R.string.ui_clinical_admission_screen_36)) },
                placeholder = { Text(stringResource(R.string.ui_clinical_admission_screen_37)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Admission.CONSULTATION_REASON_FIELD),
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
                label = { Text(stringResource(R.string.ui_clinical_admission_screen_38)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.ui_clinical_admission_screen_39),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Switch(
                    checked = state.hasCriticalAllergies,
                    onCheckedChange = {
                        onEvent(
                            ClinicalAdmissionEvent.UpdateAllergies(it, state.allergiesDescription)
                        )
                    },
                    modifier = Modifier.testTag(TestTags.Admission.ALLERGIES_SWITCH)
                )
            }

            if (state.hasCriticalAllergies) {
                OutlinedTextField(
                    value = state.allergiesDescription,
                    onValueChange = {
                        onEvent(ClinicalAdmissionEvent.UpdateAllergies(true, it))
                    },
                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_40)) },
                    isError = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.Admission.ALLERGIES_DESC_FIELD),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

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
                text = stringResource(R.string.ui_clinical_admission_screen_41),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = BiodontosDark
            )
            Text(
                text = stringResource(R.string.ui_clinical_admission_screen_42),
                fontSize = 12.sp,
                color = BiodontosGreyText
            )

            HorizontalDivider(color = SlateLight)

            AnamnesisCondition.entries.forEach { cond ->
                val isChecked = cond in state.activeAnamnesisConditions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isChecked) BiodontosErrorBg.copy(alpha = 0.5f) else BiodontosInputBg.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = cond.question,
                            fontSize = 13.sp,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                            color = if (isChecked) Carmesi else BiodontosDark,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isChecked,
                            onCheckedChange = {
                                onEvent(ClinicalAdmissionEvent.ToggleAnamnesisQuestion(cond.key, it))
                            },
                            modifier = Modifier.testTag(TestTags.Admission.anamnesisSwitch(cond.key))
                        )
                    }

                    AnimatedVisibility(visible = isChecked) {
                        OutlinedTextField(
                            value = state.anamnesisOptionNotes[cond.key].orEmpty(),
                            onValueChange = { note ->
                                onEvent(ClinicalAdmissionEvent.UpdateAnamnesisOptionNote(cond.key, note))
                            },
                            label = { Text("Nota / Detalles") },
                            placeholder = { Text(cond.noteHint) },
                            minLines = 1,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .testTag(TestTags.Admission.anamnesisNoteField(cond.key)),
                            colors = biodontosTextFieldColors(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
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
                label = { Text(stringResource(R.string.ui_clinical_admission_screen_43)) },
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
                label = { Text(stringResource(R.string.ui_clinical_admission_screen_44)) },
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
                label = { Text(stringResource(R.string.ui_clinical_admission_screen_45)) },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun AdmissionSuccessDialog(
    completion: AdmissionCompletionResult,
    onGoToDetail: (patientId: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onGoToDetail(completion.patientId) },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        modifier = Modifier.testTag(TestTags.Admission.SUCCESS_DIALOG),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Esmeralda.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Esmeralda,
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                text = "¡Registro Exitoso!",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BiodontosDark,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = completion.patientFullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BiodontosDark,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.testTag(TestTags.Admission.SUCCESS_DIALOG_PATIENT_NAME)
                )
                Text(
                    text = "Documento: ${completion.dniCedula}",
                    fontSize = 13.sp,
                    color = BiodontosGreyText,
                    modifier = Modifier.testTag(TestTags.Admission.SUCCESS_DIALOG_DNI)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Esmeralda.copy(alpha = 0.12f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "HISTORIA CLÍNICA APERTURADA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Esmeralda,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "El paciente y su anamnesis han sido registrados en el sistema. Puede continuar con el odontograma, plan de tratamiento o evoluciones desde su ficha clínica.",
                    fontSize = 12.sp,
                    color = BiodontosGreyText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGoToDetail(completion.patientId) },
                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Admission.SUCCESS_DIALOG_GO_TO_DETAIL_BUTTON)
            ) {
                Text(
                    text = "Ir a la Ficha del Paciente",
                    color = BiodontosWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
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
