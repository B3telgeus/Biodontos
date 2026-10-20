package com.example.biodontos.presentation.doctor.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biodontos.core.designsystem.*
import com.example.biodontos.core.util.DateTimeUtils
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.Patient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen(
    viewModel: DoctorProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }
    var cancelReason by remember { mutableStateOf("") }

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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mi Perfil & Agenda",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = BiodontosDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(DoctorProfileEvent.LoadData) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = BiodontosDark
                        )
                    }
                    IconButton(onClick = { viewModel.onEvent(DoctorProfileEvent.Logout) }) {
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
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            if (state.isLoading && state.doctor == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Zafiro
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- PERFIL DEL DOCTOR ---
                    item {
                        DoctorInfoCard(doctor = state.doctor)
                    }

                    // --- BANNER DE CITAS PENDIENTES ---
                    if (state.pendingAppointments.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF8E1),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "¡Tienes ${state.pendingAppointments.size} cita(s) por confirmar!",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE65100)
                                            )
                                        )
                                        Text(
                                            text = "Los pacientes están a la espera de tu aprobación para apartar su cupo.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BiodontosDark
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- SELECTOR DE PESTAÑAS ---
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BiodontosWhite, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TabButton(
                                text = "Pendientes (${state.pendingAppointments.size})",
                                isSelected = state.selectedTab == DoctorAppointmentTab.PENDING,
                                onClick = { viewModel.onEvent(DoctorProfileEvent.SelectTab(DoctorAppointmentTab.PENDING)) },
                                modifier = Modifier.weight(1f)
                            )
                            TabButton(
                                text = "Confirmadas (${state.confirmedAppointments.size})",
                                isSelected = state.selectedTab == DoctorAppointmentTab.CONFIRMED,
                                onClick = { viewModel.onEvent(DoctorProfileEvent.SelectTab(DoctorAppointmentTab.CONFIRMED)) },
                                modifier = Modifier.weight(1f)
                            )
                            TabButton(
                                text = "Historial (${state.historyAppointments.size})",
                                isSelected = state.selectedTab == DoctorAppointmentTab.HISTORY,
                                onClick = { viewModel.onEvent(DoctorProfileEvent.SelectTab(DoctorAppointmentTab.HISTORY)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // --- LISTADO SEGÚN PESTAÑA ---
                    val currentList = when (state.selectedTab) {
                        DoctorAppointmentTab.PENDING -> state.pendingAppointments
                        DoctorAppointmentTab.CONFIRMED -> state.confirmedAppointments
                        DoctorAppointmentTab.HISTORY -> state.historyAppointments
                    }

                    if (currentList.isEmpty()) {
                        item {
                            EmptyStateNotice(
                                title = when (state.selectedTab) {
                                    DoctorAppointmentTab.PENDING -> "No hay citas pendientes"
                                    DoctorAppointmentTab.CONFIRMED -> "No tienes citas confirmadas próximas"
                                    DoctorAppointmentTab.HISTORY -> "Historial sin registros"
                                },
                                subtitle = when (state.selectedTab) {
                                    DoctorAppointmentTab.PENDING -> "Las solicitudes de tus pacientes aparecerán aquí para tu confirmación."
                                    DoctorAppointmentTab.CONFIRMED -> "Las citas que confirmes se organizarán en este espacio."
                                    DoctorAppointmentTab.HISTORY -> "Tus consultas realizadas o canceladas quedarán archivadas aquí."
                                }
                            )
                        }
                    } else {
                        items(currentList, key = { it.id.ifEmpty { it.startTime } }) { appointment ->
                            val patient = state.patientsMap[appointment.patientId]
                            DoctorAppointmentItemCard(
                                appointment = appointment,
                                patient = patient,
                                isPending = !appointment.isConfirmed && appointment.status == AppointmentStatus.SCHEDULED,
                                onConfirmClick = {
                                    viewModel.onEvent(DoctorProfileEvent.ConfirmAppointment(appointment.id))
                                },
                                onCancelClick = {
                                    appointmentToCancel = appointment
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal de cancelación por el doctor
    appointmentToCancel?.let { apt ->
        AlertDialog(
            onDismissRequest = { appointmentToCancel = null },
            icon = {
                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Carmesi, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    text = "¿Rechazar o cancelar esta cita?",
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "El paciente será notificado de la cancelación de su cita del ${DateTimeUtils.formatToReadableDate(apt.startTime)}.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        placeholder = { Text("Motivo clínico o administrativo...", color = BiodontosGreyText) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = biodontosTextFieldColors(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(DoctorProfileEvent.CancelAppointment(apt.id, cancelReason.ifBlank { null }))
                        appointmentToCancel = null
                        cancelReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Carmesi)
                ) {
                    Text("Sí, Cancelar Cita", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { appointmentToCancel = null }) {
                    Text("Volver")
                }
            },
            containerColor = BiodontosWhite
        )
    }
}

@Composable
private fun DoctorInfoCard(doctor: Doctor?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Zafiro),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = BiodontosWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = doctor?.fullName ?: "Doctor(a)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ZafiroLight,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = doctor?.specialty ?: "Especialista Dental",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Zafiro
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BiodontosLightGrey)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                doctor?.licenseNumber?.let {
                    Text(
                        text = "T.P. $it",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BiodontosGreyText
                        )
                    )
                }
                doctor?.email?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText)
                    )
                }
            }
        }
    }
}

@Composable
private fun DoctorAppointmentItemCard(
    appointment: Appointment,
    patient: Patient?,
    isPending: Boolean,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (badgeText, badgeBg, badgeTextColor) = when {
                    appointment.status == AppointmentStatus.CANCELLED -> Triple("CANCELADA", Color(0xFFFFEBEE), Carmesi)
                    appointment.status == AppointmentStatus.COMPLETED -> Triple("REALIZADA", Color(0xFFE8F5E9), Esmeralda)
                    !appointment.isConfirmed -> Triple("PENDIENTE DE CONFIRMACIÓN", Color(0xFFFFF8E1), Color(0xFFE65100))
                    else -> Triple("CONFIRMADA", Color(0xFFE8F5E9), Esmeralda)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    )
                }

                Text(
                    text = DateTimeUtils.formatToShortDate(appointment.startTime),
                    style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = patient?.fullName ?: "Paciente Registrado",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
            )

            if (patient != null && patient.dniCedula.isNotEmpty()) {
                Text(
                    text = "DNI / Doc: ${patient.dniCedula} ${if (patient.phone.isNotEmpty()) "• Tel: ${patient.phone}" else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BiodontosLightGrey)
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Zafiro, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = DateTimeUtils.formatToReadableDate(appointment.startTime),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = BiodontosDark)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Zafiro, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = DateTimeUtils.extractTime(appointment.startTime),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = BiodontosDark)
                    )
                }
            }

            if (!appointment.reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Motivo: ${appointment.reason}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = BiodontosDark
                    )
                )
            }

            // BOTONES DE ACCIÓN PARA EL DOCTOR
            if (isPending) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConfirmClick,
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = Esmeralda),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirmar Cita", fontWeight = FontWeight.Bold, color = BiodontosWhite)
                    }

                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.weight(0.8f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Carmesi),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Rechazar", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (appointment.status == AppointmentStatus.SCHEDULED) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancelClick) {
                        Text("Cancelar Cita", color = Carmesi, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) BiodontosDark else Color.Transparent,
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BiodontosWhite else BiodontosGreyText
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyStateNotice(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = BiodontosBorder,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BiodontosDark)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText),
                textAlign = TextAlign.Center
            )
        }
    }
}
