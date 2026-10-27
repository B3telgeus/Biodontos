package com.example.biodontos.presentation.patientportal.appointments

import android.content.Intent
import android.provider.CalendarContract
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosScreenBgTop
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.core.designsystem.StatusConfirmed
import com.example.biodontos.core.designsystem.Zafiro
import com.example.biodontos.core.designsystem.components.BiodontosPillButton
import com.example.biodontos.core.designsystem.components.PillButtonVariant
import com.example.biodontos.core.designsystem.insets.BiodontosBottomActionBar
import com.example.biodontos.presentation.doctor.profile.DoctorInitialsAvatar
import com.example.biodontos.presentation.navigation.AppointmentConfirmation
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AppointmentConfirmationScreen(
    confirmation: AppointmentConfirmation,
    onNavigateToMyAppointments: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val title = if (confirmation.isRescheduled) {
        "¡Cita Reagendada con Éxito!"
    } else {
        "¡Cita Reservada con Éxito!"
    }

    val subtitle = if (confirmation.isRescheduled) {
        "Tu cita ha sido actualizada y reprogramada en el sistema clínico."
    } else {
        "Tu cita ha sido confirmada y agendada en el sistema clínico."
    }

    val dateTime = try {
        LocalDateTime.parse(confirmation.startIso)
    } catch (_: Exception) {
        null
    }

    val dateFormatted = dateTime?.format(
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"))
    )?.replaceFirstChar { it.uppercase() } ?: confirmation.startIso

    val timeFormatted = dateTime?.let {
        val start = it.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es-CO")))
        val end = it.plusMinutes(30).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es-CO")))
        "$start - $end"
    } ?: ""

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = BiodontosScreenBgTop,
        bottomBar = {
            BiodontosBottomActionBar(
                containerColor = BiodontosWhite,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BiodontosPillButton(
                        text = "Ver mis citas",
                        variant = PillButtonVariant.DARK,
                        onClick = onNavigateToMyAppointments,
                        modifier = Modifier.fillMaxWidth()
                    )

                    BiodontosPillButton(
                        text = "Añadir a mi calendario",
                        variant = PillButtonVariant.OUTLINED,
                        onClick = {
                            try {
                                val startMillis = dateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                                    ?: System.currentTimeMillis()
                                val endMillis = dateTime?.plusMinutes(30)?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                                    ?: (startMillis + 1800000L)

                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    data = CalendarContract.Events.CONTENT_URI
                                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                                    putExtra(CalendarContract.Events.TITLE, "Cita Odontológica Biodontos - ${confirmation.doctorName}")
                                    putExtra(CalendarContract.Events.DESCRIPTION, "Motivo: ${confirmation.reason}\n${confirmation.instructions}")
                                    putExtra(CalendarContract.Events.EVENT_LOCATION, confirmation.roomOrLocation)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // Ignorar si el dispositivo no tiene gestor de calendario
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onNavigateToHome() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Volver al inicio",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BiodontosDark
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Icon & Confirmation Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(StatusConfirmed.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Confirmación exitosa",
                            tint = StatusConfirmed,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = BiodontosGreyText,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Doctor row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val names = confirmation.doctorName.replace("Dr. ", "").replace("Dra. ", "").split(" ")
                            val first = names.firstOrNull() ?: "D"
                            val last = names.drop(1).firstOrNull() ?: "R"

                            DoctorInitialsAvatar(
                                firstName = first,
                                lastName = last,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = confirmation.doctorName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BiodontosDark
                                )
                                Text(
                                    text = confirmation.specialty,
                                    fontSize = 13.sp,
                                    color = BiodontosGreyText
                                )
                            }
                        }

                        HorizontalDivider(color = BiodontosInputBg)

                        // Date & Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = BiodontosDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = dateFormatted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BiodontosDark
                                )
                                if (timeFormatted.isNotBlank()) {
                                    Text(
                                        text = timeFormatted,
                                        fontSize = 13.sp,
                                        color = BiodontosGreyText
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BiodontosInputBg)

                        // Location
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = BiodontosDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = confirmation.roomOrLocation,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BiodontosDark
                                )
                                Text(
                                    text = "Sede Principal - Calle 100 # 15-20",
                                    fontSize = 12.sp,
                                    color = BiodontosGreyText
                                )
                            }
                        }

                        HorizontalDivider(color = BiodontosInputBg)

                        // Reason / Procedure
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BiodontosDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Procedimiento / Motivo",
                                    fontSize = 12.sp,
                                    color = BiodontosGreyText
                                )
                                Text(
                                    text = confirmation.reason,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BiodontosDark
                                )
                            }
                        }
                    }
                }
            }

            // Clinical Instructions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Zafiro.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Zafiro,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Instrucciones para tu cita",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiodontosDark
                            )
                            Text(
                                text = confirmation.instructions,
                                fontSize = 12.sp,
                                color = BiodontosDark.copy(alpha = 0.85f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppointmentConfirmationScreenPreview() {
    DentalTheme {
        AppointmentConfirmationScreen(
            confirmation = AppointmentConfirmation(
                appointmentId = "appt-100",
                doctorId = "doc-1",
                doctorName = "Dr. Carlos Mendoza",
                specialty = "Ortodoncia",
                startIso = "2026-10-24T10:00:00",
                roomOrLocation = "Sede Principal - Consultorio 102",
                reason = "Control de ortodoncia",
                instructions = "Presentarse 10 minutos antes. Traer documento de identidad.",
                isRescheduled = false
            ),
            onNavigateToMyAppointments = {},
            onNavigateToHome = {}
        )
    }
}
