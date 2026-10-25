package com.example.biodontos.presentation.patients.detail.history

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.Ambar
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorBg
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosLightGrey
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.Carmesi
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.core.designsystem.Esmeralda
import com.example.biodontos.core.designsystem.Zafiro
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import com.example.biodontos.domain.model.history.ActivityType
import com.example.biodontos.domain.model.history.AuditAction
import com.example.biodontos.domain.model.history.AuditStamp
import com.example.biodontos.domain.model.history.PatientActivity
import com.example.biodontos.domain.model.history.PatientHistorySummary
import com.example.biodontos.presentation.patients.detail.PatientHistoryUiState
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SPANISH_LOCALE = Locale.forLanguageTag("es-CO")
private val MONTH_HEADER_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", SPANISH_LOCALE)
private val DATE_TIME_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", SPANISH_LOCALE)
private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", SPANISH_LOCALE)

@OptIn(ExperimentalFoundationApi::class)
@VisibleForTesting
@Composable
fun PatientHistoryTab(
    historyState: PatientHistoryUiState,
    onSelectMonth: (YearMonth?) -> Unit,
    onToggleType: (ActivityType) -> Unit,
    onClearFilters: () -> Unit,
    onToggleExpandActivity: (String) -> Unit,
    onRequestVoidActivity: (PatientActivity) -> Unit,
    onOpenClarificationDialog: (Evolution) -> Unit,
    onOpenTreatmentStatusDialog: (Treatment) -> Unit,
    onOpenNewPaymentForTreatment: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    if (historyState.isTimelineLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = BiodontosDark)
        }
        return
    }

    if (historyState.timelineError != null && historyState.allActivities.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BiodontosWhite)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error de carga",
                        tint = Carmesi,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = historyState.timelineError,
                        color = BiodontosDark,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reintentar")
                    }
                }
            }
        }
        return
    }

    val groupedByMonth: Map<YearMonth?, List<PatientActivity>> =
        historyState.filteredActivities.groupBy { activity ->
            if (activity.isDateValid) YearMonth.from(activity.occurredAt) else null
        }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Banner de advertencia si alguna fuente falló parcialmente
        if (historyState.failedSources.isNotEmpty()) {
            item(key = "partial-failure-banner") {
                PartialSyncWarningBanner(
                    failedSources = historyState.failedSources,
                    onRetry = onRetry
                )
            }
        }

        // 2. KPI Header de Trazabilidad del Paciente
        item(key = "kpi-summary-header") {
            PatientHistoryKpiHeader(summary = historyState.summary)
        }

        // 3. Barra de Filtros por Mes y Tipo de Actividad
        item(key = "activity-filter-bar") {
            ActivityFilterBar(
                availableMonths = historyState.availableMonths,
                selectedMonth = historyState.filter.month,
                selectedTypes = historyState.filter.types,
                onSelectMonth = { month ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectMonth(month)
                },
                onToggleType = { type ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleType(type)
                },
                onClearFilters = onClearFilters
            )
        }

        // 4. Estado vacío o Línea de Tiempo agrupada por Mes
        if (historyState.filteredActivities.isEmpty()) {
            item(key = "empty-timeline") {
                EmptyHistoryStateCard(
                    hasAnyActivities = historyState.allActivities.isNotEmpty(),
                    onClearFilters = onClearFilters
                )
            }
        } else {
            groupedByMonth.forEach { (yearMonth, monthActivities) ->
                val headerKey = "header-${yearMonth ?: "invalid"}"
                stickyHeader(key = headerKey) {
                    MonthStickyHeader(yearMonth = yearMonth, count = monthActivities.size)
                }

                items(
                    items = monthActivities,
                    key = { it.id }
                ) { activity ->
                    PatientActivityTimelineCard(
                        activity = activity,
                        isExpanded = historyState.expandedActivityId == activity.id,
                        onToggleExpand = { onToggleExpandActivity(activity.id) },
                        onRequestVoid = { onRequestVoidActivity(activity) },
                        onOpenClarification = onOpenClarificationDialog,
                        onOpenTreatmentStatus = onOpenTreatmentStatusDialog,
                        onOpenNewPaymentForTreatment = onOpenNewPaymentForTreatment
                    )
                }
            }
        }
    }
}

@Composable
private fun PartialSyncWarningBanner(
    failedSources: Set<ActivityType>,
    onRetry: () -> Unit
) {
    val names = failedSources.joinToString(", ") { it.label }
    Surface(
        color = BiodontosErrorBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Sincronización parcial",
                    tint = Carmesi,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Algunas fuentes no pudieron sincronizarse ($names).",
                    fontSize = 12.sp,
                    color = Carmesi,
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(onClick = onRetry) {
                Text("Reintentar", color = Carmesi, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PatientHistoryKpiHeader(summary: PatientHistorySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Trazabilidad Clínica y Financiera",
                        color = BiodontosWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Saldo Pendiente: ${summary.pendingBalance.formatCop()}",
                        color = BiodontosWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = BiodontosWhite.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${summary.totalActivities} eventos",
                        color = BiodontosWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BiodontosWhite.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KpiMetricItem(
                    label = "Última Visita",
                    value = summary.lastVisit?.format(SHORT_DATE_FORMATTER) ?: "Sin visitas",
                    valueColor = BiodontosWhite
                )
                KpiMetricItem(
                    label = "Planes Activos",
                    value = "${summary.activeTreatmentsCount}",
                    valueColor = BiodontosWhite
                )
                KpiMetricItem(
                    label = "Total Abonado",
                    value = summary.totalPaid.formatCop(),
                    valueColor = Esmeralda
                )
            }
        }
    }
}

@Composable
private fun KpiMetricItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(text = label, color = BiodontosWhite.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(text = value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun ActivityFilterBar(
    availableMonths: List<YearMonth>,
    selectedMonth: YearMonth?,
    selectedTypes: Set<ActivityType>,
    onSelectMonth: (YearMonth?) -> Unit,
    onToggleType: (ActivityType) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtrar por Mes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BiodontosDark
                )
                val isFiltered = selectedMonth != null || selectedTypes.size != ActivityType.entries.size
                if (isFiltered) {
                    Text(
                        text = "Limpiar filtros",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Zafiro,
                        modifier = Modifier.clickable(onClick = onClearFilters)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "all-months") {
                    FilterChip(
                        selected = selectedMonth == null,
                        onClick = { onSelectMonth(null) },
                        label = { Text("Todos los meses", fontSize = 12.sp) }
                    )
                }
                items(availableMonths, key = { it.toString() }) { ym ->
                    val formatted = ym.format(MONTH_HEADER_FORMATTER)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(SPANISH_LOCALE) else it.toString() }
                    FilterChip(
                        selected = selectedMonth == ym,
                        onClick = { onSelectMonth(if (selectedMonth == ym) null else ym) },
                        label = { Text(formatted, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tipo de Actividad",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = BiodontosDark
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ActivityType.entries, key = { it.name }) { type ->
                    val isSelected = type in selectedTypes
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleType(type) },
                        label = { Text(type.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = activityAccentColor(type).copy(alpha = 0.16f),
                            selectedLabelColor = BiodontosDark
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthStickyHeader(
    yearMonth: YearMonth?,
    count: Int
) {
    val title = yearMonth?.format(MONTH_HEADER_FORMATTER)
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(SPANISH_LOCALE) else it.toString() }
        ?: "Fecha pendiente de validación"

    Surface(
        color = BiodontosLightGrey,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = BiodontosDark
            )
            Text(
                text = "$count reg.",
                fontSize = 11.sp,
                color = BiodontosGreyText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PatientActivityTimelineCard(
    activity: PatientActivity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRequestVoid: () -> Unit,
    onOpenClarification: (Evolution) -> Unit,
    onOpenTreatmentStatus: (Treatment) -> Unit,
    onOpenNewPaymentForTreatment: (String) -> Unit
) {
    val accent = activityAccentColor(activity.type)
    val isVoided = when (activity) {
        is PatientActivity.EvolutionActivity -> activity.voidInfo != null
        is PatientActivity.PaymentActivity -> activity.voidInfo != null
        else -> false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isVoided) BiodontosInputBg else BiodontosWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Cabecera de tipo + fecha + estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isVoided) Carmesi else accent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activity.type.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isVoided) Carmesi else accent
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isVoided) {
                        StatusPill(text = "ANULADO", color = Carmesi)
                    }
                    Text(
                        text = if (activity.isDateValid) {
                            activity.occurredAt.format(DATE_TIME_DISPLAY_FORMATTER)
                        } else {
                            "Fecha inválida"
                        },
                        fontSize = 11.sp,
                        color = if (activity.isDateValid) BiodontosGreyText else Carmesi
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido específico según el subtipo de actividad
            when (activity) {
                is PatientActivity.AppointmentActivity -> AppointmentActivityContent(activity)
                is PatientActivity.EvolutionActivity -> EvolutionActivityContent(activity)
                is PatientActivity.PaymentActivity -> PaymentActivityContent(activity)
                is PatientActivity.TreatmentBudgetActivity -> TreatmentBudgetActivityContent(activity)
                is PatientActivity.ConsentActivity -> ConsentActivityContent(activity)
                is PatientActivity.RecordLifecycleActivity -> RecordLifecycleActivityContent(activity)
            }

            // Sello de Auditoría ("Registrado por ...")
            Spacer(modifier = Modifier.height(8.dp))
            AuditFooterRow(stamp = activity.createdStamp)

            // Sección expandible con historial de auditoría y acciones contextuales
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BiodontosInputBg)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (activity.auditTrail.isNotEmpty()) {
                        Text(
                            text = "Trazabilidad de Auditoría:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BiodontosDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        activity.auditTrail.forEach { stamp ->
                            Text(
                                text = "• ${stamp.action.label} por ${stamp.actorName ?: "Profesional"} (${stamp.occurredAt.format(DATE_TIME_DISPLAY_FORMATTER)})",
                                fontSize = 11.sp,
                                color = BiodontosGreyText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Acciones CRUD / Normativas según tipo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (activity) {
                            is PatientActivity.EvolutionActivity -> {
                                if (activity.voidInfo == null) {
                                    OutlinedButton(
                                        onClick = { onOpenClarification(activity.evolution) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Nota Aclaratoria", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = onRequestVoid,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Anular Evolución", fontSize = 12.sp, color = Carmesi)
                                    }
                                }
                            }
                            is PatientActivity.PaymentActivity -> {
                                if (activity.voidInfo == null) {
                                    OutlinedButton(
                                        onClick = onRequestVoid,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Anular Abono Contable", fontSize = 12.sp, color = Carmesi)
                                    }
                                }
                            }
                            is PatientActivity.TreatmentBudgetActivity -> {
                                val canChangeStatus = activity.status.availableTransitions().isNotEmpty()
                                if (canChangeStatus) {
                                    OutlinedButton(
                                        onClick = { onOpenTreatmentStatus(activity.treatment) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cambiar Estado", fontSize = 12.sp)
                                    }
                                }
                                if (activity.status != TreatmentStatus.CANCELADO &&
                                    activity.status != TreatmentStatus.COMPLETADO
                                ) {
                                    Button(
                                        onClick = { onOpenNewPaymentForTreatment(activity.treatment.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Registrar Abono", fontSize = 12.sp)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentActivityContent(activity: PatientActivity.AppointmentActivity) {
    val appt = activity.appointment
    val statusColor = when (appt.status) {
        AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED -> Esmeralda
        AppointmentStatus.CANCELLED -> Carmesi
        else -> Zafiro
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appt.reason?.ifBlank { "Consulta Odontológica" } ?: "Consulta Odontológica",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BiodontosDark
            )
            Text(
                text = if (appt.isConfirmed) "Confirmada por paciente/clínica" else "Pendiente de confirmación",
                fontSize = 12.sp,
                color = BiodontosGreyText
            )
        }
        StatusPill(text = appt.status.name, color = statusColor)
    }
}

@Composable
private fun EvolutionActivityContent(activity: PatientActivity.EvolutionActivity) {
    val evo = activity.evolution
    val isVoided = activity.voidInfo != null
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (evo.isClarification) {
                StatusPill(text = "NOTA ACLARATORIA", color = Zafiro)
            }
            if (evo.toothTreated != null) {
                StatusPill(text = "Pieza FDI ${evo.toothTreated}", color = BiodontosDark)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = evo.description,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isVoided) BiodontosGreyText else BiodontosDark,
            textDecoration = if (isVoided) TextDecoration.LineThrough else TextDecoration.None
        )
        if (evo.materialsUsed.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Insumos INVIMA: ${evo.materialsUsed.size} lote(s) trazado(s)",
                fontSize = 11.sp,
                color = Zafiro,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (activity.voidInfo != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Motivo de anulación: ${activity.voidInfo.reason}",
                fontSize = 11.sp,
                color = Carmesi,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PaymentActivityContent(activity: PatientActivity.PaymentActivity) {
    val pay = activity.payment
    val isVoided = activity.voidInfo != null
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.treatmentDescription,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isVoided) BiodontosGreyText else BiodontosDark,
                    textDecoration = if (isVoided) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "Método: ${pay.paymentMethod ?: "EFECTIVO"}",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )
            }
            Text(
                text = pay.amount.formatCop(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isVoided) Carmesi else Esmeralda,
                textDecoration = if (isVoided) TextDecoration.LineThrough else TextDecoration.None
            )
        }
        if (activity.voidInfo != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Anulado: ${activity.voidInfo.reason}",
                fontSize = 11.sp,
                color = Carmesi,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TreatmentBudgetActivityContent(activity: PatientActivity.TreatmentBudgetActivity) {
    val treat = activity.treatment
    val statusColor = when (activity.status) {
        TreatmentStatus.COMPLETADO, TreatmentStatus.APROBADO -> Esmeralda
        TreatmentStatus.CANCELADO -> Carmesi
        TreatmentStatus.EN_CURSO -> Zafiro
        TreatmentStatus.PENDIENTE -> Ambar
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = treat.description,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BiodontosDark
                )
                Text(
                    text = "Presupuesto: ${treat.totalCost.formatCop()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BiodontosGreyText
                )
            }
            StatusPill(text = activity.status.displayName, color = statusColor)
        }
        if (!treat.statusReason.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Observación de estado: ${treat.statusReason}",
                fontSize = 11.sp,
                color = BiodontosGreyText
            )
        }
    }
}

@Composable
private fun ConsentActivityContent(activity: PatientActivity.ConsentActivity) {
    val consent = activity.consent
    val isSigned = consent.status == "SIGNED" || !consent.patientSignatureUrl.isNullOrBlank()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = consent.treatmentSummary.ifBlank { "Consentimiento Informado Clínico" },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BiodontosDark
            )
            Text(
                text = "Versión ${consent.documentVersion} · ${consent.procedures.size} procedimiento(s)",
                fontSize = 12.sp,
                color = BiodontosGreyText
            )
        }
        StatusPill(
            text = if (isSigned) "FIRMADO" else "PENDIENTE",
            color = if (isSigned) Esmeralda else Ambar
        )
    }
}

@Composable
private fun RecordLifecycleActivityContent(activity: PatientActivity.RecordLifecycleActivity) {
    val rec = activity.record
    Column {
        Text(
            text = activity.event.label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = BiodontosDark
        )
        Text(
            text = "Diagnóstico principal: ${rec.mainDiagnosis?.ifBlank { "En evaluación" } ?: "En evaluación"}",
            fontSize = 12.sp,
            color = BiodontosGreyText
        )
        if (!rec.closureSummary.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Resumen de cierre: ${rec.closureSummary}",
                fontSize = 11.sp,
                color = BiodontosDark
            )
        }
    }
}

@Composable
private fun AuditFooterRow(stamp: AuditStamp?) {
    val actorText = stamp?.actorName?.takeIf { it.isNotBlank() } ?: "Registro Clínico Verificado"
    Text(
        text = "Registrado por: $actorText · Toca para ver acciones y auditoría",
        fontSize = 11.sp,
        color = BiodontosGreyText
    )
}

@Composable
private fun StatusPill(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun EmptyHistoryStateCard(
    hasAnyActivities: Boolean,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasAnyActivities) {
                    "No hay actividades que coincidan con el mes o tipo seleccionado."
                } else {
                    "Aún no se registran eventos en el historial clínico de este paciente."
                },
                color = BiodontosGreyText,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            if (hasAnyActivities) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onClearFilters) {
                    Text("Restablecer Filtros")
                }
            }
        }
    }
}

private fun activityAccentColor(type: ActivityType): Color = when (type) {
    ActivityType.APPOINTMENT -> Zafiro
    ActivityType.PAYMENT -> Esmeralda
    ActivityType.EVOLUTION -> BiodontosDark
    ActivityType.TREATMENT_BUDGET -> Ambar
    ActivityType.CONSENT -> Esmeralda
    ActivityType.RECORD_LIFECYCLE -> Zafiro
}

@Preview(showBackground = true)
@Composable
private fun PatientHistoryLoadingPreview() {
    DentalTheme {
        PatientHistoryTab(
            historyState = PatientHistoryUiState(isTimelineLoading = true),
            onSelectMonth = {},
            onToggleType = {},
            onClearFilters = {},
            onToggleExpandActivity = {},
            onRequestVoidActivity = {},
            onOpenClarificationDialog = {},
            onOpenTreatmentStatusDialog = {},
            onOpenNewPaymentForTreatment = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientHistorySuccessPreview() {
    val sampleTime = LocalDateTime.of(2026, 10, 20, 14, 30)
    val sampleActivities = listOf(
        PatientActivity.EvolutionActivity(
            id = "evo-1",
            evolution = Evolution(
                id = "evo-1",
                recordId = "rec-1",
                doctorId = "doc-1",
                doctorName = "Dr. Carlos Mendoza",
                date = "2026-10-20",
                procedureTime = "14:30:00",
                description = "Profilaxis ultrasónica y aplicación de barniz fluorado",
                toothTreated = 16,
                cost = 150000.0,
                paymentAmount = 150000.0
            ),
            occurredAt = sampleTime,
            createdStamp = AuditStamp("Dr. Carlos Mendoza", AuditAction.CREATE, sampleTime)
        ),
        PatientActivity.TreatmentBudgetActivity(
            id = "treat-1",
            treatment = Treatment(
                id = "treat-1",
                patientId = PatientId("pat-1"),
                description = "Rehabilitación adhesiva sector posterior",
                totalCost = CopAmount(450000L),
                status = "EN_CURSO"
            ),
            status = TreatmentStatus.EN_CURSO,
            occurredAt = sampleTime.minusDays(2)
        ),
        PatientActivity.PaymentActivity(
            id = "pay-1",
            payment = Payment(
                id = "pay-1",
                treatmentId = "treat-1",
                amount = CopAmount(200000L),
                paymentMethod = "TRANSFERENCIA"
            ),
            treatmentDescription = "Rehabilitación adhesiva sector posterior",
            occurredAt = sampleTime.minusDays(1)
        ),
        PatientActivity.AppointmentActivity(
            id = "appt-1",
            appointment = Appointment(
                id = "appt-1",
                patientId = "pat-1",
                doctorId = "doc-1",
                startTime = sampleTime.plusDays(7),
                status = AppointmentStatus.CONFIRMED,
                isConfirmed = true,
                reason = "Control de oclusión"
            ),
            occurredAt = sampleTime.plusDays(7)
        )
    )

    DentalTheme {
        PatientHistoryTab(
            historyState = PatientHistoryUiState(
                allActivities = sampleActivities,
                filteredActivities = sampleActivities,
                availableMonths = listOf(YearMonth.of(2026, 10)),
                summary = PatientHistorySummary.from(sampleActivities)
            ),
            onSelectMonth = {},
            onToggleType = {},
            onClearFilters = {},
            onToggleExpandActivity = {},
            onRequestVoidActivity = {},
            onOpenClarificationDialog = {},
            onOpenTreatmentStatusDialog = {},
            onOpenNewPaymentForTreatment = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientHistoryErrorPreview() {
    DentalTheme {
        PatientHistoryTab(
            historyState = PatientHistoryUiState(
                isTimelineLoading = false,
                timelineError = "No fue posible sincronizar el historial clínico del paciente."
            ),
            onSelectMonth = {},
            onToggleType = {},
            onClearFilters = {},
            onToggleExpandActivity = {},
            onRequestVoidActivity = {},
            onOpenClarificationDialog = {},
            onOpenTreatmentStatusDialog = {},
            onOpenNewPaymentForTreatment = {},
            onRetry = {}
        )
    }
}
