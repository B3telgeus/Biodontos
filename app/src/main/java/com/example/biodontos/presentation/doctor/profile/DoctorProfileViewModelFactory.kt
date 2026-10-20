package com.example.biodontos.presentation.doctor.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.biodontos.BiodontosApp
import com.example.biodontos.domain.usecase.appointment.CancelAppointmentUseCase
import com.example.biodontos.domain.usecase.appointment.GetDoctorAppointmentsUseCase

object DoctorProfileViewModelFactory {
    val Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            val container = application.container
            return DoctorProfileViewModel(
                getCurrentSessionUseCase = container.getCurrentSessionUseCase,
                doctorRepository = container.doctorRepository,
                getDoctorAppointmentsUseCase = GetDoctorAppointmentsUseCase(container.appointmentRepository),
                confirmAppointmentUseCase = container.confirmAppointmentUseCase,
                cancelAppointmentUseCase = CancelAppointmentUseCase(container.appointmentRepository),
                patientRepository = container.patientRepository,
                logoutUseCase = container.logoutUseCase
            ) as T
        }
    }
}
