package com.example.biodontos.presentation.odontogram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface

@Composable
fun InteractiveTooth(
    toothNumber: Int,
    surfaceStates: Map<ToothSurface, ToothCondition>,
    onSurfaceClicked: (ToothSurface) -> Unit,
    modifier: Modifier = Modifier.size(70.dp)
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val surface = detectSurface(offset, size.width.toFloat(), size.height.toFloat())
                surface?.let { onSurfaceClicked(it) }
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val inset = w * 0.28f

        // Oclusal (Center)
        drawRect(
            color = surfaceStates[ToothSurface.OCLUSAL]?.color ?: ToothCondition.SANO.color,
            topLeft = Offset(inset, inset),
            size = Size(w - 2 * inset, h - 2 * inset)
        )
        drawRect(
            color = Color.Black,
            topLeft = Offset(inset, inset),
            size = Size(w - 2 * inset, h - 2 * inset),
            style = Stroke(width = 1.dp.toPx())
        )

        // Vestibular (Top)
        val vestibularPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w - inset, inset)
            lineTo(inset, inset)
            close()
        }
        drawPath(
            path = vestibularPath,
            color = surfaceStates[ToothSurface.VESTIBULAR]?.color ?: ToothCondition.SANO.color
        )
        drawPath(
            path = vestibularPath,
            color = Color.Black,
            style = Stroke(width = 1.dp.toPx())
        )

        // Palatino/Lingual (Bottom)
        val palatinoPath = Path().apply {
            moveTo(0f, h)
            lineTo(w, h)
            lineTo(w - inset, h - inset)
            lineTo(inset, h - inset)
            close()
        }
        drawPath(
            path = palatinoPath,
            color = surfaceStates[ToothSurface.PALATINO]?.color ?: ToothCondition.SANO.color
        )
        drawPath(
            path = palatinoPath,
            color = Color.Black,
            style = Stroke(width = 1.dp.toPx())
        )

        // Mesial (Left - assuming right-side view or standard layout)
        val mesialPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(inset, inset)
            lineTo(inset, h - inset)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = mesialPath,
            color = surfaceStates[ToothSurface.MESIAL]?.color ?: ToothCondition.SANO.color
        )
        drawPath(
            path = mesialPath,
            color = Color.Black,
            style = Stroke(width = 1.dp.toPx())
        )

        // Distal (Right)
        val distalPath = Path().apply {
            moveTo(w, 0f)
            lineTo(w - inset, inset)
            lineTo(w - inset, h - inset)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = distalPath,
            color = surfaceStates[ToothSurface.DISTAL]?.color ?: ToothCondition.SANO.color
        )
        drawPath(
            path = distalPath,
            color = Color.Black,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun detectSurface(offset: Offset, width: Float, height: Float): ToothSurface? {
    val x = offset.x
    val y = offset.y
    val inset = width * 0.28f

    // Check Oclusal
    if (x in inset..(width - inset) && y in inset..(height - inset)) {
        return ToothSurface.OCLUSAL
    }

    // Check by triangles/trapezoids using slopes
    // Top (Vestibular)
    if (y < inset && x > y && x < (width - y)) return ToothSurface.VESTIBULAR
    // Bottom (Palatino)
    if (y > (height - inset) && x > (height - y) && x < (width - (height - y))) return ToothSurface.PALATINO
    // Left (Mesial)
    if (x < inset && y > x && y < (height - x)) return ToothSurface.MESIAL
    // Right (Distal)
    if (x > (width - inset) && y > (width - x) && y < (height - (width - x))) return ToothSurface.DISTAL

    return null
}
