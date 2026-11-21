package com.example.biodontos.ui.state

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.biodontos.presentation.odontogram.OdontogramScreen
import com.example.biodontos.presentation.odontogram.OdontogramState
import com.example.biodontos.presentation.patients.PatientListScreen
import com.example.biodontos.presentation.patients.PatientListState
import com.example.biodontos.ui.setBiodontosContent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean

@MediumTest
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AnimationAndClockComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule(StandardTestDispatcher())

    @After
    fun restoreAutoAdvance() {
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun patientListScreen_indeterminateLoader_rendersWithPausedClockWithoutCancelling() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setBiodontosContent {
            PatientListScreen(
                state = PatientListState(isLoading = true),
                onPatientClick = {},
                onRetry = {}
            )
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        val beforeMs = composeTestRule.mainClock.currentTime
        composeTestRule.mainClock.advanceTimeBy(milliseconds = 120)

        assertThat(composeTestRule.mainClock.currentTime - beforeMs).isAtLeast(120L)
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun odontogramScreen_indeterminateLoader_rendersWithPausedClockWithoutCancelling() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setBiodontosContent {
            OdontogramScreen(
                state = OdontogramState(isLoading = true),
                onSurfaceClick = { _, _ -> },
                onCommentChange = {},
                onUpdateCondition = {},
                onDismissDialog = {},
                onBackClick = {}
            )
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(milliseconds = 120)

        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun animatedVisibilityAndFloatState_advanceDeterministicallyWithMainClock() {
        composeTestRule.mainClock.autoAdvance = false
        var showBanner by mutableStateOf(true)
        var bannerDisposed = false

        composeTestRule.setBiodontosContent {
            Crossfade(targetState = showBanner, label = "banner_crossfade") { visible ->
                if (visible) {
                    DisposableEffect(Unit) {
                        onDispose { bannerDisposed = true }
                    }
                    Text(
                        text = "Alerta clínica visible",
                        modifier = Modifier.testTag("anim_banner")
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(milliseconds = 320)
        composeTestRule.onNodeWithTag("anim_banner").assertIsDisplayed()

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.runOnUiThread { showBanner = false }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("anim_banner").assertDoesNotExist()
    }

    @Test
    fun asyncSynchronization_usesWaitUntilAndNodeExistenceWaiters() {
        val gate = CompletableDeferred<Unit>()
        val syncCompleted = AtomicBoolean(false)

        composeTestRule.setBiodontosContent {
            val scope = rememberCoroutineScope()
            var isSyncing by remember { mutableStateOf(false) }
            var isDone by remember { mutableStateOf(false) }

            Column {
                Button(
                    onClick = {
                        isSyncing = true
                        scope.launch {
                            gate.await()
                            syncCompleted.set(true)
                            isSyncing = false
                            isDone = true
                        }
                    },
                    modifier = Modifier.testTag("sync_trigger")
                ) {
                    Text("Sincronizar")
                }
                if (isSyncing) {
                    Text("Sincronizando...", modifier = Modifier.testTag("sync_loader"))
                }
                if (isDone) {
                    Text("Sincronización completa", modifier = Modifier.testTag("sync_done"))
                }
            }
        }

        composeTestRule.onNodeWithTag("sync_trigger").performClick()

        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasTestTag("sync_loader"),
            timeoutMillis = 2_000L
        )
        composeTestRule.onNodeWithTag("sync_done").assertDoesNotExist()

        gate.complete(Unit)

        composeTestRule.waitUntil(
            conditionDescription = "la sincronización en segundo plano debe finalizar",
            timeoutMillis = 2_000L
        ) {
            syncCompleted.get()
        }
        composeTestRule.waitUntilDoesNotExist(
            matcher = hasTestTag("sync_loader"),
            timeoutMillis = 2_000L
        )
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasTestTag("sync_done"),
            timeoutMillis = 2_000L
        )
        composeTestRule.onNodeWithTag("sync_done")
            .assertIsDisplayed()
            .assertTextEquals("Sincronización completa")
    }
}
