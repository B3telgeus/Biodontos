package com.example.biodontos.presentation.doctor.profile

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.biodontos.domain.usecase.auth.LogoutUseCase
import com.example.biodontos.domain.usecase.auth.ResetPasswordUseCase
import com.example.biodontos.domain.usecase.auth.UpdatePasswordUseCase
import com.example.biodontos.domain.usecase.doctor.UpdateDoctorProfileUseCase
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentSessionUseCase: GetCurrentSessionUseCase = mockk()
    private val doctorRepository: DoctorRepository = mockk()
    private val updateDoctorProfileUseCase: UpdateDoctorProfileUseCase = mockk()
    private val updatePasswordUseCase: UpdatePasswordUseCase = mockk()
    private val resetPasswordUseCase: ResetPasswordUseCase = mockk()
    private val logoutUseCase: LogoutUseCase = mockk(relaxed = true)

    private val dummyDoctorSession = UserSession(
        id = "doc-uid-1",
        email = "dr.carlos@biodontos.com",
        role = UserRole.DOCTOR,
        displayName = "Carlos Mendoza"
    )

    private val dummyDoctor = Doctor(
        id = DoctorId("doc-uid-1"),
        userId = "doc-uid-1",
        firstName = "Carlos",
        lastName = "Mendoza",
        specialty = "Ortodoncia & Estética",
        email = "dr.carlos@biodontos.com",
        licenseNumber = "OD-20381",
        phone = "3001234567",
        bio = "Especialista en ortodoncia con 10 años de experiencia.",
        photoUrl = "data:image/jpeg;base64,samplephoto"
    )

    @Before
    fun setup() {
        coEvery { getCurrentSessionUseCase() } returns dummyDoctorSession
        coEvery { doctorRepository.getDoctorByUserId("doc-uid-1") } returns Result.success(dummyDoctor)
    }

    private fun createViewModel(): DoctorProfileViewModel {
        return DoctorProfileViewModel(
            getCurrentSessionUseCase = getCurrentSessionUseCase,
            doctorRepository = doctorRepository,
            updateDoctorProfileUseCase = updateDoctorProfileUseCase,
            updatePasswordUseCase = updatePasswordUseCase,
            resetPasswordUseCase = resetPasswordUseCase,
            logoutUseCase = logoutUseCase
        )
    }

    @Test
    fun `initialization loads doctor profile and populates form fields`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.doctor)
        assertEquals("Carlos", state.firstName)
        assertEquals("Mendoza", state.lastName)
        assertEquals("Ortodoncia & Estética", state.specialty)
        assertEquals("OD-20381", state.licenseNumber)
        assertEquals("3001234567", state.phone)
        assertEquals("Especialista en ortodoncia con 10 años de experiencia.", state.bio)
        assertEquals("data:image/jpeg;base64,samplephoto", state.photoUrl)
        assertEquals("dr.carlos@biodontos.com", state.email)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `saveProfile with valid fields updates doctor and emits success toast`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val updatedDoctor = dummyDoctor.copy(
            firstName = "Carlos Alberto",
            specialty = "Ortodoncia Avanzada"
        )
        coEvery { updateDoctorProfileUseCase(any()) } returns Result.success(updatedDoctor)

        viewModel.onEvent(DoctorProfileEvent.UpdateFirstName("Carlos Alberto"))
        viewModel.onEvent(DoctorProfileEvent.UpdateSpecialty("Ortodoncia Avanzada"))
        viewModel.onEvent(DoctorProfileEvent.SaveProfile)
        advanceUntilIdle()

        coVerify {
            updateDoctorProfileUseCase(
                match {
                    it.firstName == "Carlos Alberto" && it.specialty == "Ortodoncia Avanzada"
                }
            )
        }
        val state = viewModel.state.value
        assertEquals("Carlos Alberto", state.doctor?.firstName)
        assertEquals("Ortodoncia Avanzada", state.doctor?.specialty)
        assertFalse(state.isSavingProfile)

        val effect = viewModel.effects.first()
        assertTrue(effect is DoctorProfileEffect.ShowToast)
        assertEquals("Perfil profesional actualizado con éxito", (effect as DoctorProfileEffect.ShowToast).message)
    }

    @Test
    fun `saveProfile with blank firstName sets error and does not call usecase`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DoctorProfileEvent.UpdateFirstName("   "))
        viewModel.onEvent(DoctorProfileEvent.SaveProfile)
        advanceUntilIdle()

        coVerify(exactly = 0) { updateDoctorProfileUseCase(any()) }
        val state = viewModel.state.value
        assertEquals("El nombre es obligatorio", state.errorMessage)
    }

    @Test
    fun `updatePhotoUrl persists photo immediately via UpdateDoctorProfileUseCase`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val newPhoto = "data:image/jpeg;base64,newphotodata"
        val updatedDoctor = dummyDoctor.copy(photoUrl = newPhoto)
        coEvery { updateDoctorProfileUseCase(any()) } returns Result.success(updatedDoctor)

        viewModel.onEvent(DoctorProfileEvent.UpdatePhotoUrl(newPhoto))
        advanceUntilIdle()

        coVerify {
            updateDoctorProfileUseCase(
                match { it.photoUrl == newPhoto }
            )
        }
        val state = viewModel.state.value
        assertEquals(newPhoto, state.photoUrl)
        assertEquals(newPhoto, state.doctor?.photoUrl)

        val effect = viewModel.effects.first()
        assertTrue(effect is DoctorProfileEffect.ShowToast)
        assertEquals("Foto de perfil actualizada con éxito", (effect as DoctorProfileEffect.ShowToast).message)
    }

    @Test
    fun `submitPasswordChange with matching passwords updates password and clears fields`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { updatePasswordUseCase("nuevaClave123") } returns Result.success(Unit)

        viewModel.onEvent(DoctorProfileEvent.UpdateNewPassword("nuevaClave123"))
        viewModel.onEvent(DoctorProfileEvent.UpdateConfirmPassword("nuevaClave123"))
        viewModel.onEvent(DoctorProfileEvent.SubmitPasswordChange)
        advanceUntilIdle()

        coVerify { updatePasswordUseCase("nuevaClave123") }
        val state = viewModel.state.value
        assertEquals("", state.newPassword)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isUpdatingPassword)

        val effect = viewModel.effects.first()
        assertTrue(effect is DoctorProfileEffect.ShowToast)
        assertEquals("Contraseña actualizada exitosamente", (effect as DoctorProfileEffect.ShowToast).message)
    }

    @Test
    fun `submitPasswordChange with mismatching passwords emits error and does not call usecase`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DoctorProfileEvent.UpdateNewPassword("clave123"))
        viewModel.onEvent(DoctorProfileEvent.UpdateConfirmPassword("clave999"))
        viewModel.onEvent(DoctorProfileEvent.SubmitPasswordChange)
        advanceUntilIdle()

        coVerify(exactly = 0) { updatePasswordUseCase(any()) }
        val state = viewModel.state.value
        assertEquals("Las contraseñas no coinciden", state.errorMessage)
    }

    @Test
    fun `sendPasswordResetEmail calls ResetPasswordUseCase with doctor email`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { resetPasswordUseCase("dr.carlos@biodontos.com") } returns Result.success(Unit)

        viewModel.onEvent(DoctorProfileEvent.SendPasswordResetEmail)
        advanceUntilIdle()

        coVerify { resetPasswordUseCase("dr.carlos@biodontos.com") }
        val effect = viewModel.effects.first()
        assertTrue(effect is DoctorProfileEffect.ShowToast)
        assertTrue((effect as DoctorProfileEffect.ShowToast).message.contains("dr.carlos@biodontos.com"))
    }

    @Test
    fun `confirmLogout invokes LogoutUseCase and emits NavigateToLogin effect`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DoctorProfileEvent.ConfirmLogout)
        advanceUntilIdle()

        coVerify { logoutUseCase() }
        val effect = viewModel.effects.first()
        assertEquals(DoctorProfileEffect.NavigateToLogin, effect)
    }
}
