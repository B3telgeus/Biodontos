package com.example.biodontos.presentation.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.DentitionType
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.Tooth
import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface
import com.example.biodontos.presentation.auth.login.LoginState
import com.example.biodontos.presentation.doctor.appointments.DoctorAppointmentFilter
import com.example.biodontos.presentation.doctor.appointments.DoctorAppointmentsUiState
import com.example.biodontos.presentation.doctor.appointments.DoctorScheduleMode
import com.example.biodontos.presentation.doctor.appointments.DoctorSlotStatus
import com.example.biodontos.presentation.doctor.appointments.DoctorTimelineSlot
import com.example.biodontos.presentation.doctor.home.DoctorAppointmentItem
import com.example.biodontos.presentation.doctor.home.DoctorHomeUiState
import com.example.biodontos.presentation.doctor.home.DoctorPaymentItem
import com.example.biodontos.presentation.doctor.materials.MaterialTabFilter
import com.example.biodontos.presentation.doctor.materials.MaterialsUiState
import com.example.biodontos.presentation.odontogram.OdontogramState
import com.example.biodontos.presentation.patientportal.appointments.AppointmentTab
import com.example.biodontos.presentation.patientportal.appointments.MyAppointmentsUiState
import com.example.biodontos.presentation.patientportal.home.PatientHomeUiState
import com.example.biodontos.presentation.patients.PatientListState
import com.example.biodontos.presentation.patients.PatientQuickFilter
import java.time.LocalDate
import java.time.LocalDateTime

internal val PreviewReferenceDate: LocalDate = LocalDate.of(2026, 11, 20)

private val previewDoctorPrimary = Doctor(
    id = DoctorId("doc-preview-1"),
    firstName = "Erika Liney",
    lastName = "Bejarano Beltrán",
    specialty = "Odontología Estética y Operatoria",
    licenseNumber = "OD-98234-COL",
    email = "erika.bejarano@biodontos.com",
    phone = "3104458899",
    bio = "Especialista en operatoria adhesiva, estética dental y rehabilitación preventiva.",
    photoUrl = null,
    clinicAddress = "Calle 100 # 15-20, Consultorio 402, Bogotá"
)

private val previewDoctorOverflow = Doctor(
    id = DoctorId("doc-preview-2"),
    firstName = "Sebastián Alejandro",
    lastName = "Villalobos Santamaría De La Torre",
    specialty = "Rehabilitación Oral, Implantología Avanzada y Estética Dental Integral",
    licenseNumber = "OD-44981-COL-REG-2026",
    email = "sebastian.villalobos@biodontos.com",
    phone = "3158901234",
    bio = "Más de 15 años liderando casos de alta complejidad en rehabilitación sobre implantes, oclusión neuromuscular y diseño de sonrisa digital.",
    photoUrl = null,
    clinicAddress = "Avenida Carrera 19 # 118-95, Centro Médico Especializado Norte, Piso 8, Consultorio 812"
)

class LoginStatePreviewProvider : PreviewParameterProvider<LoginState> {
    override val values: Sequence<LoginState> = sequenceOf(
        LoginState(
            email = "dra.bejarano@biodontos.com",
            password = "ClaveClinica2026",
            isLoading = true
        ),
        LoginState(
            email = "dra.bejarano@biodontos.com",
            password = "ClaveClinica2026",
            isPasswordVisible = false
        ),
        LoginState(
            email = "doctora.elena.valentina.salazar.montenegro@clinicaodontologicabiodontos.com.co",
            password = "ClaveExtendidaDeAltaSeguridadClinica2026!",
            isPasswordVisible = true
        ),
        LoginState(),
        LoginState(
            email = "correo.invalido",
            password = "123",
            emailError = "Ingrese un correo electrónico válido.",
            passwordError = "La contraseña debe tener al menos 6 caracteres.",
            error = "Credenciales inválidas. Verifique su correo institucional y contraseña."
        )
    )
}

class PatientListStatePreviewProvider : PreviewParameterProvider<PatientListState> {
    override val values: Sequence<PatientListState> = sequenceOf(
        PatientListState(isLoading = true),
        PatientListState(
            patients = listOf(
                Patient(
                    id = "pat-1",
                    dniCedula = "1020845123",
                    firstName = "Valentina",
                    firstLastName = "Restrepo",
                    secondLastName = "Gómez",
                    phone = "3104567890",
                    consentSigned = true,
                    hasCriticalAllergies = true,
                    allergiesDescription = "Penicilina"
                ),
                Patient(
                    id = "pat-2",
                    dniCedula = "79845120",
                    firstName = "Andrés",
                    firstLastName = "Castillo",
                    secondLastName = "Pardo",
                    phone = "3142239988",
                    consentSigned = false,
                    hasCriticalAllergies = false
                ),
                Patient(
                    id = "pat-3",
                    dniCedula = "52987321",
                    firstName = "Camila",
                    firstLastName = "Jiménez",
                    secondLastName = "Rojas",
                    phone = "3007784411",
                    consentSigned = true,
                    hasCriticalAllergies = false
                )
            ),
            selectedFilter = PatientQuickFilter.ALL
        ),
        PatientListState(
            patients = listOf(
                Patient(
                    id = "pat-overflow-1",
                    dniCedula = "10002938475612",
                    firstName = "María Fernanda De Los Ángeles",
                    firstLastName = "Castellanos",
                    secondLastName = "Villavicencio-Sarmiento",
                    phone = "3209876543",
                    consentSigned = true,
                    hasCriticalAllergies = true,
                    allergiesDescription = "Hipersensibilidad severa a Penicilina G, Lidocaína con epinefrina y derivados del látex natural"
                )
            ),
            searchQuery = "Castellanos",
            selectedFilter = PatientQuickFilter.CRITICAL_ALLERGIES
        ),
        PatientListState(
            patients = emptyList(),
            isLoading = false
        ),
        PatientListState(
            isLoading = false,
            error = "No fue posible sincronizar el directorio de pacientes. Verifica tu conexión a internet."
        )
    )
}

class DoctorHomeStatePreviewProvider : PreviewParameterProvider<DoctorHomeUiState> {
    override val values: Sequence<DoctorHomeUiState> = sequenceOf(
        DoctorHomeUiState(isLoading = true),
        DoctorHomeUiState(
            isLoading = false,
            doctor = previewDoctorPrimary,
            doctorName = previewDoctorPrimary.displayName,
            doctorPhotoUrl = null,
            patientsCount = 48,
            consentedPatientsCount = 42,
            pendingConsentCount = 6,
            todayAppointmentsCount = 4,
            upcomingAppointments = listOf(
                DoctorAppointmentItem(
                    appointmentId = "apt-doc-1",
                    patientId = "pat-1",
                    patientName = "Valentina Restrepo Gómez",
                    treatmentReason = "Control de operatoria pieza 16",
                    startTime = LocalDateTime.of(2026, 11, 20, 9, 30),
                    isConfirmed = true
                ),
                DoctorAppointmentItem(
                    appointmentId = "apt-doc-2",
                    patientId = "pat-2",
                    patientName = "Andrés Castillo Pardo",
                    treatmentReason = "Profilaxis y valoración periodontal",
                    startTime = LocalDateTime.of(2026, 11, 20, 11, 0),
                    isConfirmed = false
                )
            ),
            recentPayments = listOf(
                DoctorPaymentItem(
                    id = "pay-1",
                    patientId = "pat-1",
                    patientName = "Valentina Restrepo",
                    treatmentDescription = "Resinas estéticas sector posterior",
                    dateStr = "20/11/2026",
                    amountCop = CopAmount(350_000L),
                    paymentMethod = "TRANSFERENCIA"
                )
            ),
            recentTotalCollectedCop = CopAmount(350_000L)
        ),
        DoctorHomeUiState(
            isLoading = false,
            doctor = previewDoctorOverflow,
            doctorName = previewDoctorOverflow.fullName,
            doctorPhotoUrl = null,
            patientsCount = 1280,
            consentedPatientsCount = 1190,
            pendingConsentCount = 90,
            todayAppointmentsCount = 14,
            upcomingAppointments = listOf(
                DoctorAppointmentItem(
                    appointmentId = "apt-doc-overflow",
                    patientId = "pat-overflow-1",
                    patientName = "María Fernanda De Los Ángeles Castellanos Villavicencio",
                    treatmentReason = "Rehabilitación oral integral con coronas en disilicato de litio y ajuste oclusal",
                    startTime = LocalDateTime.of(2026, 11, 20, 16, 45),
                    isConfirmed = true
                )
            ),
            recentPayments = listOf(
                DoctorPaymentItem(
                    id = "pay-overflow",
                    patientId = "pat-overflow-1",
                    patientName = "María Fernanda Castellanos Villavicencio",
                    treatmentDescription = "Rehabilitación protésica implantosoportada arcada inferior completa",
                    dateStr = "20/11/2026",
                    amountCop = CopAmount(4_850_000L),
                    paymentMethod = "TARJETA"
                )
            ),
            recentTotalCollectedCop = CopAmount(12_850_000L)
        ),
        DoctorHomeUiState(
            isLoading = false,
            doctor = previewDoctorPrimary,
            doctorName = previewDoctorPrimary.displayName,
            doctorPhotoUrl = null,
            patientsCount = 0,
            consentedPatientsCount = 0,
            pendingConsentCount = 0,
            todayAppointmentsCount = 0,
            upcomingAppointments = emptyList(),
            recentPayments = emptyList(),
            recentTotalCollectedCop = CopAmount.ZERO
        ),
        DoctorHomeUiState(
            isLoading = false,
            doctor = previewDoctorPrimary,
            doctorName = previewDoctorPrimary.displayName,
            doctorPhotoUrl = null,
            errorMessage = "No se pudo actualizar el resumen clínico del consultorio. Reintenta en unos segundos."
        )
    )
}

class DoctorAppointmentsStatePreviewProvider : PreviewParameterProvider<DoctorAppointmentsUiState> {
    private val sampleAppt = DoctorAppointmentItem(
        appointmentId = "apt-101",
        patientId = "pat-1",
        patientName = "Valentina Restrepo Gómez",
        treatmentReason = "Restauración en resina pieza 26",
        startTime = LocalDateTime.of(2026, 11, 20, 9, 0),
        isConfirmed = true
    )

    private val overflowAppt = DoctorAppointmentItem(
        appointmentId = "apt-102",
        patientId = "pat-overflow-1",
        patientName = "María Fernanda De Los Ángeles Castellanos Villavicencio",
        treatmentReason = "Endodoncia mecanizada molar inferior y reconstrucción coronaria con poste de fibra",
        startTime = LocalDateTime.of(2026, 11, 20, 14, 30),
        isConfirmed = false
    )

    override val values: Sequence<DoctorAppointmentsUiState> = sequenceOf(
        DoctorAppointmentsUiState(
            isLoading = true,
            doctorName = previewDoctorPrimary.displayName,
            doctorId = previewDoctorPrimary.id.value,
            selectedDate = PreviewReferenceDate,
            rescheduleTargetDate = PreviewReferenceDate
        ),
        DoctorAppointmentsUiState(
            isLoading = false,
            doctorName = previewDoctorPrimary.displayName,
            doctorId = previewDoctorPrimary.id.value,
            selectedDate = PreviewReferenceDate,
            rescheduleTargetDate = PreviewReferenceDate,
            displayMode = DoctorScheduleMode.TIMELINE,
            appointments = listOf(sampleAppt),
            timelineSlots = listOf(
                DoctorTimelineSlot(
                    startTime = LocalDateTime.of(2026, 11, 20, 8, 0),
                    endTime = LocalDateTime.of(2026, 11, 20, 8, 30),
                    status = DoctorSlotStatus.Past
                ),
                DoctorTimelineSlot(
                    startTime = LocalDateTime.of(2026, 11, 20, 9, 0),
                    endTime = LocalDateTime.of(2026, 11, 20, 9, 30),
                    status = DoctorSlotStatus.Occupied(sampleAppt)
                ),
                DoctorTimelineSlot(
                    startTime = LocalDateTime.of(2026, 11, 20, 9, 30),
                    endTime = LocalDateTime.of(2026, 11, 20, 10, 0),
                    status = DoctorSlotStatus.Available
                )
            )
        ),
        DoctorAppointmentsUiState(
            isLoading = false,
            doctorName = previewDoctorOverflow.fullName,
            doctorId = previewDoctorOverflow.id.value,
            selectedDate = PreviewReferenceDate,
            rescheduleTargetDate = PreviewReferenceDate,
            displayMode = DoctorScheduleMode.LIST,
            selectedFilter = DoctorAppointmentFilter.ALL,
            appointments = listOf(overflowAppt, sampleAppt)
        ),
        DoctorAppointmentsUiState(
            isLoading = false,
            doctorName = previewDoctorPrimary.displayName,
            doctorId = previewDoctorPrimary.id.value,
            selectedDate = PreviewReferenceDate,
            rescheduleTargetDate = PreviewReferenceDate,
            displayMode = DoctorScheduleMode.TIMELINE,
            timelineSlots = emptyList(),
            appointments = emptyList()
        ),
        DoctorAppointmentsUiState(
            isLoading = false,
            doctorName = previewDoctorPrimary.displayName,
            doctorId = previewDoctorPrimary.id.value,
            selectedDate = PreviewReferenceDate,
            rescheduleTargetDate = PreviewReferenceDate,
            errorMessage = "No fue posible consultar la agenda médica del consultorio."
        )
    )
}

class MaterialsUiStatePreviewProvider : PreviewParameterProvider<MaterialsUiState> {
    private val refDate = LocalDate.now()

    override val values: Sequence<MaterialsUiState> = sequenceOf(
        MaterialsUiState(isLoading = true),
        MaterialsUiState(
            isLoading = false,
            selectedTab = MaterialTabFilter.ACTIVE,
            materials = listOf(
                ClinicalMaterial(
                    id = MaterialId("mat-1"),
                    doctorId = previewDoctorPrimary.id,
                    name = "Resina Compuesta Filtek Z350 XT A2",
                    invimaRegistration = "INVIMA 2022DM-0014512",
                    batchLotNumber = "LOT-2026-A21",
                    manufacturingDate = refDate.minusMonths(4),
                    expirationDate = refDate.plusMonths(14),
                    isActive = true
                ),
                ClinicalMaterial(
                    id = MaterialId("mat-2"),
                    doctorId = previewDoctorPrimary.id,
                    name = "Ácido Grabador Ortofosfórico 37%",
                    invimaRegistration = "INVIMA 2020DM-0008871",
                    batchLotNumber = "LOT-2025-B09",
                    manufacturingDate = refDate.minusMonths(10),
                    expirationDate = refDate.plusDays(14),
                    isActive = true
                ),
                ClinicalMaterial(
                    id = MaterialId("mat-3"),
                    doctorId = previewDoctorPrimary.id,
                    name = "Ionómero de Vidrio Restaurador",
                    invimaRegistration = "INVIMA 2019DM-0003320",
                    batchLotNumber = "LOT-2024-C44",
                    manufacturingDate = refDate.minusYears(1),
                    expirationDate = refDate.plusMonths(6),
                    isActive = false
                )
            )
        ),
        MaterialsUiState(
            isLoading = false,
            selectedTab = MaterialTabFilter.ACTIVE,
            materials = listOf(
                ClinicalMaterial(
                    id = MaterialId("mat-overflow"),
                    doctorId = previewDoctorOverflow.id,
                    name = "Sistema Adhesivo Universal Fotopolimerizable de Octava Generación con Silano Integrado 5 ml",
                    invimaRegistration = "INVIMA 2024DM-0028910-R2-EXTENDIDO",
                    batchLotNumber = "LOT-2026-ODONT-00984-A",
                    manufacturingDate = refDate.minusYears(2),
                    expirationDate = refDate.minusDays(10),
                    isActive = true
                )
            )
        ),
        MaterialsUiState(
            isLoading = false,
            materials = emptyList(),
            selectedTab = MaterialTabFilter.ACTIVE
        ),
        MaterialsUiState(
            isLoading = false,
            errorMessage = "No se pudo sincronizar el catálogo regulatorio INVIMA desde el servidor."
        )
    )
}

class OdontogramStatePreviewProvider : PreviewParameterProvider<OdontogramState> {
    override val values: Sequence<OdontogramState> = sequenceOf(
        OdontogramState(isLoading = true),
        OdontogramState(
            isLoading = false,
            dentitionType = DentitionType.ADULT,
            teeth = mapOf(
                16 to Tooth(
                    number = 16,
                    surfaceStates = mapOf(
                        ToothSurface.OCLUSAL to ToothCondition.CARIES,
                        ToothSurface.MESIAL to ToothCondition.CALZA
                    )
                ),
                26 to Tooth(
                    number = 26,
                    surfaceStates = mapOf(ToothSurface.OCLUSAL to ToothCondition.TRATADO)
                ),
                46 to Tooth(
                    number = 46,
                    surfaceStates = mapOf(ToothSurface.VESTIBULAR to ToothCondition.CORONILLA)
                )
            )
        ),
        OdontogramState(
            isLoading = false,
            dentitionType = DentitionType.MIXED,
            teeth = mapOf(
                16 to Tooth(
                    number = 16,
                    surfaceStates = mapOf(
                        ToothSurface.OCLUSAL to ToothCondition.CARIES,
                        ToothSurface.VESTIBULAR to ToothCondition.DESGASTE,
                        ToothSurface.DISTAL to ToothCondition.CALZA
                    )
                ),
                55 to Tooth(
                    number = 55,
                    surfaceStates = mapOf(ToothSurface.OCLUSAL to ToothCondition.CORONILLA)
                ),
                85 to Tooth(
                    number = 85,
                    surfaceStates = mapOf(ToothSurface.MESIAL to ToothCondition.TRATADO)
                ),
                36 to Tooth(
                    number = 36,
                    surfaceStates = mapOf(ToothSurface.PALATINO to ToothCondition.CARIES)
                )
            )
        ),
        OdontogramState(
            isLoading = false,
            dentitionType = DentitionType.ADULT,
            teeth = emptyMap()
        ),
        OdontogramState(
            isLoading = false,
            dentitionType = DentitionType.ADULT,
            error = "No fue posible sincronizar el odontograma FDI del paciente."
        )
    )
}

class MyAppointmentsStatePreviewProvider : PreviewParameterProvider<MyAppointmentsUiState> {
    private val upcomingNormal = Appointment(
        id = "apt-pat-1",
        patientId = "pat-1",
        doctorId = previewDoctorPrimary.id.value,
        doctorName = previewDoctorPrimary.displayName,
        startTime = LocalDateTime.of(2026, 11, 24, 14, 30),
        endTime = LocalDateTime.of(2026, 11, 24, 15, 0),
        status = AppointmentStatus.CONFIRMED,
        isConfirmed = true,
        reason = "Limpieza dental y control de resinas"
    )

    private val completedSample = Appointment(
        id = "apt-pat-2",
        patientId = "pat-1",
        doctorId = previewDoctorPrimary.id.value,
        doctorName = previewDoctorPrimary.displayName,
        startTime = LocalDateTime.of(2026, 10, 12, 10, 0),
        status = AppointmentStatus.COMPLETED,
        isConfirmed = true,
        reason = "Valoración inicial"
    )

    private val overflowUpcoming = Appointment(
        id = "apt-pat-overflow",
        patientId = "pat-overflow-1",
        doctorId = previewDoctorOverflow.id.value,
        doctorName = previewDoctorOverflow.fullName,
        startTime = LocalDateTime.of(2026, 11, 28, 16, 0),
        endTime = LocalDateTime.of(2026, 11, 28, 17, 0),
        status = AppointmentStatus.SCHEDULED,
        isConfirmed = false,
        reason = "Evaluación integral post-operatoria de implante osteointegrado pieza 36 y ajuste de corona provisional"
    )

    private val doctorsDirectory = mapOf(
        previewDoctorPrimary.id.value to previewDoctorPrimary,
        previewDoctorOverflow.id.value to previewDoctorOverflow
    )

    override val values: Sequence<MyAppointmentsUiState> = sequenceOf(
        MyAppointmentsUiState(isLoading = true),
        MyAppointmentsUiState(
            selectedTab = AppointmentTab.UPCOMING,
            patientId = "pat-1",
            upcomingAppointments = listOf(upcomingNormal),
            completedAppointments = listOf(completedSample),
            doctorsMap = doctorsDirectory,
            isLoading = false
        ),
        MyAppointmentsUiState(
            selectedTab = AppointmentTab.UPCOMING,
            patientId = "pat-overflow-1",
            upcomingAppointments = listOf(overflowUpcoming),
            doctorsMap = doctorsDirectory,
            isLoading = false
        ),
        MyAppointmentsUiState(
            selectedTab = AppointmentTab.UPCOMING,
            patientId = "pat-1",
            upcomingAppointments = emptyList(),
            isLoading = false
        ),
        MyAppointmentsUiState(
            selectedTab = AppointmentTab.UPCOMING,
            patientId = "pat-1",
            isLoading = false,
            errorMessage = "No se pudieron cargar tus citas programadas. Verifica tu conexión."
        )
    )
}

class PatientHomeStatePreviewProvider : PreviewParameterProvider<PatientHomeUiState> {
    private val nextAppointment = Appointment(
        id = "apt-home-1",
        patientId = "pat-1",
        doctorId = previewDoctorPrimary.id.value,
        doctorName = previewDoctorPrimary.displayName,
        startTime = LocalDateTime.of(2026, 11, 25, 9, 0),
        status = AppointmentStatus.CONFIRMED,
        isConfirmed = true,
        reason = "Control de operatoria estética"
    )

    override val values: Sequence<PatientHomeUiState> = sequenceOf(
        PatientHomeUiState(isLoading = true),
        PatientHomeUiState(
            patientName = "Laura Martínez",
            patientId = "pat-1",
            nextAppointment = nextAppointment,
            upcomingAppointments = listOf(nextAppointment),
            pendingBalance = CopAmount(280_000L),
            completedAppointmentsCount = 4,
            doctors = listOf(previewDoctorPrimary),
            isLoading = false
        ),
        PatientHomeUiState(
            patientName = "Sebastián Alejandro Montoya Villavicencio",
            patientId = "pat-overflow-1",
            nextAppointment = nextAppointment.copy(
                id = "apt-home-overflow",
                reason = "Rehabilitación oral avanzada y ajuste oclusal sobre implante dental"
            ),
            upcomingAppointments = listOf(
                nextAppointment.copy(
                    id = "apt-home-overflow",
                    reason = "Rehabilitación oral avanzada y ajuste oclusal sobre implante dental"
                )
            ),
            pendingBalance = CopAmount(4_750_000L),
            completedAppointmentsCount = 19,
            doctors = listOf(previewDoctorOverflow),
            isLoading = false
        ),
        PatientHomeUiState(
            patientName = "Camila Herrera",
            patientId = "pat-2",
            nextAppointment = null,
            upcomingAppointments = emptyList(),
            pendingBalance = CopAmount.ZERO,
            completedAppointmentsCount = 0,
            doctors = emptyList(),
            isLoading = false
        ),
        PatientHomeUiState(
            patientName = "Laura Martínez",
            patientId = "pat-1",
            isLoading = false,
            errorMessage = "No se pudo actualizar tu resumen clínico. Reintenta en unos segundos."
        )
    )
}
