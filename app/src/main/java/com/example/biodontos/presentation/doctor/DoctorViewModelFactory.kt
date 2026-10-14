package com.example.biodontos.presentation.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.biodontos.BiodontosApp
import com.example.biodontos.presentation.doctor.appointments.DoctorAppointmentsViewModel
import com.example.biodontos.presentation.doctor.home.DoctorHomeViewModel

object DoctorViewModelFactory {
    val HomeFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            val c = app.container
            return DoctorHomeViewModel(
                getCurrentDoctorUseCase = c.getCurrentDoctorUseCase,
                getDoctorAppointmentsUseCase = c.getDoctorAppointmentsUseCase,
                toggleAppointmentConfirmationUseCase = c.toggleAppointmentConfirmationUseCase,
                patientRepository = c.patientRepository,
                logoutUseCase = c.logoutUseCase,
                clinicClock = c.clinicClock,
                getDoctorRecentPaymentsUseCase = c.getDoctorRecentPaymentsUseCase,
                recordPaymentUseCase = c.recordPaymentUseCase,
                createTreatmentUseCase = c.createTreatmentUseCase,
                getPatientTreatmentsUseCase = c.getPatientTreatmentsUseCase
            ) as T
        }
    }

    val AppointmentsFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            val c = app.container
            return DoctorAppointmentsViewModel(
                getCurrentDoctorUseCase = c.getCurrentDoctorUseCase,
                getDoctorAppointmentsUseCase = c.getDoctorAppointmentsUseCase,
                toggleAppointmentConfirmationUseCase = c.toggleAppointmentConfirmationUseCase,
                getDoctorAvailabilityUseCase = c.getDoctorAvailabilityUseCase,
                saveDoctorAvailabilityUseCase = c.saveDoctorAvailabilityUseCase,
                bookAppointmentUseCase = c.bookAppointmentUseCase,
                rescheduleAppointmentUseCase = c.rescheduleAppointmentUseCase,
                cancelAppointmentUseCase = c.cancelAppointmentUseCase,
                patientRepository = c.patientRepository,
                clinicClock = c.clinicClock
            ) as T
        }
    }
}
