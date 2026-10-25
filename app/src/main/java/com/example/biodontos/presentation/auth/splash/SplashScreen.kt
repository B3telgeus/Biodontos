package com.example.biodontos.presentation.auth.splash

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.R
import com.example.biodontos.core.designsystem.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToDoctor: () -> Unit,
    onNavigateToPatient: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                SplashEffect.NavigateToDoctor -> onNavigateToDoctor()
                SplashEffect.NavigateToPatient -> onNavigateToPatient()
                SplashEffect.NavigateToOnboarding -> onNavigateToOnboarding()
            }
        }
    }

    SplashContent(startAnimation = startAnimation)
}

@VisibleForTesting
@Composable
fun SplashContent(
    startAnimation: Boolean = true,
    isLoading: Boolean = true,
    statusMessage: String = "Iniciando entorno clínico seguro...",
    errorMessage: String? = null
) {

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "splashAlpha"
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D131F),
            BiodontosDark,
            Color(0xFF080C14)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        // Halo luminoso de fondo en tono Zafiro
        Box(
            modifier = Modifier
                .size(320.dp)
                .alpha(0.22f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Zafiro, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        // Contenido Central: Logo, Marca y Subtítulo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .safeDrawingPadding()
                .padding(horizontal = 32.dp)
        ) {
            // Emblema con sombra y borde sutil
            Surface(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        spotColor = Zafiro,
                        ambientColor = Zafiro
                    ),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(3.dp, Brush.linearGradient(listOf(ZafiroLight, Zafiro)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_tooth),
                        contentDescription = "Biodontos Logo",
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Título Institucional
            Text(
                text = "BIODONTOS",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = BiodontosWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo Clínico
            Text(
                text = "PLATAFORMA ODONTOLÓGICA INTEGRAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Badge de Entorno Seguro
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Esmeralda, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sistema Clínico Interactivo FDI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Pie de Página: Loader y Estado de Seguridad
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Zafiro,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = errorMessage ?: statusMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (errorMessage != null) Carmesi else Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashLoadingPreview() {
    DentalTheme {
        SplashContent(
            startAnimation = true,
            isLoading = true,
            statusMessage = "Iniciando entorno clínico seguro..."
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashSuccessPreview() {
    DentalTheme {
        SplashContent(
            startAnimation = true,
            isLoading = false,
            statusMessage = "Sesión verificada. Redirigiendo..."
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashErrorPreview() {
    DentalTheme {
        SplashContent(
            startAnimation = true,
            isLoading = false,
            errorMessage = "Sin conexión. Modo local activado."
        )
    }
}
