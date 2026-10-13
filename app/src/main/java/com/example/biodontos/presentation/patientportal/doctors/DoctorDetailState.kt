package com.example.biodontos.presentation.patientportal.doctors

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.DoctorDetail

@Immutable
data class DoctorDetailUiState(
    val doctorId: String = "",
    val doctorDetail: DoctorDetail? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface DoctorDetailUiEvent {
    data object BookAppointment : DoctorDetailUiEvent
    data object Retry : DoctorDetailUiEvent
}

sealed interface DoctorDetailUiEffect {
    data class NavigateToBooking(val doctorId: String) : DoctorDetailUiEffect
}
