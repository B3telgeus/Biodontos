package com.example.biodontos.presentation.doctor.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite

enum class DoctorNavItem(
    val title: String,
    val icon: ImageVector
) {
    HOME("Inicio", Icons.Default.Home),
    PATIENTS("Pacientes", Icons.Default.Person),
    DOCUMENTS("Documentos", Icons.Default.AccountBox),
    APPOINTMENTS("Citas", Icons.Default.DateRange)
}

@Composable
fun DoctorPortalShell(
    selectedItem: DoctorNavItem,
    onItemSelected: (DoctorNavItem) -> Unit,
    onLogoutClick: () -> Unit,
    isExpandedScreen: Boolean = true,
    content: @Composable () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Cerrar sesión",
                    fontWeight = FontWeight.Bold,
                    color = BiodontosDark
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas salir del portal médico?",
                    color = BiodontosGreyText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text("Cerrar sesión", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = BiodontosDark)
                }
            },
            containerColor = BiodontosWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BiodontosScreenGradient)
    ) {
        if (isExpandedScreen) {
            // Layout Tablet / Landscape (#151A24 NavigationRail estilo 08_Home_Doctor.png)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                NavigationRail(
                    containerColor = BiodontosDark,
                    contentColor = BiodontosWhite,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(72.dp),
                    header = {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DoctorNavItem.entries.forEach { item ->
                                val isSelected = selectedItem == item
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { onItemSelected(item) },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = BiodontosWhite,
                                        unselectedIconColor = BiodontosWhite.copy(alpha = 0.5f),
                                        indicatorColor = Color(0xFF2C3240)
                                    )
                                )
                            }
                        }

                        // Botón de salir al fondo del rail
                        Box(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showLogoutDialog = true }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Cerrar sesión",
                                tint = BiodontosWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Área de contenido
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    content()
                }
            }
        } else {
            // Layout Móvil con BottomBar
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(
                        containerColor = BiodontosDark,
                        contentColor = BiodontosWhite
                    ) {
                        DoctorNavItem.entries.forEach { item ->
                            val isSelected = selectedItem == item
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { onItemSelected(item) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontSize = 11.sp,
                                        color = if (isSelected) BiodontosWhite else BiodontosWhite.copy(alpha = 0.6f)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BiodontosWhite,
                                    unselectedIconColor = BiodontosWhite.copy(alpha = 0.5f),
                                    indicatorColor = Color(0xFF2C3240)
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                ) {
                    content()
                }
            }
        }
    }
}
