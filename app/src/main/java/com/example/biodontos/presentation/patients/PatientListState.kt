package com.example.biodontos.presentation.patients

import com.example.biodontos.domain.model.Patient

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
