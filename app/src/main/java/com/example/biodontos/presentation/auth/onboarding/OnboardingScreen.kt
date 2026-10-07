package com.example.biodontos.presentation.auth.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.BiodontosGreyText

@Composable
fun OnboardingScreen(
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFE5E7EB),
            Color(0xFF151A24).copy(alpha = 0.25f)
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        val isTabletLandscape = maxWidth > 700.dp

        if (isTabletLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 32.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "¡SONRÍE!",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = BiodontosDark
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tu salud dental, organizada fácilmente.",
                        fontSize = 20.sp,
                        color = BiodontosGreyText,
                        lineHeight = 28.sp
                    )
                }

                // Right Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(40.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(40.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Bienvenido a Biodontos.",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(36.dp))

                        Text(
                            text = "Ya tengo una cuenta",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BiodontosGreyText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onLoginClick,
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BiodontosDark,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text("Inicia Sesión", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        Text(
                            text = "¿Es la primera vez que usas la app?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BiodontosGreyText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onCreateAccountClick,
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BiodontosDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text("Crea una cuenta", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Mobile
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¡SONRÍE!",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = BiodontosDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tu salud dental, organizada fácilmente.",
                    fontSize = 16.sp,
                    color = BiodontosGreyText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(36.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Bienvenido a Biodontos",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiodontosDark
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onLoginClick,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BiodontosDark,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Inicia Sesión", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onCreateAccountClick,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Crea una cuenta", fontWeight = FontWeight.Bold, color = BiodontosDark)
                        }
                    }
                }
            }
        }
    }
}
