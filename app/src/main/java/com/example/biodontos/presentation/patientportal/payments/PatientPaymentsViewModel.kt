package com.example.biodontos.presentation.patientportal.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.usecase.billing.GetPaymentSummaryUseCase
import com.example.biodontos.domain.usecase.patient.GetCurrentPatientUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PatientPaymentsViewModel(
    private val getCurrentPatientUseCase: GetCurrentPatientUseCase,
    private val getPaymentSummaryUseCase: GetPaymentSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientPaymentsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<PatientPaymentsUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadPayments()
    }

    fun onEvent(event: PatientPaymentsUiEvent) {
        when (event) {
            is PatientPaymentsUiEvent.Refresh -> {
                loadPayments(isRefresh = true)
            }
            is PatientPaymentsUiEvent.Retry -> {
                loadPayments(isRefresh = false)
            }
            is PatientPaymentsUiEvent.SelectFilter -> {
                _uiState.update { it.copy(selectedFilter = event.filter) }
            }
        }
    }

    fun loadPayments(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val patientResult = getCurrentPatientUseCase()
            val patient = patientResult.getOrNull()
            val patientId = patient?.id?.takeIf { it.isNotBlank() }

            if (patientId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "No se pudo identificar la sesión del paciente"
                    )
                }
                return@launch
            }

            val summaryResult = getPaymentSummaryUseCase(patientId)
            summaryResult.fold(
                onSuccess = { summary ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            patientName = patient.fullName,
                            patientId = patientId,
                            totalPending = summary.totalPending,
                            totalPaid = summary.totalPaid,
                            balances = summary.balances,
                            recentPayments = summary.recentPayments,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.localizedMessage ?: "Error al cargar la información de pagos"
                        )
                    }
                    _effects.send(PatientPaymentsUiEffect.ShowMessage("No pudimos actualizar tus pagos. Intenta de nuevo más tarde."))
                }
            )
        }
    }
}
