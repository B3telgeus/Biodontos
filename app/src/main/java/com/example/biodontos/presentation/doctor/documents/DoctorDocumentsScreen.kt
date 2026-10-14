package com.example.biodontos.presentation.doctor.documents

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite

data class DoctorDocumentItem(
    val title: String,
    val patientName: String,
    val date: String,
    val fileCount: Int
)

@Composable
fun DoctorDocumentsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sampleDocuments = listOf(
        DoctorDocumentItem("Historia Clínica y Anamnesis", "Dora Hernández", "18 Mar 2026", 4),
        DoctorDocumentItem("Consentimiento Informado - Endodoncia", "Carlos Troncoso", "19 Mar 2026", 2),
        DoctorDocumentItem("Evolución Clínica y Odontograma", "Luis Camilo", "18 Mar 2026", 3),
        DoctorDocumentItem("Periodontometría y Sondaje", "Sara Arias", "15 Abr 2026", 1),
        DoctorDocumentItem("Plan de Tratamiento Ortodoncia", "Luna Estrada", "18 Mar 2026", 2)
    )

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BiodontosScreenGradient)
                .padding(padding)
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sampleDocuments) { doc ->
                        DocumentItemCard(doc = doc)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentItemCard(doc: DoctorDocumentItem) {
    Card(
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
                        text = "Fecha: ${doc.date} · ${doc.fileCount} archivos adjuntos",
                        fontSize = 11.sp,
                        color = BiodontosGreyText
                    )
                }
            }
        }
    }
}
