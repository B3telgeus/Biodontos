package com.example.biodontos.presentation.patientportal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.biodontos.BiodontosApp
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
}
