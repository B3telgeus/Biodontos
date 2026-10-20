package com.example.biodontos.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosSlateBlue
import com.example.biodontos.core.designsystem.BiodontosWhite

enum class PillButtonVariant {
    DARK,
    OUTLINED,
    LIGHT
}

@Composable
fun BiodontosPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PillButtonVariant = PillButtonVariant.DARK,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(50.dp)
    val contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)

    if (variant == PillButtonVariant.OUTLINED) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 48.dp),
            enabled = enabled && !isLoading,
            shape = shape,
            border = BorderStroke(1.5.dp, if (enabled) BiodontosDark else Color.Gray),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = BiodontosDark
            ),
            contentPadding = contentPadding
        ) {
            ButtonContent(text, isLoading, leadingIcon, trailingIcon, BiodontosDark)
        }
    } else {
        val containerColor = when (variant) {
            PillButtonVariant.DARK -> BiodontosDark
            PillButtonVariant.LIGHT -> BiodontosWhite
            else -> BiodontosDark
        }
        val contentColor = when (variant) {
            PillButtonVariant.DARK -> BiodontosWhite
            PillButtonVariant.LIGHT -> BiodontosDark
            else -> BiodontosWhite
        }

        Button(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 48.dp),
            enabled = enabled && !isLoading,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = BiodontosSlateBlue.copy(alpha = 0.5f),
                disabledContentColor = BiodontosWhite.copy(alpha = 0.7f)
            ),
            contentPadding = contentPadding
        ) {
            ButtonContent(text, isLoading, leadingIcon, trailingIcon, contentColor)
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    contentColor: Color
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = contentColor
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            trailingIcon?.let {
                Spacer(modifier = Modifier.width(8.dp))
                it()
            }
        }
    }
}
