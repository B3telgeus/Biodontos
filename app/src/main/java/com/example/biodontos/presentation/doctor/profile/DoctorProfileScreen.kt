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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.biodontos.R
import com.example.biodontos.core.designsystem.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
@Composable
fun DoctorProfileContent(
    state: DoctorProfileState,
    onEvent: (DoctorProfileEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            onEvent(DoctorProfileEvent.UpdatePhotoUrl(it.toString()))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ui_doctor_profile_screen_308),
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
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(TestTags.Doctor.PROFILE_BACK_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ui_clinical_admission_screen_3),
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
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(TestTags.Doctor.PROFILE_REFRESH_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.ui_doctor_appointments_screen_186),
                            tint = BiodontosDark
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(DoctorProfileEvent.RequestLogout)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(TestTags.Doctor.PROFILE_TOP_LOGOUT_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.ui_doctor_profile_screen_309),
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
                .testTag(TestTags.Doctor.PROFILE_ROOT)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag(TestTags.Doctor.PROFILE_LIST),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.errorMessage?.let { errorMsg ->
                    item(key = "error_banner") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BiodontosErrorBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.Doctor.PROFILE_ERROR_BANNER)
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
                                        contentDescription = stringResource(R.string.ui_doctor_profile_screen_310),
                                        tint = BiodontosErrorRed
                                    )
                                }
                            }
                        }
                    }
                }

                state.successMessage?.let { successMsg ->
                    item(key = "success_banner") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.Doctor.PROFILE_SUCCESS_BANNER)
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
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag(TestTags.Doctor.PROFILE_LOADING),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Zafiro)
                        }
                    }
                } else {
                    item(key = "avatar_header") {
                        DoctorAvatarHeaderSection(
                            photoUrl = state.photoUrl,
                            firstName = state.firstName,
                            lastName = state.lastName,
                            specialty = state.specialty,
                            licenseNumber = state.licenseNumber,
                            clinicAddress = state.clinicAddress,
                            services = state.services,
                            isSavingPhoto = state.isSavingProfile,
                            onPickPhotoClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }

                    item(key = "professional_info") {
                        DoctorProfessionalInfoCard(
                            state = state,
                            onEvent = onEvent
                        )
                    }

                    item(key = "security_card") {
                        DoctorSecurityCard(
                            state = state,
                            onEvent = onEvent
                        )
                    }
                }
            }

            if (state.showLogoutConfirmation) {
                AlertDialog(
                    onDismissRequest = { onEvent(DoctorProfileEvent.DismissLogoutDialog) },
                    modifier = Modifier.testTag(TestTags.Doctor.PROFILE_LOGOUT_DIALOG),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = BiodontosWhite,
                    title = {
                        Text(
                            text = stringResource(R.string.ui_doctor_profile_screen_309),
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.ui_doctor_profile_screen_311),
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
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag(TestTags.Doctor.PROFILE_LOGOUT_CONFIRM_BUTTON)
                        ) {
                            Text(stringResource(R.string.ui_doctor_profile_screen_309), color = BiodontosWhite, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                onEvent(DoctorProfileEvent.DismissLogoutDialog)
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.ui_doctor_appointments_screen_191), color = BiodontosDark)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorAvatarHeaderSection(
    photoUrl: String?,
    firstName: String,
    lastName: String,
    specialty: String,
    licenseNumber: String,
    clinicAddress: String = "",
    services: String = "",
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
                DoctorAsyncAvatar(
                    photoUrl = photoUrl,
                    firstName = firstName,
                    lastName = lastName,
                    size = 88.dp
                )

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
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(TestTags.Doctor.PROFILE_PICK_PHOTO_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.ui_doctor_profile_screen_312),
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
                    text = stringResource(R.string.ui_doctor_profile_screen_313, licenseNumber),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BiodontosGreyText
                    ),
                    textAlign = TextAlign.Center
                )
            }

            if (clinicAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.ui_doctor_profile_screen_314),
                        tint = BiodontosDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = clinicAddress,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BiodontosDark,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            val servicesList = services.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (servicesList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    servicesList.take(3).forEach { service ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BiodontosInputBg,
                            border = BorderStroke(1.dp, BiodontosBorder.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = service,
                                fontSize = 11.sp,
                                color = BiodontosDark,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (servicesList.size > 3) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BiodontosInputBg
                        ) {
                            Text(
                                text = stringResource(R.string.ui_login_screen_135, servicesList.size - 3),
                                fontSize = 11.sp,
                                color = BiodontosGreyText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
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
                    text = stringResource(R.string.ui_doctor_profile_screen_315),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                )
            }

            OutlinedTextField(
                value = state.firstName,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateFirstName(it)) },
                label = { Text(stringResource(R.string.ui_doctor_profile_screen_316)) },
                placeholder = { Text(stringResource(R.string.ui_doctor_profile_screen_317)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Doctor.PROFILE_FIRST_NAME_FIELD),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.lastName,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateLastName(it)) },
                label = { Text(stringResource(R.string.ui_doctor_profile_screen_318)) },
                placeholder = { Text(stringResource(R.string.ui_doctor_profile_screen_319)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Doctor.PROFILE_LAST_NAME_FIELD),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.specialty,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateSpecialty(it)) },
                label = { Text(stringResource(R.string.ui_doctor_profile_screen_320)) },
                placeholder = { Text(stringResource(R.string.ui_doctor_profile_screen_321)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Doctor.PROFILE_SPECIALTY_FIELD),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.licenseNumber,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateLicenseNumber(it)) },
                label = { Text(stringResource(R.string.ui_doctor_profile_screen_322)) },
                placeholder = { Text(stringResource(R.string.ui_doctor_profile_screen_323)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Doctor.PROFILE_LICENSE_FIELD),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.phone,
                onValueChange = { onEvent(DoctorProfileEvent.UpdatePhone(it)) },
                label = { Text(stringResource(R.string.ui_doctor_profile_screen_324)) },
                placeholder = { Text(stringResource(R.string.ui_doctor_profile_screen_325)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Doctor.PROFILE_PHONE_FIELD),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.bio,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateBio(it)) },
                label = { Text(stringResource(R.string.ui_doctor_profile_screen_326)) },
                placeholder = { Text(stringResource(R.string.ui_doctor_profile_screen_327)) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Doctor.PROFILE_BIO_FIELD),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.clinicAddress,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateClinicAddress(it)) },
                label = { Text(stringResource(R.string.ui_doctor_profile_screen_314)) },
                placeholder = { Text(stringResource(R.string.ui_doctor_profile_screen_328)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Doctor.PROFILE_CLINIC_ADDRESS_FIELD),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.services,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateServices(it)) },
                label = { Text(stringResource(R.string.ui_doctor_profile_screen_329)) },
                placeholder = { Text(stringResource(R.string.ui_doctor_profile_screen_330)) },
                minLines = 2,
                maxLines = 4,
                supportingText = {
                    Text(stringResource(R.string.ui_doctor_profile_screen_331), fontSize = 11.sp, color = BiodontosGreyText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Doctor.PROFILE_SERVICES_FIELD),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.email,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(stringResource(R.string.ui_doctor_profile_screen_332)) },
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
                    .testTag(TestTags.Doctor.PROFILE_SAVE_BUTTON)
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
                        text = stringResource(R.string.ui_doctor_profile_screen_333),
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
                    text = stringResource(R.string.ui_doctor_profile_screen_334),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                )
            }

            Text(
                text = stringResource(R.string.ui_doctor_profile_screen_335),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BiodontosDark
                )
            )

            OutlinedTextField(
                value = state.newPassword,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateNewPassword(it)) },
                label = { Text(stringResource(R.string.ui_doctor_profile_screen_336)) },
                placeholder = { Text(stringResource(R.string.ui_doctor_profile_screen_337)) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Doctor.PROFILE_NEW_PASSWORD_FIELD),
                colors = biodontosTextFieldColors()
            )

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = { onEvent(DoctorProfileEvent.UpdateConfirmPassword(it)) },
                label = { Text(stringResource(R.string.ui_doctor_profile_screen_338)) },
                placeholder = { Text(stringResource(R.string.ui_doctor_profile_screen_339)) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.Doctor.PROFILE_CONFIRM_PASSWORD_FIELD),
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
                    .testTag(TestTags.Doctor.PROFILE_UPDATE_PASSWORD_BUTTON)
            ) {
                if (state.isUpdatingPassword) {
                    CircularProgressIndicator(
                        color = BiodontosWhite,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.ui_doctor_profile_screen_340),
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
                text = stringResource(R.string.ui_doctor_profile_screen_341),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BiodontosDark
                )
            )

            Text(
                text = stringResource(R.string.ui_doctor_profile_screen_342, state.email.ifBlank { "su correo institucional" }),
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
                    .testTag(TestTags.Doctor.PROFILE_RESET_EMAIL_BUTTON)
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
                        text = stringResource(R.string.ui_doctor_profile_screen_343),
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
                    .testTag(TestTags.Doctor.PROFILE_LOGOUT_BUTTON)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Carmesi
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ui_doctor_profile_screen_344),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Carmesi
                )
            }
        }
    }
}

@Composable
fun DoctorAsyncAvatar(
    photoUrl: String?,
    firstName: String,
    lastName: String,
    size: Dp = 88.dp,
    modifier: Modifier = Modifier
) {
    val cleanUrl = photoUrl?.trim().orEmpty()
    val isLegacyDataUri = cleanUrl.startsWith("data:image", ignoreCase = true)
    val isInspection = LocalInspectionMode.current

    val decodedBytes by produceState<ByteArray?>(initialValue = null, key1 = cleanUrl, key2 = isInspection) {
        value = if (!isInspection && isLegacyDataUri) {
            withContext(Dispatchers.Default) {
                ImageCompressor.decodeBase64ToBytes(cleanUrl)
            }
        } else {
            null
        }
    }

    val model: Any? = when {
        isInspection -> null
        cleanUrl.isEmpty() -> null
        isLegacyDataUri -> decodedBytes
        else -> cleanUrl
    }

    if (model != null && !isInspection) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = stringResource(R.string.cd_doctor_profile_photo),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            loading = {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(BiodontosInputBg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Zafiro,
                        modifier = Modifier.size(size * 0.32f),
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                DoctorInitialsAvatar(
                    firstName = firstName,
                    lastName = lastName,
                    size = size
                )
            }
        )
    } else {
        DoctorInitialsAvatar(
            firstName = firstName,
            lastName = lastName,
            size = size,
            modifier = modifier
        )
    }
}

object ImageCompressor {
    fun decodeBase64ToBytes(dataUri: String?): ByteArray? {
        if (dataUri.isNullOrBlank()) return null
        return try {
            val base64Part = if (dataUri.contains(",")) {
                dataUri.substringAfter(",")
            } else {
                dataUri
            }
            Base64.decode(base64Part.trim(), Base64.DEFAULT).takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}
