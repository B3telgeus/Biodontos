package com.example.biodontos.ui.clinical

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.biodontos.core.designsystem.TestTags
import com.example.biodontos.domain.model.DentitionType
import com.example.biodontos.domain.model.PeriodontalSiteLocation
import com.example.biodontos.domain.model.PeriodontalSiteMeasurement
import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface
import com.example.biodontos.presentation.components.SignaturePad
import com.example.biodontos.presentation.odontogram.InteractiveTooth
import com.example.biodontos.presentation.odontogram.OdontogramScreen
import com.example.biodontos.presentation.odontogram.OdontogramState
import com.example.biodontos.presentation.periodontogram.InteractivePeriodontogram
import com.example.biodontos.ui.setBiodontosContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@MediumTest
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class OdontogramPeriodontogramSignatureComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun odontogramLoadingDentitionSwitchAndErrorSnackbarRenderProperly() {
        var state by mutableStateOf(OdontogramState(isLoading = true))

        composeTestRule.setBiodontosContent {
            OdontogramScreen(
                state = state,
                onDentitionTypeChange = { state = state.copy(dentitionType = it) },
                onSurfaceClick = { _, _ -> },
                onCommentChange = {},
                onUpdateCondition = {},
                onDismissDialog = {},
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Odontogram.ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Odontogram.LOADING).assertIsDisplayed()

        composeTestRule.runOnIdle {
            state = OdontogramState(isLoading = false, dentitionType = DentitionType.ADULT)
        }

        composeTestRule.onNodeWithTag(TestTags.Odontogram.tooth(18)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Odontogram.tooth(11)).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.Odontogram.dentitionOption(DentitionType.CHILD.name))
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Odontogram.tooth(55)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Odontogram.tooth(18)).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TestTags.Odontogram.dentitionOption(DentitionType.MIXED.name))
            .performClick()
        composeTestRule.onNodeWithTag(TestTags.Odontogram.tooth(16)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Odontogram.tooth(55)).assertIsDisplayed()

        composeTestRule.runOnIdle {
            state = state.copy(error = "Error al guardar odontograma")
        }
        composeTestRule.onNodeWithTag(TestTags.Odontogram.ERROR_SNACKBAR).assertIsDisplayed()
    }

    @Test
    fun odontogramTapOnToothCenterOpensDialogAndSelectsConditionAndComment() {
        var state by mutableStateOf(OdontogramState())
        var clickedTooth by mutableStateOf<Int?>(null)
        var clickedSurface by mutableStateOf<ToothSurface?>(null)
        var updatedCondition by mutableStateOf<ToothCondition?>(null)

        composeTestRule.setBiodontosContent {
            OdontogramScreen(
                state = state,
                onDentitionTypeChange = { state = state.copy(dentitionType = it) },
                onSurfaceClick = { tooth, surface ->
                    clickedTooth = tooth
                    clickedSurface = surface
                    state = state.copy(
                        selectedTooth = tooth,
                        selectedSurface = surface,
                        showSelectionDialog = true
                    )
                },
                onCommentChange = { state = state.copy(comment = it) },
                onUpdateCondition = { cond ->
                    updatedCondition = cond
                    state = state.copy(showSelectionDialog = false)
                },
                onDismissDialog = { state = state.copy(showSelectionDialog = false) },
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.Odontogram.tooth(16))
            .assertIsDisplayed()
            .performTouchInput { click(center) }

        composeTestRule.runOnIdle {
            assertThat(clickedTooth).isEqualTo(16)
            assertThat(clickedSurface).isEqualTo(ToothSurface.OCLUSAL)
        }

        composeTestRule.onNodeWithTag(TestTags.Odontogram.SELECTION_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Odontogram.DIALOG_COMMENT_FIELD)
            .performTextInput("Caries oclusal incipiente")
        composeTestRule.onNodeWithTag(TestTags.Odontogram.conditionOption(ToothCondition.CARIES.name))
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(updatedCondition).isEqualTo(ToothCondition.CARIES)
            assertThat(state.comment).isEqualTo("Caries oclusal incipiente")
        }
        composeTestRule.onNodeWithTag(TestTags.Odontogram.SELECTION_DIALOG).assertDoesNotExist()
    }

    @Test
    fun interactiveToothDetectsSurfacesAndMeetsMinimumBounds() {
        val detectedSurfaces = mutableListOf<ToothSurface>()

        composeTestRule.setBiodontosContent {
            InteractiveTooth(
                toothNumber = 16,
                surfaceStates = emptyMap(),
                onSurfaceClicked = { detectedSurfaces.add(it) }
            )
        }

        val toothNode = composeTestRule.onNodeWithTag(TestTags.Odontogram.tooth(16))
        toothNode
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        val bounds = toothNode.getUnclippedBoundsInRoot()
        bounds.width.assertIsEqualTo(70.dp, "InteractiveTooth default width")
        bounds.height.assertIsEqualTo(70.dp, "InteractiveTooth default height")

        toothNode.performTouchInput { click(center) }
        toothNode.performTouchInput { click(Offset(center.x, height * 0.1f)) }

        composeTestRule.runOnIdle {
            assertThat(detectedSurfaces).containsExactly(
                ToothSurface.OCLUSAL,
                ToothSurface.VESTIBULAR
            ).inOrder()
        }
    }

    @Test
    fun interactivePeriodontogramScrollsRowSelectsToothAndUpdatesMobilityFurcationAndBop() {
        val adultTeeth = listOf(
            18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28,
            48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38
        )
        var selectedTooth by mutableStateOf(18)
        var siteMeasurements by mutableStateOf<Map<Pair<Int, PeriodontalSiteLocation>, PeriodontalSiteMeasurement>>(
            emptyMap()
        )

        composeTestRule.setBiodontosContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                InteractivePeriodontogram(
                    selectedToothNumber = selectedTooth,
                    availableTeeth = adultTeeth,
                    siteMeasurements = siteMeasurements,
                    onToothSelected = { selectedTooth = it },
                    onMeasurementChanged = { meas ->
                        siteMeasurements = siteMeasurements.toMutableMap().apply {
                            put(meas.toothNumber to meas.siteLocation, meas)
                        }
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.Periodontogram.ROOT)
            .assertIsDisplayed()
            .assertWidthIsAtLeast(240.dp)
            .assertHeightIsAtLeast(200.dp)

        composeTestRule.onNodeWithTag(TestTags.Periodontogram.TEETH_ROW)
            .performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithTag(TestTags.Periodontogram.TEETH_ROW)
            .performScrollToKey(36)

        composeTestRule.onNodeWithTag(TestTags.Periodontogram.toothChip(36))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(selectedTooth).isEqualTo(36)
        }

        composeTestRule.onNodeWithTag(TestTags.Periodontogram.SUMMARY_BANNER).assertIsDisplayed()

        composeTestRule.onNodeWithTag(
            TestTags.Periodontogram.bopToggle(PeriodontalSiteLocation.MESIO_VESTIBULAR.name)
        )
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Periodontogram.mobilityGrade(2))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Periodontogram.furcationGrade(3))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            val mv = checkNotNull(siteMeasurements[36 to PeriodontalSiteLocation.MESIO_VESTIBULAR])
            assertThat(mv.bleedingOnProbing).isTrue()
            assertThat(mv.mobilityGrade).isEqualTo(2)
            assertThat(mv.furcationGrade).isEqualTo(3)
        }
    }

    @Test
    fun signaturePadCapturesStrokeConfirmsClearsAndMeetsBounds() {
        var confirmedSignature by mutableStateOf("")

        composeTestRule.setBiodontosContent {
            SignaturePad(
                onSignatureConfirmed = { confirmedSignature = it }
            )
        }

        val rootNode = composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_ROOT)
        val canvasNode = composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_CANVAS)

        rootNode
            .assertIsDisplayed()
            .assertWidthIsAtLeast(200.dp)
            .assertHeightIsAtLeast(150.dp)

        canvasNode
            .assertIsDisplayed()
            .assertWidthIsAtLeast(150.dp)
            .assertHeightIsAtLeast(120.dp)

        val rootBounds = rootNode.getUnclippedBoundsInRoot()
        val canvasBounds = canvasNode.getUnclippedBoundsInRoot()
        canvasBounds.height.assertIsEqualTo(140.dp, "SignaturePad canvas height")
        assertThat(rootBounds.height.value).isGreaterThan(canvasBounds.height.value)

        composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_CONFIRM_BUTTON)
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_CONFIRMED_BADGE)
            .assertDoesNotExist()

        canvasNode.performTouchInput {
            down(topLeft + Offset(10f, 10f))
            moveTo(center)
            moveTo(bottomRight - Offset(10f, 10f))
            up()
        }

        composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_CONFIRM_BUTTON)
            .assertIsEnabled()
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_CONFIRMED_BADGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_CONFIRM_BUTTON)
            .assertIsNotEnabled()

        composeTestRule.runOnIdle {
            assertThat(confirmedSignature).startsWith("sig_vector_")
        }

        composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_CLEAR_BUTTON)
            .performClick()

        composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_CONFIRMED_BADGE)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.Components.SIGNATURE_PAD_CONFIRM_BUTTON)
            .assertIsNotEnabled()

        composeTestRule.runOnIdle {
            assertThat(confirmedSignature).isEmpty()
        }
    }
}
