package com.example.biodontos.core.designsystem.insets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.biodontos.core.designsystem.BiodontosWhite

/**
 * Contenedor estándar para barras de acción al pie de pantalla en Biodontos.
 *
 * Aplica automáticamente [Modifier.bottomActionInsets] (barra de navegación + IME)
 * para evitar que botones críticos de guardado o confirmación queden ocultos debajo
 * de la barra de navegación del sistema o del teclado.
 */
@Composable
fun BiodontosBottomActionBar(
    modifier: Modifier = Modifier,
    containerColor: Color = BiodontosWhite,
    contentColor: Color = contentColorFor(containerColor),
    shadowElevation: Dp = 8.dp,
    tonalElevation: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        tonalElevation = tonalElevation
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .bottomActionInsets()
                .padding(contentPadding)
        ) {
            content()
        }
    }
}
