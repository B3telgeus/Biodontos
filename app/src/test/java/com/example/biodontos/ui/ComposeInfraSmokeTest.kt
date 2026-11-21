package com.example.biodontos.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@MediumTest
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ComposeInfraSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun smokeRendersTextInDentalTheme() {
        composeTestRule.setBiodontosContent {
            Text(
                text = "Biodontos Infra Smoke",
                modifier = Modifier.testTag("smoke_text")
            )
        }

        composeTestRule.onNodeWithTag("smoke_text")
            .assertIsDisplayed()
            .assertTextEquals("Biodontos Infra Smoke")
    }
}
