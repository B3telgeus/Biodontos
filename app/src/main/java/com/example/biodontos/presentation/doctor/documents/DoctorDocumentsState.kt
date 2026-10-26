package com.example.biodontos.presentation.doctor.documents

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.Patient

enum class DocumentFilter(val label: String) {
    ALL("Todos"),
    CONSENT_SIGNED("Consentimiento OK"),
    CONSENT_PENDING("Firma Pendiente"),
    CRITICAL_ALLERGIES("Alergias Críticas")
}

sealed interface DoctorDocumentsUiEvent {
    data class SearchQueryChanged(val query: String) : DoctorDocumentsUiEvent
    data class FilterSelected(val filter: DocumentFilter) : DoctorDocumentsUiEvent
    data object Refresh : DoctorDocumentsUiEvent
}

sealed interface DoctorDocumentsEffect {
    data class ShowToast(val message: String) : DoctorDocumentsEffect
}

@Immutable
data class DoctorDocumentsState(
    val patients: List<Patient> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: DocumentFilter = DocumentFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val totalRecords: Int get() = patients.size
    val consentedCount: Int get() = patients.count { it.consentSigned }
    val pendingConsentCount: Int get() = patients.count { !it.consentSigned }
    val criticalAllergiesCount: Int get() = patients.count { it.hasCriticalAllergies }

    val filteredPatients: List<Patient>
        get() {
            val queryFiltered = if (searchQuery.isBlank()) {
                patients
            } else {
                val q = searchQuery.trim().lowercase()
                patients.filter { p ->
                    p.fullName.lowercase().contains(q) || p.dniCedula.contains(q)
                }
            }

            return when (activeFilter) {
                DocumentFilter.ALL -> queryFiltered
                DocumentFilter.CONSENT_SIGNED -> queryFiltered.filter { it.consentSigned }
                DocumentFilter.CONSENT_PENDING -> queryFiltered.filter { !it.consentSigned }
                DocumentFilter.CRITICAL_ALLERGIES -> queryFiltered.filter { it.hasCriticalAllergies }
            }
        }
}
