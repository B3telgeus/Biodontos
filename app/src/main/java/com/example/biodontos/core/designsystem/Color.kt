package com.example.biodontos.core.designsystem

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Zafiro = Color(0xFF006494)
val ZafiroLight = Color(0xFFE1F5FE)
val Esmeralda = Color(0xFF10B981)
val Carmesi = Color(0xFFDC2626)
val Ambar = Color(0xFFD97706)
val Slate = Color(0xFF64748B)
val SlateLight = Color(0xFFF1F5F9)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Figma Tokens Biodontos
val BiodontosDark = Color(0xFF151A24)
val BiodontosInputBg = Color(0xFFE7EAF1)
val BiodontosGreyText = Color(0xFF575F6E)
val BiodontosLightGrey = Color(0xFFF0F2F4)
val BiodontosGrayLight = Color(0xFFE2E4E8)
val BiodontosBorder = Color(0xFFB9B9C3)
val BiodontosWhite = Color(0xFFFFFFFF)
val BiodontosErrorRed = Color(0xFFDC2626)
val BiodontosErrorBg = Color(0xFFFFF1F1)

// Tokens específicos del Portal de Pacientes (Figma 07_Home_Paciente)
val BiodontosSlateBlue = Color(0xFF5B6B8E)
val BiodontosCardTop = Color(0xFFF7F8FA)
val BiodontosScreenBgTop = Color(0xFFE9E9EB)
val BiodontosScreenBgBottom = Color(0xFFD4D5D8)
val BiodontosRowHighlight = Color(0xFFE7EAF1)
val StatusConfirmed = Color(0xFF2E7D32)
val StatusPending = Color(0xFF575F6E)
val StatusCancelled = Color(0xFFB3261E)

val BiodontosCardGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
    listOf(BiodontosCardTop, BiodontosSlateBlue.copy(alpha = 0.35f))
)
val BiodontosSoftCardGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
    listOf(Color.White, BiodontosLightGrey)
)
val BiodontosScreenGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
    listOf(BiodontosScreenBgTop, BiodontosScreenBgBottom)
)

@Composable
fun biodontosTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = BiodontosInputBg,
    unfocusedContainerColor = BiodontosInputBg,
    errorContainerColor = BiodontosErrorBg,
    focusedBorderColor = BiodontosDark,
    unfocusedBorderColor = Color.Transparent,
    errorBorderColor = BiodontosErrorRed,
    focusedTextColor = BiodontosDark,
    unfocusedTextColor = BiodontosDark,
    errorTextColor = BiodontosDark,
    errorLeadingIconColor = BiodontosErrorRed,
    errorTrailingIconColor = BiodontosErrorRed,
    errorSupportingTextColor = BiodontosErrorRed,
    errorCursorColor = BiodontosErrorRed
)
