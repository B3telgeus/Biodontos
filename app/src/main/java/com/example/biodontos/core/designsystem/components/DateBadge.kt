package com.example.biodontos.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosInputBg
import java.time.LocalDateTime

@Composable
fun DateBadge(
    dateTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    val day = dateTime.dayOfMonth.toString()
    val month = dateTime.month.name.take(3).uppercase()

    Box(
        modifier = modifier
            .widthIn(min = 44.dp)
            .background(
                color = BiodontosInputBg,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )
            Text(
                text = month,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = BiodontosDark.copy(alpha = 0.7f)
            )
        }
    }
}
