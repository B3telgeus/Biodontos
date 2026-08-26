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
import com.example.biodontos.presentation.odontogram.*
import com.example.biodontos.presentation.patients.*

@Composable
fun BiodontosNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = PatientList
    ) {
        composable<PatientList> {
            val viewModel: PatientListViewModel = viewModel(factory = PatientsViewModelFactory.ListFactory)
            val state by viewModel.state.collectAsStateWithLifecycle()
            
            LaunchedEffect(Unit) {
                viewModel.loadPatients()
            }

            PatientListScreen(
                state = state,
                onAddPatientClick = { navController.navigate(PatientRegistration) },
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
                onConsentChange = viewModel::onConsentChange,
                onAllergiesChange = viewModel::onAllergiesChange,
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
    }
}
