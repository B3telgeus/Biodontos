package com.example.biodontos.presentation.patients

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.Patient

enum class PatientQuickFilter(val label: String) {
    ALL("Todos"),
    CRITICAL_ALLERGIES("Alergias Críticas"),
    PENDING_CONSENT("Consentimiento Pendiente")
}

@Immutable
data class PatientListState(
    val patients: List<Patient> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: PatientQuickFilter = PatientQuickFilter.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val totalCount: Int get() = patients.size
    val criticalAllergiesCount: Int get() = patients.count { it.hasCriticalAllergies }
    val pendingConsentCount: Int get() = patients.count { !it.consentSigned }

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
            return when (selectedFilter) {
                PatientQuickFilter.ALL -> queryFiltered
                PatientQuickFilter.CRITICAL_ALLERGIES -> queryFiltered.filter { it.hasCriticalAllergies }
                PatientQuickFilter.PENDING_CONSENT -> queryFiltered.filter { !it.consentSigned }
            }
        }
}
