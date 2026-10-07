package com.example.biodontos.presentation.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.usecase.patients.SavePatientUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PatientRegistrationViewModel(
    private val savePatientUseCase: SavePatientUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PatientRegistrationState())
    val state: StateFlow<PatientRegistrationState> = _state.asStateFlow()

    private val _events = Channel<PatientRegistrationEvent>()
    val events = _events.receiveAsFlow()

    fun onDniChange(value: String) {
        val error = if (value.isNotEmpty() && !value.matches(Regex("^[0-9]*$"))) {
            "Solo se permiten números (carácter inválido)"
        } else null
        _state.update { it.copy(dniCedula = value, dniError = error) }
    }

    fun onFirstNameChange(value: String) {
        val error = if (value.isNotEmpty() && !value.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]*$"))) {
            "Solo se permiten letras y espacios (carácter inválido)"
        } else null
        _state.update { it.copy(firstName = value, firstNameError = error) }
    }

    fun onLastNameChange(value: String) {
        val error = if (value.isNotEmpty() && !value.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]*$"))) {
            "Solo se permiten letras y espacios (carácter inválido)"
        } else null
        _state.update { it.copy(lastName = value, lastNameError = error) }
    }
    fun onConsentChange(value: Boolean) { 
        _state.update { it.copy(consentSigned = value) } 
    }
    fun onAllergiesChange(value: Boolean) { 
        _state.update { 
            it.copy(
                hasCriticalAllergies = value,
                allergiesDescription = if (!value) "Ninguna" else it.allergiesDescription
            ) 
        } 
    }

    fun onAllergiesDescriptionChange(value: String) {
        _state.update { it.copy(allergiesDescription = value) }
    }

    fun onPhoneChange(value: String) {
        _state.update { it.copy(phone = value) }
    }

    fun savePatient() {
        val currentState = _state.value
        
        var hasError = false
        if (currentState.dniCedula.isBlank()) {
            _state.update { it.copy(dniError = "El DNI es obligatorio") }
            hasError = true
        }
        if (currentState.firstName.isBlank()) {
            _state.update { it.copy(firstNameError = "El nombre es obligatorio") }
            hasError = true
        }
        if (currentState.lastName.isBlank()) {
            _state.update { it.copy(lastNameError = "Los apellidos son obligatorios") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val allergyDesc = if (currentState.hasCriticalAllergies) {
                currentState.allergiesDescription.ifBlank { "Alergias presentes" }
            } else {
                "Ninguna"
            }
            val patient = Patient(
                id = "",
                dniCedula = currentState.dniCedula,
                firstName = currentState.firstName,
                lastName = currentState.lastName,
                phone = currentState.phone.ifBlank { null },
                consentSigned = currentState.consentSigned,
                hasCriticalAllergies = currentState.hasCriticalAllergies,
                allergiesDescription = allergyDesc
            )
            savePatientUseCase(patient)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, isSaved = true) }
                    _events.send(PatientRegistrationEvent.SavedSuccessfully)
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al guardar"
                        )
                    }
                }
        }
    }
}
