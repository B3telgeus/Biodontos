package com.example.biodontos.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.StatusCancelled
import com.example.biodontos.core.designsystem.StatusConfirmed
import com.example.biodontos.core.designsystem.StatusPending
import com.example.biodontos.domain.model.AppointmentStatus

@Composable
fun AppointmentStatusChip(
    status: AppointmentStatus,
    modifier: Modifier = Modifier
) {
    val (label, color, icon) = when (status) {
        AppointmentStatus.CONFIRMED -> Triple("Confirmada", StatusConfirmed, Icons.Default.Check)
        AppointmentStatus.PENDING, AppointmentStatus.SCHEDULED -> Triple("Pendiente", StatusPending, Icons.Default.Info)
        AppointmentStatus.CANCELLED -> Triple("Cancelada", StatusCancelled, Icons.Default.Close)
        AppointmentStatus.COMPLETED -> Triple("Completada", StatusConfirmed, Icons.Default.Check)
    }

    Box(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
