package com.example.biodontos.presentation.patients

import com.example.biodontos.domain.model.Patient

data class PatientListState(
    val patients: List<Patient> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
