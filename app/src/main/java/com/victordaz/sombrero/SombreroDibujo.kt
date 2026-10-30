package com.victordaz.sombrero

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform

private val Cafe = Color(0xFF7A5230)
private val CafeClaro = Color(0xFFA0703F)
private val CafeOscuro = Color(0xFF4E3219)
private val Pliegue = Color(0xFF2B1A0C)
private val Parche = Color(0xFF8E6A45)

/**
 * Dibuja el sombrero. Todo se dibuja en un lienzo imaginario de 300x300 y luego se escala.
 *
 * @param boca qué tan abierta está la boca (0 = cerrada, 1 = muy abierta).
 * @param cejas cuánto se levantan las cejas (0 = normal, 1 = arriba).
 */
@Composable
fun SombreroDibujo(boca: Float, cejas: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val escala = minOf(size.width, size.height) / 300f
        val dx = (size.width - 300f * escala) / 2f
        val dy = (size.height - 300f * escala) / 2f
        withTransform({
            translate(dx, dy)
            scale(escala, escala, pivot = Offset.Zero)
        }) {
            dibujarSombrero(boca.coerceIn(0f, 1f), cejas.coerceIn(0f, 1f))
        }
    }
}

private fun DrawScope.dibujarSombrero(boca: Float, cejas: Float) {
    // Sombra en el piso
    drawOval(Color.Black.copy(alpha = 0.35f), topLeft = Offset(20f, 250f), size = Size(260f, 36f))

    // Parte de atrás del ala
    drawOval(CafeOscuro, topLeft = Offset(8f, 222f), size = Size(284f, 50f))

    // Cono con la punta doblada
    val cono = Path().apply {
        moveTo(52f, 248f)
        cubicTo(72f, 200f, 98f, 150f, 122f, 112f)
        cubicTo(138f, 85f, 160f, 58f, 198f, 42f)
        cubicTo(222f, 32f, 246f, 40f, 258f, 64f)
        cubicTo(236f, 56f, 214f, 64f, 202f, 86f)
        cubicTo(190f, 112f, 202f, 178f, 248f, 248f)
        close()
    }
    drawPath(cono, Brush.verticalGradient(listOf(CafeClaro, Cafe, CafeOscuro), startY = 40f, endY = 250f))

    // Pliegues y arrugas
    val arrugas = Stroke(width = 3.5f, cap = StrokeCap.Round)
    drawPath(Path().apply { moveTo(80f, 232f); quadraticTo(98f, 212f, 92f, 190f) }, CafeOscuro, style = arrugas)
    drawPath(Path().apply { moveTo(222f, 232f); quadraticTo(206f, 212f, 212f, 190f) }, CafeOscuro, style = arrugas)
    drawPath(Path().apply { moveTo(140f, 98f); quadraticTo(160f, 106f, 182f, 94f) }, CafeOscuro, style = arrugas)
    drawPath(Path().apply { moveTo(214f, 58f); quadraticTo(226f, 50f, 240f, 53f) }, CafeOscuro, style = arrugas)

    // Parche cosido
    rotate(-12f, pivot = Offset(172f, 126f)) {
        drawRect(Parche, topLeft = Offset(158f, 115f), size = Size(28f, 22f))
        drawRect(
            CafeOscuro, topLeft = Offset(160f, 117f), size = Size(24f, 18f),
            style = Stroke(width = 1.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))),
        )
    }

    // Cejas (se levantan cuando habla)
    val subir = cejas * 8f
    val ceja = Stroke(width = 4f, cap = StrokeCap.Round)
    drawPath(Path().apply { moveTo(98f, 164f - subir); quadraticTo(120f, 152f - subir, 142f, 160f - subir) }, CafeOscuro, style = ceja)
    drawPath(Path().apply { moveTo(158f, 160f - subir); quadraticTo(180f, 152f - subir, 202f, 164f - subir) }, CafeOscuro, style = ceja)

    // Ojos (dos pliegues entrecerrados)
    val ojo = Stroke(width = 6f, cap = StrokeCap.Round)
    drawPath(Path().apply { moveTo(102f, 180f); quadraticTo(121f, 168f, 140f, 178f) }, Pliegue, style = ojo)
    drawPath(Path().apply { moveTo(160f, 178f); quadraticTo(179f, 168f, 198f, 180f) }, Pliegue, style = ojo)

    // Boca: un pliegue que se abre al hablar
    val bocaPath = Path().apply {
        moveTo(88f, 222f)
        quadraticTo(150f, 214f - 4f * boca, 214f, 220f)
        quadraticTo(150f, 232f + 40f * boca, 88f, 222f)
        close()
    }
    drawPath(bocaPath, Pliegue)
    drawPath(bocaPath, CafeOscuro, style = Stroke(width = 3f))

    // Frente del ala
    val ala = Path().apply {
        moveTo(8f, 247f)
        quadraticTo(150f, 300f, 292f, 247f)
        quadraticTo(150f, 280f, 8f, 247f)
        close()
    }
    drawPath(ala, Brush.verticalGradient(listOf(Cafe, CafeOscuro), startY = 247f, endY = 275f))
}
