package com.example.biodontos.core.designsystem.insets

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier

/**
 * Extensiones de Modifier para manejo coherente de insets en Biodontos (Edge-to-Edge).
 */

/**
 * Aplica padding para la barra de navegación del sistema y el teclado virtual (IME).
 * Garantiza que botones o acciones al pie nunca queden ocultos.
 */
fun Modifier.bottomActionInsets(): Modifier = this
    .navigationBarsPadding()
    .imePadding()

/**
 * Aplica padding para el área segura de dibujo (safe drawing),
 * protegiendo el contenido contra barra de estado, cutouts y barra de navegación.
 */
fun Modifier.screenInsets(): Modifier = this
    .safeDrawingPadding()
