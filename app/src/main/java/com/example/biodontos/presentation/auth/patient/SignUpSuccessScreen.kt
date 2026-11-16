package com.example.biodontos.presentation.auth.patient

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.R
import com.example.biodontos.core.designsystem.BiodontosDark
import com.example.biodontos.core.designsystem.DentalTheme
import com.example.biodontos.core.designsystem.components.BiodontosPillButton

@Composable
fun SignUpSuccessScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE9E9EB),
            Color(0xFFD4D5D8),
            Color(0xFFB0B4BD)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 500.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.ui_sign_up_success_screen_158),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BiodontosDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.ui_sign_up_success_screen_159),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = BiodontosDark.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Diente central con botón superpuesto o debajo
            Box(
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_tooth),
                    contentDescription = stringResource(R.string.ui_login_screen_126),
                    modifier = Modifier.size(240.dp)
                )

                BiodontosPillButton(
                    text = stringResource(R.string.ui_login_screen_137),
                    onClick = onLoginClick,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpSuccessLoadingPreview() {
    DentalTheme {
        SignUpSuccessScreen(onLoginClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpSuccessSuccessPreview() {
    DentalTheme {
        SignUpSuccessScreen(onLoginClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpSuccessErrorPreview() {
    DentalTheme(darkTheme = true) {
        SignUpSuccessScreen(onLoginClick = {})
    }
}
