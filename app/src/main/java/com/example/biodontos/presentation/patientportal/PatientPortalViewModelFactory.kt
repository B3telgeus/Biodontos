package com.example.biodontos.presentation.patientportal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.biodontos.BiodontosApp
import com.example.biodontos.presentation.patientportal.appointments.BookAppointmentViewModel
import com.example.biodontos.presentation.patientportal.appointments.MyAppointmentsViewModel
import com.example.biodontos.presentation.patientportal.doctors.DoctorDetailViewModel
import com.example.biodontos.presentation.patientportal.doctors.DoctorSearchViewModel
import com.example.biodontos.presentation.patientportal.home.PatientHomeViewModel

object PatientPortalViewModelFactory {
    val HomeFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            val c = app.container
            return PatientHomeViewModel(
                authRepository = c.authRepository,
                patientRepository = c.patientRepository,
                appointmentRepository = c.appointmentRepository,
                billingRepository = c.billingRepository,
                doctorRepository = c.doctorRepository
            ) as T
        }
    }

    val DoctorSearchFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            val c = app.container
            return DoctorSearchViewModel(
                searchDoctorsUseCase = c.searchDoctorsUseCase
            ) as T
        }
    }

    fun createDoctorDetailFactory(doctorId: String) = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            val c = app.container
            return DoctorDetailViewModel(
                doctorId = doctorId,
                getDoctorDetailUseCase = c.getDoctorDetailUseCase
            ) as T
        }
    }

    val MyAppointmentsFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            val c = app.container
            return MyAppointmentsViewModel(
                getCurrentPatientUseCase = c.getCurrentPatientUseCase,
                appointmentRepository = c.appointmentRepository,
                doctorRepository = c.doctorRepository,
                cancelAppointmentUseCase = c.cancelAppointmentUseCase,
                clinicClock = c.clinicClock
            ) as T
        }
    }

    fun createBookAppointmentFactory(
        doctorId: String,
        rescheduleAppointmentId: String? = null
    ) = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            val c = app.container
            return BookAppointmentViewModel(
                doctorId = doctorId,
                rescheduleAppointmentId = rescheduleAppointmentId,
                getCurrentPatientUseCase = c.getCurrentPatientUseCase,
                doctorRepository = c.doctorRepository,
                getAvailableSlotsUseCase = c.getAvailableSlotsUseCase,
                bookAppointmentUseCase = c.bookAppointmentUseCase,
                rescheduleAppointmentUseCase = c.rescheduleAppointmentUseCase,
                clinicClock = c.clinicClock
            ) as T
        }
    }
}
