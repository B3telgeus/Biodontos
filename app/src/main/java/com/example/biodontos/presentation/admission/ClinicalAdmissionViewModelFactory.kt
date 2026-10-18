package com.example.biodontos.presentation.admission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.biodontos.BiodontosApp
import com.example.biodontos.core.di.AppContainer

class ClinicalAdmissionViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return createViewModel(appContainer) as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? BiodontosApp
        val container = application?.container ?: appContainer
        return createViewModel(container) as T
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
                return createViewModel(application.container) as T
            }
        }

        private fun createViewModel(container: AppContainer): ClinicalAdmissionViewModel {
            return ClinicalAdmissionViewModel(
                patientRepository = container.patientRepository,
                clinicalRecordRepository = container.clinicalRecordRepository,
                anamnesisRepository = container.anamnesisRepository,
                odontogramRepository = container.odontogramRepository,
                periodontalRepository = container.periodontalRepository,
                evolutionRepository = container.evolutionRepository,
                materialRepository = container.materialRepository,
                informedConsentRepository = container.informedConsentRepository,
                appointmentRepository = container.appointmentRepository,
                guardianRepository = container.guardianRepository
            )
        }
    }
}
