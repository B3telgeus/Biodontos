package com.example.biodontos.presentation.patientportal.appointments

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.biodontos.core.designsystem.BiodontosErrorRed
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.components.AppointmentStatusChip
import com.example.biodontos.core.designsystem.components.BiodontosPillButton
import com.example.biodontos.core.designsystem.components.DateBadge
import com.example.biodontos.core.designsystem.components.PillButtonVariant
import com.example.biodontos.core.designsystem.components.TwoStepConfirmationDialog
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.Doctor
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MyAppointmentsRoute(
    viewModel: MyAppointmentsViewModel,
    onNavigateToReschedule: (String, String) -> Unit,
    onNavigateToBookNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MyAppointmentsUiEffect.NavigateToReschedule -> {
                    onNavigateToReschedule(effect.doctorId, effect.appointmentId)
                }
                is MyAppointmentsUiEffect.NavigateToDoctorSearch -> {
                    onNavigateToBookNew()
                }
                is MyAppointmentsUiEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    MyAppointmentsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@VisibleForTesting
@Composable
fun MyAppointmentsScreen(
    state: MyAppointmentsUiState,
    onEvent: (MyAppointmentsUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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
                        text = "Tus Citas",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                }

                BiodontosPillButton(
                    text = "Nueva Cita",
                    variant = PillButtonVariant.DARK,
                    onClick = { onEvent(MyAppointmentsUiEvent.BookNewAppointment) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Pestañas (Segmented Pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BiodontosInputBg, shape = RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                TabItem(
                    text = "Próximas (${state.upcomingAppointments.size})",
                    isSelected = state.selectedTab == AppointmentTab.UPCOMING,
                    onClick = { onEvent(MyAppointmentsUiEvent.SelectTab(AppointmentTab.UPCOMING)) },
                    modifier = Modifier.weight(1f)
                )
                TabItem(
                    text = "Historial (${state.pastAppointments.size})",
                    isSelected = state.selectedTab == AppointmentTab.PAST,
                    onClick = { onEvent(MyAppointmentsUiEvent.SelectTab(AppointmentTab.PAST)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de Citas
            val activeList = if (state.selectedTab == AppointmentTab.UPCOMING) {
                state.upcomingAppointments
            } else {
                state.pastAppointments
            }

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
                activeList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (state.selectedTab == AppointmentTab.UPCOMING)
                                    "No tienes citas próximas agendadas."
                                else
                                    "No tienes historial de citas finalizadas.",
                                color = BiodontosGreyText,
                                fontSize = 14.sp
                            )
                            if (state.selectedTab == AppointmentTab.UPCOMING) {
                                BiodontosPillButton(
                                    text = "Buscar Especialista",
                                    variant = PillButtonVariant.OUTLINED,
                                    onClick = { onEvent(MyAppointmentsUiEvent.BookNewAppointment) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(activeList, key = { it.id }) { appointment ->
                            val doctor = appointment.doctorId?.let { state.doctorsMap[it] }
                            AppointmentItemCard(
                                appointment = appointment,
                                doctor = doctor,
                                isUpcoming = state.selectedTab == AppointmentTab.UPCOMING,
                                onRescheduleClick = { onEvent(MyAppointmentsUiEvent.RescheduleAppointment(appointment)) },
                                onCancelClick = { onEvent(MyAppointmentsUiEvent.RequestCancelAppointment(appointment)) }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Diálogo de confirmación de 2 pasos para cancelar cita (Regla GEMINI §4)
    state.appointmentToCancel?.let { appt ->
        val doctorName = appt.doctorId?.let { state.doctorsMap[it]?.displayName } ?: "el especialista"
        val timeFmt = appt.startTime.format(DateTimeFormatter.ofPattern("dd 'de' MMMM, hh:mm a", Locale.forLanguageTag("es-CO")))

        TwoStepConfirmationDialog(
            title = "Cancelar Cita",
            impactSummary = "¿Estás seguro de cancelar tu cita del $timeFmt con $doctorName?",
            confirmActionLabel = "Sí, cancelar cita",
            onConfirm = { onEvent(MyAppointmentsUiEvent.ConfirmCancelAppointment) },
            onDismiss = { onEvent(MyAppointmentsUiEvent.DismissCancelDialog) }
        )
    }
}

@Composable
private fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) BiodontosDark else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) BiodontosWhite else BiodontosDark
        )
    }
}

@Composable
private fun AppointmentItemCard(
    appointment: Appointment,
    doctor: Doctor?,
    isUpcoming: Boolean,
    onRescheduleClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es-CO"))
    val startStr = appointment.startTime.format(timeFormatter)
    val endStr = appointment.endTime?.format(timeFormatter) ?: ""
    val timeText = if (endStr.isNotBlank()) "⏰ $startStr - $endStr" else "⏰ $startStr"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateBadge(dateTime = appointment.startTime)
                    Column {
                        Text(
                            text = doctor?.displayName ?: appointment.doctorName ?: "Doctor Asignado",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                        doctor?.specialty?.let {
                            Text(
                                text = it,
                                fontSize = 12.sp,
                                color = BiodontosGreyText
                            )
                        }
                    }
                }

                AppointmentStatusChip(status = appointment.status)
            }

            Text(
                text = timeText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = BiodontosDark
            )

            appointment.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                Text(
                    text = "Motivo: $reason",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
            }

            if (isUpcoming) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BiodontosPillButton(
                        text = "Reagendar",
                        variant = PillButtonVariant.OUTLINED,
                        onClick = onRescheduleClick,
                        modifier = Modifier.weight(1f)
                    )
                    BiodontosPillButton(
                        text = "Cancelar",
                        variant = PillButtonVariant.LIGHT,
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
