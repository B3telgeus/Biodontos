package com.example.biodontos

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityScenarioTest {

    @Test
    fun mainActivityLaunchesRecreatesAndTearsDownSuccessfully() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                assertThat(activity).isNotNull()
                assertThat(activity.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            }

            scenario.recreate()
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                assertThat(activity).isNotNull()
                assertThat(activity.isFinishing).isFalse()
                assertThat(activity.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            }
        }
    }
}
