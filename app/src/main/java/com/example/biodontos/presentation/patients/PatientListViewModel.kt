package com.example.biodontos.presentation.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.usecase.patients.GetPatientsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PatientListViewModel(
    private val getPatientsUseCase: GetPatientsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PatientListState())
    val state: StateFlow<PatientListState> = _state.asStateFlow()

    init {
        loadPatients()
    }

    fun loadPatients() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getPatientsUseCase()
                .onSuccess { patients ->
                    _state.update { 
                        it.copy(
                            patients = patients,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error desconocido"
                        )
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            getPatientsUseCase()
                .onSuccess { patients ->
                    _state.update { 
                        it.copy(
                            patients = patients,
                            isRefreshing = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isRefreshing = false,
                            error = error.message ?: "Error al actualizar pacientes"
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onFilterSelected(filter: PatientQuickFilter) {
        _state.update { it.copy(selectedFilter = filter) }
    }
}
