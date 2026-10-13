package com.example.biodontos.presentation.patientportal.payments

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.TreatmentBalance

enum class PaymentFilter(val label: String) {
    ALL("Todos"),
    PENDING("Pendientes"),
    COMPLETED("Pagados")
}

@Immutable
data class PatientPaymentsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val patientName: String = "",
    val patientId: String = "",
    val totalPending: CopAmount = CopAmount(0),
    val totalPaid: CopAmount = CopAmount(0),
    val balances: List<TreatmentBalance> = emptyList(),
    val recentPayments: List<Payment> = emptyList(),
    val selectedFilter: PaymentFilter = PaymentFilter.ALL,
    val errorMessage: String? = null
) {
    val filteredBalances: List<TreatmentBalance>
        get() = when (selectedFilter) {
            PaymentFilter.ALL -> balances
            PaymentFilter.PENDING -> balances.filter { !it.isPaidInFull }
            PaymentFilter.COMPLETED -> balances.filter { it.isPaidInFull }
        }

    val hasPendingBalance: Boolean
        get() = totalPending.pesos > 0L
}

sealed interface PatientPaymentsUiEvent {
    data object Refresh : PatientPaymentsUiEvent
    data object Retry : PatientPaymentsUiEvent
    data class SelectFilter(val filter: PaymentFilter) : PatientPaymentsUiEvent
}

sealed interface PatientPaymentsUiEffect {
    data class ShowMessage(val message: String) : PatientPaymentsUiEffect
}
