package com.example.biodontos.presentation.doctor.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biodontos.core.designsystem.*
import java.io.ByteArrayOutputStream

@Composable
fun DoctorProfileScreen(
    viewModel: DoctorProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DoctorProfileEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                DoctorProfileEffect.NavigateToLogin -> {
                    onNavigateToLogin()
                }
            }
        }
    }

    DoctorProfileContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@VisibleForTesting
@Composable
fun DoctorProfileContent(
    state: DoctorProfileState,
    onEvent: (DoctorProfileEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val dataUri = ImageCompressor.compressUriToBase64(context, it, maxDimension = 300, quality = 80)
            if (dataUri != null) {
                onEvent(DoctorProfileEvent.UpdatePhotoUrl(dataUri))
            } else {
                Toast.makeText(context, "No se pudo procesar la imagen seleccionada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mi Cuenta Profesional",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateBack()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = BiodontosDark
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(DoctorProfileEvent.LoadProfile)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = BiodontosDark
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(DoctorProfileEvent.RequestLogout)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = Carmesi
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BiodontosWhite)
            )
        },
        containerColor = BiodontosLightGrey
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BiodontosScreenGradient)
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner de error si existe
                state.errorMessage?.let { errorMsg ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BiodontosErrorBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = errorMsg,
                                    color = BiodontosErrorRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onEvent(DoctorProfileEvent.DismissError) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar mensaje de error",
                                        tint = BiodontosErrorRed
                                    )
                                }
                            }
                        }
                    }
                }

                // Banner de éxito si existe
                state.successMessage?.let { successMsg ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = successMsg,
                                color = StatusConfirmed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                if (state.isLoading && state.doctor == null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Zafiro)
                        }
                    }
                } else {
                    // Sección 1: Cabecera con Avatar interactivo
                    item {
                        DoctorAvatarHeaderSection(
                            photoUrl = state.photoUrl,
                            firstName = state.firstName,
                            lastName = state.lastName,
                            specialty = state.specialty,
                            licenseNumber = state.licenseNumber,
                            isSavingPhoto = state.isSavingProfile,
                            onPickPhotoClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }

                    // Sección 2: Información Profesional (CRUD)
                    item {
                        DoctorProfessionalInfoCard(
                            state = state,
                            onEvent = onEvent
                        )
                    }

                    // Sección 3: Seguridad y Cuenta
                    item {
                        DoctorSecurityCard(
                            state = state,
                            onEvent = onEvent
                        )
                    }
                }
            }

            // Diálogo de confirmación para cerrar sesión
            if (state.showLogoutConfirmation) {
                AlertDialog(
                    onDismissRequest = { onEvent(DoctorProfileEvent.DismissLogoutDialog) },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = BiodontosWhite,
                    title = {
                        Text(
                            text = "Cerrar Sesión",
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    },
                    text = {
                        Text(
                            text = "¿Está seguro de que desea cerrar la sesión de su cuenta profesional en este dispositivo?",
                            color = BiodontosGreyText,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEvent(DoctorProfileEvent.ConfirmLogout)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Carmesi),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Cerrar Sesión", color = BiodontosWhite, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                onEvent(DoctorProfileEvent.DismissLogoutDialog)
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Cancelar", color = BiodontosDark)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DoctorAvatarHeaderSection(
    photoUrl: String?,
    firstName: String,
    lastName: String,
    specialty: String,
    licenseNumber: String,
    isSavingPhoto: Boolean,
    onPickPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        border = BorderStroke(1.dp, BiodontosBorder.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = remember(photoUrl) {
                    ImageCompressor.decodeBase64ToBitmap(photoUrl)
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto de perfil del doctor",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                    )
                } else {
                    DoctorInitialsAvatar(
                        firstName = firstName,
                        lastName = lastName,
                        size = 88.dp
                    )
                }

                if (isSavingPhoto) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BiodontosWhite,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Botón / Badge de selección de foto (Touch target garantizado >= 48.dp)
                Surface(
                    shape = CircleShape,
                    color = Zafiro,
                    contentColor = BiodontosWhite,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(48.dp)
                ) {
                    IconButton(
                        onClick = onPickPhotoClick,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Cambiar foto de perfil",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val fullName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            Text(
                text = if (fullName.isNotBlank()) "Dr. $fullName" else "Dr. Especialista",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                ),
                textAlign = TextAlign.Center
            )

            if (specialty.isNotBlank()) {
                Text(
                    text = specialty,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Zafiro,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
            }

            if (licenseNumber.isNotBlank()) {
                Text(
                    text = "Reg. Profesional: $licenseNumber",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BiodontosGreyText
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DoctorInitialsAvatar(
    firstName: String,
    lastName: String,
    size: Dp = 88.dp,
    modifier: Modifier = Modifier
) {
    val initials = buildString {
        firstName.trim().firstOrNull()?.let { append(it.uppercaseChar()) }
        lastName.trim().firstOrNull()?.let { append(it.uppercaseChar()) }
    }.ifEmpty { "DR" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Zafiro),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = BiodontosWhite,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp
        )
    }
}

@Composable
fun DoctorProfessionalInfoCard(
    state: DoctorProfileState,
    onEvent: (DoctorProfileEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        border = BorderStroke(1.dp, BiodontosBorder.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Zafiro
                )
                Text(
                    text = "Información Profesional",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                )
            }

            OutlinedTextField(
                value = state.firstName,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateFirstName(it)) },
                label = { Text("Nombre") },
                placeholder = { Text("Ej. Carlos") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.lastName,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateLastName(it)) },
                label = { Text("Apellidos") },
                placeholder = { Text("Ej. Mendoza Pérez") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.specialty,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateSpecialty(it)) },
                label = { Text("Especialidad Odontológica") },
                placeholder = { Text("Ej. Ortodoncia & Estética") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.licenseNumber,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateLicenseNumber(it)) },
                label = { Text("Tarjeta / Cédula Profesional") },
                placeholder = { Text("Ej. OD-20381") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.phone,
                onValueChange = { onEvent(DoctorProfileEvent.UpdatePhone(it)) },
                label = { Text("Teléfono de Contacto") },
                placeholder = { Text("Ej. +57 300 123 4567") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.bio,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateBio(it)) },
                label = { Text("Biografía / Perfil Clínico") },
                placeholder = { Text("Resumen de experiencia, enfoque clínico y certificaciones...") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.email,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Correo Electrónico (Cuenta de acceso)") },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(DoctorProfileEvent.SaveProfile)
                },
                enabled = !state.isSavingProfile,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Zafiro),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                if (state.isSavingProfile) {
                    CircularProgressIndicator(
                        color = BiodontosWhite,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardar Cambios Profesionales",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DoctorSecurityCard(
    state: DoctorProfileState,
    onEvent: (DoctorProfileEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        border = BorderStroke(1.dp, BiodontosBorder.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Zafiro
                )
                Text(
                    text = "Seguridad & Cuenta",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                )
            }

            Text(
                text = "Cambiar Contraseña",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BiodontosDark
                )
            )

            OutlinedTextField(
                value = state.newPassword,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateNewPassword(it)) },
                label = { Text("Nueva Contraseña") },
                placeholder = { Text("Mínimo 6 caracteres") },
                singleLine = true,
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(
                        onClick = { onEvent(DoctorProfileEvent.TogglePasswordVisibility) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = if (state.isPasswordVisible) "Ocultar" else "Ver",
                            color = BiodontosDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateConfirmPassword(it)) },
                label = { Text("Confirmar Nueva Contraseña") },
                placeholder = { Text("Repita la nueva contraseña") },
                singleLine = true,
                visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(
                        onClick = { onEvent(DoctorProfileEvent.ToggleConfirmPasswordVisibility) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = if (state.isConfirmPasswordVisible) "Ocultar" else "Ver",
                            color = BiodontosDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = biodontosTextFieldColors()
            )

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(DoctorProfileEvent.SubmitPasswordChange)
                },
                enabled = !state.isUpdatingPassword,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                if (state.isUpdatingPassword) {
                    CircularProgressIndicator(
                        color = BiodontosWhite,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Actualizar Contraseña",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = BiodontosBorder.copy(alpha = 0.3f)
            )

            Text(
                text = "Restablecimiento por Correo",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BiodontosDark
                )
            )

            Text(
                text = "Enviaremos un enlace seguro a ${state.email.ifBlank { "su correo institucional" }} para restablecer su clave de acceso.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BiodontosGreyText
                )
            )

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(DoctorProfileEvent.SendPasswordResetEmail)
                },
                enabled = !state.isSendingResetEmail,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                if (state.isSendingResetEmail) {
                    CircularProgressIndicator(
                        color = Zafiro,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enviar Enlace de Recuperación",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = BiodontosBorder.copy(alpha = 0.3f)
            )

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(DoctorProfileEvent.RequestLogout)
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Carmesi),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Carmesi),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Carmesi
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cerrar Sesión Profesional",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Carmesi
                )
            }
        }
    }
}

object ImageCompressor {
    fun compressUriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 300,
        quality: Int = 80
    ): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return null

            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > height) {
                if (width > maxDimension) maxDimension.toFloat() / width else 1.0f
            } else {
                if (height > maxDimension) maxDimension.toFloat() / height else 1.0f
            }

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBase64ToBitmap(dataUri: String?): Bitmap? {
        if (dataUri.isNullOrBlank()) return null
        return try {
            val base64Part = if (dataUri.contains(",")) {
                dataUri.substringAfter(",")
            } else {
                dataUri
            }
            val decodedBytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
