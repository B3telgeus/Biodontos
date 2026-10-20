package com.example.biodontos.presentation.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.biodontos.BiodontosApp

object PatientsViewModelFactory {
    val ListFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            return PatientListViewModel(application.container.getPatientsUseCase) as T
        }
    }

    val RegistrationFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            return PatientRegistrationViewModel(application.container.savePatientUseCase) as T
        }
    }

    fun createPatientDetailFactory(patientId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
            val c = application.container
            return com.example.biodontos.presentation.patients.detail.PatientDetailViewModel(
                patientId = patientId,
                patientRepository = c.patientRepository,
                clinicalRecordRepository = c.clinicalRecordRepository,
                anamnesisRepository = c.anamnesisRepository,
                informedConsentRepository = c.informedConsentRepository,
                evolutionRepository = c.evolutionRepository,
                billingRepository = c.billingRepository,
                appointmentRepository = c.appointmentRepository,
                materialRepository = c.materialRepository
            ) as T
        }
    }
}
