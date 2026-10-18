package com.example.biodontos.presentation.patient.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biodontos.core.designsystem.*
import com.example.biodontos.core.util.DateTimeUtils
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.Doctor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    viewModel: PatientHomeViewModel,
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PatientHomeEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                PatientHomeEffect.NavigateToLogin -> {
                    onNavigateToLogin()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hola, ${state.patientName}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Portal Odontológico Biodontos",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BiodontosGreyText
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(PatientHomeEvent.Refresh) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = BiodontosDark
                        )
                    }
                    IconButton(onClick = { viewModel.onEvent(PatientHomeEvent.Logout) }) {
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.upcomingAppointments.isEmpty() && state.historyAppointments.isEmpty()) {
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
                    // --- HERO CARD: PRÓXIMA CITA ---
                    item {
                        Text(
                            text = "Tu Próxima Cita",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val next = state.nextAppointment
                        if (next != null) {
                            NextAppointmentCard(
                                appointment = next,
                                doctor = state.doctors.firstOrNull { it.id == next.doctorId },
                                onRescheduleClick = {
                                    viewModel.onEvent(PatientHomeEvent.OpenRescheduleDialog(next))
                                },
                                onCancelClick = {
                                    viewModel.onEvent(PatientHomeEvent.OpenCancelDialog(next))
                                }
                            )
                        } else {
                            EmptyUpcomingCard(
                                onBookClick = { viewModel.onEvent(PatientHomeEvent.OpenBookingDialog) }
                            )
                        }
                    }

                    // --- BOTÓN PRINCIPAL: AGENDAR NUEVA CITA ---
                    item {
                        Button(
                            onClick = { viewModel.onEvent(PatientHomeEvent.OpenBookingDialog) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BiodontosDark,
                                contentColor = BiodontosWhite
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Agendar Nueva Cita",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    // --- PESTAÑAS: PRÓXIMAS vs HISTORIAL ---
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BiodontosWhite, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TabButton(
                                text = "Próximas (${state.upcomingAppointments.size})",
                                isSelected = state.selectedTab == AppointmentTab.UPCOMING,
                                onClick = { viewModel.onEvent(PatientHomeEvent.SelectTab(AppointmentTab.UPCOMING)) },
                                modifier = Modifier.weight(1f)
                            )
                            TabButton(
                                text = "Historial (${state.historyAppointments.size})",
                                isSelected = state.selectedTab == AppointmentTab.HISTORY,
                                onClick = { viewModel.onEvent(PatientHomeEvent.SelectTab(AppointmentTab.HISTORY)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // --- LISTADO SEGÚN PESTAÑA ---
                    if (state.selectedTab == AppointmentTab.UPCOMING) {
                        if (state.upcomingAppointments.isEmpty()) {
                            item {
                                EmptyStateNotice(
                                    title = "No tienes citas próximas",
                                    subtitle = "Cuando reserves una consulta odontológica, aparecerá aquí."
                                )
                            }
                        } else {
                            items(state.upcomingAppointments, key = { it.id.ifEmpty { it.startTime } }) { appointment ->
                                AppointmentItemCard(
                                    appointment = appointment,
                                    doctor = state.doctors.firstOrNull { it.id == appointment.doctorId },
                                    isUpcoming = true,
                                    onRescheduleClick = {
                                        viewModel.onEvent(PatientHomeEvent.OpenRescheduleDialog(appointment))
                                    },
                                    onCancelClick = {
                                        viewModel.onEvent(PatientHomeEvent.OpenCancelDialog(appointment))
                                    }
                                )
                            }
                        }
                    } else {
                        if (state.historyAppointments.isEmpty()) {
                            item {
                                EmptyStateNotice(
                                    title = "Historial sin registros",
                                    subtitle = "Tus citas completadas o canceladas quedarán registradas aquí."
                                )
                            }
                        } else {
                            items(state.historyAppointments, key = { it.id.ifEmpty { it.startTime } }) { appointment ->
                                AppointmentItemCard(
                                    appointment = appointment,
                                    doctor = state.doctors.firstOrNull { it.id == appointment.doctorId },
                                    isUpcoming = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- MODALES Y DIÁLOGOS ---
    state.bookingState?.let { bookingState ->
        BookingDialog(
            state = bookingState,
            doctors = state.doctors,
            onDismiss = { viewModel.onEvent(PatientHomeEvent.DismissBookingDialog) },
            onDoctorSelected = { viewModel.onEvent(PatientHomeEvent.SelectBookingDoctor(it)) },
            onDateSelected = { viewModel.onEvent(PatientHomeEvent.SelectBookingDate(it)) },
            onSlotSelected = { viewModel.onEvent(PatientHomeEvent.SelectBookingSlot(it)) },
            onReasonChange = { viewModel.onEvent(PatientHomeEvent.UpdateBookingReason(it)) },
            onConfirm = { viewModel.onEvent(PatientHomeEvent.ConfirmBooking) }
        )
    }

    state.rescheduleState?.let { rescheduleState ->
        RescheduleDialog(
            state = rescheduleState,
            onDismiss = { viewModel.onEvent(PatientHomeEvent.DismissRescheduleDialog) },
            onDateSelected = { viewModel.onEvent(PatientHomeEvent.SelectRescheduleDate(it)) },
            onSlotSelected = { viewModel.onEvent(PatientHomeEvent.SelectRescheduleSlot(it)) },
            onConfirm = { viewModel.onEvent(PatientHomeEvent.ConfirmReschedule) }
        )
    }

    state.cancelConfirmState?.let { cancelState ->
        CancelConfirmationDialog(
            state = cancelState,
            onDismiss = { viewModel.onEvent(PatientHomeEvent.DismissCancelDialog) },
            onSelectChip = { viewModel.onEvent(PatientHomeEvent.SelectCancelChip(it)) },
            onCustomReasonChange = { viewModel.onEvent(PatientHomeEvent.UpdateCancelCustomReason(it)) },
            onConfirm = { viewModel.onEvent(PatientHomeEvent.ConfirmCancellation) }
        )
    }

    state.successDialogState?.let { successState ->
        SuccessSummaryDialog(
            state = successState,
            onDismiss = { viewModel.onEvent(PatientHomeEvent.DismissSuccessDialog) }
        )
    }
}

@Composable
private fun NextAppointmentCard(
    appointment: Appointment,
    doctor: Doctor?,
    onRescheduleClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (appointment.isConfirmed) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                ) {
                    Text(
                        text = if (appointment.isConfirmed) "CONFIRMADA" else "PENDIENTE DE CONFIRMACIÓN",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (appointment.isConfirmed) Esmeralda else Color(0xFFE65100)
                        )
                    )
                }

                Text(
                    text = DateTimeUtils.formatToShortDate(appointment.startTime),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = BiodontosGreyText
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Zafiro),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = BiodontosWhite
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = doctor?.fullName ?: "Especialista Clínico",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    )
                    Text(
                        text = doctor?.specialty ?: "Odontología Integral",
                        style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BiodontosLightGrey)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Zafiro
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = DateTimeUtils.formatToReadableDate(appointment.startTime),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = BiodontosDark)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Zafiro
                    )
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
                    style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRescheduleClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Zafiro)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reagendar", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }

                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Carmesi)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancelar", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun EmptyUpcomingCard(onBookClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ZafiroLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Zafiro,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No tienes citas programadas",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BiodontosDark)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cuida tu salud oral agendando tu cita con nuestros especialistas.",
                style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onBookClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Zafiro)
            ) {
                Text("Agendar cita ahora", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AppointmentItemCard(
    appointment: Appointment,
    doctor: Doctor?,
    isUpcoming: Boolean,
    onRescheduleClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                text = doctor?.fullName ?: "Especialista Clínico",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
            )
            Text(
                text = doctor?.specialty ?: "Odontología",
                style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Hora: ${DateTimeUtils.extractTime(appointment.startTime)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = BiodontosDark)
                )
            }

            if (!appointment.reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Motivo: ${appointment.reason}",
                    style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isUpcoming && onRescheduleClick != null && onCancelClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onRescheduleClick) {
                        Text("Reagendar", color = Zafiro, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onCancelClick) {
                        Text("Cancelar", color = Carmesi, fontWeight = FontWeight.SemiBold)
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BiodontosWhite else BiodontosGreyText
                )
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
                imageVector = Icons.Default.Info,
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

// ==========================================
// MODAL: AGENDAR CITA
// ==========================================
@Composable
private fun BookingDialog(
    state: BookingDialogState,
    doctors: List<Doctor>,
    onDismiss: () -> Unit,
    onDoctorSelected: (String) -> Unit,
    onDateSelected: (String) -> Unit,
    onSlotSelected: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val upcomingDays = remember { DateTimeUtils.getUpcomingDays(14) }
    val reasonsQuickChips = listOf(
        "Valoración General",
        "Limpieza Dental",
        "Dolor o Molestia",
        "Ortodoncia",
        "Estética Dental"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = BiodontosWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Agendar Cita",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = BiodontosDark)
                    }
                }

                HorizontalDivider(color = BiodontosLightGrey)
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Especialista
                Text(
                    text = "1. Selecciona a tu Odontólogo(a):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BiodontosDark)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    doctors.forEach { doc ->
                        val isSelected = doc.id == state.selectedDoctorId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDoctorSelected(doc.id) },
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Zafiro) else null,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) ZafiroLight else BiodontosInputBg
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onDoctorSelected(doc.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Zafiro)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = doc.fullName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BiodontosDark)
                                    )
                                    Text(
                                        text = doc.specialty,
                                        style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Fecha
                Text(
                    text = "2. Selecciona la Fecha:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BiodontosDark)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(upcomingDays) { date ->
                        val isSelected = date == state.selectedDate
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BiodontosDark else BiodontosInputBg,
                            modifier = Modifier.clickable { onDateSelected(date) }
                        ) {
                            Text(
                                text = DateTimeUtils.formatToShortDate(date),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BiodontosWhite else BiodontosDark
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Franja Horaria
                Text(
                    text = "3. Horarios Disponibles:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BiodontosDark)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (state.isLoadingSlots) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Zafiro, modifier = Modifier.size(24.dp))
                    }
                } else if (state.availableSlots.isEmpty()) {
                    Text(
                        text = "No hay cupos disponibles con este doctor en la fecha seleccionada. Por favor elige otro día.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Carmesi)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.availableSlots.take(4).forEach { slot ->
                                SlotChip(
                                    slot = slot,
                                    isSelected = slot == state.selectedSlot,
                                    onClick = { onSlotSelected(slot) }
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.availableSlots.drop(4).forEach { slot ->
                                SlotChip(
                                    slot = slot,
                                    isSelected = slot == state.selectedSlot,
                                    onClick = { onSlotSelected(slot) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Motivo
                Text(
                    text = "4. Motivo de Consulta:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BiodontosDark)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(reasonsQuickChips) { chip ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.reason == chip) ZafiroLight else BiodontosInputBg,
                            modifier = Modifier.clickable { onReasonChange(chip) }
                        ) {
                            Text(
                                text = chip,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (state.reason == chip) Zafiro else BiodontosDark,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.reason,
                    onValueChange = onReasonChange,
                    placeholder = { Text("Escribe o complementa el motivo...", color = BiodontosGreyText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 2
                )

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall.copy(color = Carmesi, fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botón Confirmar
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                ) {
                    Text(
                        text = "Confirmar y Reservar Cita",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BiodontosWhite
                        )
                    )
                }
            }
        }
    }
}

// ==========================================
// MODAL: REAGENDAR CITA
// ==========================================
@Composable
private fun RescheduleDialog(
    state: RescheduleDialogState,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit,
    onSlotSelected: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val upcomingDays = remember { DateTimeUtils.getUpcomingDays(14) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = BiodontosWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reagendar Cita",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = BiodontosDark)
                    }
                }

                HorizontalDivider(color = BiodontosLightGrey)
                Spacer(modifier = Modifier.height(14.dp))

                // Cita actual
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ZafiroLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Cita Actual:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Zafiro)
                        )
                        Text(
                            text = "${DateTimeUtils.formatToReadableDate(state.appointment.startTime)} a las ${DateTimeUtils.extractTime(state.appointment.startTime)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = BiodontosDark)
                        )
                        state.doctor?.let {
                            Text(
                                text = "Con: ${it.fullName} (${it.specialty})",
                                style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Selecciona Nueva Fecha:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BiodontosDark)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(upcomingDays) { date ->
                        val isSelected = date == state.selectedDate
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BiodontosDark else BiodontosInputBg,
                            modifier = Modifier.clickable { onDateSelected(date) }
                        ) {
                            Text(
                                text = DateTimeUtils.formatToShortDate(date),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BiodontosWhite else BiodontosDark
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Selecciona Nuevo Horario Disponible:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BiodontosDark)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (state.isLoadingSlots) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Zafiro, modifier = Modifier.size(24.dp))
                    }
                } else if (state.availableSlots.isEmpty()) {
                    Text(
                        text = "No hay turnos disponibles para esta fecha. Intenta con otro día.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Carmesi)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.availableSlots.take(4).forEach { slot ->
                                SlotChip(
                                    slot = slot,
                                    isSelected = slot == state.selectedSlot,
                                    onClick = { onSlotSelected(slot) }
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.availableSlots.drop(4).forEach { slot ->
                                SlotChip(
                                    slot = slot,
                                    isSelected = slot == state.selectedSlot,
                                    onClick = { onSlotSelected(slot) }
                                )
                            }
                        }
                    }
                }

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall.copy(color = Carmesi, fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                ) {
                    Text(
                        text = "Guardar Nuevo Horario",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BiodontosWhite
                        )
                    )
                }
            }
        }
    }
}

// ==========================================
// MODAL: CANCELACIÓN CON CONFIRMACIÓN CLÍNICA
// ==========================================
@Composable
private fun CancelConfirmationDialog(
    state: CancelConfirmDialogState,
    onDismiss: () -> Unit,
    onSelectChip: (String) -> Unit,
    onCustomReasonChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val reasons = listOf(
        "Incompatibilidad de horario",
        "Calamidad personal",
        "Ya no requiero el servicio",
        "Otro motivo"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Carmesi,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "¿Deseas cancelar esta cita?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                ),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Cancelar tu cita del ${DateTimeUtils.formatToReadableDate(state.appointment.startTime)} liberará tu turno para otros pacientes.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Indica el motivo (opcional):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BiodontosDark)
                )
                Spacer(modifier = Modifier.height(8.dp))

                reasons.forEach { chip ->
                    val isSelected = state.selectedReasonChip == chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFFFFEBEE) else BiodontosInputBg,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Carmesi) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onSelectChip(chip) }
                    ) {
                        Text(
                            text = chip,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Carmesi else BiodontosDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.customReason,
                    onValueChange = onCustomReasonChange,
                    placeholder = { Text("Nota adicional (opcional)...", color = BiodontosGreyText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Carmesi),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Sí, Cancelar Cita", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BiodontosDark)
            ) {
                Text("Mantener Cita")
            }
        },
        containerColor = BiodontosWhite,
        shape = RoundedCornerShape(20.dp)
    )
}

// ==========================================
// MODAL: RESUMEN DE ÉXITO
// ==========================================
@Composable
private fun SuccessSummaryDialog(
    state: SuccessDialogState,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = BiodontosWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Esmeralda,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BiodontosInputBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Zafiro, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = state.doctorName, fontWeight = FontWeight.Bold, color = BiodontosDark)
                        }
                        Text(text = state.specialty, style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText), modifier = Modifier.padding(start = 26.dp))

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Zafiro, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = state.dateTimeFormatted, fontWeight = FontWeight.SemiBold, color = BiodontosDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "💡 Recuerda asistir 10 minutos antes con tu documento de identidad.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold, color = BiodontosWhite)
                }
            }
        }
    }
}

@Composable
private fun SlotChip(
    slot: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Zafiro else BiodontosInputBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = slot,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BiodontosWhite else BiodontosDark
                )
            )
        }
    }
}
