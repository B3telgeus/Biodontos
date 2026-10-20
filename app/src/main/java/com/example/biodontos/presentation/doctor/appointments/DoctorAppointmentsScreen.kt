package com.example.biodontos.presentation.doctor.appointments

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.StatusCancelled
import com.example.biodontos.core.designsystem.StatusConfirmed
import com.example.biodontos.core.designsystem.components.DateBadge
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.presentation.doctor.home.DoctorAppointmentItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun DoctorAppointmentsRoute(
    viewModel: DoctorAppointmentsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val today = remember { viewModel.clinicClock.now().toLocalDate() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DoctorAppointmentsUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    DoctorAppointmentsScreen(
        state = state,
        today = today,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState
    )
}

@VisibleForTesting
@Composable
fun DoctorAppointmentsScreen(
    state: DoctorAppointmentsUiState,
    today: LocalDate,
    onEvent: (DoctorAppointmentsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val filteredList = state.filteredAppointments(today)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BiodontosScreenGradient)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                DoctorAppointmentsHeader(
                    doctorName = state.doctorName,
                    isRefreshing = state.isRefreshing,
                    onNavigateBack = onNavigateBack,
                    onOpenAvailability = { onEvent(DoctorAppointmentsUiEvent.OpenAvailabilityEditor) },
                    onRefresh = { onEvent(DoctorAppointmentsUiEvent.Refresh) }
                )

                // Selector de modo: Cronograma vs Lista
                DoctorScheduleModeSelector(
                    currentMode = state.displayMode,
                    onModeSelected = { onEvent(DoctorAppointmentsUiEvent.ChangeDisplayMode(it)) }
                )

                if (state.displayMode == DoctorScheduleMode.TIMELINE) {
                    // Navegación de semana y Tira interactiva de días (Day Strip)
                    DoctorDayStrip(
                        selectedDate = state.selectedDate,
                        today = today,
                        appointments = state.appointments,
                        onDateSelected = { onEvent(DoctorAppointmentsUiEvent.SelectDate(it)) }
                    )

                    // Contenido del Cronograma
                    if (state.isLoading && !state.isRefreshing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BiodontosDark)
                        }
                    } else if (state.isDayOff) {
                        DoctorDayOffView(
                            date = state.selectedDate,
                            onOpenAvailability = { onEvent(DoctorAppointmentsUiEvent.OpenAvailabilityEditor) }
                        )
                    } else if (state.timelineSlots.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay bloques de horario disponibles para este día.",
                                fontSize = 14.sp,
                                color = BiodontosGreyText
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.timelineSlots, key = { it.startTime.toString() }) { slot ->
                                DoctorTimelineSlotRow(
                                    slot = slot,
                                    onBookSlot = { onEvent(DoctorAppointmentsUiEvent.OpenQuickBooking(slot)) },
                                    onToggleConfirm = { appt ->
                                        onEvent(DoctorAppointmentsUiEvent.ToggleConfirm(appt.appointmentId, appt.isConfirmed))
                                    },
                                    onReschedule = { appt ->
                                        onEvent(DoctorAppointmentsUiEvent.OpenReschedule(appt))
                                    },
                                    onCancel = { appt ->
                                        onEvent(DoctorAppointmentsUiEvent.RequestCancelAppointment(appt))
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Vista clásica de lista de citas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DoctorAppointmentFilter.entries.forEach { filter ->
                            val isSelected = state.selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BiodontosDark else BiodontosInputBg)
                                    .clickable { onEvent(DoctorAppointmentsUiEvent.SelectFilter(filter)) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BiodontosWhite else BiodontosDark
                                )
                            }
                        }
                    }

                    if (state.isLoading && !state.isRefreshing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BiodontosDark)
                        }
                    } else if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay citas registradas en este filtro.",
                                fontSize = 14.sp,
                                color = BiodontosGreyText
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredList, key = { it.appointmentId }) { item ->
                                DoctorAppointmentCard(
                                    item = item,
                                    onToggleConfirm = {
                                        onEvent(DoctorAppointmentsUiEvent.ToggleConfirm(item.appointmentId, item.isConfirmed))
                                    },
                                    onReschedule = { onEvent(DoctorAppointmentsUiEvent.OpenReschedule(item)) },
                                    onCancel = { onEvent(DoctorAppointmentsUiEvent.RequestCancelAppointment(item)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS ACTIVOS ---

    // 1. Diálogo de Configuración de Horario Semanal
    if (state.showAvailabilityDialog) {
        DoctorAvailabilityDialog(
            weeklyConfig = state.weeklyAvailability,
            isSaving = state.isSavingAvailability,
            onDismiss = { onEvent(DoctorAppointmentsUiEvent.DismissAvailabilityEditor) },
            onUpdateDay = { day, isActive, start, end, slotMin ->
                onEvent(DoctorAppointmentsUiEvent.UpdateDayAvailability(day, isActive, start, end, slotMin))
            },
            onCopyWeekdays = { onEvent(DoctorAppointmentsUiEvent.CopyWeekdayAvailability(DayOfWeek.MONDAY)) },
            onSave = { onEvent(DoctorAppointmentsUiEvent.SaveAvailability) }
        )
    }

    // 2. Diálogo de Reserva Rápida en Slot Libre
    state.bookingTargetSlot?.let { targetSlot ->
        QuickBookingDialog(
            slot = targetSlot,
            patients = state.registeredPatients,
            isBooking = state.isBooking,
            onDismiss = { onEvent(DoctorAppointmentsUiEvent.DismissQuickBooking) },
            onConfirm = { patientId, reason ->
                onEvent(DoctorAppointmentsUiEvent.ConfirmQuickBooking(patientId, reason))
            }
        )
    }

    // 3. Diálogo de Reagendamiento
    state.reschedulingAppointment?.let { appt ->
        RescheduleAppointmentDialog(
            appointment = appt,
            targetDate = state.rescheduleTargetDate,
            availableSlots = state.rescheduleAvailableSlots,
            isRescheduling = state.isRescheduling,
            onTargetDateChanged = { onEvent(DoctorAppointmentsUiEvent.ChangeRescheduleTargetDate(it)) },
            onDismiss = { onEvent(DoctorAppointmentsUiEvent.DismissReschedule) },
            onConfirm = { newStartTime ->
                onEvent(DoctorAppointmentsUiEvent.ConfirmReschedule(newStartTime))
            }
        )
    }

    // 4. Confirmación de Cancelación de Cita
    state.cancellingAppointment?.let { appt ->
        AlertDialog(
            onDismissRequest = { onEvent(DoctorAppointmentsUiEvent.DismissCancelAppointment) },
            title = {
                Text("¿Cancelar cita odontológica?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Se cancelará la cita de ${appt.patientName} programada para las ${formatTimeOnly(appt.startTime)}. El espacio quedará libre nuevamente en el cronograma."
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(DoctorAppointmentsUiEvent.ConfirmCancelAppointment) },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCancelled)
                ) {
                    if (state.isCancelling) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BiodontosWhite, strokeWidth = 2.dp)
                    } else {
                        Text("Confirmar Cancelación", color = BiodontosWhite)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(DoctorAppointmentsUiEvent.DismissCancelAppointment) }) {
                    Text("Mantener Cita", color = BiodontosDark)
                }
            }
        )
    }
}

// --- SUBCOMPONENTES ---

@Composable
private fun DoctorAppointmentsHeader(
    doctorName: String,
    isRefreshing: Boolean,
    onNavigateBack: () -> Unit,
    onOpenAvailability: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = BiodontosDark
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "CALENDARIO CLÍNICO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosGreyText,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Agenda y Horarios",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Botón Configurar Horario
            OutlinedButton(
                onClick = onOpenAvailability,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BiodontosDark)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mi Horario", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
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
}

@Composable
private fun DoctorScheduleModeSelector(
    currentMode: DoctorScheduleMode,
    onModeSelected: (DoctorScheduleMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BiodontosInputBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DoctorScheduleMode.entries.forEach { mode ->
            val isSelected = currentMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) BiodontosDark else Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BiodontosWhite else BiodontosDark
                )
            }
        }
    }
}

@Composable
private fun DoctorDayStrip(
    selectedDate: LocalDate,
    today: LocalDate,
    appointments: List<DoctorAppointmentItem>,
    onDateSelected: (LocalDate) -> Unit
) {
    val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    val monthTitle = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CO")))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cabecera del mes con flechas de semana y botón Hoy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onDateSelected(selectedDate.minusWeeks(1)) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Semana anterior",
                        modifier = Modifier.size(16.dp),
                        tint = BiodontosDark
                    )
                }

                if (selectedDate != today) {
                    TextButton(
                        onClick = { onDateSelected(today) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text("Hoy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BiodontosDark)
                    }
                }

                IconButton(
                    onClick = { onDateSelected(selectedDate.plusWeeks(1)) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Semana siguiente",
                        modifier = Modifier.size(16.dp),
                        tint = BiodontosDark
                    )
                }
            }
        }

        // Tira de 7 días
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            weekDays.forEach { date ->
                val isSelected = date == selectedDate
                val isToday = date == today
                val hasAppointments = appointments.any { it.startTime.toLocalDate() == date }

                val dayNameShort = when (date.dayOfWeek) {
                    DayOfWeek.MONDAY -> "LUN"
                    DayOfWeek.TUESDAY -> "MAR"
                    DayOfWeek.WEDNESDAY -> "MIÉ"
                    DayOfWeek.THURSDAY -> "JUE"
                    DayOfWeek.FRIDAY -> "VIE"
                    DayOfWeek.SATURDAY -> "SÁB"
                    DayOfWeek.SUNDAY -> "DOM"
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDateSelected(date) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) BiodontosDark else BiodontosWhite
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = dayNameShort,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) BiodontosWhite.copy(alpha = 0.8f) else BiodontosGreyText
                        )
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) BiodontosWhite else if (isToday) BiodontosDark else BiodontosDark.copy(alpha = 0.8f)
                        )

                        // Indicador de citas agendadas
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        hasAppointments && isSelected -> StatusConfirmed
                                        hasAppointments -> BiodontosDark
                                        else -> Color.Transparent
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorTimelineSlotRow(
    slot: DoctorTimelineSlot,
    onBookSlot: () -> Unit,
    onToggleConfirm: (DoctorAppointmentItem) -> Unit,
    onReschedule: (DoctorAppointmentItem) -> Unit,
    onCancel: (DoctorAppointmentItem) -> Unit
) {
    val startTimeFormatted = formatTimeOnly(slot.startTime)
    val endTimeFormatted = formatTimeOnly(slot.endTime)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gutter de Hora
        Column(
            modifier = Modifier.width(68.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = startTimeFormatted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )
            Text(
                text = endTimeFormatted,
                fontSize = 10.sp,
                color = BiodontosGreyText
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Tarjeta de Contenido del Bloque
        when (val status = slot.status) {
            is DoctorSlotStatus.Occupied -> {
                DoctorSlotOccupiedCard(
                    appointment = status.appointment,
                    onToggleConfirm = { onToggleConfirm(status.appointment) },
                    onReschedule = { onReschedule(status.appointment) },
                    onCancel = { onCancel(status.appointment) }
                )
            }
            DoctorSlotStatus.Available -> {
                DoctorSlotAvailableCard(
                    slot = slot,
                    onClick = onBookSlot
                )
            }
            DoctorSlotStatus.Past -> {
                DoctorSlotPastCard(slot = slot)
            }
        }
    }
}

@Composable
private fun DoctorSlotOccupiedCard(
    appointment: DoctorAppointmentItem,
    onToggleConfirm: () -> Unit,
    onReschedule: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(BiodontosDark.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = BiodontosDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = appointment.patientName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = appointment.treatmentReason,
                            fontSize = 11.sp,
                            color = BiodontosGreyText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Badge de confirmación
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (appointment.isConfirmed) StatusConfirmed.copy(alpha = 0.12f)
                            else StatusCancelled.copy(alpha = 0.12f)
                        )
                        .clickable { onToggleConfirm() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (appointment.isConfirmed) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (appointment.isConfirmed) StatusConfirmed else StatusCancelled,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (appointment.isConfirmed) "Confirmada" else "Sin Confirmar",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (appointment.isConfirmed) StatusConfirmed else StatusCancelled
                        )
                    }
                }
            }

            // Acciones de Cita (Reagendar / Cancelar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onReschedule,
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Reagendar",
                        modifier = Modifier.size(13.dp),
                        tint = BiodontosDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reagendar", fontSize = 11.sp, color = BiodontosDark, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cancelar",
                        modifier = Modifier.size(13.dp),
                        tint = StatusCancelled
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancelar", fontSize = 11.sp, color = StatusCancelled, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DoctorSlotAvailableCard(
    slot: DoctorTimelineSlot,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = BiodontosDark.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(vertical = 14.dp, horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = BiodontosDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Espacio Libre • Toca para agendar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BiodontosDark
                    )
                }

                Text(
                    text = "${slot.durationMinutes} min",
                    fontSize = 11.sp,
                    color = BiodontosGreyText
                )
            }
        }
    }
}

@Composable
private fun DoctorSlotPastCard(slot: DoctorTimelineSlot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosInputBg.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Horario finalizado",
                fontSize = 11.sp,
                color = BiodontosGreyText
            )
        }
    }
}

@Composable
private fun DoctorDayOffView(
    date: LocalDate,
    onOpenAvailability: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(BiodontosInputBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = BiodontosDark,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = "Día no laborable configurado",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )

            Text(
                text = "No tienes horas de atención configuradas para este día de la semana. Puedes activar este día y definir tu horario en cualquier momento.",
                fontSize = 13.sp,
                color = BiodontosGreyText,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onOpenAvailability,
                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Personalizar Disponibilidad", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DoctorAppointmentCard(
    item: DoctorAppointmentItem,
    onToggleConfirm: () -> Unit,
    onReschedule: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    DateBadge(dateTime = item.startTime)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = item.patientName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                        Text(
                            text = item.treatmentReason,
                            fontSize = 12.sp,
                            color = BiodontosGreyText
                        )
                        Text(
                            text = "Hora: ${formatTimeOnly(item.startTime)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BiodontosDark.copy(alpha = 0.8f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (item.isConfirmed) StatusConfirmed.copy(alpha = 0.12f)
                            else StatusCancelled.copy(alpha = 0.12f)
                        )
                        .clickable { onToggleConfirm() }
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
            }

            // Acciones Reagendar y Cancelar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReschedule,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BiodontosDark),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reagendar", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCancelled),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(13.dp), tint = StatusCancelled)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancelar", fontSize = 11.sp, color = StatusCancelled)
                }
            }
        }
    }
}

// --- DIÁLOGOS DE GESTIÓN ---

@Composable
private fun DoctorAvailabilityDialog(
    weeklyConfig: List<DayAvailabilityConfig>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onUpdateDay: (DayOfWeek, Boolean, LocalTime, LocalTime, Int) -> Unit,
    onCopyWeekdays: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BiodontosWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DISPONIBILIDAD SEMANAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosGreyText,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = "Personalizar Horario",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = BiodontosDark)
                    }
                }

                // Botón rápido para copiar Lunes a Viernes
                OutlinedButton(
                    onClick = onCopyWeekdays,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚡ Aplicar horario de Lun a todos los días hábiles", fontSize = 11.sp, color = BiodontosDark)
                }

                // Lista de días de la semana
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    weeklyConfig.forEach { config ->
                        DayAvailabilityItemRow(
                            config = config,
                            onConfigChanged = { isActive, start, end, slotMin ->
                                onUpdateDay(config.dayOfWeek, isActive, start, end, slotMin)
                            }
                        )
                    }
                }

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = BiodontosGreyText)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onSave,
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BiodontosWhite, strokeWidth = 2.dp)
                        } else {
                            Text("Guardar Horario", color = BiodontosWhite)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayAvailabilityItemRow(
    config: DayAvailabilityConfig,
    onConfigChanged: (Boolean, LocalTime, LocalTime, Int) -> Unit
) {
    val dayName = when (config.dayOfWeek) {
        DayOfWeek.MONDAY -> "Lunes"
        DayOfWeek.TUESDAY -> "Martes"
        DayOfWeek.WEDNESDAY -> "Miércoles"
        DayOfWeek.THURSDAY -> "Jueves"
        DayOfWeek.FRIDAY -> "Viernes"
        DayOfWeek.SATURDAY -> "Sábado"
        DayOfWeek.SUNDAY -> "Domingo"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosInputBg.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )

                Switch(
                    checked = config.isActive,
                    onCheckedChange = { active ->
                        onConfigChanged(active, config.startTime, config.endTime, config.slotMinutes)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BiodontosWhite,
                        checkedTrackColor = BiodontosDark
                    )
                )
            }

            if (config.isActive) {
                // Selector de horas inicio y fin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inicio", fontSize = 10.sp, color = BiodontosGreyText)
                        TimeSelectorButton(
                            currentTime = config.startTime,
                            onTimeSelected = { newStart ->
                                onConfigChanged(config.isActive, newStart, config.endTime, config.slotMinutes)
                            }
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fin", fontSize = 10.sp, color = BiodontosGreyText)
                        TimeSelectorButton(
                            currentTime = config.endTime,
                            onTimeSelected = { newEnd ->
                                onConfigChanged(config.isActive, config.startTime, newEnd, config.slotMinutes)
                            }
                        )
                    }
                }

                // Selector de duración de slots (15, 30, 45, 60 min)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Duración de cada cita", fontSize = 10.sp, color = BiodontosGreyText)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            val isSelected = config.slotMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BiodontosDark else BiodontosWhite)
                                    .clickable {
                                        onConfigChanged(config.isActive, config.startTime, config.endTime, mins)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$mins m",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BiodontosWhite else BiodontosDark
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
private fun TimeSelectorButton(
    currentTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val formatted = currentTime.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es-CO")))

    val timeOptions = remember {
        (6..21).flatMap { hour ->
            listOf(LocalTime.of(hour, 0), LocalTime.of(hour, 30))
        }
    }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = BiodontosWhite
        ) {
            Text(
                text = formatted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = BiodontosDark,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        if (expanded) {
            Dialog(onDismissRequest = { expanded = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BiodontosWhite,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .heightIn(max = 320.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        items(timeOptions) { time ->
                            val label = time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es-CO")))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (time == currentTime) FontWeight.Bold else FontWeight.Normal,
                                color = if (time == currentTime) BiodontosDark else BiodontosGreyText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onTimeSelected(time)
                                        expanded = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickBookingDialog(
    slot: DoctorTimelineSlot,
    patients: List<Patient>,
    isBooking: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (patientId: String, reason: String) -> Unit
) {
    var selectedPatientId by remember { mutableStateOf(patients.firstOrNull()?.id ?: "") }
    var reason by remember { mutableStateOf("Control odontológico") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val startFormatted = formatTimeOnly(slot.startTime)
    val endFormatted = formatTimeOnly(slot.endTime)
    val dateFormatted = slot.startTime.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CO")))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BiodontosWhite,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RESERVA RÁPIDA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosGreyText,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = "Agendar en Espacio Libre",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = BiodontosDark)
                    }
                }

                // Resumen del bloque
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BiodontosInputBg)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = dateFormatted, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BiodontosDark)
                        Text(text = "Horario: $startFormatted - $endFormatted (${slot.durationMinutes} minutos)", fontSize = 11.sp, color = BiodontosGreyText)
                    }
                }

                // Selector de Paciente
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Seleccionar Paciente", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BiodontosDark)

                    if (patients.isEmpty()) {
                        Text("No hay pacientes registrados en el sistema.", fontSize = 12.sp, color = StatusCancelled)
                    } else {
                        val currentPatient = patients.find { it.id == selectedPatientId } ?: patients.first()

                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = currentPatient.fullName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                patients.forEach { pat ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(pat.fullName, fontWeight = FontWeight.Bold)
                                                Text("Doc: ${pat.dniCedula}", fontSize = 11.sp, color = BiodontosGreyText)
                                            }
                                        },
                                        onClick = {
                                            selectedPatientId = pat.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Motivo del Procedimiento
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Motivo de la Cita", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BiodontosDark)
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Acciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = BiodontosGreyText)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirm(selectedPatientId, reason) },
                        enabled = !isBooking && selectedPatientId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isBooking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BiodontosWhite, strokeWidth = 2.dp)
                        } else {
                            Text("Confirmar Cita", color = BiodontosWhite)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RescheduleAppointmentDialog(
    appointment: DoctorAppointmentItem,
    targetDate: LocalDate,
    availableSlots: List<LocalDateTime>,
    isRescheduling: Boolean,
    onTargetDateChanged: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (newStartTime: LocalDateTime) -> Unit
) {
    var selectedStartTime by remember { mutableStateOf<LocalDateTime?>(null) }
    val candidateDates = remember {
        (0..7).map { LocalDate.now().plusDays(it.toLong()) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BiodontosWhite,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REAGENDAR CITA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosGreyText,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = appointment.patientName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = BiodontosDark)
                    }
                }

                Text(
                    text = "Selecciona la nueva fecha y uno de los bloques libres disponibles:",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )

                // Selector de Fecha Destino
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    candidateDates.take(5).forEach { date ->
                        val isSelected = date == targetDate
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BiodontosDark else BiodontosInputBg)
                                .clickable {
                                    onTargetDateChanged(date)
                                    selectedStartTime = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) BiodontosWhite else BiodontosDark
                                )
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("es-CO"))).uppercase(),
                                    fontSize = 9.sp,
                                    color = if (isSelected) BiodontosWhite.copy(alpha = 0.8f) else BiodontosGreyText
                                )
                            }
                        }
                    }
                }

                // Grid de Bloques Libres
                Text(
                    text = "Bloques Disponibles",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BiodontosDark
                )

                if (availableSlots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay espacios libres disponibles en esta fecha.",
                            fontSize = 12.sp,
                            color = BiodontosGreyText
                        )
                    }
                } else {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableSlots.forEach { slotTime ->
                            val isSelected = selectedStartTime == slotTime
                            val label = formatTimeOnly(slotTime)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BiodontosDark else BiodontosInputBg)
                                    .clickable { selectedStartTime = slotTime }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BiodontosWhite else BiodontosDark
                                )
                            }
                        }
                    }
                }

                // Acciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = BiodontosGreyText)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { selectedStartTime?.let { onConfirm(it) } },
                        enabled = !isRescheduling && selectedStartTime != null,
                        colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isRescheduling) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BiodontosWhite, strokeWidth = 2.dp)
                        } else {
                            Text("Confirmar Cambio", color = BiodontosWhite)
                        }
                    }
                }
            }
        }
    }
}

// Utilidad para formatear horas
private fun formatTimeOnly(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es-CO"))
    return runCatching { dateTime.format(formatter) }
        .getOrDefault("${dateTime.hour}:${dateTime.minute}")
}
