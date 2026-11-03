package com.example.biodontos.presentation.doctor.materials

import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biodontos.core.designsystem.Ambar
import com.example.biodontos.core.designsystem.BiodontosBorder
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosErrorBg
import com.example.biodontos.core.designsystem.BiodontosErrorRed
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosInputBg
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite
import com.example.biodontos.core.designsystem.Carmesi
import com.example.biodontos.core.designsystem.Esmeralda
import com.example.biodontos.core.designsystem.Slate
import com.example.biodontos.core.designsystem.Zafiro
import com.example.biodontos.core.designsystem.biodontosTextFieldColors
import com.example.biodontos.core.designsystem.components.TwoStepConfirmationDialog
import com.example.biodontos.domain.model.ClinicalMaterial
import java.time.format.DateTimeFormatter

@Composable
fun MaterialsRoute(
    viewModel: MaterialsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MaterialsUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MaterialsUiEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    MaterialsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
fun MaterialsScreen(
    state: MaterialsUiState,
    onEvent: (MaterialsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    MaterialsContent(
        state = state,
        onEvent = onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@VisibleForTesting
@Composable
fun MaterialsContent(
    state: MaterialsUiState,
    onEvent: (MaterialsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEvent(MaterialsUiEvent.OpenCreateForm) },
                containerColor = BiodontosDark,
                contentColor = BiodontosWhite,
                icon = { Icon(Icons.Default.Add, contentDescription = "Nuevo Insumo") },
                text = { Text("Nuevo Material", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BiodontosScreenGradient)
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Top Bar / Encabezado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(BiodontosWhite, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = BiodontosDark
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Materiales e Insumos",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                        Text(
                            text = "Catálogo regulatorio y trazabilidad INVIMA",
                            fontSize = 12.sp,
                            color = BiodontosGreyText
                        )
                    }

                    IconButton(
                        onClick = { onEvent(MaterialsUiEvent.Refresh) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(BiodontosWhite, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar catálogo",
                            tint = BiodontosDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // KPI Metrics Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MaterialMetricPill(
                        label = "Activos",
                        count = state.activeCount,
                        indicatorColor = Esmeralda,
                        modifier = Modifier.weight(1f)
                    )
                    MaterialMetricPill(
                        label = "Por Vencer",
                        count = state.nearExpirationCount,
                        indicatorColor = Ambar,
                        modifier = Modifier.weight(1f)
                    )
                    MaterialMetricPill(
                        label = "Vencidos",
                        count = state.expiredCount,
                        indicatorColor = Carmesi,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Buscador de Insumos
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { onEvent(MaterialsUiEvent.SearchQueryChanged(it)) },
                    placeholder = { Text("Buscar por nombre, INVIMA o lote...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = BiodontosGreyText
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onEvent(MaterialsUiEvent.SearchQueryChanged("")) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = BiodontosGreyText
                                )
                            }
                        }
                    },
                    colors = biodontosTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pestañas Activos / Inactivos
                TabRow(
                    selectedTabIndex = state.selectedTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = BiodontosDark,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.ordinal]),
                            color = BiodontosDark,
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    MaterialTabFilter.entries.forEach { tab ->
                        val isSelected = state.selectedTab == tab
                        val count = if (tab == MaterialTabFilter.ACTIVE) state.activeCount else state.inactiveCount
                        Tab(
                            selected = isSelected,
                            onClick = { onEvent(MaterialsUiEvent.TabSelected(tab)) },
                            text = {
                                Text(
                                    text = "${tab.label} ($count)",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BiodontosDark else BiodontosGreyText
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contenido de la lista o estados de carga/error
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BiodontosDark)
                        }
                    }

                    state.errorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
                                border = BorderStroke(1.dp, BiodontosErrorRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = BiodontosErrorRed,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.errorMessage,
                                        color = BiodontosErrorRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { onEvent(MaterialsUiEvent.Refresh) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BiodontosDark)
                                    ) {
                                        Text("Reintentar")
                                    }
                                }
                            }
                        }
                    }

                    state.filteredMaterials.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = BiodontosInputBg,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = BiodontosGreyText,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (state.searchQuery.isNotBlank()) {
                                        "No se encontraron materiales que coincidan con '${state.searchQuery}'"
                                    } else if (state.selectedTab == MaterialTabFilter.INACTIVE) {
                                        "No hay materiales clínicos inactivos"
                                    } else {
                                        "Aún no tienes materiales clínicos registrados"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BiodontosGreyText
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = state.filteredMaterials,
                                key = { it.rawId }
                            ) { material ->
                                MaterialCard(
                                    material = material,
                                    onEdit = { onEvent(MaterialsUiEvent.OpenEditForm(material)) },
                                    onDeactivate = { onEvent(MaterialsUiEvent.RequestDeactivate(material)) },
                                    onReactivate = { onEvent(MaterialsUiEvent.ReactivateMaterial(material)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Formulario de Creación / Edición
    if (state.isFormOpen) {
        MaterialFormBottomSheet(
            formState = state.formState,
            onNameChange = { onEvent(MaterialsUiEvent.FormNameChanged(it)) },
            onInvimaChange = { onEvent(MaterialsUiEvent.FormInvimaChanged(it)) },
            onLotChange = { onEvent(MaterialsUiEvent.FormLotChanged(it)) },
            onManufacturingDateChange = { onEvent(MaterialsUiEvent.FormManufacturingDateChanged(it)) },
            onExpirationDateChange = { onEvent(MaterialsUiEvent.FormExpirationDateChanged(it)) },
            onSave = { onEvent(MaterialsUiEvent.SaveMaterial) },
            onDismiss = { onEvent(MaterialsUiEvent.DismissForm) }
        )
    }

    // Diálogo de Confirmación en 2 Pasos para Desactivación
    state.materialToDeactivate?.let { material ->
        TwoStepConfirmationDialog(
            title = "Desactivar Insumo Clínico",
            impactSummary = "El insumo '${material.name}' (Lote: ${material.batchLotNumber}) quedará inactivo y no podrá seleccionarse en futuras notas de evolución. Las evoluciones pasadas conservarán sus datos y trazabilidad INVIMA intactos.",
            confirmActionLabel = "Confirmar Desactivación",
            onConfirm = { onEvent(MaterialsUiEvent.ConfirmDeactivate) },
            onDismiss = { onEvent(MaterialsUiEvent.DismissDeactivateDialog) }
        )
    }
}

@Composable
fun MaterialCard(
    material: ClinicalMaterial,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val isExpired = material.isExpired()
    val isNearExpiration = material.isNearExpiration()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        border = BorderStroke(1.dp, BiodontosBorder.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila superior: Nombre y Badge de estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = material.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Expiration / Status Badges
                when {
                    !material.isActive -> {
                        MaterialStatusBadge(
                            text = "Inactivo",
                            bgColor = BiodontosInputBg,
                            textColor = Slate
                        )
                    }
                    isExpired -> {
                        MaterialStatusBadge(
                            text = "Vencido",
                            bgColor = BiodontosErrorBg,
                            textColor = Carmesi
                        )
                    }
                    isNearExpiration -> {
                        MaterialStatusBadge(
                            text = "Vence pronto",
                            bgColor = Color(0xFFFEF3C7),
                            textColor = Ambar
                        )
                    }
                    else -> {
                        MaterialStatusBadge(
                            text = "Vigente",
                            bgColor = Color(0xFFD1FAE5),
                            textColor = Esmeralda
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fila de datos regulatorios: INVIMA y Lote
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RegulatoryTag(label = "INVIMA", value = material.invimaRegistration, modifier = Modifier.weight(1.2f))
                RegulatoryTag(label = "Lote", value = material.batchLotNumber, modifier = Modifier.weight(0.8f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fila de Fechas (Elaboración y Vencimiento)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Elaboración: ${material.manufacturingDate.format(dateFormatter)}",
                    fontSize = 12.sp,
                    color = BiodontosGreyText
                )

                val expirationColor = when {
                    !material.isActive -> BiodontosGreyText
                    isExpired -> Carmesi
                    isNearExpiration -> Ambar
                    else -> BiodontosGreyText
                }

                Text(
                    text = "Vencimiento: ${material.expirationDate.format(dateFormatter)}",
                    fontSize = 12.sp,
                    color = expirationColor,
                    fontWeight = if (isExpired || isNearExpiration) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de Acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, BiodontosBorder.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = BiodontosDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar", fontSize = 12.sp, color = BiodontosDark)
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (material.isActive) {
                    OutlinedButton(
                        onClick = onDeactivate,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, BiodontosErrorRed.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Desactivar",
                            tint = BiodontosErrorRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Desactivar", fontSize = 12.sp, color = BiodontosErrorRed)
                    }
                } else {
                    OutlinedButton(
                        onClick = onReactivate,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, Esmeralda.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Reactivar",
                            tint = Esmeralda,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reactivar", fontSize = 12.sp, color = Esmeralda)
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialStatusBadge(
    text: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun RegulatoryTag(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = BiodontosInputBg.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BiodontosGreyText
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark
            )
        }
    }
}

@Composable
private fun MaterialMetricPill(
    label: String,
    count: Int,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BiodontosWhite),
        border = BorderStroke(1.dp, BiodontosBorder.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = count.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = BiodontosGreyText
                )
            }
        }
    }
}
