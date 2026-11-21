package com.example.biodontos

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class EspressoAndUiAutomatorInteropTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var uiDevice: UiDevice

    @Before
    fun setUp() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun espressoAndUiAutomatorCrossInteractionsDriveHostAndSystemUi() {
        composeTestRule.setContent {
            Text(
                text = "Biodontos Sistema Odontológico",
                modifier = Modifier.testTag("interop_header_text")
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("interop_header_text").assertIsDisplayed()

        onView(isRoot()).check(matches(isDisplayed()))

        uiDevice.waitForIdle(2_000L)
        val hasPackage = uiDevice.wait(Until.hasObject(By.pkg("com.example.biodontos")), 3_000L)
        assertThat(hasPackage).isTrue()

        uiDevice.pressHome()
        uiDevice.waitForIdle(1_000L)
    }
}
