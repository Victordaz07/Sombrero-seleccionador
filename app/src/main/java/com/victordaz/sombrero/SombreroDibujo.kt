package com.victordaz.sombrero

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Textured hat plus a live mouth. Both layers use the same centered square, so the
 * mouth remains aligned on phones/tablets and while the parent rocks the hat.
 * Mouth coordinates match the closed seam in sombrero.png; do not crop that asset.
 * [boca] and [cejas] still come from the existing speaking animation.
 */
@Composable
fun SombreroDibujo(boca: Float, cejas: Float, modifier: Modifier = Modifier) {
    Box(modifier.graphicsLayer {
        // A subtle expressive stretch of the complete face; image and mouth move together.
        scaleY = 1f + cejas.coerceIn(0f, 1f) * 0.012f
    }) {
        Image(painterResource(R.drawable.sombrero), "Sombrero seleccionador",
            Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Canvas(Modifier.fillMaxSize()) {
            val lado = size.minDimension
            val escala = lado / 300f
            withTransform({
                translate((size.width - lado) / 2f, (size.height - lado) / 2f)
                scale(escala, escala, pivot = Offset.Zero)
            }) {
                val apertura = boca.coerceIn(0f, 1f)
                // At rest show the original photographic seam, without a second drawn mouth.
                if (apertura > 0.01f) {
                    val labio = Path().apply {
                        moveTo(91f, 195.5f)
                        cubicTo(125f, 192f, 153f, 185f, 184f, 193f)
                        quadraticTo(201f, 197f, 210f, 194.5f)
                        cubicTo(184f, 193f + 32f * apertura, 124f, 192f + 38f * apertura, 91f, 195.5f)
                        close()
                    }
                    drawPath(labio, Brush.verticalGradient(
                        listOf(Color(0xFF100907), Color(0xFF362016)), startY = 190f, endY = 225f))
                    drawPath(labio, Color(0xFF4E2F1B), style = Stroke(width = 1.4f))
                }
            }
        }
    }
}
