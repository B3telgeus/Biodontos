package com.example.biodontos.presentation.patientportal.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.usecase.doctor.GetDoctorDetailUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DoctorDetailViewModel(
    private val doctorId: String,
    private val getDoctorDetailUseCase: GetDoctorDetailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorDetailUiState(doctorId = doctorId, isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<DoctorDetailUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadDetail()
    }

    fun onEvent(event: DoctorDetailUiEvent) {
        when (event) {
            is DoctorDetailUiEvent.BookAppointment -> {
                viewModelScope.launch {
                    _effects.send(DoctorDetailUiEffect.NavigateToBooking(doctorId))
                }
            }
            is DoctorDetailUiEvent.Retry -> {
                loadDetail()
            }
        }
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = getDoctorDetailUseCase(doctorId)
            result.fold(
                onSuccess = { detail ->
                    _uiState.update {
                        it.copy(
                            doctorDetail = detail,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudo cargar la información del especialista"
                        )
                    }
                }
            )
        }
    }
}
