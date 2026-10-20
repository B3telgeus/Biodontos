package com.example.biodontos.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorRed

@Composable
fun TwoStepConfirmationDialog(
    title: String,
    impactSummary: String,
    confirmActionLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var isSecondStep by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = "Alerta",
                tint = if (isSecondStep) BiodontosErrorRed else BiodontosDark
            )
        },
        title = {
            Text(
                text = if (!isSecondStep) title else "¿Estás totalmente seguro?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BiodontosDark
            )
        },
        text = {
            Column {
                Text(
                    text = if (!isSecondStep) impactSummary 
                           else "Esta acción no se puede deshacer y el horario se liberará para otros pacientes.",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isSecondStep) {
                    Text(
                        text = "Paso 2 de 2: Confirmación definitiva requerida.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BiodontosErrorRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!isSecondStep) {
                        isSecondStep = true
                    } else {
                        onConfirm()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSecondStep) BiodontosErrorRed else BiodontosDark
                ),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = if (!isSecondStep) "Continuar" else confirmActionLabel,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(text = "Cancelar", color = BiodontosDark)
            }
        }
    )
}
