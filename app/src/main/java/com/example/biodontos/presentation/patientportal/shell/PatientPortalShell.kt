package com.example.biodontos.presentation.patientportal.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.R
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosScreenBgBottom
import com.example.biodontos.core.designsystem.BiodontosScreenBgTop
import com.example.biodontos.core.designsystem.BiodontosScreenGradient
import com.example.biodontos.core.designsystem.BiodontosWhite

enum class PatientNavItem(
    val title: String,
    val icon: ImageVector
) {
    HOME("Inicio", Icons.Default.Home),
    APPOINTMENTS("Citas", Icons.Default.DateRange),
    DOCTORS("Doctores", Icons.Default.Person),
    PAYMENTS("Pagos", Icons.Default.AccountBox)
}

@Composable
fun PatientPortalShell(
    selectedItem: PatientNavItem,
    onItemSelected: (PatientNavItem) -> Unit,
    onLogoutClick: () -> Unit,
    isExpandedScreen: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BiodontosScreenGradient)
    ) {
        if (isExpandedScreen) {
            // Layout Tablet / Horizontal (Estilo Figma)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                // Navigation Rail Izquierdo Oscuro #151A24
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
                            PatientNavItem.entries.forEach { item ->
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
                                .clickable { onLogoutClick() }
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

                // Área de contenido principal
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    content()
                }
            }
        } else {
            // Layout Móvil / Vertical
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(
                        containerColor = BiodontosDark,
                        contentColor = BiodontosWhite
                    ) {
                        PatientNavItem.entries.forEach { item ->
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
