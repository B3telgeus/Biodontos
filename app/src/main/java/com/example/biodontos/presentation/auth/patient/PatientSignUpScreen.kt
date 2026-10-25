package com.example.biodontos.presentation.auth.patient

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
                        text = "Registro de Paciente",
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                },
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
                    // --- STEP INDICATOR (Dots de progreso estilo Figma) ---
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
                            // --- PASO 1: INFORMACIÓN PERSONAL ---
                            Text(
                                text = "¡Vamos a crear tu perfil!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                            Text(
                                text = "Paso 1 de 3: Ingresa tus datos personales.",
                                fontSize = 14.sp,
                                color = BiodontosGreyText
                            )

                            // Notice Banner estilo Figma
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
                                        text = "Nos tomamos la seguridad de tus datos en serio. Tu perfil estará a salvo.",
                                        fontSize = 12.sp,
                                        color = BiodontosGreyText
                                    )
                                }
                            }

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

                            // DNI / Documento de Identidad
                            OutlinedTextField(
                                value = state.dniCedula,
                                onValueChange = onDniChange,
                                label = { Text("DNI / Cédula de Identidad *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                isError = state.dniError != null,
                                supportingText = state.dniError?.let { { Text(it) } },
                                shape = RoundedCornerShape(14.dp),
                                colors = biodontosTextFieldColors()
                            )

                            // Teléfono
                            OutlinedTextField(
                                value = state.phone,
                                onValueChange = onPhoneChange,
                                label = { Text("Teléfono Móvil (opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                                isError = state.phoneError != null,
                                supportingText = state.phoneError?.let { { Text(it) } },
                                shape = RoundedCornerShape(14.dp),
                                colors = biodontosTextFieldColors()
                            )
                        }

                        2 -> {
                            // --- PASO 2: CREDENCIALES DE ACCESO ---
                            Text(
                                text = "Tu llave de acceso",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                            Text(
                                text = "Paso 2 de 3: Configura el acceso con tu correo y contraseña segura.",
                                fontSize = 14.sp,
                                color = BiodontosGreyText
                            )

                            // Correo Electrónico
                            OutlinedTextField(
                                value = state.email,
                                onValueChange = onEmailChange,
                                label = { Text("Correo Electrónico *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                isError = state.emailError != null,
                                supportingText = state.emailError?.let { { Text(it) } },
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

                        3 -> {
                            // --- PASO 3: POLÍTICA Y TÉRMINOS (Figma 05_Registro_Paciente_Usuario2.png) ---
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Esmeralda,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Siempre protegeremos tu información",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BiodontosDark
                                )
                            }
                            Text(
                                text = "Paso 3 de 3: Revisa y acepta nuestra política de tratamiento de datos:",
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
                                        text = "Finalidad del tratamiento: Los datos personales recopilados (nombre, documento, correo, datos de contacto) serán utilizados exclusivamente para la gestión de agendas odontológicas, asignación de citas, historial clínico y notificaciones de atención en Biodontos.\n\nSeguridad y confidencialidad: Nos comprometemos a proteger tu información mediante protocolos de seguridad cifrados. Tus datos nunca serán compartidos con terceros con fines comerciales.",
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
                                    colors = CheckboxDefaults.colors(checkedColor = BiodontosDark)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Acepto los Términos de Servicio y la Política de Privacidad",
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- BARRA INFERIOR DE NAVEGACIÓN: [ <- ATRÁS ]  y  [ SIGUIENTE -> ] ---
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

                        // BOTÓN SIGUIENTE / FINALIZAR REGISTRO
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
                                    text = if (state.currentStep == 3) "Crear Cuenta" else "Siguiente",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
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
