package com.example.biodontos.presentation.patientportal.doctors

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.core.designsystem.components.BiodontosPillButton
import com.example.biodontos.core.designsystem.components.PillButtonVariant
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.presentation.doctor.profile.DoctorInitialsAvatar
import com.example.biodontos.presentation.doctor.profile.ImageCompressor

@Composable
fun DoctorAvatar(
    photoUrl: String?,
    firstName: String,
    lastName: String,
    size: Dp = 54.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(photoUrl) {
        ImageCompressor.decodeBase64ToBitmap(photoUrl)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Foto del especialista",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
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


@Composable
fun DoctorSearchRoute(
    viewModel: DoctorSearchViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToBooking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DoctorSearchUiEffect.NavigateToDetail -> onNavigateToDetail(effect.doctorId)
                is DoctorSearchUiEffect.NavigateToBooking -> onNavigateToBooking(effect.doctorId)
            }
        }
    }

    DoctorSearchScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@VisibleForTesting
@Composable
fun DoctorSearchScreen(
    state: DoctorSearchUiState,
    onEvent: (DoctorSearchUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
    ) {
        // Título de la pantalla
        Text(
            text = "DOCTORES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = BiodontosGreyText,
            letterSpacing = 1.sp
        )
        Text(
            text = "Encuentra tu Especialista",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BiodontosDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Barra de búsqueda con estilo redondeado Figma
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onEvent(DoctorSearchUiEvent.QueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Buscar por nombre del especialista...",
                    color = BiodontosGreyText,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = BiodontosGreyText
                )
            },
            trailingIcon = {
                if (state.searchQuery.isNotBlank()) {
                    IconButton(onClick = { onEvent(DoctorSearchUiEvent.QueryChanged("")) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar búsqueda",
                            tint = BiodontosGreyText
                        )
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BiodontosWhite,
                unfocusedContainerColor = BiodontosInputBg,
                focusedBorderColor = BiodontosDark,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filtro horizontal por especialidades
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(state.specialties) { specialty ->
                val isSelected = specialty == state.selectedSpecialty
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) BiodontosDark else BiodontosWhite)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) BiodontosDark else BiodontosGreyText.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEvent(DoctorSearchUiEvent.SpecialtySelected(specialty))
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = specialty,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) BiodontosWhite else BiodontosDark
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        // Contenido principal: Grilla de doctores / Carga / Error / Vacío
        when {
            state.isLoading && state.doctors.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BiodontosDark)
                }
            }
            state.errorMessage != null && state.doctors.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = BiodontosDark,
                            fontSize = 14.sp
                        )
                        BiodontosPillButton(
                            text = "Reintentar",
                            onClick = { onEvent(DoctorSearchUiEvent.Retry) }
                        )
                    }
                }
            }
            state.doctors.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No encontramos especialistas con ese criterio.",
                            color = BiodontosGreyText,
                            fontSize = 14.sp
                        )
                        BiodontosPillButton(
                            text = "Limpiar filtros",
                            variant = PillButtonVariant.OUTLINED,
                            onClick = {
                                onEvent(DoctorSearchUiEvent.QueryChanged(""))
                                onEvent(DoctorSearchUiEvent.SpecialtySelected("Todas"))
                            }
                        )
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state.doctors, key = { it.id.value }) { doctor ->
                        DoctorItemCard(
                            doctor = doctor,
                            onDoctorClick = { onEvent(DoctorSearchUiEvent.DoctorSelected(doctor.id.value)) },
                            onBookClick = { onEvent(DoctorSearchUiEvent.BookDoctor(doctor.id.value)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorItemCard(
    doctor: Doctor,
    onDoctorClick: () -> Unit,
    onBookClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDoctorClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar con soporte de foto Base64 o iniciales
                DoctorAvatar(
                    photoUrl = doctor.photoUrl,
                    firstName = doctor.firstName,
                    lastName = doctor.lastName,
                    size = 54.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doctor.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = doctor.specialty,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = BiodontosGreyText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            doctor.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                Text(
                    text = bio,
                    fontSize = 12.sp,
                    color = BiodontosGreyText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BiodontosPillButton(
                    text = "Ver perfil",
                    variant = PillButtonVariant.OUTLINED,
                    onClick = onDoctorClick,
                    modifier = Modifier.weight(1f)
                )
                BiodontosPillButton(
                    text = "Agendar",
                    variant = PillButtonVariant.DARK,
                    onClick = onBookClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorSearchLoadingPreview() {
    DentalTheme {
        DoctorSearchScreen(
            state = DoctorSearchUiState(isLoading = true),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorSearchSuccessPreview() {
    DentalTheme {
        DoctorSearchScreen(
            state = DoctorSearchUiState(
                isLoading = false,
                doctors = listOf(
                    Doctor(
                        id = DoctorId("doc-1"),
                        firstName = "Carlos",
                        lastName = "Restrepo",
                        specialty = "Ortodoncia",
                        bio = "Especialista en diseño de sonrisa y ortodoncia invisible."
                    )
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorSearchErrorPreview() {
    DentalTheme {
        DoctorSearchScreen(
            state = DoctorSearchUiState(
                isLoading = false,
                errorMessage = "Error al consultar el directorio de especialistas."
            ),
            onEvent = {}
        )
    }
}
