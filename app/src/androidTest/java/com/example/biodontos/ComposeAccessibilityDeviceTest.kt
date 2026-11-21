package com.example.biodontos

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.example.biodontos.core.designsystem.components.BiodontosPillButton
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComposeAccessibilityDeviceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            composeTestRule.enableAccessibilityChecks()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun accessibilityValidatorChecksClickableControlsOnDevice() {
        var clicked = false
        composeTestRule.setContent {
            BiodontosPillButton(
                text = "Iniciar Sesión Clínica",
                onClick = { clicked = true },
                modifier = Modifier.testTag("a11y_pill_button")
            )
        }

        composeTestRule.onNodeWithTag("a11y_pill_button").performClick()
        composeTestRule.runOnIdle {
            check(clicked) { "El botón accesible no registró el click esperado" }
        }
    }
}
