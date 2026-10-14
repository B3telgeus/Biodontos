package com.example.biodontos.presentation.doctor.appointments

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.StatusCancelled
import com.example.biodontos.core.designsystem.StatusConfirmed
import com.example.biodontos.core.designsystem.components.DateBadge
import com.example.biodontos.presentation.doctor.home.DoctorAppointmentItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
        containerColor = Color.Transparent
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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CALENDARIO CLÍNICO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosGreyText,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Agenda de Citas",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                        }
                    }

                    IconButton(
                        onClick = { onEvent(DoctorAppointmentsUiEvent.Refresh) },
                        enabled = !state.isRefreshing
                    ) {
                        if (state.isRefreshing) {
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

                // Filtros de citas
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
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BiodontosDark)
                    }
                } else if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.appointmentId }) { item ->
                            DoctorAppointmentCard(
                                item = item,
                                onToggleConfirm = {
                                    onEvent(DoctorAppointmentsUiEvent.ToggleConfirm(item.appointmentId, item.isConfirmed))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorAppointmentCard(
    item: DoctorAppointmentItem,
    onToggleConfirm: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es-CO"))
    val formattedTime = runCatching { item.startTime.format(timeFormatter) }
        .getOrDefault("${item.startTime.hour}:${item.startTime.minute}")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        text = "Hora: $formattedTime",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BiodontosDark.copy(alpha = 0.8f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
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

                IconButton(onClick = onToggleConfirm) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Cambiar confirmación",
                        tint = BiodontosDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
