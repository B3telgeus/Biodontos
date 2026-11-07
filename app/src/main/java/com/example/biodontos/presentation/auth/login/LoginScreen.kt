package com.example.biodontos.presentation.auth.login

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.biodontos.core.designsystem.*

@VisibleForTesting
@Composable
fun LoginScreen(
    state: LoginState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val isTabletLandscape = configuration.screenWidthDp > 700

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFE2E4E8),
            BiodontosDark.copy(alpha = 0.15f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Atrás",
                tint = BiodontosDark
            )
        }

        if (isTabletLandscape) {
            // Tablet Landscape Two-Pane Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding()
                    .padding(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Branding Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "¡Hola!",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = BiodontosDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inicia Sesión",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tu salud dental, organizada fácilmente con Biodontos.",
                        fontSize = 18.sp,
                        color = BiodontosGreyText,
                        lineHeight = 24.sp
                    )
                }

                // Right Card Form
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoginFormCard(
                        state = state,
                        onEmailChange = onEmailChange,
                        onPasswordChange = onPasswordChange,
                        onTogglePasswordVisibility = onTogglePasswordVisibility,
                        onLoginClick = onLoginClick,
                        onForgotPasswordClick = onForgotPasswordClick,
                        onSignUpClick = onSignUpClick,
                        onDone = {
                            focusManager.clearFocus()
                            onLoginClick()
                        }
                    )
                }
            }
        } else {
            // Mobile / Portrait Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "BIODONTOS",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = BiodontosDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "¡Bienvenido de vuelta!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
                Spacer(modifier = Modifier.height(24.dp))
                LoginFormCard(
                    state = state,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    onLoginClick = onLoginClick,
                    onForgotPasswordClick = onForgotPasswordClick,
                    onSignUpClick = onSignUpClick,
                    onDone = {
                        focusManager.clearFocus()
                        onLoginClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun LoginFormCard(
    state: LoginState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onDone: () -> Unit
) {
    var isPasswordMode by rememberSaveable { mutableStateOf(false) }
    var hasInteracted by rememberSaveable { mutableStateOf(false) }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isPasswordMode) {
        if (hasInteracted) {
            kotlinx.coroutines.delay(50)
            if (isPasswordMode) {
                runCatching { passwordFocusRequester.requestFocus() }
                    .onFailure {
                        kotlinx.coroutines.delay(50)
                        runCatching { passwordFocusRequester.requestFocus() }
                    }
            } else {
                runCatching { emailFocusRequester.requestFocus() }
                    .onFailure {
                        kotlinx.coroutines.delay(50)
                        runCatching { emailFocusRequester.requestFocus() }
                    }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¡Bienvenido de vuelta!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (isPasswordMode) {
                val hasEmailError = state.emailError != null
                // Email como chip/píldora interactivo arriba para regresar con un solo toque
                Surface(
                    onClick = {
                        hasInteracted = true
                        isPasswordMode = false
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (hasEmailError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(
                        width = if (hasEmailError) 1.5.dp else 1.dp,
                        color = if (hasEmailError) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(bottom = if (hasEmailError) 4.dp else 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.email.ifBlank { "Toca para editar tu correo" },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar correo",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cambiar",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                }

                if (state.emailError != null) {
                    Text(
                        text = state.emailError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, bottom = 12.dp)
                    )
                }

                // Campo Contraseña en posición principal superior
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Contraseña",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        placeholder = { Text("Ingresa tu contraseña", color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = BiodontosDark
                            )
                        },
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
                        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = state.passwordError != null,
                        supportingText = state.passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onDone() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = biodontosTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester)
                    )

                    // Feedback visual en vivo de los dígitos/caracteres ingresados
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val passLen = state.password.length
                        Text(
                            text = "$passLen ${if (passLen == 1) "carácter" else "caracteres"}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val count = state.password.length
                            if (count == 0) {
                                Text(
                                    text = "Mín. 6 caracteres",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            } else {
                                val maxVisible = 10
                                val visible = count.coerceAtMost(maxVisible)
                                val dotColor = if (count >= 6) Color(0xFF81C784) else Color(0xFFFFB74D)

                                repeat(visible) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                                if (count > maxVisible) {
                                    Text(
                                        text = "+${count - maxVisible}",
                                        color = dotColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Email Input en posición principal
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Email",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        placeholder = { Text("Ingresa tu Email", color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = BiodontosDark
                            )
                        },
                        isError = state.emailError != null,
                        supportingText = state.emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                hasInteracted = true
                                isPasswordMode = true
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = biodontosTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(emailFocusRequester)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Contraseña",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        placeholder = { Text("Ingresa tu contraseña", color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = BiodontosDark
                            )
                        },
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
                        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = state.passwordError != null,
                        supportingText = state.passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onDone() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = biodontosTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                if (it.isFocused) {
                                    hasInteracted = true
                                    isPasswordMode = true
                                }
                            }
                    )
                }
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Iniciar Sesión Button
            Button(
                onClick = onLoginClick,
                enabled = !state.isLoading,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = BiodontosDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BiodontosDark,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Iniciar sesión",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Forgot password Link
            Text(
                text = "Olvidé mi contraseña",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onForgotPasswordClick() }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sign Up Link
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No tienes una cuenta? ",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Crea una cuenta",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { onSignUpClick() }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginLoadingPreview() {
    DentalTheme {
        LoginScreen(
            state = LoginState(
                email = "doctor@biodontos.com",
                password = "password123",
                isLoading = true
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onSignUpClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginSuccessPreview() {
    DentalTheme {
        LoginScreen(
            state = LoginState(
                email = "doctor@biodontos.com",
                password = "password123",
                isLoading = false
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onSignUpClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginErrorPreview() {
    DentalTheme {
        LoginScreen(
            state = LoginState(
                email = "correo-invalido",
                emailError = "Ingrese un correo electrónico válido",
                error = "Credenciales inválidas. Verifique su correo y contraseña."
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onSignUpClick = {}
        )
    }
}
