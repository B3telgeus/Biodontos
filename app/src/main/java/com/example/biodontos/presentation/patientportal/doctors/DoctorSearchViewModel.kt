package com.example.biodontos.presentation.patientportal.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.usecase.doctor.SearchDoctorsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DoctorSearchViewModel(
    private val searchDoctorsUseCase: SearchDoctorsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorSearchUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<DoctorSearchUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var searchJob: Job? = null

    init {
        loadDoctors()
    }

    fun onEvent(event: DoctorSearchUiEvent) {
        when (event) {
            is DoctorSearchUiEvent.QueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                debounceSearch()
            }
            is DoctorSearchUiEvent.SpecialtySelected -> {
                _uiState.update { it.copy(selectedSpecialty = event.specialty) }
                loadDoctors()
            }
            is DoctorSearchUiEvent.DoctorSelected -> {
                viewModelScope.launch {
                    _effects.send(DoctorSearchUiEffect.NavigateToDetail(event.doctorId))
                }
            }
            is DoctorSearchUiEvent.BookDoctor -> {
                viewModelScope.launch {
                    _effects.send(DoctorSearchUiEffect.NavigateToBooking(event.doctorId))
                }
            }
            is DoctorSearchUiEvent.Retry -> {
                loadDoctors()
            }
        }
    }

    private fun debounceSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300L)
            loadDoctors()
        }
    }

    fun loadDoctors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val current = _uiState.value
            val result = searchDoctorsUseCase(
                query = current.searchQuery,
                specialty = current.selectedSpecialty.takeIf { it != "Todas" }
            )
            result.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            doctors = list,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudieron cargar los doctores"
                        )
                    }
                }
            )
        }
    }
}
