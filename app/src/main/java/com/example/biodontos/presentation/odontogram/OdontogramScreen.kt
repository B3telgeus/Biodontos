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
import com.example.biodontos.domain.model.Tooth
import com.example.biodontos.domain.model.ToothCondition

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OdontogramScreen(
    state: OdontogramState,
    onSurfaceClick: (Int, com.example.biodontos.domain.model.ToothSurface) -> Unit,
    onCommentChange: (String) -> Unit,
    onUpdateCondition: (ToothCondition) -> Unit,
    onDismissDialog: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Odontograma Adulto") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
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
                    // Upper Teeth
                    Text("Superior", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        QuadrantView(
                            toothNumbers = listOf(18, 17, 16, 15, 14, 13, 12, 11),
                            state = state,
                            onSurfaceClick = onSurfaceClick
                        )
                        QuadrantView(
                            toothNumbers = listOf(21, 22, 23, 24, 25, 26, 27, 28),
                            state = state,
                            onSurfaceClick = onSurfaceClick
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Lower Teeth
                    Text("Inferior", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        QuadrantView(
                            toothNumbers = listOf(48, 47, 46, 45, 44, 43, 42, 41),
                            state = state,
                            onSurfaceClick = onSurfaceClick
                        )
                        QuadrantView(
                            toothNumbers = listOf(31, 32, 33, 34, 35, 36, 37, 38),
                            state = state,
                            onSurfaceClick = onSurfaceClick
                        )
                    }
                }
            }
            
            if (state.showSelectionDialog) {
                ToothSelectionDialog(
                    toothNumber = state.selectedTooth!!,
                    surface = state.selectedSurface!!,
                    comment = state.comment,
                    isSaving = state.isSaving,
                    onCommentChange = onCommentChange,
                    onConditionSelected = onUpdateCondition,
                    onDismiss = onDismissDialog
                )
            }
            
            state.error?.let {
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
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
