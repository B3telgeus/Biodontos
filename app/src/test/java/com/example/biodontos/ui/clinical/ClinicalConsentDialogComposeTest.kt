package com.example.biodontos.ui.clinical

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.domain.model.ConsentType
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.presentation.patients.detail.history.ClinicalConsentDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ClinicalConsentDialogComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val samplePatient = Patient(
        id = "pat-100",
        dniCedula = "1020304050",
        firstName = "Carlos",
        firstLastName = "Rodríguez",
        phone = "3001234567",
        email = "carlos@example.com"
    )

    @Test
    fun clinicalConsentDialog_renders_header_and_general_tab_by_default() {
        composeRule.setContent {
            ClinicalConsentDialog(
                patient = samplePatient,
                initialConsentType = ConsentType.GENERAL_ODONTOLOGY,
                onDismiss = {},
                onSaveConsent = {}
            )
        }

        composeRule.onNodeWithTag(TestTags.Patients.CLINICAL_CONSENT_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.Patients.CLINICAL_CONSENT_TYPE_SELECTOR).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.Patients.CLINICAL_CONSENT_SAVE_BUTTON).assertIsDisplayed()

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Procedimientos de Odontología General autorizados:").assertIsDisplayed()
        composeRule.onNodeWithText("Fase higiénica").assertIsDisplayed()
        composeRule.onNodeWithText("Resinas").assertExists()
    }

    @Test
    fun clinicalConsentDialog_switching_to_bleaching_tab_displays_bleaching_content() {
        composeRule.setContent {
            ClinicalConsentDialog(
                patient = samplePatient,
                initialConsentType = ConsentType.GENERAL_ODONTOLOGY,
                onDismiss = {},
                onSaveConsent = {}
            )
        }

        composeRule.onNodeWithTag(TestTags.Patients.CLINICAL_CONSENT_BLEACH_TAB).performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Antecedentes, alergias a peróxidos o medicación relevante:").assertIsDisplayed()
    }

    @Test
    fun clinicalConsentDialog_switching_to_oral_rehabilitation_displays_occlusion_checkbox() {
        composeRule.setContent {
            ClinicalConsentDialog(
                patient = samplePatient,
                initialConsentType = ConsentType.ORAL_REHABILITATION,
                onDismiss = {},
                onSaveConsent = {}
            )
        }

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.Patients.CLINICAL_CONSENT_OCCLUSION_CHECKBOX).assertIsDisplayed()
        composeRule.onNodeWithText("CLÁUSULA DE PRUEBA DE OCLUSIÓN Y ESTÉTICA").assertIsDisplayed()
    }

    @Test
    fun clinicalConsentDialog_dismiss_triggers_callback() {
        var dismissed = false
        composeRule.setContent {
            ClinicalConsentDialog(
                patient = samplePatient,
                initialConsentType = ConsentType.GENERAL_ODONTOLOGY,
                onDismiss = { dismissed = true },
                onSaveConsent = {}
            )
        }

        composeRule.onNodeWithTag(TestTags.Patients.CLINICAL_CONSENT_DISMISS_BUTTON).performClick()
        composeRule.waitForIdle()
        assertTrue(dismissed)
    }
}
