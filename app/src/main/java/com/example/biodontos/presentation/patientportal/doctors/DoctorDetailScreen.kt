package com.example.biodontos.presentation.patientportal.doctors

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.components.BiodontosPillButton
import com.example.biodontos.core.designsystem.components.PillButtonVariant
import com.example.biodontos.domain.model.DoctorDetail
import com.example.biodontos.domain.model.WeeklyAvailability
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DoctorDetailRoute(
    viewModel: DoctorDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBooking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DoctorDetailUiEffect.NavigateToBooking -> onNavigateToBooking(effect.doctorId)
            }
        }
    }

    DoctorDetailScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = onNavigateBack,
        modifier = modifier
    )
}

@VisibleForTesting
@Composable
fun DoctorDetailScreen(
    state: DoctorDetailUiState,
    onEvent: (DoctorDetailUiEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE9E9EB))
            .padding(16.dp)
    ) {
        // Top navigation bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = BiodontosDark
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Perfil del Especialista",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BiodontosDark)
                }
            }
            state.errorMessage != null || state.doctorDetail == null -> {
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
                            text = state.errorMessage ?: "Doctor no encontrado",
                            color = BiodontosDark,
                            fontSize = 14.sp
                        )
                        BiodontosPillButton(
                            text = "Reintentar",
                            onClick = { onEvent(DoctorDetailUiEvent.Retry) }
                        )
                    }
                }
            }
            else -> {
                val detail = state.doctorDetail
                val doctor = detail.doctor

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tarjeta Principal (Hero)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar grande
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape)
                                        .background(BiodontosInputBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = doctor.displayName,
                                        modifier = Modifier.size(54.dp),
                                        tint = BiodontosDark.copy(alpha = 0.6f)
                                    )
                                }

                                Text(
                                    text = doctor.displayName,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BiodontosDark
                                )

                                Text(
                                    text = doctor.specialty,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BiodontosGreyText
                                )

                                if (doctor.licenseNumber.isNotBlank()) {
                                    Text(
                                        text = "Registro Profesional: ${doctor.licenseNumber}",
                                        fontSize = 12.sp,
                                        color = BiodontosGreyText.copy(alpha = 0.8f)
                                    )
                                }

                                // Chip de estado disponible hoy
                                if (detail.isAvailableToday) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF2E7D32), shape = RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "● Atiende hoy",
                                            color = BiodontosWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(BiodontosInputBg, shape = RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Atención según agenda",
                                            color = BiodontosDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Acerca del especialista
                    doctor.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "ACERCA DEL ESPECIALISTA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BiodontosGreyText,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = bio,
                                        fontSize = 14.sp,
                                        color = BiodontosDark,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    // Horario de atención semanal
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = BiodontosDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "HORARIO DE ATENCIÓN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BiodontosGreyText,
                                        letterSpacing = 1.sp
                                    )
                                }

                                if (detail.availability.isEmpty()) {
                                    Text(
                                        text = "Lunes a Viernes: 08:00 AM - 12:00 PM y 02:00 PM - 06:00 PM",
                                        fontSize = 13.sp,
                                        color = BiodontosDark
                                    )
                                } else {
                                    detail.availability.forEach { slot ->
                                        ScheduleRow(slot)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón CTA flotante
                BiodontosPillButton(
                    text = "Agendar Cita con ${doctor.firstName}",
                    variant = PillButtonVariant.DARK,
                    onClick = { onEvent(DoctorDetailUiEvent.BookAppointment) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ScheduleRow(slot: WeeklyAvailability) {
    val dayName = when (slot.dayOfWeek) {
        DayOfWeek.MONDAY -> "Lunes"
        DayOfWeek.TUESDAY -> "Martes"
        DayOfWeek.WEDNESDAY -> "Miércoles"
        DayOfWeek.THURSDAY -> "Jueves"
        DayOfWeek.FRIDAY -> "Viernes"
        DayOfWeek.SATURDAY -> "Sábado"
        DayOfWeek.SUNDAY -> "Domingo"
    }

    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es-CO"))
    val startStr = slot.startTime.format(timeFormatter)
    val endStr = slot.endTime.format(timeFormatter)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayName,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = BiodontosDark
        )
        Text(
            text = "$startStr - $endStr",
            fontSize = 13.sp,
            color = BiodontosGreyText
        )
    }
}
