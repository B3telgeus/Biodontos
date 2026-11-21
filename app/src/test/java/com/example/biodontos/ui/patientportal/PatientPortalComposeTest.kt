package com.example.biodontos.ui.patientportal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorDetail
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.TreatmentBalance
import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.scheduling.TimeSlot
import com.example.biodontos.presentation.navigation.AppointmentConfirmation
import com.example.biodontos.presentation.patientportal.appointments.AppointmentConfirmationScreen
import com.example.biodontos.presentation.patientportal.appointments.AppointmentTab
import com.example.biodontos.presentation.patientportal.appointments.BookAppointmentScreen
import com.example.biodontos.presentation.patientportal.appointments.BookAppointmentUiEvent
import com.example.biodontos.presentation.patientportal.appointments.BookAppointmentUiState
import com.example.biodontos.presentation.patientportal.appointments.MyAppointmentsScreen
import com.example.biodontos.presentation.patientportal.appointments.MyAppointmentsUiEvent
import com.example.biodontos.presentation.patientportal.appointments.MyAppointmentsUiState
import com.example.biodontos.presentation.patientportal.doctors.DoctorDetailScreen
import com.example.biodontos.presentation.patientportal.doctors.DoctorDetailUiEvent
import com.example.biodontos.presentation.patientportal.doctors.DoctorDetailUiState
import com.example.biodontos.presentation.patientportal.doctors.DoctorSearchScreen
import com.example.biodontos.presentation.patientportal.doctors.DoctorSearchUiEvent
import com.example.biodontos.presentation.patientportal.doctors.DoctorSearchUiState
import com.example.biodontos.presentation.patientportal.home.PatientHomeScreen
import com.example.biodontos.presentation.patientportal.home.PatientHomeUiEvent
import com.example.biodontos.presentation.patientportal.home.PatientHomeUiState
import com.example.biodontos.presentation.patientportal.payments.PatientPaymentsScreen
import com.example.biodontos.presentation.patientportal.payments.PatientPaymentsUiEvent
import com.example.biodontos.presentation.patientportal.payments.PatientPaymentsUiState
import com.example.biodontos.presentation.patientportal.payments.PaymentFilter
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
class PatientPortalComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun patientHomeLoadingDisplaysLoaderAndRoot() {
        composeTestRule.setBiodontosContent {
            PatientHomeScreen(
                state = PatientHomeUiState(isLoading = true),
                onEvent = {},
                isExpanded = false
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_LOADING).assertIsDisplayed()
    }

    @Test
    fun patientHomeErrorAndContentDispatchRetryAndNavigationEvents() {
        val sampleDoctor = sampleDoctor(id = "doc-1", firstName = "Carlos", lastName = "Restrepo")
        val nextAppointment = Appointment(
            id = "appt-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            doctorName = "Dr. Carlos Restrepo",
            startTime = LocalDateTime.of(2026, 10, 26, 9, 0),
            status = AppointmentStatus.CONFIRMED,
            isConfirmed = true,
            reason = "Control de Ortodoncia"
        )
        var state by mutableStateOf(
            PatientHomeUiState(
                isLoading = false,
                patientName = "Laura",
                errorMessage = "Error de conexión",
                nextAppointment = nextAppointment,
                upcomingAppointments = listOf(nextAppointment),
                pendingBalance = CopAmount(150000L),
                completedAppointmentsCount = 3,
                doctors = listOf(sampleDoctor)
            )
        )
        val events = mutableListOf<PatientHomeUiEvent>()

        composeTestRule.setBiodontosContent {
            PatientHomeScreen(
                state = state,
                onEvent = { events.add(it) },
                isExpanded = false
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_ERROR_BANNER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_RETRY_BUTTON)
            .performSemanticClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_REFRESH_BUTTON)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_AGENDA_CARD)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_GO_TO_APPOINTMENTS_BUTTON)
            .performScrollTo()
            .performSemanticClick()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_PAYMENTS_CARD)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_GO_TO_PAYMENTS_BUTTON)
            .performScrollTo()
            .performSemanticClick()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_RECORDS_CARD)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_GO_TO_RECORDS_BUTTON)
            .performScrollTo()
            .performSemanticClick()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_DOCTORS_CARD)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_SEE_ALL_DOCTORS_BUTTON)
            .performScrollTo()
            .performSemanticClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.HOME_BOOK_FEATURED_DOCTOR_BUTTON)
            .performScrollTo()
            .performSemanticClick()

        assertThat(events).containsAtLeast(
            PatientHomeUiEvent.Refresh,
            PatientHomeUiEvent.NavigateToAppointments,
            PatientHomeUiEvent.NavigateToPayments,
            PatientHomeUiEvent.NavigateToDoctors,
            PatientHomeUiEvent.BookWithDoctor("doc-1")
        )
    }

    @Test
    fun doctorSearchLoadingErrorAndEmptyStatesTriggerRetryAndResetFilters() {
        var state by mutableStateOf(DoctorSearchUiState(isLoading = true))
        val events = mutableListOf<DoctorSearchUiEvent>()

        composeTestRule.setBiodontosContent {
            DoctorSearchScreen(
                state = state,
                onEvent = { events.add(it) }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_SEARCH_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_SEARCH_LOADING).assertIsDisplayed()

        composeTestRule.runOnIdle {
            state = state.copy(
                isLoading = false,
                errorMessage = "Fallo al cargar especialistas"
            )
        }
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_SEARCH_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_SEARCH_RETRY_BUTTON)
            .performSemanticClick()
        assertThat(events).contains(DoctorSearchUiEvent.Retry)

        composeTestRule.runOnIdle {
            state = state.copy(
                errorMessage = null,
                doctors = emptyList(),
                searchQuery = "Ninguno",
                selectedSpecialty = "Ortodoncia"
            )
        }
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_SEARCH_EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_SEARCH_RESET_FILTERS_BUTTON)
            .performSemanticClick()

        assertThat(events).containsAtLeast(
            DoctorSearchUiEvent.QueryChanged(""),
            DoctorSearchUiEvent.SpecialtySelected("Todas")
        )
    }

    @Test
    fun doctorSearchLazyGrid25PlusItemsAndLazyRowScrollByKeyAndIndex() {
        val doctors = (1..28).map { index ->
            sampleDoctor(
                id = "doc-$index",
                firstName = "Especialista$index",
                lastName = "Biodontos"
            )
        }
        val specialties = listOf(
            "Todas",
            "Ortodoncia",
            "Endodoncia",
            "Periodoncia",
            "Odontopediatría",
            "Cirugía Oral",
            "Odontología General",
            "Implantología",
            "Estética Dental",
            "Rehabilitación"
        )
        var state by mutableStateOf(
            DoctorSearchUiState(
                searchQuery = "Car",
                specialties = specialties,
                doctors = doctors
            )
        )
        val events = mutableListOf<DoctorSearchUiEvent>()

        composeTestRule.setBiodontosContent {
            DoctorSearchScreen(
                state = state,
                onEvent = { event ->
                    events.add(event)
                    when (event) {
                        is DoctorSearchUiEvent.QueryChanged -> state = state.copy(searchQuery = event.query)
                        is DoctorSearchUiEvent.SpecialtySelected -> state = state.copy(selectedSpecialty = event.specialty)
                        else -> Unit
                    }
                }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_SEARCH_CLEAR_BUTTON)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_SEARCH_INPUT)
            .performTextInput("Mendoza")

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_SEARCH_SPECIALTY_LIST)
            .performScrollToKey("Rehabilitación")
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.specialtyChip("Rehabilitación"))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_SEARCH_GRID)
            .performScrollToKey("doc-27")
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.doctorItem("doc-27"))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.doctorBookButton("doc-27"))
            .performSemanticClick()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_SEARCH_GRID)
            .performScrollToIndex(0)
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.doctorItem("doc-1"))
            .assertIsDisplayed()
            .performClick()

        assertThat(events).containsAtLeast(
            DoctorSearchUiEvent.QueryChanged(""),
            DoctorSearchUiEvent.QueryChanged("Mendoza"),
            DoctorSearchUiEvent.SpecialtySelected("Rehabilitación"),
            DoctorSearchUiEvent.BookDoctor("doc-27"),
            DoctorSearchUiEvent.DoctorSelected("doc-1")
        )
    }

    @Test
    fun doctorDetailLoadingErrorAndContentWithAvailabilityBadgeAndBookButton() {
        val docId = DoctorId("doc-1")
        val detail = DoctorDetail(
            doctor = sampleDoctor(id = "doc-1", firstName = "Carlos", lastName = "Restrepo"),
            availability = listOf(
                WeeklyAvailability(
                    id = "avail-1",
                    doctorId = docId,
                    dayOfWeek = DayOfWeek.MONDAY,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(17, 0)
                )
            ),
            isAvailableToday = true
        )
        var state by mutableStateOf(DoctorDetailUiState(doctorId = "doc-1", isLoading = true))
        val events = mutableListOf<DoctorDetailUiEvent>()
        var backClicks = 0

        composeTestRule.setBiodontosContent {
            DoctorDetailScreen(
                state = state,
                onEvent = { events.add(it) },
                onBackClick = { backClicks++ }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_DETAIL_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_DETAIL_LOADING).assertIsDisplayed()

        composeTestRule.runOnIdle {
            state = state.copy(isLoading = false, errorMessage = "Especialista no encontrado")
        }
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_DETAIL_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_DETAIL_RETRY_BUTTON)
            .performSemanticClick()

        composeTestRule.runOnIdle {
            state = state.copy(errorMessage = null, doctorDetail = detail)
        }
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_DETAIL_LIST)
            .performScrollToIndex(0)
        composeTestRule.onNodeWithTag(
            TestTags.PatientPortal.DOCTOR_DETAIL_AVAILABILITY_BADGE,
            useUnmergedTree = true
        ).assertExists()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_DETAIL_LIST)
            .performScrollToIndex(2)
        composeTestRule.onNodeWithText("Blanqueamiento").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_DETAIL_LIST)
            .performScrollToIndex(3)
        composeTestRule.onNodeWithText("Lunes").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_DETAIL_BOOK_BUTTON)
            .performSemanticClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.DOCTOR_DETAIL_BACK_BUTTON)
            .performSemanticClick()

        assertThat(events).containsAtLeast(
            DoctorDetailUiEvent.Retry,
            DoctorDetailUiEvent.BookAppointment
        )
        assertThat(backClicks).isEqualTo(1)
    }

    @Test
    fun bookAppointmentSlotsLoadingErrorAndEmptyStates() {
        val baseDate = LocalDate.of(2026, 10, 24)
        var state by mutableStateOf(
            BookAppointmentUiState(
                doctorId = "doc-1",
                selectedDate = baseDate,
                availableDates = listOf(baseDate),
                isSlotsLoading = true
            )
        )
        val events = mutableListOf<BookAppointmentUiEvent>()
        var backClicks = 0

        composeTestRule.setBiodontosContent {
            BookAppointmentScreen(
                state = state,
                onEvent = { events.add(it) },
                onBackClick = { backClicks++ }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_CONFIRM_BUTTON)
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_LIST)
            .performScrollToKey("slots")
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_SLOTS_LOADING)
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            state = state.copy(
                isSlotsLoading = false,
                errorMessage = "Sin conexión al consultar horarios",
                slots = emptyList()
            )
        }
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_SLOTS_ERROR)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_RETRY_SLOTS_BUTTON)
            .performSemanticClick()

        composeTestRule.runOnIdle {
            state = state.copy(errorMessage = null, slots = emptyList())
        }
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_SLOTS_EMPTY)
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_BACK_BUTTON)
            .performSemanticClick()

        assertThat(events).contains(BookAppointmentUiEvent.RetrySlots)
        assertThat(backClicks).isEqualTo(1)
    }

    @Test
    fun bookAppointmentLazyDateStrip25PlusItemsSlotReasonAndRescheduleDialog() {
        val baseDate = LocalDate.of(2026, 10, 1)
        val availableDates = (0L..27L).map { offset -> baseDate.plusDays(offset) }
        val day26 = availableDates[25]
        val morningSlot = TimeSlot(
            start = day26.atTime(9, 0),
            end = day26.atTime(9, 30)
        )
        val afternoonSlot = TimeSlot(
            start = day26.atTime(14, 30),
            end = day26.atTime(15, 0)
        )
        var state by mutableStateOf(
            BookAppointmentUiState(
                doctorId = "doc-1",
                doctor = sampleDoctor(id = "doc-1", firstName = "Valeria", lastName = "Mendoza"),
                selectedDate = baseDate,
                availableDates = availableDates,
                slots = listOf(morningSlot, afternoonSlot)
            )
        )
        val events = mutableListOf<BookAppointmentUiEvent>()

        composeTestRule.setBiodontosContent {
            BookAppointmentScreen(
                state = state,
                onEvent = { event ->
                    events.add(event)
                    when (event) {
                        is BookAppointmentUiEvent.SelectDate -> state = state.copy(selectedDate = event.date)
                        is BookAppointmentUiEvent.SelectSlot -> state = state.copy(selectedSlot = event.slot)
                        is BookAppointmentUiEvent.SelectReason -> state = state.copy(selectedReason = event.reason)
                        is BookAppointmentUiEvent.CustomReasonChanged -> state = state.copy(customReason = event.reason)
                        is BookAppointmentUiEvent.DismissRescheduleDialog ->
                            state = state.copy(showRescheduleConfirmDialog = false)
                        is BookAppointmentUiEvent.ExecuteReschedule ->
                            state = state.copy(showRescheduleConfirmDialog = false)
                        else -> Unit
                    }
                },
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_LIST)
            .performScrollToKey("dates")
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_DATE_STRIP)
            .performScrollToKey(day26.toString())
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.dateItem(day26.toString()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_DATE_STRIP)
            .performScrollToIndex(0)
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.dateItem(baseDate.toString()))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_LIST)
            .performScrollToKey("slots")
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.slotItem("09:00"))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_LIST)
            .performScrollToKey("reason")
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.reasonChip("Limpieza dental"))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_CUSTOM_REASON_FIELD)
            .performScrollTo()
            .performTextInput("Sensibilidad molar")
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_CUSTOM_REASON_FIELD)
            .assertTextContains("Sensibilidad molar")

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.BOOK_APPT_CONFIRM_BUTTON)
            .performSemanticClick()

        composeTestRule.runOnIdle {
            state = state.copy(
                rescheduleAppointmentId = "appt-5",
                showRescheduleConfirmDialog = true
            )
        }
        composeTestRule.onNode(
            hasText("Confirmar Reagendamiento") and hasAnyAncestor(isDialog())
        ).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Continuar") and hasAnyAncestor(isDialog())
        ).performSemanticClick()
        composeTestRule.onNode(
            hasText("Sí, reagendar cita") and hasAnyAncestor(isDialog())
        ).performSemanticClick()

        assertThat(events).containsAtLeast(
            BookAppointmentUiEvent.SelectDate(day26),
            BookAppointmentUiEvent.SelectSlot(morningSlot),
            BookAppointmentUiEvent.SelectReason("Limpieza dental"),
            BookAppointmentUiEvent.CustomReasonChanged("Sensibilidad molar"),
            BookAppointmentUiEvent.ConfirmBooking,
            BookAppointmentUiEvent.ExecuteReschedule
        )
    }

    @Test
    fun appointmentConfirmationDisplaysDetailsAndTriggersNavigationActions() {
        val confirmation = AppointmentConfirmation(
            appointmentId = "appt-99",
            doctorId = "doc-1",
            doctorName = "Dr. Carlos Mendoza",
            specialty = "Ortodoncia",
            startIso = "2026-10-24T10:00:00",
            roomOrLocation = "Consultorio 102",
            reason = "Control de ortodoncia",
            instructions = "Presentarse 10 minutos antes.",
            isRescheduled = false
        )
        var myApptClicks = 0
        var homeClicks = 0

        composeTestRule.setBiodontosContent {
            AppointmentConfirmationScreen(
                confirmation = confirmation,
                onNavigateToMyAppointments = { myApptClicks++ },
                onNavigateToHome = { homeClicks++ }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.CONFIRMATION_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithText("¡Cita Reservada con Éxito!").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.CONFIRMATION_LIST)
            .performScrollToIndex(1)
        composeTestRule.onNodeWithText("Dr. Carlos Mendoza").assertExists()
        composeTestRule.onNodeWithText("Control de ortodoncia").assertExists()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.CONFIRMATION_LIST)
            .performScrollToIndex(2)
        composeTestRule.onNodeWithText("Presentarse 10 minutos antes.").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.CONFIRMATION_ADD_CALENDAR_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.CONFIRMATION_MY_APPOINTMENTS_BUTTON)
            .performSemanticClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.CONFIRMATION_HOME_BUTTON)
            .performSemanticClick()

        assertThat(myApptClicks).isEqualTo(1)
        assertThat(homeClicks).isEqualTo(1)
    }

    @Test
    fun myAppointmentsLoadingErrorEmptyAndLazyList25PlusItemsWithTabsAndActions() {
        val doctor = sampleDoctor(id = "doc-1", firstName = "Carlos", lastName = "Restrepo")
        val upcomingList = (1..28).map { index ->
            Appointment(
                id = "appt-$index",
                patientId = "pat-1",
                doctorId = "doc-1",
                doctorName = "Dr. Carlos Restrepo",
                startTime = LocalDateTime.of(2026, 11, (index % 28) + 1, 9, 0),
                endTime = LocalDateTime.of(2026, 11, (index % 28) + 1, 9, 30),
                status = AppointmentStatus.CONFIRMED,
                isConfirmed = true,
                reason = "Control #$index"
            )
        }
        val completedAppt = Appointment(
            id = "appt-done-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            doctorName = "Dr. Carlos Restrepo",
            startTime = LocalDateTime.of(2026, 9, 10, 10, 0),
            status = AppointmentStatus.COMPLETED,
            reason = "Profilaxis"
        )
        val cancelledAppt = Appointment(
            id = "appt-cancel-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            doctorName = "Dr. Carlos Restrepo",
            startTime = LocalDateTime.of(2026, 9, 12, 11, 0),
            status = AppointmentStatus.CANCELLED,
            reason = "Revisión"
        )

        var state by mutableStateOf(MyAppointmentsUiState(isLoading = true))
        val events = mutableListOf<MyAppointmentsUiEvent>()

        composeTestRule.setBiodontosContent {
            MyAppointmentsScreen(
                state = state,
                onEvent = { event ->
                    events.add(event)
                    when (event) {
                        is MyAppointmentsUiEvent.SelectTab -> state = state.copy(selectedTab = event.tab)
                        is MyAppointmentsUiEvent.RequestCancelAppointment ->
                            state = state.copy(appointmentToCancel = event.appointment)
                        is MyAppointmentsUiEvent.DismissCancelDialog,
                        is MyAppointmentsUiEvent.ConfirmCancelAppointment ->
                            state = state.copy(appointmentToCancel = null)
                        else -> Unit
                    }
                }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.MY_APPT_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.MY_APPT_LOADING).assertIsDisplayed()

        composeTestRule.runOnIdle {
            state = state.copy(isLoading = false, errorMessage = "Error cargando citas")
        }
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.MY_APPT_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.MY_APPT_RETRY_BUTTON)
            .performSemanticClick()

        composeTestRule.runOnIdle {
            state = state.copy(errorMessage = null, upcomingAppointments = emptyList())
        }
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.MY_APPT_EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithText("Buscar Especialista").performSemanticClick()

        composeTestRule.runOnIdle {
            state = state.copy(
                upcomingAppointments = upcomingList,
                completedAppointments = listOf(completedAppt),
                cancelledAppointments = listOf(cancelledAppt),
                doctorsMap = mapOf("doc-1" to doctor)
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.MY_APPT_LIST)
            .performScrollToKey("appt-27")
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.appointmentItem("appt-27"))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.appointmentRescheduleButton("appt-27"))
            .performSemanticClick()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.MY_APPT_LIST)
            .performScrollToIndex(0)
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.appointmentItem("appt-1"))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.appointmentCancelButton("appt-1"))
            .performSemanticClick()

        composeTestRule.onNode(
            hasText("Cancelar Cita") and hasAnyAncestor(isDialog())
        ).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Continuar") and hasAnyAncestor(isDialog())
        ).performSemanticClick()
        composeTestRule.onNode(
            hasText("Sí, cancelar cita") and hasAnyAncestor(isDialog())
        ).performSemanticClick()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.MY_APPT_TAB_COMPLETED)
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.appointmentItem("appt-done-1"))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.MY_APPT_TAB_CANCELLED)
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.appointmentItem("appt-cancel-1"))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.MY_APPT_REFRESH_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.MY_APPT_NEW_BUTTON).performSemanticClick()

        assertThat(events).containsAtLeast(
            MyAppointmentsUiEvent.Refresh,
            MyAppointmentsUiEvent.BookNewAppointment,
            MyAppointmentsUiEvent.RescheduleAppointment(upcomingList[26]),
            MyAppointmentsUiEvent.ConfirmCancelAppointment,
            MyAppointmentsUiEvent.SelectTab(AppointmentTab.COMPLETED),
            MyAppointmentsUiEvent.SelectTab(AppointmentTab.CANCELLED)
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w600dp-h960dp")
    fun patientPaymentsLoadingErrorFiltersAndReceiptDialog() {
        val pendingTreatment = Treatment(
            id = "trt-pending",
            patientId = PatientId("pat-1"),
            description = "Ortodoncia Correctiva",
            totalCost = CopAmount(2400000L)
        )
        val paidTreatment = Treatment(
            id = "trt-paid",
            patientId = PatientId("pat-1"),
            description = "Limpieza Profunda",
            totalCost = CopAmount(200000L)
        )
        val payment = Payment(
            id = "pay-101",
            treatmentId = "trt-pending",
            amount = CopAmount(400000L),
            paymentMethod = "TRANSFERENCIA",
            createdAt = LocalDateTime.of(2026, 10, 20, 11, 0)
        )
        val balances = listOf(
            TreatmentBalance(
                treatment = pendingTreatment,
                paidAmount = CopAmount(400000L),
                payments = listOf(payment)
            ),
            TreatmentBalance(
                treatment = paidTreatment,
                paidAmount = CopAmount(200000L),
                payments = emptyList()
            )
        )

        var state by mutableStateOf(PatientPaymentsUiState(isLoading = true))
        val events = mutableListOf<PatientPaymentsUiEvent>()
        var backClicks = 0

        composeTestRule.setBiodontosContent {
            PatientPaymentsScreen(
                state = state,
                onEvent = { event ->
                    events.add(event)
                    when (event) {
                        is PatientPaymentsUiEvent.SelectFilter -> state = state.copy(selectedFilter = event.filter)
                        is PatientPaymentsUiEvent.ViewPaymentReceipt ->
                            state = state.copy(selectedPaymentForReceipt = event.payment)
                        is PatientPaymentsUiEvent.DismissPaymentReceipt ->
                            state = state.copy(selectedPaymentForReceipt = null)
                        else -> Unit
                    }
                },
                onNavigateBack = { backClicks++ },
                canNavigateBack = true
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.PAYMENTS_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.PAYMENTS_LOADING).assertIsDisplayed()

        composeTestRule.runOnIdle {
            state = state.copy(
                isLoading = false,
                errorMessage = "Error consultando pagos"
            )
        }
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.PAYMENTS_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.PAYMENTS_RETRY_BUTTON)
            .performSemanticClick()

        composeTestRule.runOnIdle {
            state = state.copy(
                errorMessage = null,
                patientName = "Laura Martínez",
                totalPending = CopAmount(2000000L),
                totalPaid = CopAmount(600000L),
                balances = balances,
                recentPayments = listOf(payment)
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.PAYMENTS_HERO_CARD)
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.paymentFilterChip(PaymentFilter.COMPLETED.name))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.treatmentBalanceItem("trt-paid"))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.treatmentBalanceItem("trt-pending"))
            .assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.paymentFilterChip(PaymentFilter.ALL.name))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.treatmentBalanceItem("trt-pending"))
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.paymentItem("pay-101"))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.PAYMENTS_RECEIPT_DIALOG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.PAYMENTS_RECEIPT_CLOSE_BUTTON)
            .performSemanticClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.PAYMENTS_RECEIPT_DIALOG)
            .assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.PatientPortal.PAYMENTS_REFRESH_BUTTON)
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.PatientPortal.PAYMENTS_BACK_BUTTON)
            .performSemanticClick()

        assertThat(events).containsAtLeast(
            PatientPaymentsUiEvent.Retry,
            PatientPaymentsUiEvent.SelectFilter(PaymentFilter.COMPLETED),
            PatientPaymentsUiEvent.SelectFilter(PaymentFilter.ALL),
            PatientPaymentsUiEvent.ViewPaymentReceipt(payment),
            PatientPaymentsUiEvent.DismissPaymentReceipt,
            PatientPaymentsUiEvent.Refresh
        )
        assertThat(backClicks).isEqualTo(1)
    }

    private fun sampleDoctor(
        id: String,
        firstName: String,
        lastName: String
    ): Doctor = Doctor(
        id = DoctorId(id),
        firstName = firstName,
        lastName = lastName,
        specialty = "Ortodoncia",
        licenseNumber = "RM-12345",
        bio = "Especialista en ortodoncia y estética dental.",
        photoUrl = null,
        clinicAddress = "Calle 100 # 15-20",
        services = "Ortodoncia, Blanqueamiento"
    )
}
