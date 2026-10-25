package com.example.biodontos.presentation.auth.doctor

import androidx.annotation.VisibleForTesting
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
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
@VisibleForTesting
@Composable
fun DoctorRegistrationScreen(
    state: DoctorRegistrationState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onLicenseNumberChange: (String) -> Unit,
    onSpecialtyChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val specialties = listOf(
        "Odontología General",
        "Ortodoncia",
        "Endodoncia",
        "Periodoncia",
        "Odontopediatría",
        "Cirugía Maxilofacial"
    )
    var expandedSpecialty by remember { mutableStateOf(false) }

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
                title = { Text("Registro de Doctor", fontWeight = FontWeight.Bold, color = BiodontosDark) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = BiodontosDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(backgroundBrush)
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
                    // --- STEP INDICATOR ESTILO FIGMA ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 2).forEachIndexed { index, stepNum ->
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
                            if (index < 1) {
                                Spacer(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(2.dp)
                                        .background(BiodontosGrayLight)
                                )
                            }
                        }
                    }

                    when (state.currentStep) {
                        1 -> {
                            // --- PASO 1: IDENTIDAD Y ACCESO ---
                            Text(
                                text = "Identidad y Acceso",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                            Text(
                                text = "Paso 1 de 2: Datos personales y credenciales médicas de inicio.",
                                fontSize = 14.sp,
                                color = BiodontosGreyText
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Nombres y Apellidos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.firstName,
                                    onValueChange = onFirstNameChange,
                                    label = { Text("Nombres *") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    isError = state.firstNameError != null,
                                    supportingText = state.firstNameError?.let { { Text(it) } },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = biodontosTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = state.lastName,
                                    onValueChange = onLastNameChange,
                                    label = { Text("Apellidos *") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    isError = state.lastNameError != null,
                                    supportingText = state.lastNameError?.let { { Text(it) } },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = biodontosTextFieldColors()
                                )
                            }

                            // Email
                            OutlinedTextField(
                                value = state.email,
                                onValueChange = onEmailChange,
                                label = { Text("Correo Institucional / Profesional *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                isError = state.emailError != null,
                                supportingText = state.emailError?.let { { Text(it) } },
                                shape = RoundedCornerShape(14.dp),
                                colors = biodontosTextFieldColors()
                            )

                            // Teléfono
                            OutlinedTextField(
                                value = state.phone,
                                onValueChange = onPhoneChange,
                                label = { Text("Teléfono de Contacto (opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                isError = state.phoneError != null,
                                supportingText = state.phoneError?.let { { Text(it) } },
                                shape = RoundedCornerShape(14.dp),
                                colors = biodontosTextFieldColors()
                            )

                            // Contraseña
                            OutlinedTextField(
                                value = state.password,
                                onValueChange = onPasswordChange,
                                label = { Text("Contraseña (mín. 6 caracteres) *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    TextButton(onClick = onTogglePasswordVisibility) {
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

                            // Confirmar Contraseña
                            OutlinedTextField(
                                value = state.confirmPassword,
                                onValueChange = onConfirmPasswordChange,
                                label = { Text("Confirmar Contraseña *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                isError = state.confirmPasswordError != null,
                                supportingText = state.confirmPasswordError?.let { { Text(it) } },
                                shape = RoundedCornerShape(14.dp),
                                colors = biodontosTextFieldColors()
                            )
                        }

                        2 -> {
                            // --- PASO 2: PERFIL PROFESIONAL ---
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBox,
                                    contentDescription = null,
                                    tint = BiodontosDark,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Perfil Profesional",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BiodontosDark
                                )
                            }
                            Text(
                                text = "Paso 2 de 2: Cédula médica y especialidad odontológica.",
                                fontSize = 14.sp,
                                color = BiodontosGreyText
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Cédula Profesional
                            OutlinedTextField(
                                value = state.licenseNumber,
                                onValueChange = onLicenseNumberChange,
                                label = { Text("Cédula / Registro Médico Profesional *") },
                                placeholder = { Text("Ej: RM-4565345") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = state.licenseError != null,
                                supportingText = state.licenseError?.let { { Text(it) } },
                                shape = RoundedCornerShape(14.dp),
                                colors = biodontosTextFieldColors()
                            )

                            // Especialidad (Dropdown)
                            ExposedDropdownMenuBox(
                                expanded = expandedSpecialty,
                                onExpandedChange = { expandedSpecialty = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = state.specialty,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Especialidad Odontológica *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpecialty) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = biodontosTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedSpecialty,
                                    onDismissRequest = { expandedSpecialty = false }
                                ) {
                                    specialties.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                onSpecialtyChange(item)
                                                expandedSpecialty = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.error != null) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- BARRA INFERIOR DE NAVEGACIÓN: [ <- ATRÁS ]  y  [ CONTINUAR -> ] ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // BOTÓN RETROCEDER / ATRÁS
                        OutlinedButton(
                            onClick = onBackClick,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BiodontosDark),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.currentStep == 1) "Cancelar" else "Atrás",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // BOTÓN SIGUIENTE / CONTINUAR
                        Button(
                            onClick = onNextClick,
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BiodontosDark,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(54.dp)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (state.currentStep == 2) "Crear Cuenta" else "Continuar",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (state.currentStep == 2) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
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
private fun DoctorRegistrationLoadingPreview() {
    DentalTheme {
        DoctorRegistrationScreen(
            state = DoctorRegistrationState(
                currentStep = 1,
                isLoading = true
            ),
            onFirstNameChange = {},
            onLastNameChange = {},
            onLicenseNumberChange = {},
            onSpecialtyChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTogglePasswordVisibility = {},
            onNextClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorRegistrationSuccessPreview() {
    DentalTheme {
        DoctorRegistrationScreen(
            state = DoctorRegistrationState(
                currentStep = 1,
                firstName = "Carlos",
                lastName = "Restrepo",
                email = "dr.restrepo@biodontos.com",
                phone = "3109876543",
                password = "password123",
                confirmPassword = "password123"
            ),
            onFirstNameChange = {},
            onLastNameChange = {},
            onLicenseNumberChange = {},
            onSpecialtyChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTogglePasswordVisibility = {},
            onNextClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorRegistrationErrorPreview() {
    DentalTheme {
        DoctorRegistrationScreen(
            state = DoctorRegistrationState(
                currentStep = 1,
                firstNameError = "El nombre es obligatorio",
                emailError = "Formato de correo inválido",
                error = "Por favor corrija los errores marcados antes de continuar."
            ),
            onFirstNameChange = {},
            onLastNameChange = {},
            onLicenseNumberChange = {},
            onSpecialtyChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTogglePasswordVisibility = {},
            onNextClick = {},
            onBackClick = {}
        )
    }
}
