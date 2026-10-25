package com.example.biodontos.presentation.doctor.documents

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorRed
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.DentalTheme

import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.biodontos.BiodontosApp
import com.example.biodontos.domain.model.Patient

data class DoctorDocumentItem(
    val patientId: String,
    val title: String,
    val patientName: String,
    val date: String,
    val status: String
)

@Composable
fun DoctorDocumentsScreen(
    onNavigateBack: () -> Unit,
    onPatientClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as BiodontosApp
    var patients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        app.container.getPatientsUseCase()
            .onSuccess { list ->
                patients = list
            }
            .onFailure { error ->
                errorMessage = error.localizedMessage ?: "No se pudieron cargar los expedientes clínicos."
            }
        isLoading = false
    }

    DoctorDocumentsContent(
        patients = patients,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onNavigateBack = onNavigateBack,
        onPatientClick = onPatientClick,
        modifier = modifier
    )
}

@VisibleForTesting
@Composable
fun DoctorDocumentsContent(
    patients: List<Patient>,
    isLoading: Boolean,
    errorMessage: String? = null,
    onNavigateBack: () -> Unit,
    onPatientClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BiodontosScreenGradient)
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = BiodontosDark
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "EXPEDIENTES Y DOCUMENTOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosGreyText,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Historias Clínicas",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                    }
                }

                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BiodontosDark)
                        }
                    }
                    errorMessage != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = errorMessage,
                                color = BiodontosErrorRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    patients.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No hay historias clínicas registradas aún.",
                                color = BiodontosGreyText,
                                fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(patients, key = { it.id }) { p ->
                                DocumentItemCard(
                                    doc = DoctorDocumentItem(
                                        patientId = p.id,
                                        title = "Historia Clínica y Odontograma",
                                        patientName = p.fullName,
                                        date = p.birthDate,
                                        status = if (p.consentSigned) "Consentimiento Firmado" else "Pendiente de Firma"
                                    ),
                                    onClick = { onPatientClick(p.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentItemCard(
    doc: DoctorDocumentItem,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BiodontosDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = null,
                        tint = BiodontosWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = doc.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                    Text(
                        text = "Paciente: ${doc.patientName}",
                        fontSize = 12.sp,
                        color = BiodontosGreyText
                    )
                    Text(
                        text = "Nacimiento: ${doc.date} · Estado: ${doc.status}",
                        fontSize = 11.sp,
                        color = BiodontosGreyText
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorDocumentsLoadingPreview() {
    DentalTheme {
        DoctorDocumentsContent(
            patients = emptyList(),
            isLoading = true,
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorDocumentsSuccessPreview() {
    DentalTheme {
        DoctorDocumentsContent(
            patients = listOf(
                Patient(
                    id = "pat-1",
                    dniCedula = "10203040",
                    firstName = "Ana",
                    firstLastName = "Gómez",
                    birthDate = "1990-05-14",
                    consentSigned = true
                )
            ),
            isLoading = false,
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DoctorDocumentsErrorPreview() {
    DentalTheme {
        DoctorDocumentsContent(
            patients = emptyList(),
            isLoading = false,
            errorMessage = "No se pudieron cargar los expedientes clínicos.",
            onNavigateBack = {}
        )
    }
}
