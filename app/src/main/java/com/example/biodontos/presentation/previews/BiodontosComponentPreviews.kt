package com.example.biodontos.presentation.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.example.biodontos.core.designsystem.BiodontosPreviews
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.presentation.auth.login.LoginScreen
import com.example.biodontos.presentation.auth.login.LoginState
import com.example.biodontos.presentation.doctor.appointments.DoctorAppointmentsScreen
import com.example.biodontos.presentation.doctor.appointments.DoctorAppointmentsUiState
import com.example.biodontos.presentation.doctor.home.DoctorHomeScreen
import com.example.biodontos.presentation.doctor.home.DoctorHomeUiState
import com.example.biodontos.presentation.doctor.materials.MaterialsScreen
import com.example.biodontos.presentation.doctor.materials.MaterialsUiState
import com.example.biodontos.presentation.odontogram.OdontogramScreen
import com.example.biodontos.presentation.odontogram.OdontogramState
import com.example.biodontos.presentation.patientportal.appointments.MyAppointmentsScreen
import com.example.biodontos.presentation.patientportal.appointments.MyAppointmentsUiState
import com.example.biodontos.presentation.patientportal.home.PatientHomeScreen
import com.example.biodontos.presentation.patientportal.home.PatientHomeUiState
import com.example.biodontos.presentation.patients.PatientListScreen
import com.example.biodontos.presentation.patients.PatientListState

@BiodontosPreviews
@Preview(name = "width360", widthDp = 360, showBackground = true)
@Composable
private fun LoginScreenComponentPreview(
    @PreviewParameter(LoginStatePreviewProvider::class) state: LoginState
) {
    DentalTheme {
        LoginScreen(
            state = state,
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onSignUpClick = {},
            onBackClick = {}
        )
    }
}

@BiodontosPreviews
@Preview(name = "width360", widthDp = 360, showBackground = true)
@Composable
private fun PatientListScreenComponentPreview(
    @PreviewParameter(PatientListStatePreviewProvider::class) state: PatientListState
) {
    DentalTheme {
        PatientListScreen(
            state = state,
            onSearchQueryChange = {},
            onFilterSelected = {},
            onRefresh = {},
            onAddPatientClick = {},
            onClinicalAdmissionClick = {},
            onPatientClick = {},
            onDoctorProfileClick = {},
            onRetry = {}
        )
    }
}

@BiodontosPreviews
@Preview(name = "width360", widthDp = 360, showBackground = true)
@Composable
private fun DoctorHomeScreenComponentPreview(
    @PreviewParameter(DoctorHomeStatePreviewProvider::class) state: DoctorHomeUiState
) {
    DentalTheme {
        DoctorHomeScreen(
            state = state,
            onEvent = {},
            onNavigateToPatients = {},
            onNavigateToNewPatient = {},
            onNavigateToAppointments = {},
            onNavigateToDocuments = {}
        )
    }
}

@BiodontosPreviews
@Preview(name = "width360", widthDp = 360, showBackground = true)
@Composable
private fun DoctorAppointmentsScreenComponentPreview(
    @PreviewParameter(DoctorAppointmentsStatePreviewProvider::class) state: DoctorAppointmentsUiState
) {
    DentalTheme {
        DoctorAppointmentsScreen(
            state = state,
            today = PreviewReferenceDate,
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@BiodontosPreviews
@Preview(name = "width360", widthDp = 360, showBackground = true)
@Composable
private fun MaterialsScreenComponentPreview(
    @PreviewParameter(MaterialsUiStatePreviewProvider::class) state: MaterialsUiState
) {
    DentalTheme {
        MaterialsScreen(
            state = state,
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@BiodontosPreviews
@Preview(name = "width360", widthDp = 360, showBackground = true)
@Composable
private fun OdontogramScreenComponentPreview(
    @PreviewParameter(OdontogramStatePreviewProvider::class) state: OdontogramState
) {
    DentalTheme {
        OdontogramScreen(
            state = state,
            onDentitionTypeChange = {},
            onSurfaceClick = { _, _ -> },
            onCommentChange = {},
            onUpdateCondition = {},
            onDismissDialog = {},
            onBackClick = {}
        )
    }
}

@BiodontosPreviews
@Preview(name = "width360", widthDp = 360, showBackground = true)
@Composable
private fun MyAppointmentsScreenComponentPreview(
    @PreviewParameter(MyAppointmentsStatePreviewProvider::class) state: MyAppointmentsUiState
) {
    DentalTheme {
        MyAppointmentsScreen(
            state = state,
            onEvent = {}
        )
    }
}

@BiodontosPreviews
@Preview(name = "width360", widthDp = 360, showBackground = true)
@Composable
private fun PatientHomeScreenComponentPreview(
    @PreviewParameter(PatientHomeStatePreviewProvider::class) state: PatientHomeUiState
) {
    DentalTheme {
        PatientHomeScreen(
            state = state,
            onEvent = {},
            isExpanded = false
        )
    }
}
