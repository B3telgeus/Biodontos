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

    @Test
    fun setDentitionTypeSwitchesBetweenAdultAndPediatricTeeth() = runTest {
        coEvery { getOdontogramUseCase(patientId) } returns Result.success(emptyList())
        val viewModel = OdontogramViewModel(patientId, getOdontogramUseCase, updateToothUseCase)

        // Por defecto es ADULT
        assertEquals(com.example.biodontos.domain.model.DentitionType.ADULT, viewModel.state.value.dentitionType)
        assertTrue(viewModel.state.value.upperLeftQuadrant.contains(18))
        assertTrue(viewModel.state.value.upperLeftQuadrant.contains(11))

        // Cambiar a CHILD
        viewModel.setDentitionType(com.example.biodontos.domain.model.DentitionType.CHILD)
        assertEquals(com.example.biodontos.domain.model.DentitionType.CHILD, viewModel.state.value.dentitionType)
        assertEquals(listOf(55, 54, 53, 52, 51), viewModel.state.value.upperLeftQuadrant)
        assertEquals(listOf(85, 84, 83, 82, 81), viewModel.state.value.lowerLeftQuadrant)

        // Cambiar a MIXED
        viewModel.setDentitionType(com.example.biodontos.domain.model.DentitionType.MIXED)
        assertEquals(com.example.biodontos.domain.model.DentitionType.MIXED, viewModel.state.value.dentitionType)
    }
}
