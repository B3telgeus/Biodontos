package com.example.biodontos.presentation.patient.home

import com.example.biodontos.domain.model.Appointment

sealed interface PatientHomeEvent {
    data object LoadData : PatientHomeEvent
    data object Refresh : PatientHomeEvent
    data class SelectTab(val tab: AppointmentTab) : PatientHomeEvent

    // Booking
    data object OpenBookingDialog : PatientHomeEvent
    data object DismissBookingDialog : PatientHomeEvent
    data class SelectBookingDoctor(val doctorId: String) : PatientHomeEvent
    data class SelectBookingDate(val date: String) : PatientHomeEvent
    data class SelectBookingSlot(val slot: String) : PatientHomeEvent
    data class UpdateBookingReason(val reason: String) : PatientHomeEvent
    data object ConfirmBooking : PatientHomeEvent

    // Reschedule
    data class OpenRescheduleDialog(val appointment: Appointment) : PatientHomeEvent
    data object DismissRescheduleDialog : PatientHomeEvent
    data class SelectRescheduleDate(val date: String) : PatientHomeEvent
    data class SelectRescheduleSlot(val slot: String) : PatientHomeEvent
    data object ConfirmReschedule : PatientHomeEvent

    // Cancel
    data class OpenCancelDialog(val appointment: Appointment) : PatientHomeEvent
    data object DismissCancelDialog : PatientHomeEvent
    data class SelectCancelChip(val chip: String) : PatientHomeEvent
    data class UpdateCancelCustomReason(val reason: String) : PatientHomeEvent
    data object ConfirmCancellation : PatientHomeEvent

    // Success dialog
    data object DismissSuccessDialog : PatientHomeEvent

    // Session
    data object Logout : PatientHomeEvent
}

sealed interface PatientHomeEffect {
    data class ShowToast(val message: String) : PatientHomeEffect
    data object NavigateToLogin : PatientHomeEffect
}
