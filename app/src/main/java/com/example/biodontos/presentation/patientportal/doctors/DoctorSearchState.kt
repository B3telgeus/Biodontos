package com.example.biodontos.presentation.patientportal.doctors

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.Doctor

@Immutable
data class DoctorSearchUiState(
    val searchQuery: String = "",
    val selectedSpecialty: String = "Todas",
    val specialties: List<String> = listOf(
        "Todas",
        "Ortodoncia",
        "Endodoncia",
        "Periodoncia",
        "Odontopediatría",
        "Cirugía Oral",
        "Odontología General"
    ),
    val doctors: List<Doctor> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface DoctorSearchUiEvent {
    data class QueryChanged(val query: String) : DoctorSearchUiEvent
    data class SpecialtySelected(val specialty: String) : DoctorSearchUiEvent
    data class DoctorSelected(val doctorId: String) : DoctorSearchUiEvent
    data class BookDoctor(val doctorId: String) : DoctorSearchUiEvent
    data object Retry : DoctorSearchUiEvent
}

sealed interface DoctorSearchUiEffect {
    data class NavigateToDetail(val doctorId: String) : DoctorSearchUiEffect
    data class NavigateToBooking(val doctorId: String) : DoctorSearchUiEffect
}
