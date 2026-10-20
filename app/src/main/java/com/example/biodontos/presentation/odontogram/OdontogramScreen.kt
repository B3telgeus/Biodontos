package com.example.biodontos.presentation.odontogram

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.domain.model.DentitionType
import com.example.biodontos.domain.model.Tooth
import com.example.biodontos.domain.model.ToothCondition

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OdontogramScreen(
    state: OdontogramState,
    onDentitionTypeChange: (DentitionType) -> Unit = {},
    onSurfaceClick: (Int, com.example.biodontos.domain.model.ToothSurface) -> Unit,
    onCommentChange: (String) -> Unit,
    onUpdateCondition: (ToothCondition) -> Unit,
    onDismissDialog: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Odontograma FDI Interactivo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .fillMaxSize()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Selector de tipo de dentición
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        listOf(
                            DentitionType.ADULT to "Adulto (32)",
                            DentitionType.CHILD to "Infantil (20)",
                            DentitionType.MIXED to "Mixta"
                        ).forEach { (type, label) ->
                            val isSelected = state.dentitionType == type
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) BiodontosDark else BiodontosInputBg,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clickable { onDentitionTypeChange(type) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BiodontosWhite else BiodontosDark,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Leyenda clínica
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ToothCondition.entries.forEach { cond ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(cond.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(cond.name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Maxilar Superior
                    Text(
                        "Maxilar Superior (Cuadrantes 1 y 2 / 5 y 6)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuadrantView(
                            toothNumbers = state.upperLeftQuadrant,
                            state = state,
                            onSurfaceClick = onSurfaceClick
                        )
                        QuadrantView(
                            toothNumbers = state.upperRightQuadrant,
                            state = state,
                            onSurfaceClick = onSurfaceClick
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Mandíbula Inferior
                    Text(
                        "Mandíbula Inferior (Cuadrantes 4 y 3 / 8 y 7)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuadrantView(
                            toothNumbers = state.lowerLeftQuadrant,
                            state = state,
                            onSurfaceClick = onSurfaceClick
                        )
                        QuadrantView(
                            toothNumbers = state.lowerRightQuadrant,
                            state = state,
                            onSurfaceClick = onSurfaceClick
                        )
                    }
                }
            }
            
            val selectedTooth = state.selectedTooth
            val selectedSurface = state.selectedSurface
            if (state.showSelectionDialog && selectedTooth != null && selectedSurface != null) {
                ToothSelectionDialog(
                    toothNumber = selectedTooth,
                    surface = selectedSurface,
                    comment = state.comment,
                    isSaving = state.isSaving,
                    onCommentChange = onCommentChange,
                    onConditionSelected = onUpdateCondition,
                    onDismiss = onDismissDialog
                )
            }
            
            state.error?.let {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Text(it)
                }
            }
        }
    }
}

@Composable
fun QuadrantView(
    toothNumbers: List<Int>,
    state: OdontogramState,
    onSurfaceClick: (Int, com.example.biodontos.domain.model.ToothSurface) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        toothNumbers.chunked(4).forEach { row ->
            Row {
                row.forEach { number ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(number.toString(), style = MaterialTheme.typography.labelSmall)
                        InteractiveTooth(
                            toothNumber = number,
                            surfaceStates = state.teeth[number]?.surfaceStates ?: emptyMap(),
                            onSurfaceClicked = { onSurfaceClick(number, it) },
                            modifier = Modifier.size(45.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToothSelectionDialog(
    toothNumber: Int,
    surface: com.example.biodontos.domain.model.ToothSurface,
    comment: String,
    isSaving: Boolean,
    onCommentChange: (String) -> Unit,
    onConditionSelected: (ToothCondition) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Diente $toothNumber - Superficie ${surface.name}",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                
                Text("Seleccionar Condición", style = MaterialTheme.typography.labelMedium)
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ToothCondition.entries.forEach { condition ->
                        Button(
                            onClick = { onConditionSelected(condition) },
                            modifier = Modifier.padding(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = condition.color),
                            enabled = !isSaving
                        ) {
                            Text(condition.name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    label = { Text("Comentarios de tratamiento") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("Cancelar")
                }
            }
        }
    }
}
