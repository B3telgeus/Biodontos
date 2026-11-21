package com.example.biodontos.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.presentation.auth.onboarding.OnboardingScreen
import com.example.biodontos.presentation.auth.onboarding.UserTypeSelectionScreen
import com.example.biodontos.presentation.auth.patient.SignUpSuccessScreen
import com.example.biodontos.presentation.auth.reset.ForgotPasswordScreen
import com.example.biodontos.presentation.auth.reset.ForgotPasswordState
import com.example.biodontos.presentation.auth.splash.SplashContent
import com.example.biodontos.ui.performSemanticClick
import com.example.biodontos.ui.setBiodontosContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@MediumTest
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AuthFlowComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun splashLoadingDisplaysLogoLoaderAndStatusMessage() {
        composeTestRule.setBiodontosContent {
            SplashContent(
                startAnimation = true,
                isLoading = true,
                statusMessage = "Iniciando entorno clínico seguro..."
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.SPLASH_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Auth.SPLASH_LOGO).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Biodontos Logo").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Auth.SPLASH_LOADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Auth.SPLASH_STATUS)
            .assertIsDisplayed()
            .assertTextEquals("Iniciando entorno clínico seguro...")
    }

    @Test
    fun splashErrorHidesLoaderAndDisplaysErrorMessage() {
        composeTestRule.setBiodontosContent {
            SplashContent(
                startAnimation = true,
                isLoading = false,
                errorMessage = "Sin conexión. Modo local activado."
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.SPLASH_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Auth.SPLASH_LOADER).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Auth.SPLASH_STATUS)
            .assertIsDisplayed()
            .assertTextEquals("Sin conexión. Modo local activado.")
    }

    @Test
    fun onboardingDisplaysLogoCopyAndTriggersLoginAndCreateAccount() {
        var loginClicks = 0
        var createAccountClicks = 0

        composeTestRule.setBiodontosContent {
            OnboardingScreen(
                onLoginClick = { loginClicks++ },
                onCreateAccountClick = { createAccountClicks++ }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.ONBOARDING_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Logo Biodontos").assertIsDisplayed()
        composeTestRule.onNodeWithText("¡SONRÍE!").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Auth.ONBOARDING_LOGIN_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Auth.ONBOARDING_CREATE_ACCOUNT_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertThat(loginClicks).isEqualTo(1)
        assertThat(createAccountClicks).isEqualTo(1)
    }

    @Test
    fun userTypeSelectionDisplaysButtonsAndDispatchesDoctorPatientAndBack() {
        var doctorClicks = 0
        var patientClicks = 0
        var backClicks = 0

        composeTestRule.setBiodontosContent {
            UserTypeSelectionScreen(
                onDoctorClick = { doctorClicks++ },
                onPatientClick = { patientClicks++ },
                onBackClick = { backClicks++ }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.USER_TYPE_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Atrás").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Auth.USER_TYPE_DOCTOR_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Auth.USER_TYPE_PATIENT_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Auth.USER_TYPE_BACK_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertThat(doctorClicks).isEqualTo(1)
        assertThat(patientClicks).isEqualTo(1)
        assertThat(backClicks).isEqualTo(1)
    }

    @Test
    fun signUpSuccessDisplaysConfirmationAndTriggersLoginCallback() {
        var loginClicks = 0

        composeTestRule.setBiodontosContent {
            SignUpSuccessScreen(
                onLoginClick = { loginClicks++ }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.SIGNUP_SUCCESS_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithText("¡Hemos creado tu cuenta con éxito!").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("BIODONTOS").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Auth.SIGNUP_SUCCESS_LOGIN_BUTTON)
            .performSemanticClick()

        assertThat(loginClicks).isEqualTo(1)
    }

    @Test
    fun forgotPasswordRequestErrorAndSuccessTransitionsWorkCorrectly() {
        var state by mutableStateOf(
            ForgotPasswordState(
                email = "paciente@biodontos.com",
                emailError = "Formato de correo inválido",
                error = "No se encontró ninguna cuenta asociada."
            )
        )
        var submitClicks = 0
        var backClicks = 0

        composeTestRule.setBiodontosContent {
            ForgotPasswordScreen(
                state = state,
                onEmailChange = { state = state.copy(email = it) },
                onSubmitClick = { submitClicks++ },
                onBackClick = { backClicks++ }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.FORGOT_PASSWORD_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Auth.FORGOT_PASSWORD_EMAIL_FIELD)
            .assertIsDisplayed()
            .assertTextContains("paciente@biodontos.com")
        composeTestRule.onNodeWithText("Formato de correo inválido").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Auth.FORGOT_PASSWORD_ERROR_TEXT)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("No se encontró ninguna cuenta asociada.")

        composeTestRule.onNodeWithTag(TestTags.Auth.FORGOT_PASSWORD_SUBMIT_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        assertThat(submitClicks).isEqualTo(1)

        composeTestRule.runOnIdle {
            state = state.copy(isLoading = true)
        }
        composeTestRule.onNodeWithTag(TestTags.Auth.FORGOT_PASSWORD_SUBMIT_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()

        composeTestRule.runOnIdle {
            state = state.copy(
                isLoading = false,
                error = null,
                emailError = null,
                isSubmittedSuccess = true
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Auth.FORGOT_PASSWORD_SUCCESS_VIEW).assertIsDisplayed()
        composeTestRule.onNodeWithText("¡Correo Enviado!").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Auth.FORGOT_PASSWORD_EMAIL_FIELD).assertDoesNotExist()

        composeTestRule.onNodeWithText("Volver al Inicio de Sesión")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Auth.FORGOT_PASSWORD_BACK_BUTTON)
            .assertIsDisplayed()
            .performClick()

        assertThat(backClicks).isEqualTo(2)
    }
}
