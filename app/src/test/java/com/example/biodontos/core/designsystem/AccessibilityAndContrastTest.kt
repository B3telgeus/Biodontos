package com.example.biodontos.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.biodontos.R
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AccessibilityAndContrastTest {

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val l1 = foreground.luminance()
        val l2 = background.luminance()
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    @Test
    fun `primary text and interactive tokens meet WCAG AA contrast ratios`() {
        val darkOnWhite = contrastRatio(BiodontosDark, BiodontosWhite)
        assertTrue("BiodontosDark on BiodontosWhite ($darkOnWhite) must be >= 4.5", darkOnWhite >= 4.5f)

        val darkOnInputBg = contrastRatio(BiodontosDark, BiodontosInputBg)
        assertTrue("BiodontosDark on BiodontosInputBg ($darkOnInputBg) must be >= 4.5", darkOnInputBg >= 4.5f)

        val greyTextOnWhite = contrastRatio(BiodontosGreyText, BiodontosWhite)
        assertTrue("BiodontosGreyText on BiodontosWhite ($greyTextOnWhite) must be >= 4.5", greyTextOnWhite >= 4.5f)

        val whiteOnZafiro = contrastRatio(BiodontosWhite, Zafiro)
        assertTrue("BiodontosWhite on Zafiro ($whiteOnZafiro) must be >= 4.5", whiteOnZafiro >= 4.5f)

        val whiteOnConfirmed = contrastRatio(BiodontosWhite, StatusConfirmed)
        assertTrue("BiodontosWhite on StatusConfirmed ($whiteOnConfirmed) must be >= 4.5", whiteOnConfirmed >= 4.5f)

        val whiteOnCancelled = contrastRatio(BiodontosWhite, StatusCancelled)
        assertTrue("BiodontosWhite on StatusCancelled ($whiteOnCancelled) must be >= 4.5", whiteOnCancelled >= 4.5f)

        val errorRedOnErrorBg = contrastRatio(BiodontosErrorRed, BiodontosErrorBg)
        assertTrue("BiodontosErrorRed on BiodontosErrorBg ($errorRedOnErrorBg) must be >= 4.5", errorRedOnErrorBg >= 4.5f)
    }

    @Test
    fun `accessibility content descriptions and localized empty states resolve from Android resources`() {
        val context = RuntimeEnvironment.getApplication()

        assertEquals("Volver", context.getString(R.string.action_back))
        assertEquals("Cerrar sesión", context.getString(R.string.action_logout))
        assertEquals("Diente FDI 16", context.getString(R.string.cd_tooth_fdi, 16))
        assertEquals("Panel de firma digital", context.getString(R.string.cd_signature_pad))
        assertEquals("Foto de perfil del especialista", context.getString(R.string.cd_doctor_profile_photo))
        assertEquals(
            "El especialista aún no ha configurado horarios disponibles.",
            context.getString(R.string.doctor_availability_empty)
        )

        val uiText = com.example.biodontos.core.ui.UiText.StringResource(R.string.cd_tooth_fdi, 21)
        assertEquals("Diente FDI 21", uiText.asString(context))
    }
}
