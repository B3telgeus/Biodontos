package com.example.biodontos.domain.model

sealed interface DomainError {
    data class NetworkOrPersistenceFailure(val reason: String) : DomainError
    data object SessionExpired : DomainError
    data object PatientProfileNotFound : DomainError
    data object ConsentNotGranted : DomainError
    data class InvalidToothNumber(val number: Int) : DomainError
    data class PatientNotFound(val patientId: String) : DomainError
}

sealed interface AppointmentError : DomainError {
    data object SlotUnavailable : AppointmentError
    data object OutsideDoctorSchedule : AppointmentError
    data object SlotInPast : AppointmentError
    data class ModificationWindowClosed(val minHoursBefore: Long = 2) : AppointmentError
    data object AlreadyCancelled : AppointmentError
    data object NotOwner : AppointmentError
    data object AppointmentNotFound : AppointmentError
}

sealed interface ClinicalHistoryError : DomainError {
    val userMessage: String

    data class VoidReasonTooShort(val minLength: Int = 10) : ClinicalHistoryError {
        override val userMessage: String =
            "El motivo de anulación es obligatorio y debe tener al menos $minLength caracteres."
    }

    data object AlreadyVoided : ClinicalHistoryError {
        override val userMessage: String =
            "Este registro ya fue anulado previamente y no admite nuevas modificaciones."
    }

    data class InvalidStatusTransition(val from: String, val to: String) : ClinicalHistoryError {
        override val userMessage: String =
            "Transición de estado no permitida de $from a $to."
    }

    data object StatusReasonRequired : ClinicalHistoryError {
        override val userMessage: String =
            "Debe ingresar una justificación clínica o administrativa para cancelar el presupuesto."
    }

    data object ClarificationEmpty : ClinicalHistoryError {
        override val userMessage: String =
            "El texto de la nota aclaratoria no puede estar vacío."
    }
}
