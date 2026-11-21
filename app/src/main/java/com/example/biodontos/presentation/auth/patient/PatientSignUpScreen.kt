package com.example.biodontos.presentation.auth.patient

import com.example.biodontos.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientSignUpScreen(
    state: PatientSignUpState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onDniChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFE5E7EB),
            BiodontosDark.copy(alpha = 0.2f)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ui_patient_sign_up_screen_145),
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(TestTags.Auth.PATIENT_SIGNUP_BACK_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ui_doctor_registration_screen_110),
                            tint = BiodontosDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier
            .background(backgroundBrush)
            .testTag(TestTags.Auth.PATIENT_SIGNUP_ROOT)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 2, 3).forEachIndexed { index, stepNum ->
                            val isActive = state.currentStep == stepNum
                            val isCompleted = state.currentStep > stepNum
                            Box(
                                modifier = Modifier
                                    .size(if (isActive) 12.dp else 10.dp)
                                    .background(
                                        color = when {
                                            isActive -> BiodontosDark
                                            isCompleted -> Esmeralda
                                            else -> BiodontosGrayLight
                                        },
                                        shape = CircleShape
                                    )
                            )
                            if (index < 2) {
                                Spacer(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(2.dp)
                                        .background(BiodontosGrayLight)
                                )
                            }
                        }
                    }

                    when (state.currentStep) {
                        1 -> {
                            Text(
                                text = stringResource(R.string.ui_patient_sign_up_screen_146),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                            Text(
                                text = stringResource(R.string.ui_patient_sign_up_screen_147),
                                fontSize = 14.sp,
                                color = BiodontosGreyText
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = BiodontosGreyText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.ui_patient_sign_up_screen_148),
                                        fontSize = 12.sp,
                                        color = BiodontosGreyText
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.firstName,
                                    onValueChange = onFirstNameChange,
                                    label = { Text(stringResource(R.string.ui_clinical_admission_screen_9)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(TestTags.Auth.PATIENT_SIGNUP_FIRST_NAME_FIELD),
                                    singleLine = true,
                                    isError = state.firstNameError != null,
                                    supportingText = state.firstNameError?.let { { Text(it) } },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = biodontosTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = state.lastName,
                                    onValueChange = onLastNameChange,
                                    label = { Text(stringResource(R.string.ui_doctor_registration_screen_113)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(TestTags.Auth.PATIENT_SIGNUP_LAST_NAME_FIELD),
                                    singleLine = true,
                                    isError = state.lastNameError != null,
                                    supportingText = state.lastNameError?.let { { Text(it) } },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = biodontosTextFieldColors()
                                )
                            }

                            OutlinedTextField(
                                value = state.dniCedula,
                                onValueChange = onDniChange,
                                label = { Text(stringResource(R.string.ui_patient_sign_up_screen_149)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.Auth.PATIENT_SIGNUP_DNI_FIELD),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                isError = state.dniError != null,
                                supportingText = state.dniError?.let { { Text(it) } },
                                shape = RoundedCornerShape(14.dp),
                                colors = biodontosTextFieldColors()
                            )

                            OutlinedTextField(
                                value = state.phone,
                                onValueChange = onPhoneChange,
                                label = { Text(stringResource(R.string.ui_patient_sign_up_screen_150)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.Auth.PATIENT_SIGNUP_PHONE_FIELD),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                                isError = state.phoneError != null,
                                supportingText = state.phoneError?.let { { Text(it) } },
                                shape = RoundedCornerShape(14.dp),
                                colors = biodontosTextFieldColors()
                            )
                        }

                        2 -> {
                            Text(
                                text = stringResource(R.string.ui_patient_sign_up_screen_151),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                            Text(
                                text = stringResource(R.string.ui_patient_sign_up_screen_152),
                                fontSize = 14.sp,
                                color = BiodontosGreyText
                            )

                            OutlinedTextField(
                                value = state.email,
                                onValueChange = onEmailChange,
                                label = { Text(stringResource(R.string.ui_patient_sign_up_screen_153)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.Auth.PATIENT_SIGNUP_EMAIL_FIELD),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                isError = state.emailError != null,
                                supportingText = state.emailError?.let { { Text(it) } },
                                shape = RoundedCornerShape(14.dp),
                                colors = biodontosTextFieldColors()
                            )

                            OutlinedTextField(
                                value = state.password,
                                onValueChange = onPasswordChange,
                                label = { Text(stringResource(R.string.ui_doctor_registration_screen_116)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.Auth.PATIENT_SIGNUP_PASSWORD_FIELD),
                                singleLine = true,
                                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    TextButton(
                                        onClick = onTogglePasswordVisibility,
                                        modifier = Modifier.testTag(TestTags.Auth.PATIENT_SIGNUP_PASSWORD_VISIBILITY_BUTTON)
                                    ) {
                                        Text(
                                            text = if (state.isPasswordVisible) "Ocultar" else "Ver",
                                            color = BiodontosDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                isError = state.passwordError != null,
                                supportingText = state.passwordError?.let { { Text(it) } },
                                shape = RoundedCornerShape(14.dp),
                                colors = biodontosTextFieldColors()
                            )

                            OutlinedTextField(
                                value = state.confirmPassword,
                                onValueChange = onConfirmPasswordChange,
                                label = { Text(stringResource(R.string.ui_doctor_registration_screen_117)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.Auth.PATIENT_SIGNUP_CONFIRM_PASSWORD_FIELD),
                                singleLine = true,
                                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                isError = state.confirmPasswordError != null,
                                supportingText = state.confirmPasswordError?.let { { Text(it) } },
                                shape = RoundedCornerShape(14.dp),
                                colors = biodontosTextFieldColors()
                            )
                        }

                        3 -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Esmeralda,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.ui_patient_sign_up_screen_154),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BiodontosDark
                                )
                            }
                            Text(
                                text = stringResource(R.string.ui_patient_sign_up_screen_155),
                                fontSize = 14.sp,
                                color = BiodontosGreyText
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                shape = RoundedCornerShape(16.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(R.string.ui_patient_sign_up_screen_156),
                                        fontSize = 13.sp,
                                        color = BiodontosDark,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Checkbox(
                                    checked = state.termsAccepted,
                                    onCheckedChange = onTermsAcceptedChange,
                                    colors = CheckboxDefaults.colors(checkedColor = BiodontosDark),
                                    modifier = Modifier.testTag(TestTags.Auth.PATIENT_SIGNUP_TERMS_CHECKBOX)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.ui_patient_sign_up_screen_157),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BiodontosDark
                                )
                            }
                        }
                    }

                    if (state.error != null) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.Auth.PATIENT_SIGNUP_ERROR_TEXT)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onBackClick,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BiodontosDark),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.currentStep == 1) "Cancelar" else "Atrás",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = onNextClick,
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BiodontosDark,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag(TestTags.Auth.PATIENT_SIGNUP_NEXT_BUTTON)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (state.currentStep == 3) "Crear Cuenta" else "Siguiente",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (state.currentStep == 3) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
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
private fun PatientSignUpLoadingPreview() {
    DentalTheme {
        PatientSignUpScreen(
            state = PatientSignUpState(
                currentStep = 1,
                isLoading = true
            ),
            onFirstNameChange = {},
            onLastNameChange = {},
            onDniChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTermsAcceptedChange = {},
            onTogglePasswordVisibility = {},
            onNextClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientSignUpSuccessPreview() {
    DentalTheme {
        PatientSignUpScreen(
            state = PatientSignUpState(
                currentStep = 1,
                firstName = "Laura",
                lastName = "Martínez",
                dniCedula = "1032456789",
                phone = "3004567890"
            ),
            onFirstNameChange = {},
            onLastNameChange = {},
            onDniChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTermsAcceptedChange = {},
            onTogglePasswordVisibility = {},
            onNextClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientSignUpErrorPreview() {
    DentalTheme {
        PatientSignUpScreen(
            state = PatientSignUpState(
                currentStep = 1,
                firstNameError = "El nombre es obligatorio",
                dniError = "El documento debe contener solo números",
                error = "Complete todos los campos obligatorios."
            ),
            onFirstNameChange = {},
            onLastNameChange = {},
            onDniChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTermsAcceptedChange = {},
            onTogglePasswordVisibility = {},
            onNextClick = {},
            onBackClick = {}
        )
    }
}
