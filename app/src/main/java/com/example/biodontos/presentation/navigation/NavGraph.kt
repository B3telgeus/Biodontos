package com.example.biodontos.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.biodontos.presentation.odontogram.*
import com.example.biodontos.presentation.patientportal.PatientPortalViewModelFactory
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
import com.example.biodontos.presentation.patients.*

@Composable
fun BiodontosNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = SessionGate
    ) {
        // --- GATEWAY DE SESIÓN SEGÚN ROL ---
        composable<SessionGate> {
            val context = LocalContext.current
            val app = context.applicationContext as BiodontosApp
            LaunchedEffect(Unit) {
                val session = app.container.getCurrentSessionUseCase()
                when (session?.role) {
                    UserRole.DOCTOR -> {
                        navController.navigate(PatientList) {
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
                            navController.navigate(SessionGate) {
                                popUpTo(Login) { inclusive = true }
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
                            navController.navigate(PatientList) {
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
                    }
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
                onNavigateToMyAppointments = {
                    navController.navigate(MyAppointments) {
                        popUpTo(BookAppointment(doctorId = route.doctorId)) { inclusive = true }
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

        // --- PACIENTES Y CLÍNICA DEL DOCTOR (EXISTENTE) ---
        composable<PatientList> {
            val viewModel: PatientListViewModel = viewModel(factory = PatientsViewModelFactory.ListFactory)
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.loadPatients()
            }

            PatientListScreen(
                state = state,
                onAddPatientClick = { navController.navigate(PatientRegistration) },
                onClinicalAdmissionClick = { navController.navigate(ClinicalAdmission()) },
                onPatientClick = { patient ->
                    navController.navigate(Odontogram(patientId = patient.id))
                },
                onRetry = { viewModel.loadPatients() }
            )
        }

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

        composable<Odontogram> { backStackEntry ->
            val route: Odontogram = backStackEntry.toRoute()
            val viewModel: OdontogramViewModel = viewModel(
                factory = OdontogramViewModelFactory(route.patientId)
            )
            val state by viewModel.state.collectAsStateWithLifecycle()

            OdontogramScreen(
                state = state,
                onSurfaceClick = viewModel::onSurfaceClick,
                onCommentChange = viewModel::onCommentChange,
                onUpdateCondition = viewModel::updateTooth,
                onDismissDialog = viewModel::onDismissDialog,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ClinicalAdmission> {
            val viewModel: ClinicalAdmissionViewModel = viewModel(factory = ClinicalAdmissionViewModelFactory.Factory)

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
    }
}
