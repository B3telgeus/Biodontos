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
import com.example.biodontos.presentation.patients.PatientListScreen
import com.example.biodontos.presentation.patients.PatientListState
import com.example.biodontos.presentation.patients.PatientQuickFilter
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
    fun clinicalAdmissionNavigatesTwoStepsShowsSuccessDialogAndNavigatesToDetail() {
        var state by mutableStateOf(
            ClinicalAdmissionState(
                currentStep = 1
            )
        )
        var finishedPatientId: String? by mutableStateOf(null)

        fun handleEvent(event: ClinicalAdmissionEvent) {
            state = when (event) {
                is ClinicalAdmissionEvent.JumpToStep -> state.copy(currentStep = event.step)
                is ClinicalAdmissionEvent.NextStep -> {
                    if (state.currentStep == 1) {
                        state.copy(currentStep = 2)
                    } else {
                        state.copy(
                            completionResult = com.example.biodontos.presentation.admission.AdmissionCompletionResult(
                                patientId = "pat-123",
                                patientFullName = "${state.firstName} ${state.firstLastName}".trim(),
                                dniCedula = state.dniCedula,
                                clinicalRecordId = "rec-123",
                                isNewAdmission = true
                            )
                        )
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
                is ClinicalAdmissionEvent.ToggleAnamnesisQuestion -> {
                    val cond = com.example.biodontos.domain.model.AnamnesisCondition.fromKey(event.questionKey)
                    if (cond != null) {
                        val updated = if (event.value) state.activeAnamnesisConditions + cond else state.activeAnamnesisConditions - cond
                        state.copy(activeAnamnesisConditions = updated)
                    } else state
                }
                is ClinicalAdmissionEvent.UpdateAnamnesisOptionNote -> state.copy(
                    anamnesisOptionNotes = state.anamnesisOptionNotes + (event.questionKey to event.note)
                )
                is ClinicalAdmissionEvent.DismissSuccessDialog -> state.copy(completionResult = null)
                else -> state
            }
        }

        composeTestRule.setBiodontosContent {
            ClinicalAdmissionContent(
                state = state,
                onEvent = ::handleEvent,
                onNavigateBack = {},
                onGoToDetail = { patientId -> finishedPatientId = patientId }
            )
        }

        // Paso 1: Identificación
        composeTestRule.onNodeWithTag(TestTags.Admission.DNI_FIELD).performTextInput("10203040")
        composeTestRule.onNodeWithTag(TestTags.Admission.FIRST_NAME_FIELD).performTextInput("Ana")
        composeTestRule.onNodeWithTag(TestTags.Admission.FIRST_LAST_NAME_FIELD).performTextInput("López")
        composeTestRule.onNodeWithTag(TestTags.Admission.PHONE_FIELD).performTextInput("3001234567")

        composeTestRule.onNodeWithTag(TestTags.Admission.NEXT_BUTTON).performClick()

        // Paso 2: Anamnesis
        composeTestRule.onNodeWithTag(TestTags.Admission.anamnesisSwitch("alergias"))
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Admission.anamnesisNoteField("alergias"))
            .performScrollTo()
            .performTextInput("Penicilina y derivados")

        // Finalizar admisión (Paso 2 -> diálogo de éxito)
        composeTestRule.onNodeWithTag(TestTags.Admission.NEXT_BUTTON).performClick()

        // Validar modal de registro exitoso
        composeTestRule.onNodeWithTag(TestTags.Admission.SUCCESS_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Admission.SUCCESS_DIALOG_PATIENT_NAME)
            .assertTextContains("Ana López", substring = true)
        composeTestRule.onNodeWithTag(TestTags.Admission.SUCCESS_DIALOG_DNI)
            .assertTextContains("10203040", substring = true)

        // Clic en botón "Ir a la Ficha del Paciente"
        composeTestRule.onNodeWithTag(TestTags.Admission.SUCCESS_DIALOG_GO_TO_DETAIL_BUTTON).performClick()

        composeTestRule.runOnIdle {
            assertThat(finishedPatientId).isEqualTo("pat-123")
        }
    }

    @Test
    fun patientDetailPeriodontogramTabRendersAndHandlesInteractions() {
        var selectedTooth by mutableStateOf(11)
        var savedCalled by mutableStateOf(false)
        var observationsText by mutableStateOf("")

        val detailState = PatientDetailState(
            patientId = "pat-123",
            patient = Patient(
                id = "pat-123",
                dniCedula = "10203040",
                firstName = "Ana",
                firstLastName = "López",
                secondLastName = "Pérez",
                birthDate = "1995-05-20",
                phone = "3001234567",
                email = "ana.lopez@example.com",
                locality = "Chapinero"
            ),
            selectedTab = PatientDetailTab.PERIODONTOGRAM,
            periodontalSelectedTooth = selectedTooth,
            periodontalObservations = observationsText,
            isLoading = false
        )

        composeTestRule.setBiodontosContent {
            PatientDetailScreen(
                state = detailState,
                onTabSelected = {},
                onNavigateBack = {},
                onSelectPeriodontalTooth = { selectedTooth = it },
                onPeriodontalObservationsChange = { observationsText = it },
                onSavePeriodontogram = { savedCalled = true }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Patients.PERIODONTOGRAM_TAB_CONTENT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Periodontogram.ROOT).assertIsDisplayed()

        // Seleccionar diente 18
        composeTestRule.onNodeWithTag(TestTags.Periodontogram.toothChip(18))
            .assertExists()
            .performClick()

        // Modificar observaciones periodontales
        composeTestRule.onNodeWithTag(TestTags.Patients.PERIODONTOGRAM_OBSERVATIONS_INPUT)
            .performScrollTo()
            .performTextInput("Gingivitis localizada en sextante anterosuperior")

        // Guardar periodontograma
        composeTestRule.onNodeWithTag(TestTags.Patients.PERIODONTOGRAM_SAVE_BUTTON)
            .performScrollTo()
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(savedCalled).isTrue()
        }
    }
}
