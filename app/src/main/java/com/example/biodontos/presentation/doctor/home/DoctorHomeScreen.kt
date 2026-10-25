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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.Treatment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosLightGrey
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.domain.model.CopAmount
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
        containerColor = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
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
                                    onNewPaymentClick = { onEvent(DoctorHomeUiEvent.OpenRecordPaymentDialog) },
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
                                    payments = state.recentPayments,
                                    onNewPaymentClick = { onEvent(DoctorHomeUiEvent.OpenRecordPaymentDialog) }
                                )
                            }
                        }
                    }
                }
            }

            if (state.isRecordPaymentDialogOpen) {
                RecordPaymentDialog(
                    state = state,
                    onDismiss = { onEvent(DoctorHomeUiEvent.DismissRecordPaymentDialog) },
                    onSelectPatient = { onEvent(DoctorHomeUiEvent.SelectPatientForPayment(it)) },
                    onSelectTreatment = { onEvent(DoctorHomeUiEvent.SelectTreatmentForPayment(it)) },
                    onSubmit = { patientId, treatmentId, newDesc, newCost, amount, method ->
                        onEvent(DoctorHomeUiEvent.SubmitPayment(patientId, treatmentId, newDesc, newCost, amount, method))
                    }
                )
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
    onNewPaymentClick: () -> Unit,
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
                    text = "Tus últimos pagos registrados",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
            }

            if (payments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF7F8FA))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No hay abonos registrados aún",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = BiodontosGreyText
                        )
                        Text(
                            text = "Pulsa el botón inferior para ingresar el primer abono.",
                            fontSize = 11.sp,
                            color = BiodontosGreyText
                        )
                    }
                }
            } else {
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
                                    text = "${payment.treatmentDescription} • ${payment.dateStr}",
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
            }

            BiodontosPillButton(
                text = "Nuevo Pago o Abono",
                variant = PillButtonVariant.DARK,
                onClick = onNewPaymentClick
            )
        }
    }
}

@Composable
private fun RecordPaymentDialog(
    state: DoctorHomeUiState,
    onDismiss: () -> Unit,
    onSelectPatient: (Patient) -> Unit,
    onSelectTreatment: (Treatment?) -> Unit,
    onSubmit: (patientId: String, treatmentId: String?, newDesc: String?, newCost: Long?, amount: Long, method: String) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("EFECTIVO") }
    var isNewTreatmentMode by remember(state.selectedPatient, state.availableTreatments) {
        mutableStateOf(state.availableTreatments.isEmpty())
    }
    var newTreatmentDesc by remember { mutableStateOf("") }
    var newTreatmentCostInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val methods = listOf("EFECTIVO", "TARJETA", "TRANSFERENCIA")

    AlertDialog(
        onDismissRequest = { if (!state.isRecordingPayment) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = BiodontosWhite,
        title = {
            Column {
                Text(
                    text = "Registrar Abono / Pago",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
                Text(
                    text = "Vincula el abono al tratamiento del paciente",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Selección de Paciente
                Text(
                    text = "1. Paciente",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )

                if (state.selectedPatient == null) {
                    if (state.availablePatients.isEmpty()) {
                        Text(
                            text = "No hay pacientes registrados en el sistema.",
                            fontSize = 12.sp,
                            color = BiodontosGreyText
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.availablePatients.forEach { pat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BiodontosLightGrey)
                                        .clickable { onSelectPatient(pat) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = pat.fullName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BiodontosDark
                                        )
                                        Text(
                                            text = "DNI: ${pat.dniCedula}",
                                            fontSize = 11.sp,
                                            color = BiodontosGreyText
                                        )
                                    }
                                    Text(
                                        text = "Seleccionar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BiodontosDark
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8ECEF))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = state.selectedPatient.fullName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                            Text(
                                text = "DNI: ${state.selectedPatient.dniCedula}",
                                fontSize = 11.sp,
                                color = BiodontosGreyText
                            )
                        }
                        TextButton(onClick = { onDismiss() }) {
                            Text("Cambiar", color = BiodontosDark, fontSize = 12.sp)
                        }
                    }

                    // 2. Selección de Tratamiento
                    Text(
                        text = "2. Tratamiento / Concepto",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )

                    if (state.isLoadingTreatments) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BiodontosDark)
                    } else {
                        if (state.availableTreatments.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val hasExisting = !isNewTreatmentMode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (hasExisting) BiodontosDark else BiodontosLightGrey)
                                        .clickable { isNewTreatmentMode = false }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Existente",
                                        fontSize = 11.sp,
                                        color = if (hasExisting) BiodontosWhite else BiodontosDark
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isNewTreatmentMode) BiodontosDark else BiodontosLightGrey)
                                        .clickable { 
                                            isNewTreatmentMode = true
                                            onSelectTreatment(null)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Nuevo Plan",
                                        fontSize = 11.sp,
                                        color = if (isNewTreatmentMode) BiodontosWhite else BiodontosDark
                                    )
                                }
                            }
                        }

                        if (!isNewTreatmentMode && state.availableTreatments.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                state.availableTreatments.forEach { tr ->
                                    val isSelected = state.selectedTreatment?.id == tr.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFFD6E4FF) else BiodontosLightGrey)
                                            .clickable { onSelectTreatment(tr) }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tr.description,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = BiodontosDark
                                        )
                                        Text(
                                            text = tr.totalCost.formatCop(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BiodontosDark
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = newTreatmentDesc,
                                onValueChange = { newTreatmentDesc = it },
                                label = { Text("Descripción del Tratamiento") },
                                placeholder = { Text("Ej. Profilaxis y Resinas, Ortodoncia") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = newTreatmentCostInput,
                                onValueChange = { newTreatmentCostInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Costo Total Proyectado (COP)") },
                                placeholder = { Text("Ej. 500000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // 3. Monto y Método
                    Text(
                        text = "3. Monto del Abono y Método",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Monto a abonar (COP)") },
                        placeholder = { Text("Ej. 150000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        methods.forEach { method ->
                            val isSelected = selectedMethod == method
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BiodontosDark else BiodontosLightGrey)
                                    .clickable { selectedMethod = method }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = method.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BiodontosWhite else BiodontosDark
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pat = state.selectedPatient
                    if (pat == null) {
                        errorMessage = "Por favor selecciona un paciente"
                        return@Button
                    }
                    val amountLong = amountInput.toLongOrNull() ?: 0L
                    if (amountLong <= 0) {
                        errorMessage = "El monto debe ser mayor a 0"
                        return@Button
                    }

                    val treatmentId = if (!isNewTreatmentMode) state.selectedTreatment?.id else null
                    if (!isNewTreatmentMode && treatmentId == null && state.availableTreatments.isNotEmpty()) {
                        errorMessage = "Selecciona un tratamiento existente o marca 'Nuevo Plan'"
                        return@Button
                    }

                    val newDesc = if (isNewTreatmentMode) newTreatmentDesc.trim().ifBlank { "Tratamiento Odontológico" } else null
                    val newCost = if (isNewTreatmentMode) newTreatmentCostInput.toLongOrNull() ?: amountLong else null

                    errorMessage = null
                    onSubmit(pat.id, treatmentId, newDesc, newCost, amountLong, selectedMethod)
                },
                enabled = !state.isRecordingPayment && state.selectedPatient != null,
                colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (state.isRecordingPayment) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BiodontosWhite, strokeWidth = 2.dp)
                } else {
                    Text("Guardar Abono", color = BiodontosWhite, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isRecordingPayment
            ) {
                Text("Cancelar", color = BiodontosDark)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun DoctorHomeLoadingPreview() {
    DentalTheme {
        DoctorHomeScreen(
            state = DoctorHomeUiState(isLoading = true),
            onEvent = {},
            onNavigateToPatients = {},
            onNavigateToNewPatient = {},
            onNavigateToAppointments = {},
            onNavigateToDocuments = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorHomeSuccessPreview() {
    DentalTheme {
        DoctorHomeScreen(
            state = DoctorHomeUiState(
                isLoading = false,
                doctorName = "Dr. Carlos Restrepo",
                patientsCount = 24,
                todayAppointmentsCount = 5,
                upcomingAppointments = listOf(
                    DoctorAppointmentItem(
                        appointmentId = "apt-1",
                        patientName = "María Gómez",
                        treatmentReason = "Limpieza dental",
                        startTime = LocalDateTime.of(2026, 10, 23, 10, 0),
                        isConfirmed = true
                    )
                ),
                recentPayments = listOf(
                    DoctorPaymentItem(
                        id = "pay-1",
                        patientName = "María Gómez",
                        treatmentDescription = "Profilaxis",
                        dateStr = "23/10/2026",
                        amountCop = CopAmount(120000L),
                        paymentMethod = "EFECTIVO"
                    )
                )
            ),
            onEvent = {},
            onNavigateToPatients = {},
            onNavigateToNewPatient = {},
            onNavigateToAppointments = {},
            onNavigateToDocuments = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorHomeErrorPreview() {
    DentalTheme {
        DoctorHomeScreen(
            state = DoctorHomeUiState(
                isLoading = false,
                doctorName = "Dr. Carlos Restrepo",
                errorMessage = "Error de conexión al sincronizar el panel del doctor."
            ),
            onEvent = {},
            onNavigateToPatients = {},
            onNavigateToNewPatient = {},
            onNavigateToAppointments = {},
            onNavigateToDocuments = {}
        )
    }
}
