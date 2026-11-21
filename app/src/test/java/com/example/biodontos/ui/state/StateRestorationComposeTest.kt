package com.example.biodontos.ui.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.presentation.auth.login.LoginScreen
import com.example.biodontos.presentation.auth.login.LoginState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@MediumTest
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class StateRestorationComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_passwordModeAndFormEmail_restoredAfterSavedInstanceStateRestore() {
        val restorationTester = StateRestorationTester(composeTestRule)
        var emailState: MutableState<String>? = null

        restorationTester.setContent {
            DentalTheme {
                val savedEmail = rememberSaveable { mutableStateOf("") }
                emailState = savedEmail
                LoginScreen(
                    state = LoginState(email = savedEmail.value),
                    onEmailChange = { savedEmail.value = it },
                    onPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onLoginClick = {},
                    onForgotPasswordClick = {},
                    onSignUpClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Cambiar").assertDoesNotExist()

        composeTestRule.onNode(hasImeAction(ImeAction.Next))
            .performScrollTo()
            .performTextReplacement("dra.bejarano@biodontos.com")
        composeTestRule.onNode(hasImeAction(ImeAction.Next))
            .performImeAction()

        composeTestRule.onNodeWithText("Cambiar")
            .performScrollTo()
            .assertIsDisplayed()

        val capturedEmail = composeTestRule.runOnIdle {
            checkNotNull(emailState).value
        }
        assertThat(capturedEmail).isEqualTo("dra.bejarano@biodontos.com")

        emailState = null

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.runOnIdle {
            assertThat(checkNotNull(emailState).value).isEqualTo(capturedEmail)
        }
        composeTestRule.onNodeWithText("Cambiar")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("dra.bejarano@biodontos.com")
            .assertIsDisplayed()
    }

    @Test
    fun clinicalFormAndSelectedTooth_rememberSaveable_restoredAfterSavedInstanceStateRestore() {
        val restorationTester = StateRestorationTester(composeTestRule)
        var notesState: MutableState<String>? = null
        var selectedToothState: MutableState<Int?>? = null

        restorationTester.setContent {
            DentalTheme {
                val notes = rememberSaveable { mutableStateOf("") }
                val selectedTooth = rememberSaveable { mutableStateOf<Int?>(null) }
                notesState = notes
                selectedToothState = selectedTooth

                Column {
                    OutlinedTextField(
                        value = notes.value,
                        onValueChange = { notes.value = it },
                        label = { Text("Nota clínica") },
                        modifier = Modifier.testTag("clinical_notes_input")
                    )
                    Row {
                        listOf(16, 26, 36).forEach { fdi ->
                            Button(
                                onClick = { selectedTooth.value = fdi },
                                modifier = Modifier.testTag("fdi_tooth_$fdi")
                            ) {
                                Text("FDI $fdi")
                            }
                        }
                    }
                    Text(
                        text = "Diente activo: ${selectedTooth.value ?: "ninguno"}",
                        modifier = Modifier.testTag("selected_tooth_summary")
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("clinical_notes_input")
            .performTextReplacement("Caries oclusal profunda en pieza 26")
        composeTestRule.onNodeWithTag("fdi_tooth_26").performClick()

        val (capturedNotes, capturedTooth) = composeTestRule.runOnIdle {
            checkNotNull(notesState).value to checkNotNull(selectedToothState).value
        }
        assertThat(capturedNotes).isEqualTo("Caries oclusal profunda en pieza 26")
        assertThat(capturedTooth).isEqualTo(26)

        notesState = null
        selectedToothState = null

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.runOnIdle {
            assertThat(checkNotNull(notesState).value).isEqualTo(capturedNotes)
            assertThat(checkNotNull(selectedToothState).value).isEqualTo(capturedTooth)
        }
        composeTestRule.onNodeWithTag("selected_tooth_summary")
            .assertIsDisplayed()
            .assertTextEquals("Diente activo: 26")
    }

    @Test
    fun lazyColumn_rememberLazyListState_restoresScrollAfterPerformScrollToIndex6() {
        val restorationTester = StateRestorationTester(composeTestRule)
        var listState: LazyListState? = null

        restorationTester.setContent {
            DentalTheme {
                val state = rememberLazyListState()
                listState = state

                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .requiredSize(width = 320.dp, height = 240.dp)
                        .testTag("restorable_lazy_list")
                ) {
                    items(25, key = { "pat_$it" }) { index ->
                        Box(
                            modifier = Modifier
                                .requiredHeight(48.dp)
                                .fillMaxWidth()
                                .testTag("pat_row_$index")
                        ) {
                            Text("Paciente #$index")
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("restorable_lazy_list").performScrollToIndex(6)

        val (capturedIndex, capturedOffset) = composeTestRule.runOnIdle {
            val current = checkNotNull(listState)
            current.firstVisibleItemIndex to current.firstVisibleItemScrollOffset
        }
        assertThat(capturedIndex).isGreaterThan(0)

        listState = null

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.runOnIdle {
            val restored = checkNotNull(listState)
            assertThat(restored.firstVisibleItemIndex).isEqualTo(capturedIndex)
            assertThat(restored.firstVisibleItemScrollOffset).isEqualTo(capturedOffset)
        }
        composeTestRule.onNodeWithTag("pat_row_6").assertIsDisplayed()
    }
}
