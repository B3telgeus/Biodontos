package com.example.biodontos.presentation.patients.detail

import com.example.biodontos.core.util.toUserFriendlyMessage
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.ClinicalRecordStatus
import com.example.biodontos.domain.model.PeriodontalExamination
import com.example.biodontos.domain.model.PeriodontalSiteMeasurement
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import com.example.biodontos.domain.repository.PeriodontalRepository
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import java.time.LocalDate

class PeriodontogramDelegate(
    private val periodontalRepository: PeriodontalRepository,
    private val clinicalRecordRepository: ClinicalRecordRepository,
    private val getCurrentDoctorUseCase: GetCurrentDoctorUseCase?,
    private val updateState: ((PatientDetailState) -> PatientDetailState) -> Unit,
    private val sendEffect: suspend (PatientDetailEffect) -> Unit
) {
    fun selectTooth(toothNumber: Int) {
        updateState { it.copy(periodontalSelectedTooth = toothNumber) }
    }

    fun updateMeasurement(measurement: PeriodontalSiteMeasurement) {
        updateState { current ->
            val updated = current.periodontalSiteMeasurements.toMutableMap()
            updated[measurement.toothNumber to measurement.siteLocation] = measurement
            current.copy(periodontalSiteMeasurements = updated)
        }
    }

    fun updateObservations(notes: String) {
        updateState { it.copy(periodontalObservations = notes) }
    }

    suspend fun loadExamination(recordId: String) {
        periodontalRepository.getExaminationByRecordId(recordId).onSuccess { exam ->
            if (exam != null) {
                val siteMap = exam.sites.associateBy { it.toothNumber to it.siteLocation }
                updateState {
                    it.copy(
                        periodontalExaminationId = exam.id,
                        periodontalSiteMeasurements = siteMap,
                        periodontalObservations = exam.generalObservations ?: ""
                    )
                }
            }
        }
    }

    suspend fun save(patientId: String, state: PatientDetailState) {
        updateState { it.copy(isSavingPeriodontogram = true, errorMessage = null) }

        var resolvedDoctorId = ""
        val doctorRes = getCurrentDoctorUseCase?.invoke()
        if (doctorRes != null && doctorRes.isSuccess) {
            resolvedDoctorId = doctorRes.getOrNull()?.id?.value?.trim().orEmpty()
        }

        if (resolvedDoctorId.isBlank()) {
            updateState {
                it.copy(
                    isSavingPeriodontogram = false,
                    errorMessage = "Se requiere una sesión de doctor activa y válida para registrar el periodontograma."
                )
            }
            return
        }

        var effectiveRecordId = state.clinicalRecord?.id.orEmpty()
        if (effectiveRecordId.isBlank()) {
            val newRecord = ClinicalRecord(
                patientId = patientId,
                status = ClinicalRecordStatus.ABIERTA
            )
            val saveRecordRes = clinicalRecordRepository.saveRecord(newRecord)
            if (saveRecordRes.isSuccess) {
                val savedRec = saveRecordRes.getOrThrow()
                effectiveRecordId = savedRec.id
                updateState { it.copy(clinicalRecord = savedRec) }
            } else {
                val err = saveRecordRes.exceptionOrNull()
                updateState {
                    it.copy(
                        isSavingPeriodontogram = false,
                        errorMessage = "Error al inicializar historia clínica para periodontograma: ${err.toUserFriendlyMessage()}"
                    )
                }
                return
            }
        }

        val exam = PeriodontalExamination(
            id = state.periodontalExaminationId,
            recordId = effectiveRecordId,
            doctorId = resolvedDoctorId,
            examinationDate = LocalDate.now().toString(),
            generalObservations = state.periodontalObservations.ifBlank { null },
            sites = state.periodontalSiteMeasurements.values.toList()
        )

        periodontalRepository.saveExamination(exam)
            .onSuccess { savedExam ->
                val siteMap = savedExam.sites.associateBy { it.toothNumber to it.siteLocation }
                updateState {
                    it.copy(
                        isSavingPeriodontogram = false,
                        periodontalExaminationId = savedExam.id,
                        periodontalSiteMeasurements = siteMap,
                        successMessage = "Periodontograma guardado correctamente"
                    )
                }
                sendEffect(PatientDetailEffect.ShowSnackbar("Periodontograma guardado correctamente"))
            }
            .onFailure { err ->
                updateState {
                    it.copy(
                        isSavingPeriodontogram = false,
                        errorMessage = "Error al guardar periodontograma: ${err.toUserFriendlyMessage()}"
                    )
                }
            }
    }
}
