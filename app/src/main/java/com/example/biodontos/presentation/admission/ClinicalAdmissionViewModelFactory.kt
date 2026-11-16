package com.example.biodontos.presentation.admission

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.biodontos.BiodontosApp
import com.example.biodontos.core.di.AppContainer

class ClinicalAdmissionViewModelFactory(
    private val appContainer: AppContainer,
    private val patientId: String? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return createViewModel(appContainer, patientId, null) as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? BiodontosApp
        val container = application?.container ?: appContainer
        val savedStateHandle = try {
            extras.createSavedStateHandle()
        } catch (_: Exception) {
            null
        }
        return createViewModel(container, patientId, savedStateHandle) as T
    }

    companion object {
        fun createFactory(container: AppContainer, patientId: String? = null): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return createViewModel(container, patientId, null) as T
                }

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    val savedStateHandle = try {
                        extras.createSavedStateHandle()
                    } catch (_: Exception) {
                        null
                    }
                    return createViewModel(container, patientId, savedStateHandle) as T
                }
            }
        }

        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BiodontosApp
                val savedStateHandle = try {
                    extras.createSavedStateHandle()
                } catch (_: Exception) {
                    null
                }
                return createViewModel(application.container, null, savedStateHandle) as T
            }
        }

        private fun createViewModel(
            container: AppContainer,
            patientId: String?,
            savedStateHandle: SavedStateHandle?
        ): ClinicalAdmissionViewModel {
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
                guardianRepository = container.guardianRepository,
                initialPatientId = patientId,
                getCurrentDoctorUseCase = container.getCurrentDoctorUseCase,
                doctorRepository = container.doctorRepository,
                admissionDraftDao = container.admissionDraftDao,
                saveAdmissionEvolutionAndCloseUseCase = container.saveAdmissionEvolutionAndCloseUseCase,
                scheduleAdmissionFollowUpUseCase = container.scheduleAdmissionFollowUpUseCase,
                savedStateHandle = savedStateHandle
            )
        }
    }
}
