package com.example.biodontos.core.di

import com.example.biodontos.core.util.SystemClinicClock
import com.example.biodontos.data.repository.AnamnesisRepositoryImpl
import com.example.biodontos.data.repository.AppointmentRepositoryImpl
import com.example.biodontos.data.repository.AuthRepositoryImpl
import com.example.biodontos.data.repository.BillingRepositoryImpl
import com.example.biodontos.data.repository.ClinicalAuditRepositoryImpl
import com.example.biodontos.data.repository.ClinicalRecordRepositoryImpl
import com.example.biodontos.data.repository.DoctorRepositoryImpl
import com.example.biodontos.data.repository.EvolutionRepositoryImpl
import com.example.biodontos.data.repository.GuardianRepositoryImpl
import com.example.biodontos.data.repository.InformedConsentRepositoryImpl
import com.example.biodontos.data.repository.MaterialRepositoryImpl
import com.example.biodontos.data.repository.OdontogramRepositoryImpl
import com.example.biodontos.data.repository.PatientRepositoryImpl
import com.example.biodontos.data.repository.PeriodontalRepositoryImpl
import com.example.biodontos.domain.repository.AnamnesisRepository
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.AuthRepository
import com.example.biodontos.domain.repository.BillingRepository
import com.example.biodontos.domain.repository.ClinicalAuditRepository
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.repository.EvolutionRepository
import com.example.biodontos.domain.repository.GuardianRepository
import com.example.biodontos.domain.repository.InformedConsentRepository
import com.example.biodontos.domain.repository.MaterialRepository
import com.example.biodontos.domain.repository.OdontogramRepository
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.repository.PeriodontalRepository
import com.example.biodontos.domain.scheduling.AppointmentModificationPolicy
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.usecase.appointment.ConfirmAppointmentUseCase
import com.example.biodontos.domain.usecase.appointment.GetDoctorBusySlotsUseCase
import com.example.biodontos.domain.usecase.appointment.GetPatientAppointmentsUseCase
import com.example.biodontos.domain.usecase.appointments.BookAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.CancelAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.GetAvailableSlotsUseCase
import com.example.biodontos.domain.usecase.appointments.GetDoctorAppointmentsUseCase
import com.example.biodontos.domain.usecase.appointments.RescheduleAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.ToggleAppointmentConfirmationUseCase
import com.example.biodontos.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.biodontos.domain.usecase.auth.LoginUseCase
import com.example.biodontos.domain.usecase.auth.LogoutUseCase
import com.example.biodontos.domain.usecase.auth.RegisterDoctorUseCase
import com.example.biodontos.domain.usecase.auth.RegisterPatientAuthUseCase
import com.example.biodontos.domain.usecase.auth.ResetPasswordUseCase
import com.example.biodontos.domain.usecase.auth.UpdatePasswordUseCase
import com.example.biodontos.domain.usecase.billing.CreateTreatmentUseCase
import com.example.biodontos.domain.usecase.billing.GetDoctorRecentPaymentsUseCase
import com.example.biodontos.domain.usecase.billing.GetPatientTreatmentsUseCase
import com.example.biodontos.domain.usecase.billing.GetPaymentSummaryUseCase
import com.example.biodontos.domain.usecase.billing.RecordPaymentUseCase
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import com.example.biodontos.domain.usecase.doctor.GetDoctorAvailabilityUseCase
import com.example.biodontos.domain.usecase.doctor.GetDoctorDetailUseCase
import com.example.biodontos.domain.usecase.doctor.GetMyDoctorsUseCase
import com.example.biodontos.domain.usecase.doctor.SaveDoctorAvailabilityUseCase
import com.example.biodontos.domain.usecase.doctor.SearchDoctorsUseCase
import com.example.biodontos.domain.usecase.doctor.UpdateDoctorProfileUseCase
import com.example.biodontos.domain.usecase.materials.BuildEvolutionMaterialUsageUseCase
import com.example.biodontos.domain.usecase.materials.GetMaterialsUseCase
import com.example.biodontos.domain.usecase.materials.SaveMaterialUseCase
import com.example.biodontos.domain.usecase.materials.SetMaterialActiveUseCase
import com.example.biodontos.domain.usecase.history.AddEvolutionClarificationUseCase
import com.example.biodontos.domain.usecase.history.GetPatientActivityTimelineUseCase
import com.example.biodontos.domain.usecase.history.UpdateTreatmentStatusUseCase
import com.example.biodontos.domain.usecase.history.VoidEvolutionUseCase
import com.example.biodontos.domain.usecase.history.VoidPaymentUseCase
import com.example.biodontos.domain.usecase.odontogram.GetOdontogramUseCase
import com.example.biodontos.domain.usecase.odontogram.UpdateToothUseCase
import com.example.biodontos.domain.usecase.patient.GetCurrentPatientUseCase
import com.example.biodontos.domain.usecase.patients.GetPatientsUseCase
import com.example.biodontos.domain.usecase.patients.SavePatientUseCase

interface AppContainer {
    val authRepository: AuthRepository
    val patientRepository: PatientRepository
    val guardianRepository: GuardianRepository
    val odontogramRepository: OdontogramRepository
    val clinicalRecordRepository: ClinicalRecordRepository
    val anamnesisRepository: AnamnesisRepository
    val periodontalRepository: PeriodontalRepository
    val evolutionRepository: EvolutionRepository
    val materialRepository: MaterialRepository
    val informedConsentRepository: InformedConsentRepository
    val appointmentRepository: AppointmentRepository
    val doctorRepository: DoctorRepository
    val billingRepository: BillingRepository
    val clinicalAuditRepository: ClinicalAuditRepository
    val clinicClock: ClinicClock

    val loginUseCase: LoginUseCase
    val registerPatientAuthUseCase: RegisterPatientAuthUseCase
    val registerDoctorUseCase: RegisterDoctorUseCase
    val resetPasswordUseCase: ResetPasswordUseCase
    val getCurrentSessionUseCase: GetCurrentSessionUseCase
    val getCurrentPatientUseCase: GetCurrentPatientUseCase
    val logoutUseCase: LogoutUseCase

    val getPatientsUseCase: GetPatientsUseCase
    val savePatientUseCase: SavePatientUseCase
    val getOdontogramUseCase: GetOdontogramUseCase
    val updateToothUseCase: UpdateToothUseCase

    val searchDoctorsUseCase: SearchDoctorsUseCase
    val getDoctorDetailUseCase: GetDoctorDetailUseCase
    val getMyDoctorsUseCase: GetMyDoctorsUseCase

    val appointmentModificationPolicy: AppointmentModificationPolicy
    val getAvailableSlotsUseCase: GetAvailableSlotsUseCase
    val bookAppointmentUseCase: BookAppointmentUseCase
    val rescheduleAppointmentUseCase: RescheduleAppointmentUseCase
    val cancelAppointmentUseCase: CancelAppointmentUseCase
    val getPaymentSummaryUseCase: GetPaymentSummaryUseCase
    val getCurrentDoctorUseCase: GetCurrentDoctorUseCase
    val getDoctorAppointmentsUseCase: GetDoctorAppointmentsUseCase
    val toggleAppointmentConfirmationUseCase: ToggleAppointmentConfirmationUseCase
    val getDoctorAvailabilityUseCase: GetDoctorAvailabilityUseCase
    val saveDoctorAvailabilityUseCase: SaveDoctorAvailabilityUseCase
    val recordPaymentUseCase: RecordPaymentUseCase
    val createTreatmentUseCase: CreateTreatmentUseCase
    val getDoctorRecentPaymentsUseCase: GetDoctorRecentPaymentsUseCase
    val getPatientTreatmentsUseCase: GetPatientTreatmentsUseCase

    val getPatientAppointmentsUseCase: GetPatientAppointmentsUseCase
    val getDoctorBusySlotsUseCase: GetDoctorBusySlotsUseCase
    val confirmAppointmentUseCase: ConfirmAppointmentUseCase

    val getPatientActivityTimelineUseCase: GetPatientActivityTimelineUseCase
    val voidEvolutionUseCase: VoidEvolutionUseCase
    val voidPaymentUseCase: VoidPaymentUseCase
    val addEvolutionClarificationUseCase: AddEvolutionClarificationUseCase
    val updateTreatmentStatusUseCase: UpdateTreatmentStatusUseCase

    val updateDoctorProfileUseCase: UpdateDoctorProfileUseCase
    val updatePasswordUseCase: UpdatePasswordUseCase

    val getMaterialsUseCase: GetMaterialsUseCase
    val saveMaterialUseCase: SaveMaterialUseCase
    val setMaterialActiveUseCase: SetMaterialActiveUseCase
    val buildEvolutionMaterialUsageUseCase: BuildEvolutionMaterialUsageUseCase
}

class DefaultAppContainer : AppContainer {
    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl()
    }

    override val patientRepository: PatientRepository by lazy {
        PatientRepositoryImpl()
    }

    override val guardianRepository: GuardianRepository by lazy {
        GuardianRepositoryImpl()
    }

    override val odontogramRepository: OdontogramRepository by lazy {
        OdontogramRepositoryImpl()
    }

    override val clinicalRecordRepository: ClinicalRecordRepository by lazy {
        ClinicalRecordRepositoryImpl()
    }

    override val anamnesisRepository: AnamnesisRepository by lazy {
        AnamnesisRepositoryImpl()
    }

    override val periodontalRepository: PeriodontalRepository by lazy {
        PeriodontalRepositoryImpl()
    }

    override val evolutionRepository: EvolutionRepository by lazy {
        EvolutionRepositoryImpl()
    }

    override val materialRepository: MaterialRepository by lazy {
        MaterialRepositoryImpl()
    }

    override val informedConsentRepository: InformedConsentRepository by lazy {
        InformedConsentRepositoryImpl()
    }

    override val appointmentRepository: AppointmentRepository by lazy {
        AppointmentRepositoryImpl()
    }

    override val doctorRepository: DoctorRepository by lazy {
        DoctorRepositoryImpl()
    }

    override val billingRepository: BillingRepository by lazy {
        BillingRepositoryImpl()
    }

    override val clinicalAuditRepository: ClinicalAuditRepository by lazy {
        ClinicalAuditRepositoryImpl()
    }

    override val clinicClock: ClinicClock by lazy {
        SystemClinicClock()
    }

    override val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(authRepository)
    }

    override val registerPatientAuthUseCase: RegisterPatientAuthUseCase by lazy {
        RegisterPatientAuthUseCase(authRepository)
    }

    override val registerDoctorUseCase: RegisterDoctorUseCase by lazy {
        RegisterDoctorUseCase(authRepository)
    }

    override val resetPasswordUseCase: ResetPasswordUseCase by lazy {
        ResetPasswordUseCase(authRepository)
    }

    override val getCurrentSessionUseCase: GetCurrentSessionUseCase by lazy {
        GetCurrentSessionUseCase(authRepository)
    }

    override val getCurrentPatientUseCase: GetCurrentPatientUseCase by lazy {
        GetCurrentPatientUseCase(authRepository, patientRepository)
    }

    override val logoutUseCase: LogoutUseCase by lazy {
        LogoutUseCase(authRepository)
    }

    override val getPatientsUseCase: GetPatientsUseCase by lazy {
        GetPatientsUseCase(patientRepository)
    }

    override val savePatientUseCase: SavePatientUseCase by lazy {
        SavePatientUseCase(patientRepository)
    }

    override val getOdontogramUseCase: GetOdontogramUseCase by lazy {
        GetOdontogramUseCase(odontogramRepository)
    }

    override val updateToothUseCase: UpdateToothUseCase by lazy {
        UpdateToothUseCase(odontogramRepository)
    }

    override val searchDoctorsUseCase: SearchDoctorsUseCase by lazy {
        SearchDoctorsUseCase(doctorRepository)
    }

    override val getDoctorDetailUseCase: GetDoctorDetailUseCase by lazy {
        GetDoctorDetailUseCase(doctorRepository, clinicClock)
    }

    override val getMyDoctorsUseCase: GetMyDoctorsUseCase by lazy {
        GetMyDoctorsUseCase(doctorRepository, appointmentRepository)
    }

    override val appointmentModificationPolicy: AppointmentModificationPolicy by lazy {
        AppointmentModificationPolicy(minHoursBefore = 2)
    }

    override val getAvailableSlotsUseCase: GetAvailableSlotsUseCase by lazy {
        GetAvailableSlotsUseCase(doctorRepository, appointmentRepository, clinicClock)
    }

    override val bookAppointmentUseCase: BookAppointmentUseCase by lazy {
        BookAppointmentUseCase(appointmentRepository, clinicClock)
    }

    override val rescheduleAppointmentUseCase: RescheduleAppointmentUseCase by lazy {
        RescheduleAppointmentUseCase(appointmentRepository, appointmentModificationPolicy, clinicClock)
    }

    override val cancelAppointmentUseCase: CancelAppointmentUseCase by lazy {
        CancelAppointmentUseCase(appointmentRepository, appointmentModificationPolicy, clinicClock)
    }

    override val getPaymentSummaryUseCase: GetPaymentSummaryUseCase by lazy {
        GetPaymentSummaryUseCase(billingRepository)
    }

    override val getCurrentDoctorUseCase: GetCurrentDoctorUseCase by lazy {
        GetCurrentDoctorUseCase(authRepository, doctorRepository)
    }

    override val getDoctorAppointmentsUseCase: GetDoctorAppointmentsUseCase by lazy {
        GetDoctorAppointmentsUseCase(appointmentRepository)
    }

    override val toggleAppointmentConfirmationUseCase: ToggleAppointmentConfirmationUseCase by lazy {
        ToggleAppointmentConfirmationUseCase(appointmentRepository)
    }

    override val getDoctorAvailabilityUseCase: GetDoctorAvailabilityUseCase by lazy {
        GetDoctorAvailabilityUseCase(doctorRepository)
    }

    override val saveDoctorAvailabilityUseCase: SaveDoctorAvailabilityUseCase by lazy {
        SaveDoctorAvailabilityUseCase(doctorRepository)
    }

    override val recordPaymentUseCase: RecordPaymentUseCase by lazy {
        RecordPaymentUseCase(billingRepository)
    }

    override val createTreatmentUseCase: CreateTreatmentUseCase by lazy {
        CreateTreatmentUseCase(billingRepository)
    }

    override val getDoctorRecentPaymentsUseCase: GetDoctorRecentPaymentsUseCase by lazy {
        GetDoctorRecentPaymentsUseCase(billingRepository)
    }

    override val getPatientTreatmentsUseCase: GetPatientTreatmentsUseCase by lazy {
        GetPatientTreatmentsUseCase(billingRepository)
    }

    override val getPatientAppointmentsUseCase: GetPatientAppointmentsUseCase by lazy {
        GetPatientAppointmentsUseCase(appointmentRepository)
    }

    override val getDoctorBusySlotsUseCase: GetDoctorBusySlotsUseCase by lazy {
        GetDoctorBusySlotsUseCase(appointmentRepository)
    }

    override val confirmAppointmentUseCase: ConfirmAppointmentUseCase by lazy {
        ConfirmAppointmentUseCase(appointmentRepository)
    }

    override val getPatientActivityTimelineUseCase: GetPatientActivityTimelineUseCase by lazy {
        GetPatientActivityTimelineUseCase(
            appointmentRepository = appointmentRepository,
            billingRepository = billingRepository,
            evolutionRepository = evolutionRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            informedConsentRepository = informedConsentRepository,
            clinicalAuditRepository = clinicalAuditRepository
        )
    }

    override val voidEvolutionUseCase: VoidEvolutionUseCase by lazy {
        VoidEvolutionUseCase(evolutionRepository)
    }

    override val voidPaymentUseCase: VoidPaymentUseCase by lazy {
        VoidPaymentUseCase(billingRepository)
    }

    override val addEvolutionClarificationUseCase: AddEvolutionClarificationUseCase by lazy {
        AddEvolutionClarificationUseCase(evolutionRepository)
    }

    override val updateTreatmentStatusUseCase: UpdateTreatmentStatusUseCase by lazy {
        UpdateTreatmentStatusUseCase(billingRepository)
    }

    override val updateDoctorProfileUseCase: UpdateDoctorProfileUseCase by lazy {
        UpdateDoctorProfileUseCase(doctorRepository)
    }

    override val updatePasswordUseCase: UpdatePasswordUseCase by lazy {
        UpdatePasswordUseCase(authRepository)
    }

    override val getMaterialsUseCase: GetMaterialsUseCase by lazy {
        GetMaterialsUseCase(materialRepository)
    }

    override val saveMaterialUseCase: SaveMaterialUseCase by lazy {
        SaveMaterialUseCase(materialRepository, clinicClock)
    }

    override val setMaterialActiveUseCase: SetMaterialActiveUseCase by lazy {
        SetMaterialActiveUseCase(materialRepository)
    }

    override val buildEvolutionMaterialUsageUseCase: BuildEvolutionMaterialUsageUseCase by lazy {
        BuildEvolutionMaterialUsageUseCase(clinicClock)
    }
}
