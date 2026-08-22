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
        _state.update { it.copy(dniCedula = value, dniError = null) } 
    }
    fun onFirstNameChange(value: String) { 
        _state.update { it.copy(firstName = value, firstNameError = null) } 
    }
    fun onLastNameChange(value: String) { 
        _state.update { it.copy(lastName = value, lastNameError = null) } 
    }
    fun onConsentChange(value: Boolean) { 
        _state.update { it.copy(consentSigned = value) } 
    }
    fun onAllergiesChange(value: Boolean) { 
        _state.update { it.copy(hasCriticalAllergies = value) } 
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
            val patient = Patient(
                id = "",
                dniCedula = currentState.dniCedula,
                firstName = currentState.firstName,
                lastName = currentState.lastName,
                consentSigned = currentState.consentSigned,
                hasCriticalAllergies = currentState.hasCriticalAllergies
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
