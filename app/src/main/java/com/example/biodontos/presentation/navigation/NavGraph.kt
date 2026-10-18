package com.example.biodontos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
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
import com.example.biodontos.presentation.auth.reset.ForgotPasswordScreen
import com.example.biodontos.presentation.auth.reset.ForgotPasswordViewModel
import com.example.biodontos.presentation.admission.ClinicalAdmissionScreen
import com.example.biodontos.presentation.admission.ClinicalAdmissionViewModel
import com.example.biodontos.presentation.admission.ClinicalAdmissionViewModelFactory
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.presentation.patient.home.PatientHomeScreen
import com.example.biodontos.presentation.patient.home.PatientHomeViewModel
import com.example.biodontos.presentation.patient.home.PatientHomeViewModelFactory
import com.example.biodontos.presentation.doctor.profile.DoctorProfileScreen
import com.example.biodontos.presentation.doctor.profile.DoctorProfileViewModel
import com.example.biodontos.presentation.auth.splash.SplashScreen
import com.example.biodontos.presentation.auth.splash.SplashViewModel
import com.example.biodontos.presentation.doctor.profile.DoctorProfileViewModelFactory
import com.example.biodontos.presentation.odontogram.*
import com.example.biodontos.presentation.patients.*

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
                    navController.navigate(PatientList) {
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
                                    popUpTo(Onboarding) { inclusive = true }
                                }
                            } else {
                                navController.navigate(PatientList) {
                                    popUpTo(Onboarding) { inclusive = true }
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
                            navController.navigate(PatientList) {
                                popUpTo(Onboarding) { inclusive = true }
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
                            navController.navigate(PatientHome) {
                                popUpTo(Onboarding) { inclusive = true }
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

        // --- PACIENTES Y CLÍNICA (EXISTENTE) ---
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
                onDoctorProfileClick = { navController.navigate(DoctorProfile) },
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

        // --- PROCESO COMPLETO DE INGRESO Y ADMISIÓN CLÍNICA ---
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

        // --- HOMESCREEN DEL PACIENTE ---
        composable<PatientHome> {
            val viewModel: PatientHomeViewModel = viewModel(factory = PatientHomeViewModelFactory.Factory)
            PatientHomeScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navController.navigate(Login) {
                        popUpTo(PatientHome) { inclusive = true }
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
