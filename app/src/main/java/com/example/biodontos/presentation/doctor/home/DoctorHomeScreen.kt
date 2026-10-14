package com.example.biodontos.presentation.doctor.home

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.biodontos.core.designsystem.BiodontosLightGrey
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.StatusCancelled
import com.example.biodontos.core.designsystem.StatusConfirmed
import com.example.biodontos.core.designsystem.components.BiodontosPillButton
import com.example.biodontos.core.designsystem.components.PillButtonVariant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DoctorHomeRoute(
    viewModel: DoctorHomeViewModel,
    onNavigateToPatients: () -> Unit,
    onNavigateToNewPatient: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DoctorHomeUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is DoctorHomeUiEffect.NavigateToLogin -> {
                    onNavigateToLogin()
                }
            }
        }
    }

    DoctorHomeScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToPatients = onNavigateToPatients,
        onNavigateToNewPatient = onNavigateToNewPatient,
        onNavigateToAppointments = onNavigateToAppointments,
        onNavigateToDocuments = onNavigateToDocuments,
        snackbarHostState = snackbarHostState
    )
}

@VisibleForTesting
@Composable
fun DoctorHomeScreen(
    state: DoctorHomeUiState,
    onEvent: (DoctorHomeUiEvent) -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToNewPatient: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BiodontosScreenGradient)
                .padding(paddingValues)
        ) {
            if (state.isLoading && !state.isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BiodontosDark)
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isExpanded = maxWidth >= 760.dp

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = if (isExpanded) 32.dp else 16.dp, vertical = 20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Header Principal: "Hola, Doc!"
                        DoctorHomeHeader(
                            doctorName = state.doctorName,
                            isRefreshing = state.isRefreshing,
                            onRefresh = { onEvent(DoctorHomeUiEvent.Refresh) }
                        )

                        // Fila Superior (3 Tarjetas): Pacientes & Evolución, Documentos, Tu agenda
                        if (isExpanded) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                PatientsEvolutionCard(
                                    patientsCount = state.patientsCount,
                                    onNewEvolutionClick = onNavigateToPatients,
                                    onNewPatientClick = onNavigateToNewPatient,
                                    modifier = Modifier.weight(1f)
                                )
                                DocumentsSummaryCard(
                                    onViewDocumentsClick = onNavigateToDocuments,
                                    modifier = Modifier.weight(1f)
                                )
                                AgendaSummaryCard(
                                    todayCount = state.todayAppointmentsCount,
                                    onManageCalendarClick = onNavigateToAppointments,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                PatientsEvolutionCard(
                                    patientsCount = state.patientsCount,
                                    onNewEvolutionClick = onNavigateToPatients,
                                    onNewPatientClick = onNavigateToNewPatient
                                )
                                DocumentsSummaryCard(
                                    onViewDocumentsClick = onNavigateToDocuments
                                )
                                AgendaSummaryCard(
                                    todayCount = state.todayAppointmentsCount,
                                    onManageCalendarClick = onNavigateToAppointments
                                )
                            }
                        }

                        // Fila Inferior (2 Bloques): Próximas Citas (ancha) + Pagos y Abonos
                        if (isExpanded) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                UpcomingAppointmentsCard(
                                    appointments = state.upcomingAppointments,
                                    onToggleConfirm = { id, confirmed ->
                                        onEvent(DoctorHomeUiEvent.ToggleConfirmAppointment(id, confirmed))
                                    },
                                    onSeeAll = onNavigateToAppointments,
                                    modifier = Modifier.weight(1.5f)
                                )

                                PaymentsSummaryCard(
                                    payments = state.recentPayments,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                UpcomingAppointmentsCard(
                                    appointments = state.upcomingAppointments,
                                    onToggleConfirm = { id, confirmed ->
                                        onEvent(DoctorHomeUiEvent.ToggleConfirmAppointment(id, confirmed))
                                    },
                                    onSeeAll = onNavigateToAppointments
                                )

                                PaymentsSummaryCard(
                                    payments = state.recentPayments
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorHomeHeader(
    doctorName: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hola, Doc!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )
            if (doctorName.isNotBlank()) {
                Text(
                    text = doctorName,
                    fontSize = 14.sp,
                    color = BiodontosGreyText,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        IconButton(onClick = onRefresh, enabled = !isRefreshing) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = BiodontosDark
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar",
                    tint = BiodontosDark
                )
            }
        }
    }
}

@Composable
private fun PatientsEvolutionCard(
    patientsCount: Int,
    onNewEvolutionClick: () -> Unit,
    onNewPatientClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pacientes & Evolución",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )

            Text(
                text = "$patientsCount pacientes activos en registro",
                fontSize = 13.sp,
                color = BiodontosGreyText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BiodontosPillButton(
                    text = "Nueva evolución",
                    variant = PillButtonVariant.DARK,
                    onClick = onNewEvolutionClick,
                    modifier = Modifier.weight(1f)
                )
                BiodontosPillButton(
                    text = "Nuevo Paciente",
                    variant = PillButtonVariant.DARK,
                    onClick = onNewPatientClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DocumentsSummaryCard(
    onViewDocumentsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                Text(text = "Ultimo control", fontSize = 13.sp, color = BiodontosGreyText)
                Text(text = "18 Mar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BiodontosDark)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Archivos", fontSize = 13.sp, color = BiodontosGreyText)
                Text(text = "12", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BiodontosDark)
            }

            BiodontosPillButton(
                text = "Historias clínicas",
                variant = PillButtonVariant.DARK,
                onClick = onViewDocumentsClick
            )
        }
    }
}

@Composable
private fun AgendaSummaryCard(
    todayCount: Int,
    onManageCalendarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tu agenda",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )

            Text(
                text = if (todayCount > 0) "$todayCount citas programadas para hoy" else "No tienes consultas agendadas hoy",
                fontSize = 13.sp,
                color = BiodontosGreyText
            )

            BiodontosPillButton(
                text = "Gestión de calendario",
                variant = PillButtonVariant.DARK,
                onClick = onManageCalendarClick
            )
        }
    }
}

@Composable
private fun UpcomingAppointmentsCard(
    appointments: List<DoctorAppointmentItem>,
    onToggleConfirm: (String, Boolean) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tus próximas citas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )

                BiodontosPillButton(
                    text = "Ver agenda",
                    variant = PillButtonVariant.LIGHT,
                    onClick = onSeeAll
                )
            }

            if (appointments.isEmpty()) {
                Text(
                    text = "No tienes citas próximas registradas.",
                    fontSize = 13.sp,
                    color = BiodontosGreyText,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    appointments.forEach { appt ->
                        AppointmentTableRow(
                            item = appt,
                            onToggleConfirm = { onToggleConfirm(appt.appointmentId, appt.isConfirmed) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentTableRow(
    item: DoctorAppointmentItem,
    onToggleConfirm: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))
    val formattedTime = runCatching { item.startTime.format(timeFormatter) }
        .getOrDefault("${item.startTime.hour}:${item.startTime.minute}")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF7F8FA))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BiodontosDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = BiodontosWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = item.patientName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
                Text(
                    text = item.treatmentReason,
                    fontSize = 11.sp,
                    color = BiodontosGreyText
                )
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = BiodontosGreyText
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Chip de confirmación interactivo
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (item.isConfirmed) StatusConfirmed.copy(alpha = 0.12f)
                        else StatusCancelled.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.isConfirmed) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (item.isConfirmed) StatusConfirmed else StatusCancelled,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.isConfirmed) "Confirmada" else "Sin Confirmar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isConfirmed) StatusConfirmed else StatusCancelled
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onToggleConfirm,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Cambiar estado de confirmación",
                    tint = BiodontosDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PaymentsSummaryCard(
    payments: List<DoctorPaymentItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "Pagos y Abonos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
                Text(
                    text = "Tus últimos pagos",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
            }

            payments.forEach { payment ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF7F8FA))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BiodontosDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = BiodontosWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = payment.patientName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                            Text(
                                text = payment.dateStr,
                                fontSize = 11.sp,
                                color = BiodontosGreyText
                            )
                        }
                    }

                    Text(
                        text = payment.amountCop.formatCop(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                }
            }

            BiodontosPillButton(
                text = "Nuevo Pago o Abono",
                variant = PillButtonVariant.DARK,
                onClick = {}
            )
        }
    }
}
