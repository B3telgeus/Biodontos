package com.example.biodontos.presentation.doctor.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.biodontos.domain.usecase.auth.LogoutUseCase
import com.example.biodontos.domain.usecase.auth.ResetPasswordUseCase
import com.example.biodontos.domain.usecase.auth.UpdatePasswordUseCase
import com.example.biodontos.domain.usecase.doctor.UpdateDoctorProfileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DoctorProfileViewModel(
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val doctorRepository: DoctorRepository,
    private val updateDoctorProfileUseCase: UpdateDoctorProfileUseCase,
    private val updatePasswordUseCase: UpdatePasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DoctorProfileState())
    val state = _state.asStateFlow()

    private val _effects = Channel<DoctorProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadProfile()
    }

    fun onEvent(event: DoctorProfileEvent) {
        when (event) {
            DoctorProfileEvent.LoadProfile -> loadProfile()
            is DoctorProfileEvent.UpdateFirstName -> _state.update { it.copy(firstName = event.value, errorMessage = null) }
            is DoctorProfileEvent.UpdateLastName -> _state.update { it.copy(lastName = event.value, errorMessage = null) }
            is DoctorProfileEvent.UpdateSpecialty -> _state.update { it.copy(specialty = event.value, errorMessage = null) }
            is DoctorProfileEvent.UpdateLicenseNumber -> _state.update { it.copy(licenseNumber = event.value, errorMessage = null) }
            is DoctorProfileEvent.UpdatePhone -> _state.update { it.copy(phone = event.value, errorMessage = null) }
            is DoctorProfileEvent.UpdateBio -> _state.update { it.copy(bio = event.value, errorMessage = null) }
            is DoctorProfileEvent.UpdatePhotoUrl -> updatePhoto(event.photoUrl)
            DoctorProfileEvent.SaveProfile -> saveProfile()
            is DoctorProfileEvent.UpdateNewPassword -> _state.update { it.copy(newPassword = event.value, errorMessage = null) }
            is DoctorProfileEvent.UpdateConfirmPassword -> _state.update { it.copy(confirmPassword = event.value, errorMessage = null) }
            DoctorProfileEvent.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            DoctorProfileEvent.ToggleConfirmPasswordVisibility -> _state.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            DoctorProfileEvent.SubmitPasswordChange -> submitPasswordChange()
            DoctorProfileEvent.SendPasswordResetEmail -> sendPasswordResetEmail()
            DoctorProfileEvent.RequestLogout -> _state.update { it.copy(showLogoutConfirmation = true) }
            DoctorProfileEvent.DismissLogoutDialog -> _state.update { it.copy(showLogoutConfirmation = false) }
            DoctorProfileEvent.ConfirmLogout -> confirmLogout()
            DoctorProfileEvent.DismissError -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val session = getCurrentSessionUseCase()
            if (session == null) {
                _effects.send(DoctorProfileEffect.NavigateToLogin)
                return@launch
            }

            val doctorResult = doctorRepository.getDoctorByUserId(session.id)
            val doctor = doctorResult.getOrNull() ?: Doctor(
                id = DoctorId(session.id),
                userId = session.id,
                firstName = session.displayName.split(" ").firstOrNull() ?: "Doctor",
                lastName = session.displayName.split(" ").drop(1).joinToString(" ").ifEmpty { "Especialista" },
                specialty = "Odontología Integral",
                email = session.email
            )

            _state.update {
                it.copy(
                    doctor = doctor,
                    firstName = doctor.firstName,
                    lastName = doctor.lastName,
                    specialty = doctor.specialty,
                    licenseNumber = doctor.licenseNumber,
                    phone = doctor.phone.orEmpty(),
                    bio = doctor.bio.orEmpty(),
                    photoUrl = doctor.photoUrl,
                    email = doctor.email,
                    isLoading = false
                )
            }
        }
    }

    private fun updatePhoto(photoUrl: String) {
        viewModelScope.launch {
            _state.update { it.copy(photoUrl = photoUrl, isSavingProfile = true, errorMessage = null) }
            val currentDoctor = _state.value.doctor
            if (currentDoctor == null) {
                _state.update { it.copy(isSavingProfile = false, errorMessage = "No se ha cargado el doctor") }
                return@launch
            }

            val updatedDoctor = currentDoctor.copy(photoUrl = photoUrl)
            val result = updateDoctorProfileUseCase(updatedDoctor)
            result.onSuccess { savedDoctor ->
                _state.update {
                    it.copy(
                        doctor = savedDoctor,
                        photoUrl = savedDoctor.photoUrl,
                        isSavingProfile = false
                    )
                }
                _effects.send(DoctorProfileEffect.ShowToast("Foto de perfil actualizada con éxito"))
            }.onFailure { ex ->
                _state.update {
                    it.copy(
                        isSavingProfile = false,
                        errorMessage = "Error al guardar foto: ${ex.toUserFriendlyMessage()}"
                    )
                }
            }
        }
    }

    private fun saveProfile() {
        val currentState = _state.value
        val currentDoctor = currentState.doctor
        if (currentDoctor == null) {
            _state.update { it.copy(errorMessage = "No se ha cargado el doctor") }
            return
        }

        if (currentState.firstName.isBlank()) {
            _state.update { it.copy(errorMessage = "El nombre es obligatorio") }
            return
        }
        if (currentState.lastName.isBlank()) {
            _state.update { it.copy(errorMessage = "El apellido es obligatorio") }
            return
        }
        if (currentState.specialty.isBlank()) {
            _state.update { it.copy(errorMessage = "La especialidad es obligatoria") }
            return
        }
        if (currentState.licenseNumber.isBlank()) {
            _state.update { it.copy(errorMessage = "La tarjeta o cédula profesional es obligatoria") }
            return
        }

        val updatedDoctor = currentDoctor.copy(
            firstName = currentState.firstName.trim(),
            lastName = currentState.lastName.trim(),
            specialty = currentState.specialty.trim(),
            licenseNumber = currentState.licenseNumber.trim(),
            phone = currentState.phone.trim().ifEmpty { null },
            bio = currentState.bio.trim().ifEmpty { null }
        )

        viewModelScope.launch {
            _state.update { it.copy(isSavingProfile = true, errorMessage = null, successMessage = null) }
            val result = updateDoctorProfileUseCase(updatedDoctor)
            result.onSuccess { savedDoctor ->
                _state.update {
                    it.copy(
                        doctor = savedDoctor,
                        isSavingProfile = false,
                        successMessage = "Perfil profesional actualizado con éxito"
                    )
                }
                _effects.send(DoctorProfileEffect.ShowToast("Perfil profesional actualizado con éxito"))
            }.onFailure { ex ->
                _state.update {
                    it.copy(
                        isSavingProfile = false,
                        errorMessage = "Error al actualizar perfil: ${ex.toUserFriendlyMessage()}"
                    )
                }
            }
        }
    }

    private fun submitPasswordChange() {
        val currentState = _state.value
        val newPassword = currentState.newPassword
        val confirmPassword = currentState.confirmPassword

        if (newPassword.isBlank()) {
            _state.update { it.copy(errorMessage = "La contraseña no puede estar vacía") }
            return
        }
        if (newPassword.length < 6) {
            _state.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres") }
            return
        }
        if (newPassword != confirmPassword) {
            _state.update { it.copy(errorMessage = "Las contraseñas no coinciden") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isUpdatingPassword = true, errorMessage = null, successMessage = null) }
            val result = updatePasswordUseCase(newPassword)
            result.onSuccess {
                _state.update {
                    it.copy(
                        isUpdatingPassword = false,
                        newPassword = "",
                        confirmPassword = "",
                        successMessage = "Contraseña actualizada exitosamente"
                    )
                }
                _effects.send(DoctorProfileEffect.ShowToast("Contraseña actualizada exitosamente"))
            }.onFailure { ex ->
                _state.update {
                    it.copy(
                        isUpdatingPassword = false,
                        errorMessage = "Error al actualizar contraseña: ${ex.toUserFriendlyMessage()}"
                    )
                }
            }
        }
    }

    private fun sendPasswordResetEmail() {
        val email = _state.value.email.ifBlank { _state.value.doctor?.email.orEmpty() }
        if (email.isBlank()) {
            _state.update { it.copy(errorMessage = "El correo electrónico no está configurado") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSendingResetEmail = true, errorMessage = null) }
            val result = resetPasswordUseCase(email)
            result.onSuccess {
                _state.update { it.copy(isSendingResetEmail = false) }
                _effects.send(DoctorProfileEffect.ShowToast("Enlace de recuperación enviado a $email"))
            }.onFailure { ex ->
                _state.update {
                    it.copy(
                        isSendingResetEmail = false,
                        errorMessage = "Error al enviar correo: ${ex.toUserFriendlyMessage()}"
                    )
                }
            }
        }
    }

    private fun confirmLogout() {
        viewModelScope.launch {
            _state.update { it.copy(showLogoutConfirmation = false) }
            logoutUseCase()
            _effects.send(DoctorProfileEffect.NavigateToLogin)
        }
    }
}
