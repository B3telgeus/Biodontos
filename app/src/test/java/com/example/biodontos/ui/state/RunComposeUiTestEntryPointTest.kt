package com.example.biodontos.ui.state

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.presentation.patientportal.home.PatientHomeScreen
import com.example.biodontos.presentation.patientportal.home.PatientHomeUiState
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@MediumTest
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RunComposeUiTestEntryPointTest {

    @Test
    fun patientHomeScreen_rendersSummaryCards_usingRunComposeUiTest() =
        runComposeUiTest {
            val nextAppointment = Appointment(
                id = "apt-run-1",
                patientId = "pat-1",
                doctorId = "doc-1",
                doctorName = "Dr. Carlos Restrepo",
                startTime = LocalDateTime.of(2026, 11, 25, 9, 0),
                status = AppointmentStatus.CONFIRMED,
                isConfirmed = true,
                reason = "Control de operatoria estética"
            )

            setContent {
                DentalTheme {
                    PatientHomeScreen(
                        state = PatientHomeUiState(
                            patientName = "Laura Martínez",
                            patientId = "pat-1",
                            nextAppointment = nextAppointment,
                            upcomingAppointments = listOf(nextAppointment),
                            pendingBalance = CopAmount(280_000L),
                            completedAppointmentsCount = 4,
                            doctors = listOf(
                                Doctor(
                                    id = DoctorId("doc-1"),
                                    firstName = "Carlos",
                                    lastName = "Restrepo",
                                    specialty = "Operatoria y Estética",
                                    photoUrl = null
                                )
                            ),
                            isLoading = false
                        ),
                        onEvent = {},
                        isExpanded = false
                    )
                }
            }

            onNodeWithText("¡Hola, Laura Martínez!")
                .assertIsDisplayed()
            onNodeWithText("$ 280.000")
                .performScrollTo()
                .assertIsDisplayed()
            onNodeWithText("Dr. Carlos Restrepo")
                .performScrollTo()
                .assertIsDisplayed()
        }

    @Test
    fun launchedEffect_withStandardTestDispatcher_updatesUiAfterClockAdvance() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            var status by mutableStateOf("Sincronizando agenda...")

            setContent {
                DentalTheme {
                    LaunchedEffect(Unit) {
                        status = "Agenda odontológica lista"
                    }
                    Text(
                        text = status,
                        modifier = Modifier.testTag("run_compose_ui_status")
                    )
                }
            }

            mainClock.advanceTimeByFrame()

            onNodeWithTag("run_compose_ui_status")
                .assertIsDisplayed()
                .assertTextEquals("Agenda odontológica lista")
        }
}
