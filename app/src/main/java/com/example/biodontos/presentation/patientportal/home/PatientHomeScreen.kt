package com.example.biodontos.presentation.patientportal.home

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biodontos.R
import com.example.biodontos.core.designsystem.BiodontosCardGradient
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosRowHighlight
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.core.designsystem.Esmeralda
import com.example.biodontos.core.designsystem.components.AppointmentStatusChip
import com.example.biodontos.core.designsystem.components.BiodontosPillButton
import com.example.biodontos.core.designsystem.components.DateBadge
import com.example.biodontos.core.designsystem.components.PillButtonVariant
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PatientHomeRoute(
    viewModel: PatientHomeViewModel,
    onNavigateToAppointments: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToDoctors: () -> Unit,
    onNavigateToBooking: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PatientHomeUiEffect.NavigateToAppointments -> onNavigateToAppointments()
                is PatientHomeUiEffect.NavigateToPayments -> onNavigateToPayments()
                is PatientHomeUiEffect.NavigateToDoctors -> onNavigateToDoctors()
                is PatientHomeUiEffect.NavigateToBooking -> onNavigateToBooking(effect.doctorId)
                is PatientHomeUiEffect.NavigateToLogin -> onNavigateToLogin()
                is PatientHomeUiEffect.ShowToast -> {}
            }
        }
    }

    PatientHomeScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@VisibleForTesting
@Composable
fun PatientHomeScreen(
    state: PatientHomeUiState,
    onEvent: (PatientHomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true
) {
    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = BiodontosDark)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¡Hola, ${state.patientName.ifBlank { "Usuario" }}!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_tooth),
                contentDescription = "Biodontos",
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isExpanded) {
            // Layout Tablet Horizontal (Figma 07_Home_Paciente)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Columna Izquierda (Agenda + Pagos & Documentos)
                Column(
                    modifier = Modifier.weight(1.8f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    AgendaCard(
                        nextAppointment = state.nextAppointment,
                        upcomingAppointments = state.upcomingAppointments,
                        onGoToAppointments = { onEvent(PatientHomeUiEvent.NavigateToAppointments) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PaymentsSummaryCard(
                            amountCop = state.pendingBalance.formatCop(),
                            onGoToPayments = { onEvent(PatientHomeUiEvent.NavigateToPayments) },
                            modifier = Modifier.weight(1f)
                        )
                        ClinicalRecordsSummaryCard(
                            completedCount = state.completedAppointmentsCount,
                            onGoToRecords = { /* Próximamente */ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Columna Derecha (Tus Doctores)
                Box(modifier = Modifier.weight(1f)) {
                    DoctorsCard(
                        doctors = state.doctors,
                        onBookDoctor = { doctorId -> onEvent(PatientHomeUiEvent.BookWithDoctor(doctorId)) },
                        onSeeAllDoctors = { onEvent(PatientHomeUiEvent.NavigateToDoctors) }
                    )
                }
            }
        } else {
            // Layout Móvil Vertical
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AgendaCard(
                    nextAppointment = state.nextAppointment,
                    upcomingAppointments = state.upcomingAppointments,
                    onGoToAppointments = { onEvent(PatientHomeUiEvent.NavigateToAppointments) }
                )
                PaymentsSummaryCard(
                    amountCop = state.pendingBalance.formatCop(),
                    onGoToPayments = { onEvent(PatientHomeUiEvent.NavigateToPayments) }
                )
                ClinicalRecordsSummaryCard(
                    completedCount = state.completedAppointmentsCount,
                    onGoToRecords = { }
                )
                DoctorsCard(
                    doctors = state.doctors,
                    onBookDoctor = { doctorId -> onEvent(PatientHomeUiEvent.BookWithDoctor(doctorId)) },
                    onSeeAllDoctors = { onEvent(PatientHomeUiEvent.NavigateToDoctors) }
                )
            }
        }
    }
}

@Composable
private fun AgendaCard(
    nextAppointment: Appointment?,
    upcomingAppointments: List<Appointment>,
    onGoToAppointments: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header de la tarjeta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AGENDA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosGreyText,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tus próximas citas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                }
                BiodontosPillButton(
                    text = "Ir a Citas",
                    onClick = onGoToAppointments,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (nextAppointment == null && upcomingAppointments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes citas programadas en este momento.",
                        color = BiodontosGreyText,
                        fontSize = 14.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Panel de Próxima Cita (Estilo Figma con fondo degradado suave)
                    nextAppointment?.let { next ->
                        val day = next.startTime.dayOfMonth.toString()
                        val month = next.startTime.month.getDisplayName(TextStyle.FULL, Locale("es", "CO"))
                            .replaceFirstChar { it.uppercase() }
                        val year = next.startTime.year.toString()
                        val hour = "%02d:%02d %s".format(
                            if (next.startTime.hour > 12) next.startTime.hour - 12 else if (next.startTime.hour == 0) 12 else next.startTime.hour,
                            next.startTime.minute,
                            if (next.startTime.hour >= 12) "PM" else "AM"
                        )

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(160.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = BiodontosRowHighlight)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "PRÓXIMA CITA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BiodontosDark
                                )
                                Column {
                                    Text(
                                        text = day,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BiodontosDark,
                                        lineHeight = 44.sp
                                    )
                                    Text(
                                        text = "$month $year",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BiodontosDark
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = BiodontosDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = hour,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BiodontosDark
                                    )
                                }
                            }
                        }
                    }

                    // Lista de siguientes citas
                    Column(
                        modifier = Modifier.weight(1.3f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val displayList = if (nextAppointment != null) upcomingAppointments else upcomingAppointments.take(3)
                        displayList.forEach { appt ->
                            val timeStr = "%02d:%02d %s".format(
                                if (appt.startTime.hour > 12) appt.startTime.hour - 12 else if (appt.startTime.hour == 0) 12 else appt.startTime.hour,
                                appt.startTime.minute,
                                if (appt.startTime.hour >= 12) "PM" else "AM"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF7F8FA))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DateBadge(dateTime = appt.startTime)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = appt.reason ?: "Control dental",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BiodontosDark
                                        )
                                        Text(
                                            text = timeStr,
                                            fontSize = 12.sp,
                                            color = BiodontosGreyText
                                        )
                                    }
                                }
                                AppointmentStatusChip(status = appt.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentsSummaryCard(
    amountCop: String,
    onGoToPayments: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BiodontosCardGradient)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PAGOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosGreyText,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Saldo del mes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
                Text(
                    text = amountCop,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BiodontosDark
                )
                Text(
                    text = "Revisa tus abonos y planes activos",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
                BiodontosPillButton(
                    text = "Ver pagos",
                    variant = PillButtonVariant.LIGHT,
                    onClick = onGoToPayments,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ClinicalRecordsSummaryCard(
    completedCount: Int,
    onGoToRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "HISTORIA CLÍNICA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosGreyText,
                letterSpacing = 1.sp
            )
            Text(
                text = "Documentos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Atenciones finalizadas", fontSize = 13.sp, color = BiodontosGreyText)
                Text(text = "$completedCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BiodontosDark)
            }
            Spacer(modifier = Modifier.height(16.dp))
            BiodontosPillButton(
                text = "Ir a documentos",
                onClick = onGoToRecords
            )
        }
    }
}

@Composable
private fun DoctorsCard(
    doctors: List<Doctor>,
    onBookDoctor: (String) -> Unit,
    onSeeAllDoctors: () -> Unit,
    modifier: Modifier = Modifier
) {
    val doctor = doctors.firstOrNull()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "TUS DOCTORES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosGreyText,
                letterSpacing = 1.sp
            )

            if (doctor == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay doctores disponibles",
                        color = BiodontosGreyText,
                        fontSize = 14.sp
                    )
                }
            } else {
                Text(
                    text = doctor.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
                Text(
                    text = doctor.specialty,
                    fontSize = 13.sp,
                    color = BiodontosGreyText
                )

                // Placeholder / avatar de doctor con estilo circular o redondeado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(BiodontosInputBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = doctor.displayName,
                        modifier = Modifier.size(64.dp),
                        tint = BiodontosDark.copy(alpha = 0.5f)
                    )
                }

                // Badge de disponibilidad
                Box(
                    modifier = Modifier
                        .background(BiodontosDark, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Disponible hoy",
                        color = BiodontosWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                doctor.bio?.let { bio ->
                    Text(
                        text = bio,
                        fontSize = 12.sp,
                        color = BiodontosGreyText,
                        maxLines = 3
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BiodontosPillButton(
                        text = "Agendar",
                        onClick = { onBookDoctor(doctor.id.value) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientHomeLoadingPreview() {
    DentalTheme {
        PatientHomeScreen(
            state = PatientHomeUiState(isLoading = true),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientHomeSuccessPreview() {
    val nextApt = Appointment(
        id = "apt-1",
        patientId = "pat-1",
        doctorId = "doc-1",
        doctorName = "Dr. Carlos Restrepo",
        startTime = LocalDateTime.of(2026, 10, 26, 9, 0),
        status = AppointmentStatus.CONFIRMED,
        isConfirmed = true,
        reason = "Control de Ortodoncia"
    )
    DentalTheme {
        PatientHomeScreen(
            state = PatientHomeUiState(
                isLoading = false,
                patientName = "Laura Martínez",
                nextAppointment = nextApt,
                upcomingAppointments = listOf(nextApt),
                pendingBalance = CopAmount(150000L),
                doctors = listOf(
                    Doctor(
                        id = DoctorId("doc-1"),
                        firstName = "Carlos",
                        lastName = "Restrepo",
                        specialty = "Ortodoncia"
                    )
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientHomeErrorPreview() {
    DentalTheme {
        PatientHomeScreen(
            state = PatientHomeUiState(
                isLoading = false,
                patientName = "Laura Martínez",
                errorMessage = "No se pudo actualizar tu resumen clínico."
            ),
            onEvent = {}
        )
    }
}
