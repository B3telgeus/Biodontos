package com.example.biodontos.presentation.patientportal.payments

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.biodontos.core.designsystem.Ambar
import com.example.biodontos.core.designsystem.BiodontosCardGradient
import com.example.biodontos.core.designsystem.BiodontosCardTop
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorRed
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosLightGrey
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.core.designsystem.StatusConfirmed
import com.example.biodontos.core.designsystem.components.BiodontosPillButton
import com.example.biodontos.core.designsystem.components.PillButtonVariant
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.TreatmentBalance
import java.time.LocalDateTime

@Composable
fun PatientPaymentsRoute(
    viewModel: PatientPaymentsViewModel,
    onNavigateBack: () -> Unit = {},
    canNavigateBack: Boolean = false
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PatientPaymentsUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    PatientPaymentsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        canNavigateBack = canNavigateBack,
        snackbarHostState = snackbarHostState
    )
}

@VisibleForTesting
@Composable
fun PatientPaymentsScreen(
    state: PatientPaymentsUiState,
    onEvent: (PatientPaymentsUiEvent) -> Unit,
    onNavigateBack: () -> Unit = {},
    canNavigateBack: Boolean = false,
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
                .imePadding()
        ) {
            if (state.isLoading && !state.isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BiodontosDark)
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val isExpanded = maxWidth >= 760.dp

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = if (isExpanded) 32.dp else 20.dp, vertical = 20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header Bar
                        PaymentsHeader(
                            patientName = state.patientName,
                            isRefreshing = state.isRefreshing,
                            onRefreshClick = { onEvent(PatientPaymentsUiEvent.Refresh) },
                            onBackClick = onNavigateBack,
                            canNavigateBack = canNavigateBack
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (state.errorMessage != null && state.balances.isEmpty()) {
                            ErrorCard(
                                message = state.errorMessage,
                                onRetry = { onEvent(PatientPaymentsUiEvent.Retry) }
                            )
                        } else {
                            if (isExpanded) {
                                // Layout 2 Columnas para Pantalla Grande / Tablet
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Columna Izquierda: Hero Card + Tratamientos
                                    Column(
                                        modifier = Modifier.weight(1.2f),
                                        verticalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        HeroBalanceCard(
                                            totalPendingFormatted = state.totalPending.formatCop(),
                                            totalPaidFormatted = state.totalPaid.formatCop(),
                                            hasPending = state.hasPendingBalance
                                        )

                                        TreatmentsSection(
                                            balances = state.filteredBalances,
                                            selectedFilter = state.selectedFilter,
                                            onFilterSelected = { onEvent(PatientPaymentsUiEvent.SelectFilter(it)) }
                                        )
                                    }

                                    // Columna Derecha: Abonos Recientes
                                    Column(
                                        modifier = Modifier.weight(0.9f),
                                        verticalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        RecentPaymentsCard(
                                            payments = state.recentPayments,
                                            onPaymentClick = { onEvent(PatientPaymentsUiEvent.ViewPaymentReceipt(it)) }
                                        )
                                        FinancialInfoCard()
                                    }
                                }
                            } else {
                                // Layout Vertical para Móvil
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    HeroBalanceCard(
                                        totalPendingFormatted = state.totalPending.formatCop(),
                                        totalPaidFormatted = state.totalPaid.formatCop(),
                                        hasPending = state.hasPendingBalance
                                    )

                                    TreatmentsSection(
                                        balances = state.filteredBalances,
                                        selectedFilter = state.selectedFilter,
                                        onFilterSelected = { onEvent(PatientPaymentsUiEvent.SelectFilter(it)) }
                                    )

                                    RecentPaymentsCard(
                                        payments = state.recentPayments,
                                        onPaymentClick = { onEvent(PatientPaymentsUiEvent.ViewPaymentReceipt(it)) }
                                    )
                                    FinancialInfoCard()
                                }
                            }
                        }
                    }
                }
            }

            if (state.selectedPaymentForReceipt != null) {
                PaymentReceiptDialog(
                    payment = state.selectedPaymentForReceipt,
                    balances = state.balances,
                    onDismiss = { onEvent(PatientPaymentsUiEvent.DismissPaymentReceipt) }
                )
            }
        }
    }
}

@Composable
private fun PaymentsHeader(
    patientName: String,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    canNavigateBack: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canNavigateBack) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = BiodontosDark
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column {
                Text(
                    text = "ESTADO DE CUENTA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosGreyText,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Pagos y Abonos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
            }
        }

        IconButton(
            onClick = onRefreshClick,
            enabled = !isRefreshing,
            modifier = Modifier.size(48.dp)
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

@Composable
private fun HeroBalanceCard(
    totalPendingFormatted: String,
    totalPaidFormatted: String,
    hasPending: Boolean,
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
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (hasPending) "SALDO TOTAL PENDIENTE" else "ESTADO GENERAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosGreyText,
                    letterSpacing = 1.sp
                )

                Text(
                    text = if (hasPending) totalPendingFormatted else "$ 0",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BiodontosDark
                )

                Text(
                    text = if (hasPending) {
                        "Total abonado acumulado: $totalPaidFormatted"
                    } else {
                        "¡Estás al día con todos tus tratamientos! 🎉"
                    },
                    fontSize = 13.sp,
                    color = BiodontosDark.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TreatmentsSection(
    balances: List<TreatmentBalance>,
    selectedFilter: PaymentFilter,
    onFilterSelected: (PaymentFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tratamientos y Procedimientos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )

            // Filtros de estado (Pills con altura mínima 48dp y háptica)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PaymentFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) BiodontosDark else BiodontosInputBg)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onFilterSelected(filter)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
        }

        if (balances.isEmpty()) {
            EmptyTreatmentsCard(filter = selectedFilter)
        } else {
            balances.forEach { balance ->
                TreatmentBalanceCard(balance = balance)
            }
        }
    }
}

@Composable
private fun TreatmentBalanceCard(
    balance: TreatmentBalance,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = balance.treatment.description.ifBlank { "Tratamiento Odontológico" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                    Text(
                        text = "Costo Total: ${balance.treatment.totalCost.formatCop()}",
                        fontSize = 12.sp,
                        color = BiodontosGreyText
                    )
                }

                // Badge de estado de pago
                if (balance.isPaidInFull) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusConfirmed.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = StatusConfirmed,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PAGADO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusConfirmed
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Ambar.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PENDIENTE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ambar
                        )
                    }
                }
            }

            // Barra de Progreso
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { balance.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (balance.isPaidInFull) StatusConfirmed else BiodontosDark,
                    trackColor = BiodontosInputBg
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Abonado: ${balance.paidAmount.formatCop()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BiodontosDark
                    )
                    Text(
                        text = "Saldo: ${balance.pendingAmount.formatCop()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (balance.isPaidInFull) StatusConfirmed else BiodontosErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentPaymentsCard(
    payments: List<Payment>,
    onPaymentClick: (Payment) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Historial de Abonos Recientes",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )

            if (payments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BiodontosCardTop)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BiodontosGreyText,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Aún no registras abonos o pagos en la clínica.",
                            fontSize = 13.sp,
                            color = BiodontosGreyText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                payments.forEach { payment ->
                    PaymentRow(
                        payment = payment,
                        onClick = { onPaymentClick(payment) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(
    payment: Payment,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dateStr = payment.createdAt?.let { dt ->
        "%02d/%02d/%d".format(dt.dayOfMonth, dt.monthValue, dt.year)
    } ?: "Fecha reciente"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BiodontosCardTop)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BiodontosDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = BiodontosWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = payment.paymentMethod?.ifBlank { "Abono a tratamiento" } ?: "Abono a tratamiento",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
            }
        }

        Text(
            text = "+ ${payment.amount.formatCop()}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = StatusConfirmed
        )
    }
}

@Composable
private fun FinancialInfoCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosCardTop),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = BiodontosDark,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Información sobre abonos",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
                Text(
                    text = "Los abonos realizados en la clínica se ven reflejados de forma inmediata. Para facturación electrónica con NIT o solicitudes de crédito, comunícate con el área de recepción.",
                    fontSize = 12.sp,
                    color = BiodontosGreyText,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyTreatmentsCard(
    filter: PaymentFilter,
    modifier: Modifier = Modifier
) {
    val (title, description, isSuccess) = when (filter) {
        PaymentFilter.PENDING -> Triple(
            "¡Al día!",
            "¡Excelente! No tienes saldos pendientes por pagar.",
            true
        )
        PaymentFilter.COMPLETED -> Triple(
            "Sin tratamientos completados",
            "Aún no tienes tratamientos totalmente pagados en tu historial.",
            false
        )
        PaymentFilter.ALL -> Triple(
            "Sin tratamientos en esta sección",
            "Tus planes dentales y presupuestos aprobados aparecerán aquí.",
            false
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSuccess) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(StatusConfirmed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = StatusConfirmed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSuccess) StatusConfirmed else BiodontosDark
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = BiodontosGreyText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                color = BiodontosErrorRed,
                fontWeight = FontWeight.Medium
            )
            BiodontosPillButton(
                text = "Reintentar",
                variant = PillButtonVariant.DARK,
                onClick = onRetry
            )
        }
    }
}

@Composable
fun PaymentReceiptDialog(
    payment: Payment,
    balances: List<TreatmentBalance>,
    onDismiss: () -> Unit
) {
    val treatmentDesc = balances.find { it.treatment.id == payment.treatmentId }
        ?.treatment?.description?.ifBlank { null }
        ?: "Tratamiento Odontológico General"

    val receiptNumber = if (payment.id.isNotBlank()) {
        "#REC-${payment.id.takeLast(6).uppercase()}"
    } else {
        "#REC-BIODONTOS"
    }

    val formattedDateTime = payment.createdAt?.let { dt ->
        val hour12 = if (dt.hour % 12 == 0) 12 else dt.hour % 12
        val amPm = if (dt.hour < 12) "a. m." else "p. m."
        "%02d/%02d/%d, %02d:%02d %s".format(dt.dayOfMonth, dt.monthValue, dt.year, hour12, dt.minute, amPm)
    } ?: "Fecha registrada en clínica"

    val methodLabel = payment.paymentMethod?.ifBlank { null } ?: "Abono en Sede"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .imePadding(),
            colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Encabezado y botón cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BIODONTOS CLÍNICA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosGreyText,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Comprobante de Abono",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar comprobante",
                            tint = BiodontosDark
                        )
                    }
                }

                // Número de comprobante y estado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BiodontosCardTop)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = receiptNumber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (payment.isVoided) BiodontosErrorRed.copy(alpha = 0.12f)
                                else StatusConfirmed.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (payment.isVoided) "ANULADO" else "APLICADO A CUENTA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (payment.isVoided) BiodontosErrorRed else StatusConfirmed
                        )
                    }
                }

                // Monto destacado
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(BiodontosCardTop)
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Monto Abonado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = BiodontosGreyText
                    )
                    Text(
                        text = "+ ${payment.amount.formatCop()}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (payment.isVoided) BiodontosGreyText else StatusConfirmed
                    )
                }

                // Desglose de información
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReceiptDetailRow(label = "Tratamiento Imputado", value = treatmentDesc)
                    ReceiptDetailRow(label = "Método de Pago", value = methodLabel)
                    ReceiptDetailRow(label = "Fecha y Hora", value = formattedDateTime)
                }

                // Nota informativa legal / sede clínica
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BiodontosCardTop)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BiodontosGreyText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Comprobante generado electrónicamente. Válido como soporte de abono en sede clínica.",
                        fontSize = 12.sp,
                        color = BiodontosGreyText,
                        lineHeight = 16.sp
                    )
                }

                // Botón Cerrar (minHeight >= 48dp)
                BiodontosPillButton(
                    text = "Entendido / Cerrar",
                    variant = PillButtonVariant.DARK,
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun ReceiptDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = BiodontosGreyText,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = BiodontosDark,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientPaymentsLoadingPreview() {
    DentalTheme {
        PatientPaymentsScreen(
            state = PatientPaymentsUiState(isLoading = true),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientPaymentsSuccessPreview() {
    val sampleTreatment = Treatment(
        id = "trt-1",
        patientId = PatientId("pat-1"),
        description = "Ortodoncia Correctiva",
        totalCost = CopAmount(2400000L)
    )
    val samplePayment = Payment(
        id = "pay-1",
        treatmentId = "trt-1",
        amount = CopAmount(400000L),
        paymentMethod = "TRANSFERENCIA",
        createdAt = LocalDateTime.of(2026, 10, 20, 11, 0)
    )
    DentalTheme {
        PatientPaymentsScreen(
            state = PatientPaymentsUiState(
                isLoading = false,
                patientName = "Laura Martínez",
                patientId = "pat-1",
                totalPending = CopAmount(2000000L),
                totalPaid = CopAmount(400000L),
                balances = listOf(
                    TreatmentBalance(
                        treatment = sampleTreatment,
                        paidAmount = CopAmount(400000L),
                        payments = listOf(samplePayment)
                    )
                ),
                recentPayments = listOf(samplePayment)
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientPaymentsErrorPreview() {
    DentalTheme {
        PatientPaymentsScreen(
            state = PatientPaymentsUiState(
                isLoading = false,
                errorMessage = "No fue posible consultar tu estado de cuenta."
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
