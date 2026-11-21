package com.example.biodontos.ui.doctor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.presentation.doctor.documents.DoctorDocumentsScreen
import com.example.biodontos.presentation.doctor.documents.DocumentFilter
import com.example.biodontos.presentation.doctor.documents.DoctorDocumentsState
import com.example.biodontos.presentation.doctor.materials.MaterialFormState
import com.example.biodontos.presentation.doctor.materials.MaterialTabFilter
import com.example.biodontos.presentation.doctor.materials.MaterialsScreen
import com.example.biodontos.presentation.doctor.materials.MaterialsUiEvent
import com.example.biodontos.presentation.doctor.materials.MaterialsUiState
import com.example.biodontos.presentation.doctor.profile.DoctorAsyncAvatar
import com.example.biodontos.presentation.doctor.profile.DoctorProfileContent
import com.example.biodontos.presentation.doctor.profile.DoctorProfileEvent
import com.example.biodontos.presentation.doctor.profile.DoctorProfileState
import com.example.biodontos.ui.performSemanticClick
import com.example.biodontos.ui.setBiodontosContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@MediumTest
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DoctorMaterialsDocumentsProfileComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun materialsScreenLoadingErrorAndEmptyStatesRenderAndRetry() {
        val events = mutableListOf<MaterialsUiEvent>()
        var state by mutableStateOf(MaterialsUiState(isLoading = true))

        composeTestRule.setBiodontosContent {
            MaterialsScreen(
                state = state,
                onEvent = { events.add(it) },
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_LOADING).assertIsDisplayed()

        composeTestRule.runOnIdle {
            state = MaterialsUiState(
                isLoading = false,
                errorMessage = "No se pudo cargar el catálogo INVIMA"
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_ERROR).assertExists()
        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_RETRY_BUTTON).performSemanticClick()

        composeTestRule.runOnIdle {
            assertThat(events).contains(MaterialsUiEvent.Refresh)
            state = MaterialsUiState(isLoading = false, materials = emptyList())
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_EMPTY).assertExists()
    }

    @Test
    fun materialsScreenSearchTabsAndScroll28ItemsAssertIsDisplayed() {
        val events = mutableListOf<MaterialsUiEvent>()
        val activeMaterials = List(28) { index ->
            ClinicalMaterial(
                id = MaterialId("mat-$index"),
                name = "Resina Compuesta $index",
                invimaRegistration = "INV-2026-$index",
                batchLotNumber = "LOT-$index",
                manufacturingDate = LocalDate.of(2025, 1, 10),
                expirationDate = LocalDate.of(2027, 6, 10),
                isActive = true
            )
        }
        val inactiveMaterials = listOf(
            ClinicalMaterial(
                id = MaterialId("mat-inact-0"),
                name = "Adhesivo Inactivo",
                invimaRegistration = "INV-OLD-0",
                batchLotNumber = "LOT-OLD-0",
                manufacturingDate = LocalDate.of(2024, 1, 10),
                expirationDate = LocalDate.of(2026, 12, 10),
                isActive = false
            )
        )
        var state by mutableStateOf(
            MaterialsUiState(
                materials = activeMaterials + inactiveMaterials,
                selectedTab = MaterialTabFilter.ACTIVE
            )
        )

        composeTestRule.setBiodontosContent {
            MaterialsScreen(
                state = state,
                onEvent = { events.add(it) },
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_LIST).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_LIST).performScrollToIndex(25)
        composeTestRule.onNodeWithTag(TestTags.Doctor.materialItem("mat-25")).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_LIST).performScrollToKey("mat-1")
        composeTestRule.onNodeWithTag(TestTags.Doctor.materialItem("mat-1")).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_SEARCH_FIELD)
            .performTextInput("Resina Compuesta 20")

        composeTestRule.runOnIdle {
            assertThat(events).contains(MaterialsUiEvent.SearchQueryChanged("Resina Compuesta 20"))
            state = state.copy(searchQuery = "Resina Compuesta 20")
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.materialItem("mat-20")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_SEARCH_CLEAR_BUTTON).performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_TAB_INACTIVE).performClick()

        composeTestRule.runOnIdle {
            assertThat(events).containsAtLeast(
                MaterialsUiEvent.SearchQueryChanged(""),
                MaterialsUiEvent.TabSelected(MaterialTabFilter.INACTIVE)
            )
            state = state.copy(searchQuery = "", selectedTab = MaterialTabFilter.INACTIVE)
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.materialItem("mat-inact-0")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.materialReactivateButton("mat-inact-0"))
            .performScrollTo()
            .performSemanticClick()

        composeTestRule.runOnIdle {
            assertThat(events).contains(MaterialsUiEvent.ReactivateMaterial(inactiveMaterials.first()))
        }
    }

    @Test
    fun materialsScreenDeactivateTwoStepDialogRequiresTwoConfirmations() {
        val events = mutableListOf<MaterialsUiEvent>()
        val material = ClinicalMaterial(
            id = MaterialId("mat-1"),
            name = "Resina Filtek Z350",
            invimaRegistration = "INVIMA 2020DM-001",
            batchLotNumber = "LOT-89",
            manufacturingDate = LocalDate.of(2025, 2, 1),
            expirationDate = LocalDate.of(2027, 2, 1),
            isActive = true
        )
        var state by mutableStateOf(MaterialsUiState(materials = listOf(material)))

        composeTestRule.setBiodontosContent {
            MaterialsScreen(
                state = state,
                onEvent = { events.add(it) },
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.materialDeactivateButton("mat-1"))
            .performScrollTo()
            .performSemanticClick()

        composeTestRule.runOnIdle {
            assertThat(events).contains(MaterialsUiEvent.RequestDeactivate(material))
            state = state.copy(materialToDeactivate = material)
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Desactivar Insumo Clínico").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continuar").performClick()

        composeTestRule.runOnIdle {
            assertThat(events).doesNotContain(MaterialsUiEvent.ConfirmDeactivate)
        }

        composeTestRule.onNodeWithText("¿Estás totalmente seguro?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmar Desactivación").performClick()

        composeTestRule.runOnIdle {
            assertThat(events).contains(MaterialsUiEvent.ConfirmDeactivate)
        }
    }

    @Test
    fun materialsFormBottomSheetValidationErrorsAndSaveWorkCorrectly() {
        val events = mutableListOf<MaterialsUiEvent>()
        var state by mutableStateOf(
            MaterialsUiState(
                isFormOpen = true,
                formState = MaterialFormState(
                    nameError = "El nombre comercial es obligatorio",
                    invimaError = "El registro INVIMA es obligatorio",
                    lotError = "El número de lote es obligatorio",
                    dateError = "La fecha de vencimiento debe ser posterior a la de elaboración"
                )
            )
        )

        composeTestRule.setBiodontosContent {
            MaterialsScreen(
                state = state,
                onEvent = { event ->
                    events.add(event)
                    when (event) {
                        is MaterialsUiEvent.FormNameChanged -> state = state.copy(
                            formState = state.formState.copy(name = event.name, nameError = null)
                        )
                        is MaterialsUiEvent.FormInvimaChanged -> state = state.copy(
                            formState = state.formState.copy(
                                invimaRegistration = event.invima,
                                invimaError = null
                            )
                        )
                        is MaterialsUiEvent.FormLotChanged -> state = state.copy(
                            formState = state.formState.copy(
                                batchLotNumber = event.lot,
                                lotError = null
                            )
                        )
                        else -> Unit
                    }
                },
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIAL_FORM_SHEET).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIAL_FORM_NAME_FIELD)
            .performScrollTo()
            .assertTextContains("El nombre comercial es obligatorio", substring = true)
        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIAL_FORM_INVIMA_FIELD)
            .performScrollTo()
            .assertTextContains("El registro INVIMA es obligatorio", substring = true)
        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIAL_FORM_LOT_FIELD)
            .performScrollTo()
            .assertTextContains("El número de lote es obligatorio", substring = true)
        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIAL_FORM_DATE_ERROR)
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            state = state.copy(formState = MaterialFormState())
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIAL_FORM_NAME_FIELD)
            .performScrollTo()
            .performTextInput("Ionómero Vítreo")
        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIAL_FORM_INVIMA_FIELD)
            .performScrollTo()
            .performTextInput("INVIMA-2026-99")
        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIAL_FORM_LOT_FIELD)
            .performScrollTo()
            .performTextInput("LOT-2026-A")

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIAL_FORM_SAVE_BUTTON)
            .performSemanticClick()
        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIAL_FORM_CLOSE_BUTTON)
            .performSemanticClick()

        composeTestRule.runOnIdle {
            assertThat(events).containsAtLeast(
                MaterialsUiEvent.FormNameChanged("Ionómero Vítreo"),
                MaterialsUiEvent.FormInvimaChanged("INVIMA-2026-99"),
                MaterialsUiEvent.FormLotChanged("LOT-2026-A"),
                MaterialsUiEvent.SaveMaterial,
                MaterialsUiEvent.DismissForm
            )
        }
    }

    @Test
    fun doctorDocumentsScreenKpiSearchFiltersScroll28ItemsAndButtonsWork() {
        val patients = List(28) { index ->
            Patient(
                id = "pat-$index",
                dniCedula = "1000$index",
                firstName = "Paciente",
                firstLastName = "Número$index",
                birthDate = "1992-03-10",
                consentSigned = index % 2 == 0,
                hasCriticalAllergies = index % 5 == 0,
                allergiesDescription = if (index % 5 == 0) "Penicilina" else "Ninguna"
            )
        }
        var searchQuery by mutableStateOf("")
        val selectedFilters = mutableListOf<DocumentFilter>()
        val detailNavigations = mutableListOf<Pair<String, String>>()
        val admissionNavigations = mutableListOf<String>()
        var refreshCount = 0

        composeTestRule.setBiodontosContent {
            DoctorDocumentsScreen(
                state = DoctorDocumentsState(
                    patients = patients,
                    searchQuery = searchQuery,
                    activeFilter = DocumentFilter.ALL,
                    isLoading = false
                ),
                onSearchQueryChange = { searchQuery = it },
                onFilterSelect = { selectedFilters.add(it) },
                onRefresh = { refreshCount++ },
                onNavigateBack = {},
                onNavigateToPatientDetail = { id, tab -> detailNavigations.add(id to tab) },
                onNavigateToAdmission = { admissionNavigations.add(it) }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.DOCS_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.DOCS_KPI_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.DOCS_LIST).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Doctor.DOCS_LIST).performScrollToIndex(25)
        composeTestRule.onNodeWithTag(TestTags.Doctor.documentItem("pat-25")).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Doctor.DOCS_LIST).performScrollToKey("pat-1")
        composeTestRule.onNodeWithTag(TestTags.Doctor.documentItem("pat-1")).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Doctor.documentHistoryButton("pat-1"))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Doctor.documentOdontogramButton("pat-1"))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Doctor.documentAdmissionButton("pat-1"))
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.documentFilterChip(DocumentFilter.CRITICAL_ALLERGIES.name))
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.DOCS_SEARCH_FIELD)
            .performTextInput("100025")
        composeTestRule.onNodeWithTag(TestTags.Doctor.DOCS_SEARCH_CLEAR_BUTTON)
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Doctor.DOCS_REFRESH_BUTTON)
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(detailNavigations).containsExactly(
                "pat-1" to "HISTORY",
                "pat-1" to "ODONTOGRAM"
            ).inOrder()
            assertThat(admissionNavigations).containsExactly("pat-1")
            assertThat(selectedFilters).containsExactly(DocumentFilter.CRITICAL_ALLERGIES)
            assertThat(searchQuery).isEmpty()
            assertThat(refreshCount).isEqualTo(1)
        }
    }

    @Test
    fun doctorProfileScreenEditFieldsSavePasswordAndLogoutWorkCorrectly() {
        val events = mutableListOf<DoctorProfileEvent>()
        val doctor = Doctor(
            id = DoctorId("doc-1"),
            firstName = "Carlos",
            lastName = "Mendoza",
            specialty = "Ortodoncia",
            licenseNumber = "OD-20381",
            email = "carlos@biodontos.com",
            phone = "3001234567",
            bio = "Especialista en estética dental",
            clinicAddress = "Calle 100 # 15-20",
            services = "Ortodoncia, Resinas"
        )
        var state by mutableStateOf(
            DoctorProfileState(
                doctor = doctor,
                firstName = doctor.firstName,
                lastName = doctor.lastName,
                specialty = doctor.specialty,
                licenseNumber = doctor.licenseNumber,
                phone = doctor.phone.orEmpty(),
                bio = doctor.bio.orEmpty(),
                clinicAddress = doctor.clinicAddress.orEmpty(),
                services = doctor.services.orEmpty(),
                email = doctor.email
            )
        )

        composeTestRule.setBiodontosContent {
            DoctorProfileContent(
                state = state,
                onEvent = { events.add(it) },
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_LIST)
            .performScrollToKey("professional_info")

        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_FIRST_NAME_FIELD)
            .performScrollTo()
            .performTextReplacement("Andrés")
        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_SAVE_BUTTON)
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_LIST)
            .performScrollToKey("security_card")

        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_NEW_PASSWORD_FIELD)
            .performScrollTo()
            .performTextInput("ClaveSegura123")
        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_CONFIRM_PASSWORD_FIELD)
            .performScrollTo()
            .performTextInput("ClaveSegura123")
        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_UPDATE_PASSWORD_BUTTON)
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_LOGOUT_BUTTON)
            .performScrollTo()
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(events).containsAtLeast(
                DoctorProfileEvent.UpdateFirstName("Andrés"),
                DoctorProfileEvent.SaveProfile,
                DoctorProfileEvent.UpdateNewPassword("ClaveSegura123"),
                DoctorProfileEvent.UpdateConfirmPassword("ClaveSegura123"),
                DoctorProfileEvent.SubmitPasswordChange,
                DoctorProfileEvent.RequestLogout
            )
            state = state.copy(showLogoutConfirmation = true)
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_LOGOUT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_LOGOUT_CONFIRM_BUTTON).performClick()

        composeTestRule.runOnIdle {
            assertThat(events).contains(DoctorProfileEvent.ConfirmLogout)
        }
    }

    @Test
    fun doctorProfileAsyncAvatarInspectionModeShowsInitialsWithoutSubcomposeAsyncImage() {
        composeTestRule.setBiodontosContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                DoctorAsyncAvatar(
                    photoUrl = "https://invalid.example.com/avatar.jpg",
                    firstName = "Carlos",
                    lastName = "Mendoza"
                )
            }
        }

        composeTestRule.onNodeWithText("CM").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun doctorControlsMouseHoverClickAndKeyboardTabShiftTabTraversalWork() {
        val materialEvents = mutableListOf<MaterialsUiEvent>()
        var showProfile by mutableStateOf(false)

        composeTestRule.setBiodontosContent {
            if (!showProfile) {
                MaterialsScreen(
                    state = MaterialsUiState(materials = emptyList()),
                    onEvent = { materialEvents.add(it) },
                    onNavigateBack = {}
                )
            } else {
                DoctorProfileContent(
                    state = DoctorProfileState(
                        doctor = Doctor(
                            id = DoctorId("doc-1"),
                            firstName = "Carlos",
                            lastName = "Mendoza",
                            specialty = "Ortodoncia"
                        ),
                        firstName = "Carlos",
                        lastName = "Mendoza",
                        specialty = "Ortodoncia"
                    ),
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_FAB_ADD)
            .performMouseInput {
                enter(center)
                exit(Offset(-1f, -1f))
                click()
            }

        composeTestRule.runOnIdle {
            assertThat(materialEvents).contains(MaterialsUiEvent.OpenCreateForm)
            showProfile = true
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_LIST)
            .performScrollToKey("professional_info")
        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_LAST_NAME_FIELD)
            .performScrollTo()
        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_FIRST_NAME_FIELD)
            .performScrollTo()
            .requestFocus()
            .assertIsFocused()
            .performKeyInput {
                pressKey(Key.Tab)
            }

        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_LAST_NAME_FIELD)
            .assertIsFocused()
            .performKeyInput {
                withKeyDown(Key.ShiftLeft) {
                    pressKey(Key.Tab)
                }
            }

        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_FIRST_NAME_FIELD)
            .assertIsFocused()
    }

    @Test
    fun doctorPrimaryButtonsAndFabMeetMinimum48DpHeight() {
        var showProfile by mutableStateOf(false)

        composeTestRule.setBiodontosContent {
            if (!showProfile) {
                MaterialsScreen(
                    state = MaterialsUiState(materials = emptyList()),
                    onEvent = {},
                    onNavigateBack = {}
                )
            } else {
                DoctorProfileContent(
                    state = DoctorProfileState(
                        doctor = Doctor(
                            id = DoctorId("doc-1"),
                            firstName = "Carlos",
                            lastName = "Mendoza",
                            specialty = "Ortodoncia"
                        ),
                        firstName = "Carlos",
                        lastName = "Mendoza",
                        specialty = "Ortodoncia"
                    ),
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.MATERIALS_FAB_ADD)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.runOnIdle {
            showProfile = true
        }

        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_LIST)
            .performScrollToKey("professional_info")
        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_SAVE_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_LIST)
            .performScrollToKey("security_card")
        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_UPDATE_PASSWORD_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag(TestTags.Doctor.PROFILE_LOGOUT_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }
}
