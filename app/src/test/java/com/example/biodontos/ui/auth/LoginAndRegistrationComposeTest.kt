package com.example.biodontos.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.presentation.auth.doctor.DoctorRegistrationScreen
import com.example.biodontos.presentation.auth.doctor.DoctorRegistrationState
import com.example.biodontos.presentation.auth.login.LoginScreen
import com.example.biodontos.presentation.auth.login.LoginState
import com.example.biodontos.presentation.auth.patient.PatientSignUpScreen
import com.example.biodontos.presentation.auth.patient.PatientSignUpState
import com.example.biodontos.ui.setBiodontosContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@MediumTest
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LoginAndRegistrationComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginTextActionsImeTransitionAndSemanticsTreeDiagnostics() {
        var state by mutableStateOf(LoginState())
        var loginClicks = 0

        composeTestRule.setBiodontosContent {
            LoginScreen(
                state = state,
                onEmailChange = { state = state.copy(email = it) },
                onPasswordChange = { state = state.copy(password = it) },
                onTogglePasswordVisibility = {
                    state = state.copy(isPasswordVisible = !state.isPasswordVisible)
                },
                onLoginClick = { loginClicks++ },
                onForgotPasswordClick = {},
                onSignUpClick = {},
                onBackClick = {}
            )
        }

        val rootTreeDump = composeTestRule.onRoot().printToString()
        assertThat(rootTreeDump).contains(TestTags.Auth.LOGIN_ROOT)

        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_EMAIL_FIELD)
            .performTextInput("wrong@biodontos.com")
        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_EMAIL_FIELD)
            .performTextReplacement("doc@biodontos.com")
        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_EMAIL_FIELD)
            .assertTextContains("doc@biodontos.com")

        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_EMAIL_FIELD)
            .performImeAction()

        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_EMAIL_PILL)
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_PASSWORD_FIELD)
            .performTextInput("secret12")
        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_PASSWORD_VISIBILITY_BUTTON)
            .performClick()
        composeTestRule.runOnIdle {
            assertThat(state.isPasswordVisible).isTrue()
        }
        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_PASSWORD_FIELD)
            .assertTextContains("secret12")

        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_PASSWORD_FIELD)
            .performTextClearance()
        composeTestRule.runOnIdle {
            assertThat(state.password).isEmpty()
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_PASSWORD_FIELD)
            .performTextInput("secret123")
        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_PASSWORD_FIELD)
            .performImeAction()

        assertThat(loginClicks).isEqualTo(1)

        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_EMAIL_PILL).performClick()
        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_EMAIL_FIELD).assertIsDisplayed()
    }

    @Test
    fun loginComposedMatchersTreeTraversalAndNavigationLinks() {
        var state by mutableStateOf(
            LoginState(
                email = "bad-email",
                isLoading = true,
                error = "Credenciales inválidas"
            )
        )
        var forgotClicks = 0
        var signUpClicks = 0
        var backClicks = 0

        composeTestRule.setBiodontosContent {
            LoginScreen(
                state = state,
                onEmailChange = { state = state.copy(email = it) },
                onPasswordChange = { state = state.copy(password = it) },
                onTogglePasswordVisibility = {},
                onLoginClick = {},
                onForgotPasswordClick = { forgotClicks++ },
                onSignUpClick = { signUpClicks++ },
                onBackClick = { backClicks++ }
            )
        }

        composeTestRule.onNode(
            hasTestTag(TestTags.Auth.LOGIN_SUBMIT_BUTTON) and !isEnabled()
        )
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNode(
            hasTestTag(TestTags.Auth.LOGIN_ERROR_TEXT) and
                hasAnyAncestor(hasTestTag(TestTags.Auth.LOGIN_ROOT))
        )
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("Credenciales inválidas")

        composeTestRule.runOnIdle {
            state = state.copy(isLoading = false)
        }

        composeTestRule.onNode(
            hasTestTag(TestTags.Auth.LOGIN_SUBMIT_BUTTON) and isEnabled()
        )
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_SIGN_UP_LINK)
            .onParent()
            .onChildren()
            .filterToOne(hasText("¿No tienes una cuenta?", substring = true))
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_FORGOT_PASSWORD_LINK)
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_SIGN_UP_LINK)
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Auth.LOGIN_BACK_BUTTON)
            .performClick()

        assertThat(forgotClicks).isEqualTo(1)
        assertThat(signUpClicks).isEqualTo(1)
        assertThat(backClicks).isEqualTo(1)
    }

    @Test
    fun patientSignUpHandlesAllStepsTextActionsAndTreeTraversal() {
        var state by mutableStateOf(PatientSignUpState(currentStep = 1))
        var nextClicks = 0
        var backClicks = 0

        composeTestRule.setBiodontosContent {
            PatientSignUpScreen(
                state = state,
                onFirstNameChange = { state = state.copy(firstName = it) },
                onLastNameChange = { state = state.copy(lastName = it) },
                onDniChange = { state = state.copy(dniCedula = it) },
                onEmailChange = { state = state.copy(email = it) },
                onPhoneChange = { state = state.copy(phone = it) },
                onPasswordChange = { state = state.copy(password = it) },
                onConfirmPasswordChange = { state = state.copy(confirmPassword = it) },
                onTermsAcceptedChange = { state = state.copy(termsAccepted = it) },
                onTogglePasswordVisibility = {
                    state = state.copy(isPasswordVisible = !state.isPasswordVisible)
                },
                onNextClick = { nextClicks++ },
                onBackClick = { backClicks++ }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_ROOT).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_FIRST_NAME_FIELD)
            .performTextInput("Laura")
        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_LAST_NAME_FIELD)
            .performTextInput("Martínez")
        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_DNI_FIELD)
            .performTextInput("1032456789")
        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_PHONE_FIELD)
            .performScrollTo()
            .performTextInput("3001112233")
        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_PHONE_FIELD)
            .performTextReplacement("3004567890")

        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_FIRST_NAME_FIELD)
            .onParent()
            .onChildren()
            .filterToOne(hasTestTag(TestTags.Auth.PATIENT_SIGNUP_LAST_NAME_FIELD))
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            assertThat(state.firstName).isEqualTo("Laura")
            assertThat(state.lastName).isEqualTo("Martínez")
            assertThat(state.dniCedula).isEqualTo("1032456789")
            assertThat(state.phone).isEqualTo("3004567890")
        }

        composeTestRule.onNode(
            hasTestTag(TestTags.Auth.PATIENT_SIGNUP_NEXT_BUTTON) and isEnabled()
        )
            .performScrollTo()
            .performClick()
        assertThat(nextClicks).isEqualTo(1)

        composeTestRule.runOnIdle {
            state = state.copy(currentStep = 2)
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_EMAIL_FIELD)
            .performTextInput("laura@biodontos.com")
        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_PASSWORD_FIELD)
            .performTextInput("pass123")
        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_PASSWORD_VISIBILITY_BUTTON)
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_PASSWORD_FIELD)
            .assertTextContains("pass123")
        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_CONFIRM_PASSWORD_FIELD)
            .performScrollTo()
            .performTextInput("pass123")

        composeTestRule.runOnIdle {
            assertThat(state.email).isEqualTo("laura@biodontos.com")
            assertThat(state.confirmPassword).isEqualTo("pass123")
            state = state.copy(
                currentStep = 3,
                termsAccepted = false,
                error = "Debes aceptar los términos"
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_TERMS_CHECKBOX)
            .performScrollTo()
            .assertIsOff()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_TERMS_CHECKBOX)
            .assertIsOn()
        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_ERROR_TEXT)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("Debes aceptar los términos")

        composeTestRule.onNodeWithTag(TestTags.Auth.PATIENT_SIGNUP_BACK_BUTTON).performClick()
        assertThat(backClicks).isEqualTo(1)
    }

    @Test
    fun doctorRegistrationHandlesStepsDropdownLoadingAndErrors() {
        var state by mutableStateOf(DoctorRegistrationState(currentStep = 1))
        var nextClicks = 0
        var backClicks = 0

        composeTestRule.setBiodontosContent {
            DoctorRegistrationScreen(
                state = state,
                onFirstNameChange = { state = state.copy(firstName = it) },
                onLastNameChange = { state = state.copy(lastName = it) },
                onLicenseNumberChange = { state = state.copy(licenseNumber = it) },
                onSpecialtyChange = { state = state.copy(specialty = it) },
                onEmailChange = { state = state.copy(email = it) },
                onPhoneChange = { state = state.copy(phone = it) },
                onPasswordChange = { state = state.copy(password = it) },
                onConfirmPasswordChange = { state = state.copy(confirmPassword = it) },
                onTogglePasswordVisibility = {
                    state = state.copy(isPasswordVisible = !state.isPasswordVisible)
                },
                onNextClick = { nextClicks++ },
                onBackClick = { backClicks++ }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_ROOT).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_FIRST_NAME_FIELD)
            .performTextInput("Carlos")
        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_LAST_NAME_FIELD)
            .performTextInput("Restrepo")
        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_EMAIL_FIELD)
            .performTextInput("dr.restrepo@biodontos.com")
        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_PHONE_FIELD)
            .performScrollTo()
            .performTextInput("3109876543")
        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_PHONE_FIELD)
            .performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_PASSWORD_FIELD)
            .performScrollTo()
            .performTextInput("docpass12")
        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_PASSWORD_VISIBILITY_BUTTON)
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_PASSWORD_FIELD)
            .assertTextContains("docpass12")
        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_CONFIRM_PASSWORD_FIELD)
            .performScrollTo()
            .performTextInput("docpass12")

        composeTestRule.runOnIdle {
            assertThat(state.firstName).isEqualTo("Carlos")
            assertThat(state.phone).isEmpty()
        }

        composeTestRule.onNode(
            hasTestTag(TestTags.Auth.DOCTOR_REG_NEXT_BUTTON) and isEnabled()
        )
            .performScrollTo()
            .performClick()
        assertThat(nextClicks).isEqualTo(1)

        composeTestRule.runOnIdle {
            state = state.copy(currentStep = 2)
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_LICENSE_FIELD)
            .performScrollTo()
            .performTextInput("RM-4565345")
        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_SPECIALTY_DROPDOWN)
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Auth.doctorRegSpecialtyOption("Ortodoncia"))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(state.licenseNumber).isEqualTo("RM-4565345")
            assertThat(state.specialty).isEqualTo("Ortodoncia")
            state = state.copy(
                isLoading = true,
                error = "No se pudo completar el registro"
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_ERROR_TEXT)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("No se pudo completar el registro")

        composeTestRule.onNode(
            hasTestTag(TestTags.Auth.DOCTOR_REG_NEXT_BUTTON) and !isEnabled()
        )
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Auth.DOCTOR_REG_BACK_BUTTON).performClick()
        assertThat(backClicks).isEqualTo(1)
    }
}
