package com.example.biodontos.presentation.patients

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.Patient

@Immutable
data class PatientListState(
    val patients: List<Patient> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filteredPatients: List<Patient>
        get() = if (searchQuery.isBlank()) {
            patients
        } else {
            val q = searchQuery.trim().lowercase()
            patients.filter { p ->
                p.fullName.lowercase().contains(q) || p.dniCedula.contains(q)
            }
        }
}
