package com.example.biodontos.presentation.doctor.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.usecase.patients.GetPatientsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DoctorDocumentsViewModel(
    private val getPatientsUseCase: GetPatientsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorDocumentsState(isLoading = true))
    val uiState: StateFlow<DoctorDocumentsState> = _uiState.asStateFlow()

    private val _effects = Channel<DoctorDocumentsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadDocuments()
    }

    fun onEvent(event: DoctorDocumentsUiEvent) {
        when (event) {
            is DoctorDocumentsUiEvent.SearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is DoctorDocumentsUiEvent.FilterSelected -> {
                _uiState.update { it.copy(activeFilter = event.filter) }
            }
            is DoctorDocumentsUiEvent.Refresh -> {
                loadDocuments()
            }
        }
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getPatientsUseCase()
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            patients = list,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "No se pudieron cargar los expedientes clínicos."
                        )
                    }
                }
        }
    }
}
