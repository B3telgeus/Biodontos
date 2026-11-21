package com.example.biodontos.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biodontos.R
import com.example.biodontos.core.designsystem.*

@Composable
fun SignaturePad(
    title: String = "Firma Digital del Paciente",
    subtitle: String = "Dibuje su firma con el dedo o lápiz óptico dentro del recuadro.",
    currentSignature: String? = null,
    onSignatureConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isConfirmed by remember { mutableStateOf(!currentSignature.isNullOrBlank()) }

    Column(
        modifier = modifier
            .testTag(TestTags.Components.SIGNATURE_PAD_ROOT)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BiodontosWhite)
            .border(1.dp, BiodontosBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BiodontosDark
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = BiodontosGreyText)
                )
            }
            if (isConfirmed) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Esmeralda.copy(alpha = 0.15f),
                    modifier = Modifier.testTag(TestTags.Components.SIGNATURE_PAD_CONFIRMED_BADGE)
                ) {
                    Text(
                        text = stringResource(R.string.ui_signature_pad_170),
                        color = Esmeralda,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BiodontosInputBg.copy(alpha = 0.6f))
                .border(
                    width = 1.dp,
                    color = if (isConfirmed) Esmeralda else BiodontosBorder,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            val padContentDescription = stringResource(R.string.cd_signature_pad)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(TestTags.Components.SIGNATURE_PAD_CANVAS)
                    .semantics { contentDescription = padContentDescription }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isConfirmed = false
                                currentStroke = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentStroke = currentStroke + change.position
                            },
                            onDragEnd = {
                                if (currentStroke.isNotEmpty()) {
                                    strokes.add(currentStroke)
                                    currentStroke = emptyList()
                                }
                            }
                        )
                    }
            ) {
                val lineY = size.height * 0.82f
                drawLine(
                    color = BiodontosBorder.copy(alpha = 0.5f),
                    start = Offset(24f, lineY),
                    end = Offset(size.width - 24f, lineY),
                    strokeWidth = 2f
                )

                for (stroke in strokes) {
                    if (stroke.size > 1) {
                        val path = Path().apply {
                            moveTo(stroke[0].x, stroke[0].y)
                            for (i in 1 until stroke.size) {
                                lineTo(stroke[i].x, stroke[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = BiodontosDark,
                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                if (currentStroke.size > 1) {
                    val path = Path().apply {
                        moveTo(currentStroke[0].x, currentStroke[0].y)
                        for (i in 1 until currentStroke.size) {
                            lineTo(currentStroke[i].x, currentStroke[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = BiodontosDark,
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            if (strokes.isEmpty() && currentStroke.isEmpty() && !isConfirmed) {
                Text(
                    text = stringResource(R.string.ui_signature_pad_171),
                    color = BiodontosGreyText.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    strokes.clear()
                    currentStroke = emptyList()
                    isConfirmed = false
                    onSignatureConfirmed("")
                },
                colors = ButtonDefaults.textButtonColors(contentColor = BiodontosGreyText),
                modifier = Modifier.testTag(TestTags.Components.SIGNATURE_PAD_CLEAR_BUTTON)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.ui_signature_pad_172), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.ui_signature_pad_172), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (strokes.isNotEmpty()) {
                        isConfirmed = true
                        val signatureDigest = "sig_vector_${strokes.size}_points_${strokes.sumOf { it.size }}_${System.currentTimeMillis()}"
                        onSignatureConfirmed(signatureDigest)
                    }
                },
                enabled = strokes.isNotEmpty() && !isConfirmed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BiodontosDark,
                    contentColor = BiodontosWhite
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag(TestTags.Components.SIGNATURE_PAD_CONFIRM_BUTTON)
            ) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.ui_signature_pad_173), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.ui_signature_pad_174), fontSize = 13.sp)
            }
        }
    }
}
