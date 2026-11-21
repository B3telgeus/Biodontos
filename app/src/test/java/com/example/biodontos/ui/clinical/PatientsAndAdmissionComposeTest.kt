package com.example.biodontos.ui.clinical

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.domain.model.Anamnesis
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.DentitionType
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import com.example.biodontos.domain.model.history.ActivityFilter
import com.example.biodontos.domain.model.history.ActivityType
import com.example.biodontos.domain.model.history.AuditAction
import com.example.biodontos.domain.model.history.AuditStamp
import com.example.biodontos.domain.model.history.PatientActivity
import com.example.biodontos.domain.model.history.PatientHistorySummary
import com.example.biodontos.presentation.admission.ClinicalAdmissionContent
import com.example.biodontos.presentation.admission.ClinicalAdmissionEvent
import com.example.biodontos.presentation.admission.ClinicalAdmissionState
import com.example.biodontos.presentation.admission.OperativeConsentDialog
import com.example.biodontos.presentation.patients.PatientListScreen
import com.example.biodontos.presentation.patients.PatientListState
import com.example.biodontos.presentation.patients.PatientQuickFilter
import com.example.biodontos.presentation.patients.PatientRegistrationScreen
import com.example.biodontos.presentation.patients.PatientRegistrationState
import com.example.biodontos.presentation.patients.detail.PatientDetailScreen
import com.example.biodontos.presentation.patients.detail.PatientDetailState
import com.example.biodontos.presentation.patients.detail.PatientDetailTab
import com.example.biodontos.presentation.patients.detail.PatientHistoryUiState
import com.example.biodontos.presentation.patients.detail.history.ClarificationNoteDialog
import com.example.biodontos.presentation.patients.detail.history.NewPaymentDialog
import com.example.biodontos.presentation.patients.detail.history.NewTreatmentBudgetDialog
import com.example.biodontos.presentation.patients.detail.history.PatientHistoryTab
import com.example.biodontos.presentation.patients.detail.history.QuickCreateActivityBottomSheet
import com.example.biodontos.presentation.patients.detail.history.TreatmentStatusUpdateDialog
import com.example.biodontos.presentation.patients.detail.history.VoidActivityDialog
import com.example.biodontos.ui.performSemanticClick
import com.example.biodontos.ui.setBiodontosContent
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@MediumTest
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PatientsAndAdmissionComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun patientListShowsThirtyPlusPatientsScrollsByKeyAndIndexAndHandlesClick() {
        val patients = (0 until 35).map { idx ->
            Patient(
                id = "pat-$idx",
                dniCedula = "1000$idx",
                firstName = "Paciente",
                firstLastName = "Numero$idx",
                phone = "300100$idx",
                consentSigned = idx % 2 == 0,
                hasCriticalAllergies = idx % 5 == 0,
                allergiesDescription = if (idx % 5 == 0) "Penicilina" else "Ninguna"
            )
        }
        var clickedPatient by mutableStateOf<Patient?>(null)

        composeTestRule.setBiodontosContent {
            PatientListScreen(
                state = PatientListState(patients = patients),
                onPatientClick = { clickedPatient = it },
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.LIST_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.LIST_LAZY_COLUMN)
            .assertIsDisplayed()
            .performScrollToKey("pat-30")

        composeTestRule.onNodeWithTag(TestTags.Patients.patientItem("pat-30")).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Patients.LIST_LAZY_COLUMN)
            .performScrollToIndex(0)

        composeTestRule.onNodeWithTag(TestTags.Patients.patientItem("pat-0"))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(clickedPatient?.id).isEqualTo("pat-0")
        }
    }

    @Test
    fun patientListFiltersBySearchQueryAndQuickFilterChips() {
        val samplePatients = listOf(
            Patient(
                id = "pat-allergy",
                dniCedula = "101010",
                firstName = "Carlos",
                firstLastName = "Gómez",
                consentSigned = true,
                hasCriticalAllergies = true,
                allergiesDescription = "Látex"
            ),
            Patient(
                id = "pat-pending",
                dniCedula = "202020",
                firstName = "Laura",
                firstLastName = "Méndez",
                consentSigned = false,
                hasCriticalAllergies = false
            )
        )
        var state by mutableStateOf(PatientListState(patients = samplePatients))

        composeTestRule.setBiodontosContent {
            PatientListScreen(
                state = state,
                onSearchQueryChange = { state = state.copy(searchQuery = it) },
                onFilterSelected = { state = state.copy(selectedFilter = it) },
                onPatientClick = {},
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.filterChip(PatientQuickFilter.CRITICAL_ALLERGIES.name))
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Patients.patientItem("pat-allergy")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.patientItem("pat-pending")).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.Patients.filterChip(PatientQuickFilter.PENDING_CONSENT.name))
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Patients.patientItem("pat-pending")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.patientItem("pat-allergy")).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.Patients.filterChip(PatientQuickFilter.ALL.name))
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Patients.LIST_SEARCH_FIELD)
            .performTextInput("Carlos")

        composeTestRule.onNodeWithTag(TestTags.Patients.patientItem("pat-allergy")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.patientItem("pat-pending")).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.Patients.LIST_SEARCH_CLEAR_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.Patients.patientItem("pat-pending")).assertIsDisplayed()
    }

    @Test
    fun patientListRendersLoadingErrorAndEmptyStatesWithRetry() {
        var state by mutableStateOf(PatientListState(isLoading = true))
        var retryCount by mutableIntStateOf(0)

        composeTestRule.setBiodontosContent {
            PatientListScreen(
                state = state,
                onPatientClick = {},
                onRetry = { retryCount += 1 }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.LIST_LOADING).assertIsDisplayed()

        composeTestRule.runOnIdle {
            state = PatientListState(isLoading = false, error = "Fallo al sincronizar pacientes")
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.LIST_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.LIST_RETRY_BUTTON).performClick()
        composeTestRule.runOnIdle {
            assertThat(retryCount).isEqualTo(1)
        }

        composeTestRule.runOnIdle {
            state = PatientListState(isLoading = false, patients = emptyList())
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.LIST_EMPTY).assertIsDisplayed()
    }

    @Test
    fun patientRegistrationValidatesInputsAllergiesConsentAndButtons() {
        var state by mutableStateOf(PatientRegistrationState())
        var saveClicked by mutableStateOf(false)
        var backClicked by mutableStateOf(false)

        composeTestRule.setBiodontosContent {
            PatientRegistrationScreen(
                state = state,
                onDniChange = { state = state.copy(dniCedula = it, dniError = null) },
                onFirstNameChange = { state = state.copy(firstName = it, firstNameError = null) },
                onLastNameChange = { state = state.copy(lastName = it, lastNameError = null) },
                onPhoneChange = { state = state.copy(phone = it) },
                onConsentChange = { state = state.copy(consentSigned = it) },
                onAllergiesChange = { state = state.copy(hasCriticalAllergies = it) },
                onAllergiesDescriptionChange = { state = state.copy(allergiesDescription = it) },
                onSaveClick = { saveClicked = true },
                onBackClick = { backClicked = true }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.REG_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.REG_DNI_FIELD).performTextInput("10203040")
        composeTestRule.onNodeWithTag(TestTags.Patients.REG_FIRST_NAME_FIELD).performTextInput("María")
        composeTestRule.onNodeWithTag(TestTags.Patients.REG_LAST_NAME_FIELD).performTextInput("Gómez")
        composeTestRule.onNodeWithTag(TestTags.Patients.REG_PHONE_FIELD).performTextInput("3001234567")

        composeTestRule.onNodeWithTag(TestTags.Patients.REG_CONSENT_ROW)
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()

        composeTestRule.onNodeWithTag(TestTags.Patients.REG_ALLERGIES_DESC_FIELD).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Patients.REG_ALLERGIES_ROW)
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()

        composeTestRule.onNodeWithTag(TestTags.Patients.REG_ALLERGIES_DESC_FIELD)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput("Penicilina")

        composeTestRule.runOnIdle {
            state = state.copy(
                dniError = "DNI inválido",
                firstNameError = "Nombre requerido",
                error = "Revise los campos obligatorios"
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.REG_ERROR_TEXT)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Revise los campos obligatorios")

        composeTestRule.onNodeWithTag(TestTags.Patients.REG_SAVE_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Patients.REG_CANCEL_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(saveClicked).isTrue()
            assertThat(backClicked).isTrue()
            assertThat(state.allergiesDescription).isEqualTo("Penicilina")
        }
    }

    @Test
    fun patientDetailSwitchesAllTabsAndRendersClinicalData() {
        val now = LocalDateTime.of(2026, 10, 20, 14, 30)
        val patient = Patient(
            id = "pat-1",
            dniCedula = "10203040",
            firstName = "María",
            firstLastName = "Gómez",
            eps = "Sura",
            phone = "3001234567",
            consentSigned = true,
            hasCriticalAllergies = true,
            allergiesDescription = "Amoxicilina"
        )
        val evolution = Evolution(
            id = "evo-1",
            recordId = "rec-1",
            doctorId = "doc-1",
            date = "2026-10-20",
            procedureTime = "14:30:00",
            description = "Obturación resina pieza 16",
            toothTreated = 16,
            cost = 150000.0,
            paymentAmount = 100000.0
        )
        val treatment = Treatment(
            id = "treat-1",
            patientId = PatientId("pat-1"),
            description = "Rehabilitación posterior",
            totalCost = CopAmount(450000L),
            status = "EN_CURSO"
        )
        val appointment = Appointment(
            id = "appt-1",
            patientId = "pat-1",
            startTime = now.plusDays(5),
            status = AppointmentStatus.CONFIRMED,
            isConfirmed = true,
            reason = "Control operatoria"
        )

        var state by mutableStateOf(
            PatientDetailState(
                patientId = "pat-1",
                patient = patient,
                clinicalRecord = ClinicalRecord(
                    id = "rec-1",
                    patientId = "pat-1",
                    dentitionType = DentitionType.ADULT,
                    mainDiagnosis = "K02.1 Caries de la dentina"
                ),
                anamnesis = Anamnesis(
                    id = "ana-1",
                    patientId = "pat-1",
                    alergias = true,
                    observaciones = "Sin novedades sistémicas adicionales"
                ),
                consent = InformedConsent(
                    id = "cons-1",
                    patientId = "pat-1",
                    doctorId = "doc-1",
                    treatmentSummary = "Plan Integral",
                    status = "SIGNED"
                ),
                evolutions = listOf(evolution),
                treatments = listOf(treatment),
                appointments = listOf(appointment),
                isLoading = false,
                selectedTab = PatientDetailTab.SUMMARY
            )
        )
        var odontogramOpened by mutableStateOf(false)

        var appointmentSaved by mutableStateOf(false)

        composeTestRule.setBiodontosContent {
            PatientDetailScreen(
                state = state,
                onTabSelected = { state = state.copy(selectedTab = it) },
                onNavigateBack = {},
                onNavigateToOdontogram = { odontogramOpened = true },
                onNavigateToAdmission = {},
                onOpenNewEvolutionDialog = { state = state.copy(showNewEvolutionDialog = true) },
                onDismissNewEvolutionDialog = { state = state.copy(showNewEvolutionDialog = false) },
                onEvolutionDescriptionChange = { state = state.copy(newEvolutionDescription = it) },
                onEvolutionToothChange = { state = state.copy(newEvolutionToothTreated = it) },
                onEvolutionCostChange = { state = state.copy(newEvolutionCost = it) },
                onEvolutionPaymentChange = { state = state.copy(newEvolutionPayment = it) },
                onSelectCatalogMaterial = {},
                onAddMaterial = {},
                onRemoveMaterial = {},
                onSaveEvolution = {},
                onOpenNewAppointmentDialog = { state = state.copy(showNewAppointmentDialog = true) },
                onDismissNewAppointmentDialog = { state = state.copy(showNewAppointmentDialog = false) },
                onAppointmentDateChange = { state = state.copy(newAppointmentDate = it) },
                onAppointmentTimeChange = { state = state.copy(newAppointmentTime = it) },
                onAppointmentReasonChange = { state = state.copy(newAppointmentReason = it) },
                onSaveAppointment = { appointmentSaved = true },
                onClearMessages = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_STATUS_BAR).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Patients.detailTab(PatientDetailTab.ODONTOGRAM.name))
            .performScrollTo()
            .performClick()
            .assertIsSelected()

        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_OPEN_ADMISSION_BUTTON)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_OPEN_ODONTOGRAM_BUTTON)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(odontogramOpened).isTrue()
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.detailTab(PatientDetailTab.EVOLUTIONS.name))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Patients.evolutionItem("evo-1")).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_FAB).performClick()
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_NEW_EVOLUTION_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_NEW_EVOLUTION_DESC_FIELD)
            .performTextInput("Sellante oclusal")
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_NEW_EVOLUTION_COST_FIELD)
            .performTextReplacement("90000")
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_NEW_EVOLUTION_PAYMENT_FIELD)
            .performTextReplacement("45000")

        composeTestRule.runOnIdle {
            state = state.copy(showNewEvolutionDialog = false)
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.detailTab(PatientDetailTab.TREATMENTS.name))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_LIST)
            .performScrollToKey("treat-1")
        composeTestRule.onNodeWithTag(TestTags.Patients.treatmentItem("treat-1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_LIST)
            .performScrollToKey("appt-1")
        composeTestRule.onNodeWithTag(TestTags.Patients.appointmentItem("appt-1")).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_FAB).performClick()
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_NEW_APPT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_NEW_APPT_DATE_FIELD)
            .performTextInput("2026-11-20")
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_NEW_APPT_TIME_FIELD)
            .performTextReplacement("15:00")
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_NEW_APPT_REASON_FIELD)
            .performTextReplacement("Control post operatoria")
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_NEW_APPT_SAVE_BUTTON)
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(appointmentSaved).isTrue()
            state = state.copy(showNewAppointmentDialog = false, isLoading = true)
        }
        composeTestRule.onNodeWithTag(TestTags.Patients.DETAIL_LOADING).assertIsDisplayed()
    }

    @Test
    fun patientHistoryTabFiltersExpandsAndTriggersClinicalActions() {
        val sampleTime = LocalDateTime.of(2026, 10, 20, 14, 30)
        val evo = Evolution(
            id = "evo-1",
            recordId = "rec-1",
            doctorId = "doc-1",
            doctorName = "Dr. Mendoza",
            date = "2026-10-20",
            procedureTime = "14:30:00",
            description = "Profilaxis ultrasónica",
            toothTreated = 16
        )
        val treatment = Treatment(
            id = "treat-1",
            patientId = PatientId("pat-1"),
            description = "Corona circonio",
            totalCost = CopAmount(800000L),
            status = "EN_CURSO"
        )
        val activities = listOf(
            PatientActivity.EvolutionActivity(
                id = "evo-1",
                evolution = evo,
                occurredAt = sampleTime,
                createdStamp = AuditStamp("Dr. Mendoza", AuditAction.CREATE, sampleTime)
            ),
            PatientActivity.TreatmentBudgetActivity(
                id = "treat-1",
                treatment = treatment,
                status = TreatmentStatus.EN_CURSO,
                occurredAt = sampleTime.minusDays(1)
            ),
            PatientActivity.PaymentActivity(
                id = "pay-1",
                payment = Payment(
                    id = "pay-1",
                    treatmentId = "treat-1",
                    amount = CopAmount(300000L),
                    paymentMethod = "TRANSFERENCIA"
                ),
                treatmentDescription = "Corona circonio",
                occurredAt = sampleTime.minusHours(4)
            )
        )

        var historyState by mutableStateOf(
            PatientHistoryUiState(
                allActivities = activities,
                filteredActivities = activities,
                availableMonths = listOf(YearMonth.of(2026, 10)),
                summary = PatientHistorySummary.from(activities)
            )
        )
        var voidRequestedFor by mutableStateOf<String?>(null)
        var clarifyRequestedFor by mutableStateOf<String?>(null)
        var statusRequestedFor by mutableStateOf<String?>(null)
        var paymentRequestedFor by mutableStateOf<String?>(null)

        composeTestRule.setBiodontosContent {
            PatientHistoryTab(
                historyState = historyState,
                onSelectMonth = { ym ->
                    val newFilter = historyState.filter.copy(month = ym)
                    historyState = historyState.copy(
                        filter = newFilter,
                        filteredActivities = newFilter.apply(activities)
                    )
                },
                onToggleType = { type ->
                    val next = if (type in historyState.filter.types) {
                        historyState.filter.types - type
                    } else {
                        historyState.filter.types + type
                    }
                    val newFilter = historyState.filter.copy(types = next)
                    historyState = historyState.copy(
                        filter = newFilter,
                        filteredActivities = newFilter.apply(activities)
                    )
                },
                onClearFilters = {
                    val newFilter = ActivityFilter()
                    historyState = historyState.copy(
                        filter = newFilter,
                        filteredActivities = newFilter.apply(activities)
                    )
                },
                onToggleExpandActivity = { id ->
                    val nextId = if (historyState.expandedActivityId == id) null else id
                    historyState = historyState.copy(expandedActivityId = nextId)
                },
                onRequestVoidActivity = { voidRequestedFor = it.id },
                onOpenClarificationDialog = { clarifyRequestedFor = it.id },
                onOpenTreatmentStatusDialog = { statusRequestedFor = it.id },
                onOpenNewPaymentForTreatment = { paymentRequestedFor = it },
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.HISTORY_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.HISTORY_KPI_CARD).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Patients.historyTypeChip(ActivityType.PAYMENT.name))
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Patients.HISTORY_CLEAR_FILTERS_BUTTON)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Patients.HISTORY_LIST)
            .performScrollToKey("evo-1")
        composeTestRule.onNodeWithTag(TestTags.Patients.historyItem("evo-1"))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Patients.historyClarifyButton("evo-1"))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Patients.historyVoidButton("evo-1"))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Patients.HISTORY_LIST)
            .performScrollToKey("treat-1")
        composeTestRule.onNodeWithTag(TestTags.Patients.historyItem("treat-1"))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Patients.historyStatusButton("treat-1"))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Patients.historyPaymentButton("treat-1"))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(clarifyRequestedFor).isEqualTo("evo-1")
            assertThat(voidRequestedFor).isEqualTo("evo-1")
            assertThat(statusRequestedFor).isEqualTo("treat-1")
            assertThat(paymentRequestedFor).isEqualTo("treat-1")
        }
    }

    @Test
    fun patientHistoryTabRendersLoadingErrorAndEmptyStates() {
        var historyState by mutableStateOf(PatientHistoryUiState(isTimelineLoading = true))
        var retryClicks by mutableIntStateOf(0)

        composeTestRule.setBiodontosContent {
            PatientHistoryTab(
                historyState = historyState,
                onSelectMonth = {},
                onToggleType = {},
                onClearFilters = {},
                onToggleExpandActivity = {},
                onRequestVoidActivity = {},
                onOpenClarificationDialog = {},
                onOpenTreatmentStatusDialog = {},
                onOpenNewPaymentForTreatment = {},
                onRetry = { retryClicks += 1 }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.HISTORY_LOADING).assertIsDisplayed()

        composeTestRule.runOnIdle {
            historyState = PatientHistoryUiState(
                isTimelineLoading = false,
                timelineError = "Error cargando línea de tiempo"
            )
        }
        composeTestRule.onNodeWithTag(TestTags.Patients.HISTORY_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.HISTORY_RETRY_BUTTON).performClick()
        composeTestRule.runOnIdle {
            assertThat(retryClicks).isEqualTo(1)
            historyState = PatientHistoryUiState(isTimelineLoading = false)
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.HISTORY_EMPTY).assertIsDisplayed()
    }

    @Test
    fun clinicalHistoryDialogsHandleVoidClarificationStatusTreatmentAndPayment() {
        val sampleEvo = Evolution(
            id = "evo-1",
            recordId = "rec-1",
            doctorId = "doc-1",
            date = "2026-10-20",
            procedureTime = "10:00:00",
            description = "Endodoncia pieza 16"
        )
        val sampleTreatment = Treatment(
            id = "treat-1",
            patientId = PatientId("pat-1"),
            description = "Corona metal porcelana",
            totalCost = CopAmount(600000L),
            status = "EN_CURSO"
        )
        val sampleActivity = PatientActivity.EvolutionActivity(
            id = "evo-1",
            evolution = sampleEvo,
            occurredAt = LocalDateTime.of(2026, 10, 20, 10, 0)
        )

        var activeDialog by mutableStateOf("VOID")
        var voidReason by mutableStateOf("")
        var voidStep by mutableIntStateOf(1)
        var voidConfirmed by mutableStateOf(false)

        var clarificationText by mutableStateOf("")
        var clarificationSaved by mutableStateOf(false)

        var targetStatus by mutableStateOf<TreatmentStatus?>(null)
        var statusReason by mutableStateOf("")
        var statusConfirmed by mutableStateOf(false)

        var treatmentDesc by mutableStateOf("")
        var treatmentCost by mutableStateOf("")
        var treatmentSaved by mutableStateOf(false)

        var paymentAmount by mutableStateOf("")
        var paymentMethod by mutableStateOf("EFECTIVO")
        var paymentSaved by mutableStateOf(false)
        var quickEvolutionClicked by mutableStateOf(false)

        composeTestRule.setBiodontosContent {
            when (activeDialog) {
                "QUICK" -> QuickCreateActivityBottomSheet(
                    onCreateEvolution = { quickEvolutionClicked = true },
                    onCreateAppointment = {},
                    onCreateTreatmentBudget = {},
                    onCreatePayment = {},
                    onDismiss = {}
                )
                "VOID" -> VoidActivityDialog(
                    target = sampleActivity,
                    reason = voidReason,
                    step = voidStep,
                    isProcessing = false,
                    onReasonChange = { voidReason = it },
                    onAdvanceStep = { voidStep = 2 },
                    onConfirmVoid = { voidConfirmed = true },
                    onDismiss = {}
                )
                "CLARIFY" -> ClarificationNoteDialog(
                    originalEvolution = sampleEvo,
                    clarificationText = clarificationText,
                    isSaving = false,
                    onTextChange = { clarificationText = it },
                    onSave = { clarificationSaved = true },
                    onDismiss = {}
                )
                "STATUS" -> TreatmentStatusUpdateDialog(
                    treatment = sampleTreatment,
                    selectedTargetStatus = targetStatus,
                    reason = statusReason,
                    isUpdating = false,
                    onSelectStatus = { targetStatus = it },
                    onReasonChange = { statusReason = it },
                    onConfirm = { statusConfirmed = true },
                    onDismiss = {}
                )
                "TREATMENT" -> NewTreatmentBudgetDialog(
                    description = treatmentDesc,
                    totalCost = treatmentCost,
                    isSaving = false,
                    onDescriptionChange = { treatmentDesc = it },
                    onTotalCostChange = { treatmentCost = it },
                    onSave = { treatmentSaved = true },
                    onDismiss = {}
                )
                "PAYMENT" -> NewPaymentDialog(
                    treatments = listOf(sampleTreatment),
                    selectedTreatmentId = sampleTreatment.id,
                    amount = paymentAmount,
                    method = paymentMethod,
                    isSaving = false,
                    onSelectTreatment = {},
                    onAmountChange = { paymentAmount = it },
                    onMethodChange = { paymentMethod = it },
                    onSave = { paymentSaved = true },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.VOID_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.VOID_CONFIRM_BUTTON).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.Patients.VOID_REASON_FIELD)
            .performTextInput("Error en registro de pieza dental")
        composeTestRule.onNodeWithTag(TestTags.Patients.VOID_CONFIRM_BUTTON)
            .assertIsEnabled()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Patients.VOID_CONFIRM_BUTTON)
            .assertIsEnabled()
            .performClick()
        composeTestRule.runOnIdle {
            assertThat(voidConfirmed).isTrue()
            activeDialog = "CLARIFY"
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.CLARIFICATION_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.CLARIFICATION_TEXT_FIELD)
            .performTextInput("Se rectifica pieza tratada a 26")
        composeTestRule.onNodeWithTag(TestTags.Patients.CLARIFICATION_SAVE_BUTTON)
            .assertIsEnabled()
            .performClick()
        composeTestRule.runOnIdle {
            assertThat(clarificationSaved).isTrue()
            activeDialog = "STATUS"
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.STATUS_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.statusOption(TreatmentStatus.CANCELADO.name))
            .performClick()
            .assertIsSelected()
        composeTestRule.onNodeWithTag(TestTags.Patients.STATUS_REASON_FIELD)
            .performTextInput("Paciente pospone tratamiento")
        composeTestRule.onNodeWithTag(TestTags.Patients.STATUS_CONFIRM_BUTTON)
            .assertIsEnabled()
            .performClick()
        composeTestRule.runOnIdle {
            assertThat(statusConfirmed).isTrue()
            activeDialog = "TREATMENT"
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.NEW_TREATMENT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.NEW_TREATMENT_DESC_FIELD)
            .performTextInput("Ortodoncia interceptiva")
        composeTestRule.onNodeWithTag(TestTags.Patients.NEW_TREATMENT_COST_FIELD)
            .performTextInput("1200000")
        composeTestRule.onNodeWithTag(TestTags.Patients.NEW_TREATMENT_SAVE_BUTTON).performClick()
        composeTestRule.runOnIdle {
            assertThat(treatmentSaved).isTrue()
            activeDialog = "PAYMENT"
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.NEW_PAYMENT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.NEW_PAYMENT_AMOUNT_FIELD)
            .performTextInput("250000")
        composeTestRule.onNodeWithTag(TestTags.Patients.paymentMethodChip("TARJETA"))
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Patients.NEW_PAYMENT_SAVE_BUTTON).performClick()
        composeTestRule.runOnIdle {
            assertThat(paymentMethod).isEqualTo("TARJETA")
            assertThat(paymentSaved).isTrue()
            activeDialog = "QUICK"
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.QUICK_SHEET).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Patients.QUICK_SHEET_EVOLUTION)
            .performSemanticClick()
        composeTestRule.runOnIdle {
            assertThat(quickEvolutionClicked).isTrue()
        }
    }

    @Test
    fun clinicalAdmissionNavigatesAllSevenStepsAndCapturesClinicalInputs() {
        val activeMaterial = ClinicalMaterial(
            id = MaterialId("mat-1"),
            name = "Resina Filtek",
            invimaRegistration = "INVIMA 2024DM-001",
            batchLotNumber = "LOT-01",
            manufacturingDate = LocalDate.now().minusMonths(2),
            expirationDate = LocalDate.now().plusYears(1)
        )
        var state by mutableStateOf(
            ClinicalAdmissionState(
                currentStep = 1,
                materialsCatalog = listOf(activeMaterial),
                selectedCatalogMaterial = activeMaterial
            )
        )
        var finishedTriggered by mutableStateOf(false)

        fun handleEvent(event: ClinicalAdmissionEvent) {
            state = when (event) {
                is ClinicalAdmissionEvent.JumpToStep -> state.copy(currentStep = event.step)
                is ClinicalAdmissionEvent.NextStep -> {
                    if (state.currentStep == 7) {
                        finishedTriggered = true
                        state
                    } else {
                        state.copy(currentStep = state.currentStep + 1)
                    }
                }
                is ClinicalAdmissionEvent.PreviousStep ->
                    state.copy(currentStep = (state.currentStep - 1).coerceAtLeast(1))
                is ClinicalAdmissionEvent.UpdateDni -> state.copy(dniCedula = event.value)
                is ClinicalAdmissionEvent.UpdateFirstName -> state.copy(firstName = event.value)
                is ClinicalAdmissionEvent.UpdateFirstLastName -> state.copy(firstLastName = event.value)
                is ClinicalAdmissionEvent.ToggleHasGuardian -> state.copy(hasGuardian = event.value)
                is ClinicalAdmissionEvent.UpdateAntecedents -> state.copy(
                    lastDentalVisit = event.lastVisit,
                    consultationReason = event.reason,
                    currentConsultationReason = event.currentReason,
                    illnessHistory = event.illnessHistory
                )
                is ClinicalAdmissionEvent.UpdateAllergies -> state.copy(
                    hasCriticalAllergies = event.hasAllergies,
                    allergiesDescription = event.description
                )
                is ClinicalAdmissionEvent.ToggleAnamnesisQuestion -> when (event.questionKey) {
                    "diabetes" -> state.copy(diabetes = event.value)
                    else -> state
                }
                is ClinicalAdmissionEvent.SelectConditionToApply ->
                    state.copy(selectedConditionToApply = event.condition)
                is ClinicalAdmissionEvent.ApplySurfaceCondition -> {
                    val toothMap = state.teethSurfaces[event.toothNumber]?.toMutableMap() ?: mutableMapOf()
                    toothMap[event.surface] = state.selectedConditionToApply
                    val updated = state.teethSurfaces.toMutableMap()
                    updated[event.toothNumber] = toothMap
                    state.copy(teethSurfaces = updated)
                }
                is ClinicalAdmissionEvent.UpdateDiagnoses -> state.copy(
                    mainDiagnosis = event.main,
                    relatedDiagnoses = event.related,
                    complementaryExams = event.exams,
                    treatmentPlanDescription = event.plan
                )
                is ClinicalAdmissionEvent.ConfirmConsentSignature -> state.copy(
                    consentPatientSignature = event.signature,
                    consentSigned = event.signature.isNotBlank()
                )
                is ClinicalAdmissionEvent.UpdateEvolution -> state.copy(
                    evolutionDescription = event.description,
                    evolutionCost = event.cost,
                    evolutionPayment = event.payment
                )
                is ClinicalAdmissionEvent.SelectMaterialFromCatalog ->
                    state.copy(selectedCatalogMaterial = event.material)
                is ClinicalAdmissionEvent.AddMaterialUsed -> {
                    val mat = checkNotNull(state.selectedCatalogMaterial)
                    val used = EvolutionMaterialUsed(
                        materialId = mat.id.value,
                        materialName = mat.name,
                        quantityUsed = event.quantity,
                        lotNumberSnapshot = mat.batchLotNumber,
                        invimaSnapshot = mat.invimaRegistration,
                        expirationDateSnapshot = mat.expirationDate.toString()
                    )
                    state.copy(usedMaterials = state.usedMaterials + used)
                }
                is ClinicalAdmissionEvent.ConfirmEvolutionSignature ->
                    state.copy(evolutionPatientSignature = event.signature)
                is ClinicalAdmissionEvent.ToggleCloseRecord ->
                    state.copy(closeRecordOnFinish = event.close, closureSummary = event.summary)
                is ClinicalAdmissionEvent.UpdateNextAppointment -> state.copy(
                    nextAppointmentDate = event.date,
                    nextAppointmentTime = event.time,
                    nextAppointmentReason = event.reason
                )
                else -> state
            }
        }

        composeTestRule.setBiodontosContent {
            ClinicalAdmissionContent(
                state = state,
                onEvent = ::handleEvent,
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Admission.ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Admission.DNI_FIELD).performTextInput("10987654")
        composeTestRule.onNodeWithTag(TestTags.Admission.FIRST_NAME_FIELD).performTextInput("Sofía")
        composeTestRule.onNodeWithTag(TestTags.Admission.FIRST_LAST_NAME_FIELD).performTextInput("Martínez")

        composeTestRule.onNodeWithTag(TestTags.Admission.GUARDIAN_SWITCH)
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()

        composeTestRule.onNodeWithTag(TestTags.Admission.CONSULTATION_REASON_FIELD)
            .performScrollTo()
            .performTextInput("Control general")

        composeTestRule.onNodeWithTag(TestTags.Admission.ALLERGIES_SWITCH)
            .performScrollTo()
            .performClick()
            .assertIsOn()

        composeTestRule.onNodeWithTag(TestTags.Admission.ALLERGIES_DESC_FIELD)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput("Sulfas")

        composeTestRule.onNodeWithTag(TestTags.Admission.STEP_HEADER_ROW).performScrollToKey(2)
        composeTestRule.onNodeWithTag(TestTags.Admission.stepChip(2)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Admission.anamnesisSwitch("diabetes"))
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()

        composeTestRule.onNodeWithTag(TestTags.Admission.STEP_HEADER_ROW).performScrollToKey(3)
        composeTestRule.onNodeWithTag(TestTags.Admission.stepChip(3)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Odontogram.conditionOption(ToothCondition.CARIES.name))
            .performClick()

        composeTestRule.onNodeWithText("Arco Dental", substring = true).performScrollTo()
        composeTestRule.onNode(
            hasTestTag(TestTags.Admission.STEP_LIST).not()
                .and(hasScrollToKeyAction())
                .and(hasAnyDescendant(hasTestTag(TestTags.Odontogram.tooth(18)))),
            useUnmergedTree = true
        ).performScrollToKey(28)

        composeTestRule.onNodeWithTag(TestTags.Odontogram.tooth(28), useUnmergedTree = true)
            .assertExists()
            .performTouchInput { click(center) }

        composeTestRule.runOnIdle {
            assertThat(state.teethSurfaces[28]?.get(ToothSurface.OCLUSAL)).isEqualTo(ToothCondition.CARIES)
        }

        composeTestRule.onNodeWithTag(TestTags.Admission.STEP_HEADER_ROW).performScrollToKey(4)
        composeTestRule.onNodeWithTag(TestTags.Admission.stepChip(4)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Periodontogram.ROOT).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Admission.STEP_HEADER_ROW).performScrollToKey(5)
        composeTestRule.onNodeWithTag(TestTags.Admission.stepChip(5)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Admission.MAIN_DIAGNOSIS_FIELD)
            .performScrollTo()
            .performTextInput("K02.1 Caries de la dentina")
        composeTestRule.onNodeWithTag(TestTags.Admission.TREATMENT_PLAN_FIELD)
            .performScrollTo()
            .performTextInput("Resina compuesta pieza 28")
        composeTestRule.onNodeWithTag(TestTags.Admission.OPEN_OPERATIVE_CONSENT_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_CANVAS)
            .performScrollTo()
            .performTouchInput {
                down(topLeft + Offset(10f, 10f))
                moveTo(center)
                moveTo(bottomRight - Offset(10f, 10f))
                up()
            }
        composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_CONFIRM_BUTTON)
            .performScrollTo()
            .performClick()
        composeTestRule.runOnIdle {
            assertThat(state.consentSigned).isTrue()
        }

        composeTestRule.onNodeWithTag(TestTags.Admission.STEP_HEADER_ROW).performScrollToKey(6)
        composeTestRule.onNodeWithTag(TestTags.Admission.stepChip(6)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Admission.EVOLUTION_DESC_FIELD)
            .performScrollTo()
            .performTextInput("Restauración estética")
        composeTestRule.onNodeWithTag(TestTags.Admission.EVOLUTION_COST_FIELD)
            .performScrollTo()
            .performTextReplacement("180000")
        composeTestRule.onNodeWithTag(TestTags.Admission.EVOLUTION_PAYMENT_FIELD)
            .performScrollTo()
            .performTextReplacement("180000")
        composeTestRule.onNodeWithText("Seleccione del inventario de clínica:").performScrollTo()
        composeTestRule.onNodeWithTag(TestTags.Admission.catalogMaterialItem("mat-1"))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("Registrar Uso en Evolución")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Admission.usedMaterialItem("mat-1"))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Admission.CLOSE_RECORD_SWITCH)
            .performScrollTo()
            .performClick()
            .assertIsOn()

        composeTestRule.onNodeWithTag(TestTags.Admission.STEP_HEADER_ROW).performScrollToKey(7)
        composeTestRule.onNodeWithTag(TestTags.Admission.stepChip(7)).performClick()
        composeTestRule.onNodeWithTag(TestTags.Admission.NEXT_APPT_DATE_FIELD)
            .performScrollTo()
            .performTextInput("2026-11-15")
        composeTestRule.onNodeWithTag(TestTags.Admission.NEXT_APPT_TIME_FIELD)
            .performScrollTo()
            .performTextReplacement("11:30")
        composeTestRule.onNodeWithTag(TestTags.Admission.NEXT_APPT_REASON_FIELD)
            .performScrollTo()
            .performTextReplacement("Control oclusal")
        composeTestRule.onNodeWithTag(TestTags.Admission.NEXT_BUTTON).performClick()

        composeTestRule.runOnIdle {
            assertThat(finishedTriggered).isTrue()
        }
    }

    @Test
    fun operativeConsentDialogSwitchesSignerAddsProceduresAndSaves() {
        var state by mutableStateOf(
            ClinicalAdmissionState(
                firstName = "Ana",
                firstLastName = "López",
                dniCedula = "102030",
                showOperativeConsentDialog = true
            )
        )
        var savedCalled by mutableStateOf(false)
        var dismissedCalled by mutableStateOf(false)

        composeTestRule.setBiodontosContent {
            OperativeConsentDialog(
                state = state,
                onDismiss = { dismissedCalled = true },
                onUpdateTreatment = {
                    state = state.copy(operativeConsentProposedTreatment = it)
                },
                onUpdateSigner = { signerType, repName, repId ->
                    state = state.copy(
                        operativeConsentSignerType = signerType,
                        operativeConsentRepresentativeName = repName,
                        operativeConsentRepresentativeId = repId
                    )
                },
                onAddProcedure = { tooth, date ->
                    val proc = com.example.biodontos.domain.model.InformedConsentProcedure(
                        consentId = "",
                        toothNumber = tooth,
                        procedureDate = date
                    )
                    state = state.copy(
                        operativeConsentProcedures = state.operativeConsentProcedures + proc
                    )
                },
                onRemoveProcedure = { index ->
                    val updated = state.operativeConsentProcedures.toMutableList()
                    if (index in updated.indices) {
                        updated.removeAt(index)
                    }
                    state = state.copy(operativeConsentProcedures = updated)
                },
                onConfirmSignatures = { pSig, dSig ->
                    state = state.copy(
                        operativeConsentPatientSignature = pSig,
                        operativeConsentDoctorSignature = dSig
                    )
                },
                onSaveConsent = { savedCalled = true }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Admission.OPERATIVE_CONSENT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Admission.OPERATIVE_CONSENT_SIGNER_PATIENT)
            .assertIsSelected()
        composeTestRule.onNodeWithTag(TestTags.Admission.OPERATIVE_CONSENT_SIGNER_GUARDIAN)
            .performClick()
            .assertIsSelected()

        composeTestRule.onNodeWithTag(TestTags.Admission.OPERATIVE_CONSENT_REP_NAME_FIELD)
            .assertIsDisplayed()
            .performTextInput("Marta López")
        composeTestRule.onNodeWithTag(TestTags.Admission.OPERATIVE_CONSENT_REP_ID_FIELD)
            .assertIsDisplayed()
            .performTextInput("52123456")

        composeTestRule.onNode(
            hasScrollToIndexAction()
                .and(hasAnyAncestor(hasTestTag(TestTags.Admission.OPERATIVE_CONSENT_DIALOG)))
        ).performScrollToIndex(1)

        composeTestRule.onNodeWithTag(TestTags.Admission.OPERATIVE_CONSENT_TREATMENT_FIELD)
            .performTextInput("Resina compuesta piezas 16 y 26")

        composeTestRule.onNode(
            hasScrollToIndexAction()
                .and(hasAnyAncestor(hasTestTag(TestTags.Admission.OPERATIVE_CONSENT_DIALOG)))
        ).performScrollToIndex(3)

        composeTestRule.onNodeWithTag(TestTags.Admission.OPERATIVE_CONSENT_TOOTH_INPUT)
            .performTextReplacement("26")
        composeTestRule.onNodeWithTag(TestTags.Admission.OPERATIVE_CONSENT_ADD_PROCEDURE_BUTTON)
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Admission.operativeProcedureItem(0))
            .assertIsDisplayed()

        composeTestRule.onNode(
            hasScrollToIndexAction()
                .and(hasAnyAncestor(hasTestTag(TestTags.Admission.OPERATIVE_CONSENT_DIALOG)))
        ).performScrollToIndex(5)
        composeTestRule.onAllNodesWithTag(TestTags.Components.SIGNATURE_PAD_ROOT)
            .assertCountEquals(2)
            .let { pads ->
                pads[0].assertIsDisplayed()
                pads[1].assertIsDisplayed()
            }

        composeTestRule.onNodeWithTag(TestTags.Admission.OPERATIVE_CONSENT_SAVE_BUTTON)
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Admission.OPERATIVE_CONSENT_DISMISS_BUTTON)
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(savedCalled).isTrue()
            assertThat(dismissedCalled).isTrue()
        }
    }
}
