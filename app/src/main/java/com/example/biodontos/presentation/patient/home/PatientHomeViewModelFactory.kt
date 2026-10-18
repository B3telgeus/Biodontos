package com.example.biodontos.presentation.patient.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.biodontos.BiodontosApp

object PatientHomeViewModelFactory {
    val Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            val container = application.container
            return PatientHomeViewModel(
                getCurrentSessionUseCase = container.getCurrentSessionUseCase,
                patientRepository = container.patientRepository,
                doctorRepository = container.doctorRepository,
                getPatientAppointmentsUseCase = container.getPatientAppointmentsUseCase,
                getDoctorBusySlotsUseCase = container.getDoctorBusySlotsUseCase,
                bookAppointmentUseCase = container.bookAppointmentUseCase,
                rescheduleAppointmentUseCase = container.rescheduleAppointmentUseCase,
                cancelAppointmentUseCase = container.cancelAppointmentUseCase,
                logoutUseCase = container.logoutUseCase
            ) as T
        }
    }
}
