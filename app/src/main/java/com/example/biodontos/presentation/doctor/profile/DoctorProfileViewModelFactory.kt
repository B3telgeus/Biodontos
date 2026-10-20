package com.example.biodontos.presentation.doctor.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.biodontos.BiodontosApp

object DoctorProfileViewModelFactory {
    val Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            val container = application.container
            return DoctorProfileViewModel(
                getCurrentSessionUseCase = container.getCurrentSessionUseCase,
                doctorRepository = container.doctorRepository,
                getDoctorAppointmentsUseCase = container.getDoctorAppointmentsUseCase,
                confirmAppointmentUseCase = container.confirmAppointmentUseCase,
                cancelAppointmentUseCase = container.cancelAppointmentUseCase,
                patientRepository = container.patientRepository,
                logoutUseCase = container.logoutUseCase
            ) as T
        }
    }
}
