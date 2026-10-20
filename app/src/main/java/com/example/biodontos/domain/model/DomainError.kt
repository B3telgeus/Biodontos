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
