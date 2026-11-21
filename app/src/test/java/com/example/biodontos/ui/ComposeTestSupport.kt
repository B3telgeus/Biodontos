package com.example.biodontos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performSemanticsAction
import com.example.biodontos.core.designsystem.DentalTheme

fun ComposeContentTestRule.setBiodontosContent(
    content: @Composable () -> Unit
) {
    setContent {
        DentalTheme {
            content()
        }
    }
}

fun ComposeContentTestRule.setBiodontosContent(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    setContent {
        DentalTheme(darkTheme = darkTheme) {
            content()
        }
    }
}

fun SemanticsNodeInteraction.assertIsDisplayedAndEnabled(): SemanticsNodeInteraction =
    assertIsDisplayed().assertIsEnabled()

fun SemanticsNodeInteraction.performSemanticClick(): SemanticsNodeInteraction =
    assertIsEnabled()
        .assertHasClickAction()
        .performSemanticsAction(SemanticsActions.OnClick)
