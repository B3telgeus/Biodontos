package com.example.biodontos.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.biodontos.BiodontosApp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.presentation.admission.ClinicalAdmissionScreen
import com.example.biodontos.presentation.admission.ClinicalAdmissionViewModel
import com.example.biodontos.presentation.admission.ClinicalAdmissionViewModelFactory
import com.example.biodontos.presentation.auth.AuthViewModelFactory
import com.example.biodontos.presentation.auth.doctor.DoctorRegistrationEvent
import com.example.biodontos.presentation.auth.doctor.DoctorRegistrationScreen
import com.example.biodontos.presentation.auth.doctor.DoctorRegistrationViewModel
import com.example.biodontos.presentation.auth.login.LoginEvent
import com.example.biodontos.presentation.auth.login.LoginScreen
import com.example.biodontos.presentation.auth.login.LoginViewModel
import com.example.biodontos.presentation.auth.onboarding.OnboardingScreen
import com.example.biodontos.presentation.auth.onboarding.UserTypeSelectionScreen
import com.example.biodontos.presentation.auth.patient.PatientSignUpEvent
import com.example.biodontos.presentation.auth.patient.PatientSignUpScreen
import com.example.biodontos.presentation.auth.patient.PatientSignUpViewModel
import com.example.biodontos.presentation.auth.patient.SignUpSuccessScreen
import com.example.biodontos.presentation.auth.reset.ForgotPasswordScreen
import com.example.biodontos.presentation.auth.reset.ForgotPasswordViewModel
import com.example.biodontos.presentation.auth.splash.SplashScreen
import com.example.biodontos.presentation.auth.splash.SplashViewModel
import com.example.biodontos.presentation.doctor.DoctorViewModelFactory
import com.example.biodontos.presentation.doctor.appointments.DoctorAppointmentsRoute
import com.example.biodontos.presentation.doctor.appointments.DoctorAppointmentsViewModel
import com.example.biodontos.presentation.doctor.documents.DoctorDocumentsRoute
import com.example.biodontos.presentation.doctor.documents.DoctorDocumentsScreen
import com.example.biodontos.presentation.doctor.documents.DoctorDocumentsViewModel
import com.example.biodontos.presentation.doctor.home.DoctorHomeRoute
import com.example.biodontos.presentation.doctor.home.DoctorHomeUiEvent
import com.example.biodontos.presentation.doctor.home.DoctorHomeViewModel
import com.example.biodontos.presentation.doctor.profile.DoctorProfileScreen
import com.example.biodontos.presentation.doctor.profile.DoctorProfileViewModel
import com.example.biodontos.presentation.doctor.profile.DoctorProfileViewModelFactory
import com.example.biodontos.presentation.doctor.shell.DoctorNavItem
import com.example.biodontos.presentation.doctor.shell.DoctorPortalShell
import com.example.biodontos.presentation.odontogram.OdontogramScreen
import com.example.biodontos.presentation.odontogram.OdontogramViewModel
import com.example.biodontos.presentation.odontogram.OdontogramViewModelFactory
import com.example.biodontos.presentation.patientportal.PatientPortalViewModelFactory
import com.example.biodontos.presentation.patientportal.appointments.AppointmentConfirmationScreen
import com.example.biodontos.presentation.patientportal.appointments.BookAppointmentRoute
import com.example.biodontos.presentation.patientportal.appointments.BookAppointmentViewModel
import com.example.biodontos.presentation.patientportal.appointments.MyAppointmentsRoute

import com.example.biodontos.presentation.patientportal.appointments.MyAppointmentsViewModel
import com.example.biodontos.presentation.patientportal.doctors.DoctorDetailRoute
import com.example.biodontos.presentation.patientportal.doctors.DoctorDetailViewModel
import com.example.biodontos.presentation.patientportal.doctors.DoctorSearchRoute
import com.example.biodontos.presentation.patientportal.doctors.DoctorSearchViewModel
import com.example.biodontos.presentation.patientportal.home.PatientHomeRoute
import com.example.biodontos.presentation.patientportal.home.PatientHomeUiEvent
import com.example.biodontos.presentation.patientportal.home.PatientHomeViewModel
import com.example.biodontos.presentation.patientportal.payments.PatientPaymentsRoute
import com.example.biodontos.presentation.patientportal.payments.PatientPaymentsViewModel
import com.example.biodontos.presentation.patientportal.shell.PatientNavItem
import com.example.biodontos.presentation.patientportal.shell.PatientPortalShell
import com.example.biodontos.presentation.patients.PatientListScreen
import com.example.biodontos.presentation.patients.PatientListViewModel
import com.example.biodontos.presentation.patients.PatientRegistrationEvent
import com.example.biodontos.presentation.patients.PatientRegistrationScreen
import com.example.biodontos.presentation.patients.PatientRegistrationViewModel
import com.example.biodontos.presentation.patients.PatientsViewModelFactory

@Composable
fun BiodontosNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Splash
    ) {
        // --- SPLASH SCREEN (INICIO CON AUTO-LOGIN) ---
        composable<Splash> {
            val viewModel: SplashViewModel = viewModel(factory = AuthViewModelFactory.SplashFactory)
            SplashScreen(
                viewModel = viewModel,
                onNavigateToDoctor = {
                    navController.navigate(DoctorHome) {
                        popUpTo(Splash) { inclusive = true }
                    }
                },
                onNavigateToPatient = {
                    navController.navigate(PatientHome) {
                        popUpTo(Splash) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Onboarding) {
                        popUpTo(Splash) { inclusive = true }
                    }
                }
            )
        }

        // --- GATEWAY DE SESIÓN SEGÚN ROL (COMPATIBILIDAD) ---
        composable<SessionGate> {
            val context = LocalContext.current
            val app = context.applicationContext as BiodontosApp
            LaunchedEffect(Unit) {
                val session = app.container.getCurrentSessionUseCase()
                when (session?.role) {
                    UserRole.DOCTOR -> {
                        navController.navigate(DoctorHome) {
                            popUpTo(SessionGate) { inclusive = true }
                        }
                    }
                    UserRole.PATIENT -> {
                        navController.navigate(PatientHome) {
                            popUpTo(SessionGate) { inclusive = true }
                        }
                    }
                    else -> {
                        navController.navigate(Onboarding) {
                            popUpTo(SessionGate) { inclusive = true }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BiodontosDark)
            }
        }

        // --- ONBOARDING & SELECCIÓN ---
        composable<Onboarding> {
            OnboardingScreen(
                onLoginClick = { navController.navigate(Login) },
                onCreateAccountClick = { navController.navigate(UserTypeSelection) }
            )
        }

        composable<UserTypeSelection> {
            UserTypeSelectionScreen(
                onDoctorClick = { navController.navigate(DoctorRegistration) },
                onPatientClick = { navController.navigate(PatientSignUp) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- LOGIN ---
        composable<Login> {
            val viewModel: LoginViewModel = viewModel(factory = AuthViewModelFactory.LoginFactory)
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is LoginEvent.LoginSuccess -> {
                            if (event.session.role == UserRole.PATIENT) {
                                navController.navigate(PatientHome) {
                                    popUpTo(Login) { inclusive = true }
                                }
                            } else {
                                navController.navigate(DoctorHome) {
                                    popUpTo(Login) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            }

            LoginScreen(
                state = state,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                onLoginClick = viewModel::login,
                onForgotPasswordClick = { navController.navigate(ForgotPassword) },
                onSignUpClick = { navController.navigate(UserTypeSelection) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- REGISTRO DE DOCTOR ---
        composable<DoctorRegistration> {
            val viewModel: DoctorRegistrationViewModel = viewModel(factory = AuthViewModelFactory.DoctorRegistrationFactory)
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is DoctorRegistrationEvent.RegisteredSuccess -> {
                            navController.navigate(DoctorHome) {
                                popUpTo(DoctorRegistration) { inclusive = true }
                            }
                        }
                    }
                }
            }

            DoctorRegistrationScreen(
                state = state,
                onFirstNameChange = viewModel::onFirstNameChange,
                onLastNameChange = viewModel::onLastNameChange,
                onLicenseNumberChange = viewModel::onLicenseNumberChange,
                onSpecialtyChange = viewModel::onSpecialtyChange,
                onEmailChange = viewModel::onEmailChange,
                onPhoneChange = viewModel::onPhoneChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                onNextClick = viewModel::nextStep,
                onBackClick = { viewModel.previousStep { navController.popBackStack() } }
            )
        }

        // --- REGISTRO DE PACIENTE (AUTH) ---
        composable<PatientSignUp> {
            val viewModel: PatientSignUpViewModel = viewModel(factory = AuthViewModelFactory.PatientSignUpFactory)
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is PatientSignUpEvent.RegisteredSuccess -> {
                            navController.navigate(SignUpSuccess) {
                                popUpTo(PatientSignUp) { inclusive = true }
                            }
                        }
                    }
                }
            }

            PatientSignUpScreen(
                state = state,
                onFirstNameChange = viewModel::onFirstNameChange,
                onLastNameChange = viewModel::onLastNameChange,
                onDniChange = viewModel::onDniChange,
                onEmailChange = viewModel::onEmailChange,
                onPhoneChange = viewModel::onPhoneChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onTermsAcceptedChange = viewModel::onTermsAcceptedChange,
                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                onNextClick = viewModel::nextStep,
                onBackClick = { viewModel.previousStep { navController.popBackStack() } }
            )
        }

        // --- CONFIRMACIÓN DE REGISTRO EXITOSO (FIGMA) ---
        composable<SignUpSuccess> {
            SignUpSuccessScreen(
                onLoginClick = {
                    navController.navigate(Login) {
                        popUpTo(SignUpSuccess) { inclusive = true }
                    }
                }
            )
        }

        // --- RESETEO DE CONTRASEÑA ---
        composable<ForgotPassword> {
            val viewModel: ForgotPasswordViewModel = viewModel(factory = AuthViewModelFactory.ForgotPasswordFactory)
            val state by viewModel.state.collectAsStateWithLifecycle()

            ForgotPasswordScreen(
                state = state,
                onEmailChange = viewModel::onEmailChange,
                onSubmitClick = viewModel::submit,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- PORTAL DE PACIENTES ---
        composable<PatientHome> {
            val viewModel: PatientHomeViewModel = viewModel(factory = PatientPortalViewModelFactory.HomeFactory)
            val isExpanded = LocalConfiguration.current.screenWidthDp >= 600
            PatientPortalShell(
                selectedItem = PatientNavItem.HOME,
                onItemSelected = { item ->
                    when (item) {
                        PatientNavItem.HOME -> {}
                        PatientNavItem.APPOINTMENTS -> navController.navigate(MyAppointments)
                        PatientNavItem.DOCTORS -> navController.navigate(DoctorSearch)
                        PatientNavItem.PAYMENTS -> navController.navigate(PatientPayments)
                    }
                },
                onLogoutClick = {
                    viewModel.onEvent(PatientHomeUiEvent.Logout)
                }
            ) {
                PatientHomeRoute(
                    viewModel = viewModel,
                    onNavigateToAppointments = { navController.navigate(MyAppointments) },
                    onNavigateToPayments = { navController.navigate(PatientPayments) },
                    onNavigateToDoctors = { navController.navigate(DoctorSearch) },
                    onNavigateToBooking = { doctorId -> navController.navigate(BookAppointment(doctorId)) },
                    onNavigateToLogin = {
                        navController.navigate(Login) {
                            popUpTo(PatientHome) { inclusive = true }
                        }
                    },
                    isExpanded = isExpanded
                )
            }
        }

        // --- BÚSQUEDA DE DOCTORES (PACIENTE) ---
        composable<DoctorSearch> {
            val viewModel: DoctorSearchViewModel = viewModel(factory = PatientPortalViewModelFactory.DoctorSearchFactory)
            PatientPortalShell(
                selectedItem = PatientNavItem.DOCTORS,
                onItemSelected = { item ->
                    when (item) {
                        PatientNavItem.HOME -> navController.navigate(PatientHome)
                        PatientNavItem.APPOINTMENTS -> navController.navigate(MyAppointments)
                        PatientNavItem.DOCTORS -> {}
                        PatientNavItem.PAYMENTS -> navController.navigate(PatientPayments)
                    }
                },
                onLogoutClick = {
                    navController.navigate(Login) {
                        popUpTo(DoctorSearch) { inclusive = true }
                    }
                }
            ) {
                DoctorSearchRoute(
                    viewModel = viewModel,
                    onNavigateToDetail = { doctorId -> navController.navigate(DoctorDetail(doctorId)) },
                    onNavigateToBooking = { doctorId -> navController.navigate(BookAppointment(doctorId)) }
                )
            }
        }

        // --- DETALLE DEL DOCTOR ---
        composable<DoctorDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<DoctorDetail>()
            val viewModel: DoctorDetailViewModel = viewModel(
                factory = PatientPortalViewModelFactory.createDoctorDetailFactory(route.doctorId)
            )
            DoctorDetailRoute(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBooking = { doctorId -> navController.navigate(BookAppointment(doctorId)) }
            )
        }

        // --- MIS CITAS (PACIENTE) ---
        composable<MyAppointments> {
            val viewModel: MyAppointmentsViewModel = viewModel(factory = PatientPortalViewModelFactory.MyAppointmentsFactory)
            PatientPortalShell(
                selectedItem = PatientNavItem.APPOINTMENTS,
                onItemSelected = { item ->
                    when (item) {
                        PatientNavItem.HOME -> navController.navigate(PatientHome)
                        PatientNavItem.APPOINTMENTS -> {}
                        PatientNavItem.DOCTORS -> navController.navigate(DoctorSearch)
                        PatientNavItem.PAYMENTS -> navController.navigate(PatientPayments)
                    }
                },
                onLogoutClick = {
                    navController.navigate(Login) {
                        popUpTo(MyAppointments) { inclusive = true }
                    }
                }
            ) {
                MyAppointmentsRoute(
                    viewModel = viewModel,
                    onNavigateToReschedule = { doctorId, apptId ->
                        navController.navigate(BookAppointment(doctorId = doctorId, rescheduleAppointmentId = apptId))
                    },
                    onNavigateToBookNew = {
                        navController.navigate(DoctorSearch)
                    }
                )
            }
        }

        // --- AGENDAR / REAGENDAR CITA ---
        composable<BookAppointment> { backStackEntry ->
            val route = backStackEntry.toRoute<BookAppointment>()
            val viewModel: BookAppointmentViewModel = viewModel(
                factory = PatientPortalViewModelFactory.createBookAppointmentFactory(
                    doctorId = route.doctorId,
                    rescheduleAppointmentId = route.rescheduleAppointmentId
                )
            )
            BookAppointmentRoute(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConfirmation = { confirmation ->
                    navController.navigate(confirmation) {
                        popUpTo(BookAppointment(doctorId = route.doctorId, rescheduleAppointmentId = route.rescheduleAppointmentId)) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToMyAppointments = {
                    navController.navigate(MyAppointments) {
                        popUpTo(BookAppointment(doctorId = route.doctorId, rescheduleAppointmentId = route.rescheduleAppointmentId)) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // --- RESUMEN Y CONFIRMACIÓN DE CITA (PACIENTE) ---
        composable<AppointmentConfirmation> { backStackEntry ->
            val route = backStackEntry.toRoute<AppointmentConfirmation>()
            AppointmentConfirmationScreen(
                confirmation = route,
                onNavigateToMyAppointments = {
                    navController.navigate(MyAppointments) {
                        popUpTo(PatientHome) { inclusive = false }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(PatientHome) {
                        popUpTo(PatientHome) { inclusive = true }
                    }
                }
            )
        }


        // --- PAGOS Y ABONOS (PACIENTE) ---
        composable<PatientPayments> {
            val viewModel: PatientPaymentsViewModel = viewModel(factory = PatientPortalViewModelFactory.PaymentsFactory)
            PatientPortalShell(
                selectedItem = PatientNavItem.PAYMENTS,
                onItemSelected = { item ->
                    when (item) {
                        PatientNavItem.HOME -> navController.navigate(PatientHome)
                        PatientNavItem.APPOINTMENTS -> navController.navigate(MyAppointments)
                        PatientNavItem.DOCTORS -> navController.navigate(DoctorSearch)
                        PatientNavItem.PAYMENTS -> {}
                    }
                },
                onLogoutClick = {
                    navController.navigate(Login) {
                        popUpTo(PatientPayments) { inclusive = true }
                    }
                }
            ) {
                PatientPaymentsRoute(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // --- PORTAL DE DOCTORES (FIGMA 08_Home_Doctor.png) ---
        composable<DoctorHome> {
            val viewModel: DoctorHomeViewModel = viewModel(factory = DoctorViewModelFactory.HomeFactory)
            DoctorPortalShell(
                selectedItem = DoctorNavItem.HOME,
                onItemSelected = { item ->
                    when (item) {
                        DoctorNavItem.HOME -> {}
                        DoctorNavItem.PATIENTS -> navController.navigate(PatientList)
                        DoctorNavItem.DOCUMENTS -> navController.navigate(DoctorDocuments)
                        DoctorNavItem.APPOINTMENTS -> navController.navigate(DoctorAppointments)
                    }
                },
                onLogoutClick = {
                    viewModel.onEvent(DoctorHomeUiEvent.Logout)
                }
            ) {
                DoctorHomeRoute(
                    viewModel = viewModel,
                    onNavigateToPatients = { navController.navigate(PatientList) },
                    onNavigateToNewPatient = { navController.navigate(ClinicalAdmission()) },
                    onNavigateToAppointments = { navController.navigate(DoctorAppointments) },
                    onNavigateToDocuments = { navController.navigate(DoctorDocuments) },
                    onNavigateToClinicalAdmission = { navController.navigate(ClinicalAdmission()) },
                    onNavigateToProfile = { navController.navigate(DoctorProfile) },
                    onNavigateToPatientDetail = { patientId ->
                        navController.navigate(PatientDetail(patientId = patientId, initialTab = "HISTORY"))
                    },
                    onNavigateToLogin = {
                        navController.navigate(Login) {
                            popUpTo(DoctorHome) { inclusive = true }
                        }
                    }
                )
            }
        }

        // --- LISTA DE PACIENTES (DOCTOR) ---
        composable<PatientList> {
            val viewModel: PatientListViewModel = viewModel(factory = PatientsViewModelFactory.ListFactory)
            val state by viewModel.state.collectAsStateWithLifecycle()

            DoctorPortalShell(
                selectedItem = DoctorNavItem.PATIENTS,
                onItemSelected = { item ->
                    when (item) {
                        DoctorNavItem.HOME -> navController.navigate(DoctorHome)
                        DoctorNavItem.PATIENTS -> {}
                        DoctorNavItem.DOCUMENTS -> navController.navigate(DoctorDocuments)
                        DoctorNavItem.APPOINTMENTS -> navController.navigate(DoctorAppointments)
                    }
                },
                onLogoutClick = {
                    navController.navigate(Login) {
                        popUpTo(PatientList) { inclusive = true }
                    }
                }
            ) {
                PatientListScreen(
                    state = state,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
onFilterSelected = viewModel::onFilterSelected,
                    onRefresh = viewModel::refresh,
                    onAddPatientClick = { navController.navigate(ClinicalAdmission()) },
                    onClinicalAdmissionClick = { navController.navigate(ClinicalAdmission()) },
                    onPatientClick = { patient ->
                        navController.navigate(PatientDetail(patientId = patient.id, initialTab = "SUMMARY"))
                    },
                    onDoctorProfileClick = { navController.navigate(DoctorProfile) },
                    onRetry = { viewModel.loadPatients() }
                )
            }
        }

        // --- DETALLE / HUB CLÍNICO DEL PACIENTE ---
        composable<PatientDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<PatientDetail>()
            val viewModel: com.example.biodontos.presentation.patients.detail.PatientDetailViewModel = viewModel(
                factory = PatientsViewModelFactory.createPatientDetailFactory(route.patientId, route.initialTab)
            )
            LaunchedEffect(route.initialTab) {
                viewModel.selectTab(
                    com.example.biodontos.presentation.patients.detail.PatientDetailViewModel.parseTab(route.initialTab)
                )
            }
            val state by viewModel.state.collectAsStateWithLifecycle()

            com.example.biodontos.presentation.patients.detail.PatientDetailScreen(
                state = state,
                onTabSelected = viewModel::selectTab,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOdontogram = { navController.navigate(Odontogram(route.patientId)) },
                onNavigateToAdmission = { navController.navigate(ClinicalAdmission(route.patientId)) },
                onOpenNewEvolutionDialog = viewModel::openNewEvolutionDialog,
                onDismissNewEvolutionDialog = viewModel::dismissNewEvolutionDialog,
                onEvolutionDescriptionChange = viewModel::onNewEvolutionDescriptionChange,
                onEvolutionToothChange = viewModel::onNewEvolutionToothChange,
                onEvolutionCostChange = viewModel::onNewEvolutionCostChange,
                onEvolutionPaymentChange = viewModel::onNewEvolutionPaymentChange,
                onSelectCatalogMaterial = viewModel::onSelectCatalogMaterial,
                onAddMaterial = viewModel::addMaterialToEvolution,
                onRemoveMaterial = viewModel::removeMaterialFromEvolution,
                onSaveEvolution = viewModel::saveEvolution,
                onOpenNewAppointmentDialog = viewModel::openNewAppointmentDialog,
                onDismissNewAppointmentDialog = viewModel::dismissNewAppointmentDialog,
                onAppointmentDateChange = viewModel::onNewAppointmentDateChange,
                onAppointmentTimeChange = viewModel::onNewAppointmentTimeChange,
                onAppointmentReasonChange = viewModel::onNewAppointmentReasonChange,
                onSaveAppointment = viewModel::saveAppointment,
                onClearMessages = viewModel::clearMessages,
                onSelectMonthFilter = viewModel::selectMonthFilter,
                onToggleActivityTypeFilter = viewModel::toggleActivityTypeFilter,
                onClearHistoryFilters = viewModel::clearHistoryFilters,
                onToggleExpandActivity = viewModel::toggleExpandActivity,
                onOpenQuickActionSheet = viewModel::openQuickActionSheet,
                onDismissQuickActionSheet = viewModel::dismissQuickActionSheet,
                onRequestVoidActivity = viewModel::requestVoidActivity,
                onVoidReasonChange = viewModel::onVoidReasonChange,
                onAdvanceVoidStep = viewModel::advanceVoidConfirmationStep,
                onConfirmVoidActivity = viewModel::confirmVoidActivity,
                onDismissVoidDialog = viewModel::dismissVoidDialog,
                onOpenClarificationDialog = viewModel::openClarificationDialog,
                onClarificationTextChange = viewModel::onClarificationTextChange,
                onSaveClarification = viewModel::saveClarification,
                onDismissClarificationDialog = viewModel::dismissClarificationDialog,
                onOpenTreatmentStatusDialog = viewModel::openTreatmentStatusDialog,
                onSelectTargetTreatmentStatus = viewModel::onSelectTargetTreatmentStatus,
                onTreatmentStatusReasonChange = viewModel::onTreatmentStatusReasonChange,
                onConfirmTreatmentStatusUpdate = viewModel::confirmTreatmentStatusUpdate,
                onDismissTreatmentStatusDialog = viewModel::dismissTreatmentStatusDialog,
                onOpenNewTreatmentDialog = viewModel::openNewTreatmentDialog,
                onDismissNewTreatmentDialog = viewModel::dismissNewTreatmentDialog,
                onNewTreatmentDescriptionChange = viewModel::onNewTreatmentDescriptionChange,
                onNewTreatmentTotalCostChange = viewModel::onNewTreatmentTotalCostChange,
                onSaveTreatment = viewModel::saveTreatment,
                onOpenNewPaymentDialog = viewModel::openNewPaymentDialog,
                onDismissNewPaymentDialog = viewModel::dismissNewPaymentDialog,
                onSelectPaymentTreatment = viewModel::onSelectPaymentTreatment,
                onNewPaymentAmountChange = viewModel::onNewPaymentAmountChange,
                onNewPaymentMethodChange = viewModel::onNewPaymentMethodChange,
                onSavePayment = viewModel::savePayment,
                onRefreshTimeline = viewModel::refreshTimeline
            )
        }

        // --- AGENDA DE CITAS (DOCTOR) ---
        composable<DoctorAppointments> {
            val viewModel: DoctorAppointmentsViewModel = viewModel(factory = DoctorViewModelFactory.AppointmentsFactory)
            DoctorPortalShell(
                selectedItem = DoctorNavItem.APPOINTMENTS,
                onItemSelected = { item ->
                    when (item) {
                        DoctorNavItem.HOME -> navController.navigate(DoctorHome)
                        DoctorNavItem.PATIENTS -> navController.navigate(PatientList)
                        DoctorNavItem.DOCUMENTS -> navController.navigate(DoctorDocuments)
                        DoctorNavItem.APPOINTMENTS -> {}
                    }
                },
                onLogoutClick = {
                    navController.navigate(Login) {
                        popUpTo(DoctorAppointments) { inclusive = true }
                    }
                }
            ) {
                DoctorAppointmentsRoute(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onPatientClick = { patientId ->
                        navController.navigate(PatientDetail(patientId = patientId, initialTab = "HISTORY"))
                    }
                )
            }
        }

        // --- DOCUMENTOS E HISTORIAS CLÍNICAS (DOCTOR) ---
        composable<DoctorDocuments> {
            val viewModel: DoctorDocumentsViewModel = viewModel(factory = DoctorViewModelFactory.DocumentsFactory)
            DoctorPortalShell(
                selectedItem = DoctorNavItem.DOCUMENTS,
                onItemSelected = { item ->
                    when (item) {
                        DoctorNavItem.HOME -> navController.navigate(DoctorHome)
                        DoctorNavItem.PATIENTS -> navController.navigate(PatientList)
                        DoctorNavItem.DOCUMENTS -> {}
                        DoctorNavItem.APPOINTMENTS -> navController.navigate(DoctorAppointments)
                    }
                },
                onLogoutClick = {
                    navController.navigate(Login) {
                        popUpTo(DoctorDocuments) { inclusive = true }
                    }
                }
            ) {
                DoctorDocumentsRoute(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPatientDetail = { patientId, initialTab ->
                        navController.navigate(PatientDetail(patientId = patientId, initialTab = initialTab))
                    },
                    onNavigateToAdmission = { patientId ->
                        navController.navigate(ClinicalAdmission(patientId = patientId))
                    }
                )
            }
        }

        // --- REGISTRO DE PACIENTE ---
        composable<PatientRegistration> {
            val viewModel: PatientRegistrationViewModel = viewModel(factory = PatientsViewModelFactory.RegistrationFactory)
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        PatientRegistrationEvent.SavedSuccessfully -> navController.popBackStack()
                    }
                }
            }

            PatientRegistrationScreen(
                state = state,
                onDniChange = viewModel::onDniChange,
                onFirstNameChange = viewModel::onFirstNameChange,
                onLastNameChange = viewModel::onLastNameChange,
                onPhoneChange = viewModel::onPhoneChange,
                onConsentChange = viewModel::onConsentChange,
                onAllergiesChange = viewModel::onAllergiesChange,
                onAllergiesDescriptionChange = viewModel::onAllergiesDescriptionChange,
                onSaveClick = viewModel::savePatient,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- ODONTOGRAMA ---
        composable<Odontogram> { backStackEntry ->
            val route: Odontogram = backStackEntry.toRoute()
            val viewModel: OdontogramViewModel = viewModel(
                factory = OdontogramViewModelFactory(route.patientId)
            )
            val state by viewModel.state.collectAsStateWithLifecycle()

            OdontogramScreen(
                state = state,
                onDentitionTypeChange = viewModel::setDentitionType,
                onSurfaceClick = viewModel::onSurfaceClick,
                onCommentChange = viewModel::onCommentChange,
                onUpdateCondition = viewModel::updateTooth,
                onDismissDialog = viewModel::onDismissDialog,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- INGRESO CLÍNICO ---
        composable<ClinicalAdmission> { backStackEntry ->
            val route = backStackEntry.toRoute<ClinicalAdmission>()
            val context = LocalContext.current
            val app = context.applicationContext as BiodontosApp
            val viewModel: ClinicalAdmissionViewModel = viewModel(
                factory = ClinicalAdmissionViewModelFactory.createFactory(app.container, route.patientId)
            )

            ClinicalAdmissionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onAdmissionFinished = {
                    navController.navigate(PatientList) {
                        popUpTo(PatientList) { inclusive = true }
                    }
                }
            )
        }

        // --- PERFIL Y AGENDA DEL DOCTOR ---
        composable<DoctorProfile> {
            val viewModel: DoctorProfileViewModel = viewModel(factory = DoctorProfileViewModelFactory.Factory)
            DoctorProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Login) {
                        popUpTo(DoctorProfile) { inclusive = true }
                    }
                }
            )
        }
    }
}

