package com.example.biodontos.ui.doctor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.presentation.doctor.appointments.DayAvailabilityConfig
import com.example.biodontos.presentation.doctor.appointments.DoctorAppointmentFilter
import com.example.biodontos.presentation.doctor.appointments.DoctorAppointmentsScreen
import com.example.biodontos.presentation.doctor.appointments.DoctorAppointmentsUiEvent
import com.example.biodontos.presentation.doctor.appointments.DoctorAppointmentsUiState
import com.example.biodontos.presentation.doctor.appointments.DoctorScheduleMode
import com.example.biodontos.presentation.doctor.appointments.DoctorSlotStatus
import com.example.biodontos.presentation.doctor.appointments.DoctorTimelineSlot
import com.example.biodontos.presentation.doctor.home.DoctorAppointmentItem
import com.example.biodontos.presentation.doctor.home.DoctorHomeScreen
import com.example.biodontos.presentation.doctor.home.DoctorHomeUiEvent
import com.example.biodontos.presentation.doctor.home.DoctorHomeUiState
import com.example.biodontos.presentation.doctor.home.DoctorPaymentItem
import com.example.biodontos.ui.performSemanticClick
import com.example.biodontos.ui.setBiodontosContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@MediumTest
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DoctorHomeAndAppointmentsComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleToday: LocalDate = LocalDate.of(2026, 10, 23)

    @Test
    fun doctorHomeLoadingStateShowsLoadingIndicator() {
        composeTestRule.setBiodontosContent {
            DoctorHomeScreen(
                state = DoctorHomeUiState(isLoading = true, isRefreshing = false),
                onEvent = {},
                onNavigateToPatients = {},
                onNavigateToNewPatient = {},
                onNavigateToAppointments = {},
                onNavigateToDocuments = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_LOADING).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_ERROR_BANNER).assertDoesNotExist()
    }

    @Test
    fun doctorHomeErrorBannerAndRetryDispatchesRefresh() {
        val events = mutableListOf<DoctorHomeUiEvent>()

        composeTestRule.setBiodontosContent {
            DoctorHomeScreen(
                state = DoctorHomeUiState(
                    isLoading = false,
                    doctorName = "Dr. Carlos Restrepo",
                    errorMessage = "Error al cargar el panel clínico"
                ),
                onEvent = { events.add(it) },
                onNavigateToPatients = {},
                onNavigateToNewPatient = {},
                onNavigateToAppointments = {},
                onNavigateToDocuments = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_ERROR_BANNER)
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText("Error al cargar el panel clínico", substring = true)))

        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_RETRY_BUTTON)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(events).containsExactly(DoctorHomeUiEvent.Refresh)
        }
    }

    @Test
    fun doctorHomeQuickActionCardsInvokeNavigationCallbacks() {
        var navigatedToPatients = false
        var navigatedToAdmission = false
        var navigatedToDocuments = false
        var navigatedToAppointments = false
        var navigatedToProfile = false

        composeTestRule.setBiodontosContent {
            DoctorHomeScreen(
                state = DoctorHomeUiState(
                    isLoading = false,
                    doctorName = "Dr. Carlos Restrepo",
                    patientsCount = 15,
                    consentedPatientsCount = 12,
                    pendingConsentCount = 3,
                    todayAppointmentsCount = 4
                ),
                onEvent = {},
                onNavigateToPatients = { navigatedToPatients = true },
                onNavigateToNewPatient = {},
                onNavigateToAppointments = { navigatedToAppointments = true },
                onNavigateToDocuments = { navigatedToDocuments = true },
                onNavigateToClinicalAdmission = { navigatedToAdmission = true },
                onNavigateToProfile = { navigatedToProfile = true }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_AVATAR_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_ADMISSION_BUTTON)
            .performScrollTo()
            .performSemanticClick()
        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_PATIENTS_BUTTON)
            .performScrollTo()
            .performSemanticClick()
        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_DOCUMENTS_BUTTON)
            .performScrollTo()
            .performSemanticClick()
        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_CALENDAR_BUTTON)
            .performScrollTo()
            .performSemanticClick()

        composeTestRule.runOnIdle {
            assertThat(navigatedToProfile).isTrue()
            assertThat(navigatedToAdmission).isTrue()
            assertThat(navigatedToPatients).isTrue()
            assertThat(navigatedToDocuments).isTrue()
            assertThat(navigatedToAppointments).isTrue()
        }
    }

    @Test
    fun doctorHomeAppointmentsAndPaymentsToggleConfirmAndOpenDialog() {
        val events = mutableListOf<DoctorHomeUiEvent>()
        val navigatedPatientIds = mutableListOf<String>()
        var seeAllClicked = false

        val appointments = listOf(
            DoctorAppointmentItem(
                appointmentId = "apt-1",
                patientId = "pat-1",
                patientName = "María Gómez",
                treatmentReason = "Limpieza dental",
                startTime = LocalDateTime.of(2026, 10, 23, 9, 0),
                isConfirmed = true
            ),
            DoctorAppointmentItem(
                appointmentId = "apt-2",
                patientId = "pat-2",
                patientName = "Juan López",
                treatmentReason = "Ortodoncia",
                startTime = LocalDateTime.of(2026, 10, 23, 10, 30),
                isConfirmed = false
            )
        )
        val payments = listOf(
            DoctorPaymentItem(
                id = "pay-1",
                patientId = "pat-1",
                patientName = "María Gómez",
                treatmentDescription = "Profilaxis",
                dateStr = "23/10/2026",
                amountCop = CopAmount(120000L),
                paymentMethod = "EFECTIVO"
            )
        )

        composeTestRule.setBiodontosContent {
            DoctorHomeScreen(
                state = DoctorHomeUiState(
                    isLoading = false,
                    doctorName = "Dr. Carlos Restrepo",
                    upcomingAppointments = appointments,
                    recentPayments = payments,
                    recentTotalCollectedCop = CopAmount(120000L)
                ),
                onEvent = { events.add(it) },
                onNavigateToPatients = {},
                onNavigateToNewPatient = {},
                onNavigateToAppointments = { seeAllClicked = true },
                onNavigateToDocuments = {},
                onNavigateToPatientDetail = { navigatedPatientIds.add(it) }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.homeAppointmentItem("apt-1"))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.homeAppointmentConfirmChip("apt-2"))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_SEE_ALL_APPOINTMENTS_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.homePaymentItem("pay-1"))
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_NEW_PAYMENT_BUTTON)
            .performScrollTo()
            .performSemanticClick()

        composeTestRule.runOnIdle {
            assertThat(navigatedPatientIds).containsExactly("pat-1")
            assertThat(seeAllClicked).isTrue()
            assertThat(events).containsAtLeast(
                DoctorHomeUiEvent.ToggleConfirmAppointment("apt-2", false),
                DoctorHomeUiEvent.OpenRecordPaymentDialog
            )
        }
    }

    @Test
    fun doctorHomeRecordPaymentDialogSelectPatientAndSubmitPayment() {
        val patient1 = Patient(
            id = "pat-1",
            dniCedula = "10203040",
            firstName = "Ana",
            firstLastName = "Gómez"
        )
        val patient2 = Patient(
            id = "pat-2",
            dniCedula = "98765432",
            firstName = "Carlos",
            firstLastName = "Pérez"
        )
        val events = mutableListOf<DoctorHomeUiEvent>()
        var state by mutableStateOf(
            DoctorHomeUiState(
                isLoading = false,
                doctorName = "Dr. Carlos Restrepo",
                isRecordPaymentDialogOpen = true,
                availablePatients = listOf(patient1, patient2),
                availableTreatments = emptyList(),
                selectedPatient = null
            )
        )

        composeTestRule.setBiodontosContent {
            DoctorHomeScreen(
                state = state,
                onEvent = { events.add(it) },
                onNavigateToPatients = {},
                onNavigateToNewPatient = {},
                onNavigateToAppointments = {},
                onNavigateToDocuments = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_PAYMENT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_PAYMENT_SUBMIT_BUTTON).assertIsNotEnabled()

        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_PAYMENT_PATIENT_SEARCH)
            .performTextInput("Ana")

        composeTestRule.onNode(
            hasText("Ana Gómez", substring = true) and hasAnyAncestor(isDialog())
        ).assertIsDisplayed().performClick()

        composeTestRule.runOnIdle {
            assertThat(events).contains(DoctorHomeUiEvent.SelectPatientForPayment(patient1))
            state = state.copy(selectedPatient = patient1)
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_PAYMENT_AMOUNT_FIELD)
            .performScrollTo()
            .performTextInput("180000")

        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_PAYMENT_SUBMIT_BUTTON)
            .assertIsEnabled()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.HOME_PAYMENT_CANCEL_BUTTON)
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(events).contains(
                DoctorHomeUiEvent.SubmitPayment(
                    patientId = "pat-1",
                    treatmentId = null,
                    newTreatmentDescription = "Tratamiento Odontológico",
                    newTreatmentCost = 180000L,
                    amount = 180000L,
                    paymentMethod = "EFECTIVO"
                )
            )
            assertThat(events).contains(DoctorHomeUiEvent.DismissRecordPaymentDialog)
        }
    }

    @Test
    fun doctorAppointmentsLoadingAndErrorRetryRenderCorrectly() {
        val events = mutableListOf<DoctorAppointmentsUiEvent>()
        var state by mutableStateOf(
            DoctorAppointmentsUiState(
                isLoading = true,
                isRefreshing = false,
                selectedDate = sampleToday
            )
        )

        composeTestRule.setBiodontosContent {
            DoctorAppointmentsScreen(
                state = state,
                today = sampleToday,
                onEvent = { events.add(it) },
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_LOADING).assertIsDisplayed()

        composeTestRule.runOnIdle {
            state = state.copy(
                isLoading = false,
                errorMessage = "Fallo al sincronizar agenda",
                isDayOff = true
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_ERROR_BANNER)
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText("Fallo al sincronizar agenda", substring = true)))
        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_DAY_OFF_VIEW).assertExists()

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_RETRY_BUTTON).performClick()

        composeTestRule.runOnIdle {
            assertThat(events).contains(DoctorAppointmentsUiEvent.Refresh)
        }
    }

    @Test
    fun doctorAppointmentsDayStripAndTimelineSlotsSelectDateAndQuickBook() {
        val events = mutableListOf<DoctorAppointmentsUiEvent>()
        val occupiedAppt = DoctorAppointmentItem(
            appointmentId = "appt-1",
            patientId = "pat-1",
            patientName = "Sofía Ruiz",
            treatmentReason = "Control",
            startTime = sampleToday.atTime(9, 0),
            isConfirmed = false
        )
        val availableSlot = DoctorTimelineSlot(
            startTime = sampleToday.atTime(9, 30),
            endTime = sampleToday.atTime(10, 0),
            status = DoctorSlotStatus.Available
        )
        val timelineSlots = listOf(
            DoctorTimelineSlot(
                startTime = sampleToday.atTime(9, 0),
                endTime = sampleToday.atTime(9, 30),
                status = DoctorSlotStatus.Occupied(occupiedAppt)
            ),
            availableSlot
        )

        composeTestRule.setBiodontosContent {
            DoctorAppointmentsScreen(
                state = DoctorAppointmentsUiState(
                    isLoading = false,
                    doctorName = "Dr. Carlos Restrepo",
                    selectedDate = sampleToday,
                    displayMode = DoctorScheduleMode.TIMELINE,
                    appointments = listOf(occupiedAppt),
                    timelineSlots = timelineSlots
                ),
                today = sampleToday,
                onEvent = { events.add(it) },
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_LIST).assertIsDisplayed()

        val thursday = sampleToday.minusDays(1)
        composeTestRule.onNodeWithTag(TestTags.Doctor.dayChip(thursday.toString()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_NEXT_WEEK_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_PREV_WEEK_BUTTON).performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.appointmentConfirmChip("appt-1"))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_LIST)
            .performScrollToKey(availableSlot.startTime.toString())
        composeTestRule.onNodeWithTag(TestTags.Doctor.timelineSlot(availableSlot.startTime.toString()))
            .onChildren()
            .filterToOne(hasClickAction())
            .performSemanticClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_MODE_LIST_BUTTON).performClick()

        composeTestRule.runOnIdle {
            assertThat(events).containsAtLeast(
                DoctorAppointmentsUiEvent.SelectDate(thursday),
                DoctorAppointmentsUiEvent.SelectDate(sampleToday.plusWeeks(1)),
                DoctorAppointmentsUiEvent.SelectDate(sampleToday.minusWeeks(1)),
                DoctorAppointmentsUiEvent.ToggleConfirm("appt-1", false),
                DoctorAppointmentsUiEvent.OpenQuickBooking(availableSlot),
                DoctorAppointmentsUiEvent.ChangeDisplayMode(DoctorScheduleMode.LIST)
            )
        }
    }

    @Test
    fun doctorAppointmentsListModeScrolls28ItemsWithIndexAndKeyAndFilters() {
        val events = mutableListOf<DoctorAppointmentsUiEvent>()
        val items = List(28) { index ->
            DoctorAppointmentItem(
                appointmentId = "appt-$index",
                patientId = "pat-$index",
                patientName = "Paciente $index",
                treatmentReason = "Consulta $index",
                startTime = if (index < 10) {
                    sampleToday.atTime(8 + (index % 8), 0)
                } else {
                    sampleToday.plusDays(1).atTime(8 + (index % 8), 0)
                },
                isConfirmed = index % 2 == 0
            )
        }
        var state by mutableStateOf(
            DoctorAppointmentsUiState(
                isLoading = false,
                selectedDate = sampleToday,
                displayMode = DoctorScheduleMode.LIST,
                selectedFilter = DoctorAppointmentFilter.ALL,
                appointments = items
            )
        )

        composeTestRule.setBiodontosContent {
            DoctorAppointmentsScreen(
                state = state,
                today = sampleToday,
                onEvent = { events.add(it) },
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_LIST).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_LIST).performScrollToIndex(26)
        composeTestRule.onNodeWithTag(TestTags.Doctor.appointmentItem("appt-26")).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_LIST).performScrollToKey("appt-2")
        composeTestRule.onNodeWithTag(TestTags.Doctor.appointmentItem("appt-2")).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Doctor.filterChip(DoctorAppointmentFilter.CONFIRMED.name))
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(events).contains(DoctorAppointmentsUiEvent.SelectFilter(DoctorAppointmentFilter.CONFIRMED))
            state = state.copy(selectedFilter = DoctorAppointmentFilter.CONFIRMED)
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_LIST).performScrollToIndex(0)
        composeTestRule.onNodeWithTag(TestTags.Doctor.appointmentItem("appt-0")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.appointmentItem("appt-1")).assertDoesNotExist()
    }

    @Test
    fun doctorAppointmentsAvailabilityDialogAndTimeSelectorLazyColumnScrollAndSave() {
        val events = mutableListOf<DoctorAppointmentsUiEvent>()
        val mondayConfig = DayAvailabilityConfig(
            dayOfWeek = DayOfWeek.MONDAY,
            isActive = true,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(17, 0),
            slotMinutes = 30
        )

        composeTestRule.setBiodontosContent {
            DoctorAppointmentsScreen(
                state = DoctorAppointmentsUiState(
                    isLoading = false,
                    selectedDate = sampleToday,
                    displayMode = DoctorScheduleMode.TIMELINE,
                    timelineSlots = emptyList(),
                    showAvailabilityDialog = true,
                    weeklyAvailability = listOf(mondayConfig)
                ),
                today = sampleToday,
                onEvent = { events.add(it) },
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_AVAILABILITY_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_AVAILABILITY_COPY_WEEKDAYS_BUTTON).performClick()

        composeTestRule.onAllNodesWithText("08:00", substring = true)
            .onFirst()
            .performClick()

        val timePickerList = composeTestRule.onNode(
            hasScrollToKeyAction() and hasAnyAncestor(isDialog())
        )
        timePickerList.performScrollToKey(LocalTime.of(15, 0).toString())

        composeTestRule.onNode(
            hasText("03:00", substring = true) and hasAnyAncestor(isDialog())
        ).assertIsDisplayed().performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_AVAILABILITY_SAVE_BUTTON)
            .performSemanticClick()

        composeTestRule.runOnIdle {
            assertThat(events).containsAtLeast(
                DoctorAppointmentsUiEvent.CopyWeekdayAvailability(DayOfWeek.MONDAY),
                DoctorAppointmentsUiEvent.UpdateDayAvailability(
                    day = DayOfWeek.MONDAY,
                    isActive = true,
                    startTime = LocalTime.of(15, 0),
                    endTime = LocalTime.of(17, 0),
                    slotMinutes = 30
                ),
                DoctorAppointmentsUiEvent.SaveAvailability
            )
        }
    }

    @Test
    fun doctorAppointmentsQuickBookRescheduleAndCancelDialogsWorkCorrectly() {
        val events = mutableListOf<DoctorAppointmentsUiEvent>()
        val patient1 = Patient(id = "pat-1", dniCedula = "111", firstName = "Laura", firstLastName = "Mora")
        val patient2 = Patient(id = "pat-2", dniCedula = "222", firstName = "Carlos", firstLastName = "Pérez")
        val targetSlot = DoctorTimelineSlot(
            startTime = sampleToday.atTime(10, 0),
            endTime = sampleToday.atTime(10, 30),
            status = DoctorSlotStatus.Available
        )
        val existingAppt = DoctorAppointmentItem(
            appointmentId = "appt-9",
            patientId = "pat-2",
            patientName = "Carlos Pérez",
            treatmentReason = "Endodoncia",
            startTime = sampleToday.atTime(11, 0),
            isConfirmed = true
        )
        val rescheduleSlot = sampleToday.plusDays(1).atTime(14, 0)

        var state by mutableStateOf(
            DoctorAppointmentsUiState(
                isLoading = false,
                selectedDate = sampleToday,
                registeredPatients = listOf(patient1, patient2),
                bookingTargetSlot = targetSlot
            )
        )

        composeTestRule.setBiodontosContent {
            DoctorAppointmentsScreen(
                state = state,
                today = sampleToday,
                onEvent = { events.add(it) },
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_QUICK_BOOK_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_QUICK_BOOK_SEARCH_FIELD)
            .performTextInput("Carlos")
        composeTestRule.onNodeWithText("Carlos Pérez").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_QUICK_BOOK_REASON_FIELD)
            .performTextReplacement("Valoración inicial")
        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_QUICK_BOOK_CONFIRM_BUTTON)
            .performSemanticClick()

        composeTestRule.runOnIdle {
            state = state.copy(
                bookingTargetSlot = null,
                reschedulingAppointment = existingAppt,
                rescheduleTargetDate = sampleToday.plusDays(1),
                rescheduleAvailableSlots = listOf(rescheduleSlot)
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_RESCHEDULE_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_RESCHEDULE_CONFIRM_BUTTON).assertIsNotEnabled()
        composeTestRule.onNode(
            hasText("02:00", substring = true) and hasAnyAncestor(isDialog())
        ).assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_RESCHEDULE_CONFIRM_BUTTON)
            .assertIsEnabled()
            .performClick()

        composeTestRule.runOnIdle {
            state = state.copy(
                reschedulingAppointment = null,
                cancellingAppointment = existingAppt
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_CANCEL_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.APPT_CANCEL_CONFIRM_BUTTON).performClick()

        composeTestRule.runOnIdle {
            assertThat(events).containsAtLeast(
                DoctorAppointmentsUiEvent.ConfirmQuickBooking("pat-2", "Valoración inicial"),
                DoctorAppointmentsUiEvent.ConfirmReschedule(rescheduleSlot),
                DoctorAppointmentsUiEvent.ConfirmCancelAppointment
            )
        }
    }
}
