package com.example.biodontos.presentation.patientportal.appointments

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.components.BiodontosPillButton
import com.example.biodontos.core.designsystem.components.PillButtonVariant
import com.example.biodontos.domain.scheduling.TimeSlot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BookAppointmentRoute(
    viewModel: BookAppointmentViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMyAppointments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BookAppointmentUiEffect.NavigateToMyAppointments -> onNavigateToMyAppointments()
                is BookAppointmentUiEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    BookAppointmentScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@VisibleForTesting
@Composable
fun BookAppointmentScreen(
    state: BookAppointmentUiState,
    onEvent: (BookAppointmentUiEvent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val isRescheduling = state.rescheduleAppointmentId != null
    val titleText = if (isRescheduling) "Reagendar Cita" else "Agendar Cita"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFE9E9EB),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Barra inferior con botón de confirmación consciente de insets
            com.example.biodontos.core.designsystem.insets.BiodontosBottomActionBar(
                containerColor = BiodontosWhite,
                contentPadding = PaddingValues(16.dp)
            ) {
                BiodontosPillButton(
                    text = when {
                        state.isSubmitting -> "Procesando..."
                        isRescheduling -> "Confirmar Reagendamiento"
                        else -> "Confirmar Reserva"
                    },
                    variant = PillButtonVariant.DARK,
                    enabled = state.selectedSlot != null && !state.isSubmitting,
                    isLoading = state.isSubmitting,
                    onClick = { onEvent(BookAppointmentUiEvent.ConfirmBooking) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Barra superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
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
                    text = titleText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Tarjeta del Doctor
                state.doctor?.let { doctor ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(BiodontosInputBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = BiodontosDark.copy(alpha = 0.6f),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = doctor.displayName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BiodontosDark
                                    )
                                    Text(
                                        text = doctor.specialty,
                                        fontSize = 13.sp,
                                        color = BiodontosGreyText
                                    )
                                }
                            }
                        }
                    }
                }

                // Sección 1: Selector de fecha (Horizontal Strip)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "1. SELECCIONA LA FECHA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosGreyText,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.availableDates) { date ->
                                    DateStripItem(
                                        date = date,
                                        isSelected = date == state.selectedDate,
                                        onClick = { onEvent(BookAppointmentUiEvent.SelectDate(date)) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Sección 2: Horarios disponibles
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "2. HORARIOS DISPONIBLES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosGreyText,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            when {
                                state.isSlotsLoading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = BiodontosDark)
                                    }
                                }
                                state.slots.isEmpty() -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No hay horarios disponibles para esta fecha. Intenta con otro día.",
                                            fontSize = 13.sp,
                                            color = BiodontosGreyText
                                        )
                                    }
                                }
                                else -> {
                                    val morningSlots = state.slots.filter { it.start.hour < 12 }
                                    val afternoonSlots = state.slots.filter { it.start.hour >= 12 }

                                    if (morningSlots.isNotEmpty()) {
                                        Text(
                                            text = "Mañana",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BiodontosDark
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            morningSlots.forEach { slot ->
                                                SlotChip(
                                                    slot = slot,
                                                    isSelected = slot == state.selectedSlot,
                                                    onClick = { onEvent(BookAppointmentUiEvent.SelectSlot(slot)) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }

                                    if (afternoonSlots.isNotEmpty()) {
                                        Text(
                                            text = "Tarde",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BiodontosDark
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            afternoonSlots.forEach { slot ->
                                                SlotChip(
                                                    slot = slot,
                                                    isSelected = slot == state.selectedSlot,
                                                    onClick = { onEvent(BookAppointmentUiEvent.SelectSlot(slot)) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sección 3: Motivo de consulta
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "3. MOTIVO DE CONSULTA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosGreyText,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.reasonOptions.forEach { reason ->
                                    val isSelected = reason == state.selectedReason
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) BiodontosDark else BiodontosInputBg)
                                            .clickable { onEvent(BookAppointmentUiEvent.SelectReason(reason)) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = reason,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) BiodontosWhite else BiodontosDark
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.customReason,
                                onValueChange = { onEvent(BookAppointmentUiEvent.CustomReasonChanged(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = "Observación adicional (opcional)",
                                        fontSize = 13.sp,
                                        color = BiodontosGreyText
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = BiodontosWhite,
                                    unfocusedContainerColor = BiodontosInputBg,
                                    focusedBorderColor = BiodontosDark,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateStripItem(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("es-CO"))
    val dayName = date.format(dayFormatter).uppercase().take(3)
    val dayNumber = date.dayOfMonth.toString()

    Column(
        modifier = Modifier
            .width(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) BiodontosDark else BiodontosInputBg)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) BiodontosWhite.copy(alpha = 0.8f) else BiodontosGreyText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dayNumber,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) BiodontosWhite else BiodontosDark
        )
    }
}

@Composable
private fun SlotChip(
    slot: TimeSlot,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es-CO"))
    val timeStr = slot.start.format(timeFormatter)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) BiodontosDark else BiodontosInputBg)
            .border(
                width = 1.dp,
                color = if (isSelected) BiodontosDark else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = timeStr,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) BiodontosWhite else BiodontosDark
        )
    }
}
