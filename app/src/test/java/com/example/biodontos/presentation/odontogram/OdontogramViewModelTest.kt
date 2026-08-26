package com.example.biodontos.presentation.odontogram

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.ToothSurface
import com.example.biodontos.domain.usecase.odontogram.GetOdontogramUseCase
import com.example.biodontos.domain.usecase.odontogram.UpdateToothUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class OdontogramViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getOdontogramUseCase = mockk<GetOdontogramUseCase>()
    private val updateToothUseCase = mockk<UpdateToothUseCase>()
    private val patientId = "patient123"

    @Test
    fun onSurfaceClickUpdatesStateWithSelection() {
        runTest {
            coEvery { getOdontogramUseCase(patientId) } returns Result.success(emptyList())
            val viewModel = OdontogramViewModel(patientId, getOdontogramUseCase, updateToothUseCase)

            viewModel.onSurfaceClick(11, ToothSurface.OCLUSAL)

            val state = viewModel.state.value
            assertEquals(11, state.selectedTooth)
            assertEquals(ToothSurface.OCLUSAL, state.selectedSurface)
            assertTrue(state.showSelectionDialog)
        }
    }

    @Test
    fun onCommentChangeUpdatesComment() {
        runTest {
            coEvery { getOdontogramUseCase(patientId) } returns Result.success(emptyList())
            val viewModel = OdontogramViewModel(patientId, getOdontogramUseCase, updateToothUseCase)

            viewModel.onCommentChange("Needs cleaning")

            assertEquals("Needs cleaning", viewModel.state.value.comment)
        }
    }
}
