package com.victordaz.sombrero

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private val Dorado = Color(0xFFE8C15A)
private val NocheArriba = Color(0xFF0B0820)
private val NocheAbajo = Color(0xFF2A1650)

@Composable
fun PantallaSombrero(vm: SombreroViewModel) {
    val estado = vm.estado
    val casa = estado.casa
    var mostrarAjustes by rememberSaveable { mutableStateOf(false) }
    val haptico = LocalHapticFeedback.current

    LaunchedEffect(casa) {
        if (casa != null) haptico.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val arriba by animateColorAsState(casa?.colorPrincipal ?: NocheArriba, tween(900), label = "arriba")
    val abajo by animateColorAsState(
        casa?.colorPrincipal?.let { lerpNegro(it) } ?: NocheAbajo, tween(900), label = "abajo",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(arriba, abajo)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                vm.alTocar()
            },
    ) {
        Estrellas(Modifier.fillMaxSize(), color = casa?.colorSecundario ?: Color.White)

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Encabezado(estado, casa)
            Spacer(Modifier.weight(1f))
            SombreroAnimado(estado)
            Spacer(Modifier.height(16.dp))
            Subtitulo(estado)
            Spacer(Modifier.weight(1f))
            PiePagina(estado)
        }

        IconButton(
            onClick = { if (estado.fase != Fase.PENSANDO) mostrarAjustes = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(4.dp),
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = Color.White.copy(alpha = 0.35f))
        }
    }

    if (mostrarAjustes) {
        AjustesDialogo(vm, alCerrar = { mostrarAjustes = false })
    }
}

@Composable
private fun Encabezado(estado: EstadoUi, casa: Casa?) {
    Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = casa,
            transitionSpec = { (fadeIn(tween(600)) + scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))) togetherWith fadeOut(tween(300)) },
            label = "encabezado",
        ) { c ->
            if (c == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "El Sombrero\nSeleccionador",
                        color = Dorado, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                        fontSize = 38.sp, lineHeight = 42.sp, textAlign = TextAlign.Center,
                    )
                    if (estado.vozLista == false) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⚠ No se encontró voz en español. Instala \"Servicios de voz de Google\" y su paquete en español.",
                            color = Color(0xFFFFB4A0), fontSize = 13.sp, textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Escudo(c, Modifier.size(96.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        c.nombre.uppercase(),
                        color = c.colorSecundario, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black,
                        fontSize = 44.sp, letterSpacing = 3.sp, textAlign = TextAlign.Center,
                    )
                    Text(c.lema, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SombreroAnimado(estado: EstadoUi) {
    val infinita = rememberInfiniteTransition(label = "sombrero")
    val pensando = estado.fase == Fase.PENSANDO

    // Balanceo: suave en reposo, más marcado mientras piensa.
    val fase by infinita.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "balanceo",
    )
    val amplitud by animateFloatAsState(if (pensando) 7f else 2.5f, tween(600), label = "amplitud")

    // Boca: se abre y cierra rápido mientras habla.
    val movBoca by infinita.animateFloat(
        0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Restart), label = "boca",
    )
    val abierta = if (estado.hablando) 0.2f + 0.8f * abs(sin(movBoca * 5f * PI.toFloat())) else 0f
    val boca by animateFloatAsState(abierta, tween(70), label = "bocaSuave")
    val cejas by animateFloatAsState(if (estado.hablando) 1f else 0f, tween(300), label = "cejas")

    // Brillo mágico detrás del sombrero.
    val pulso by infinita.animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulso",
    )
    val brillo by animateFloatAsState(if (pensando || estado.casa != null) 1f else 0.35f, tween(800), label = "brillo")
    val colorBrillo = estado.casa?.colorSecundario ?: Dorado

    Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(
                    listOf(colorBrillo.copy(alpha = 0.45f * brillo * pulso), Color.Transparent),
                    center = center, radius = size.minDimension / 2f,
                ),
            )
        }
        SombreroDibujo(
            boca = boca,
            cejas = cejas,
            modifier = Modifier
                .size(270.dp)
                .graphicsLayer {
                    rotationZ = (fase - 0.5f) * 2f * amplitud
                    transformOrigin = TransformOrigin(0.5f, 0.85f)
                },
        )
    }
}

@Composable
private fun Subtitulo(estado: EstadoUi) {
    val texto = when {
        estado.subtitulo.isNotEmpty() -> estado.subtitulo
        estado.fase == Fase.ESPERANDO -> "Siéntate... y toca el sombrero"
        else -> ""
    }
    val parpadeo by rememberInfiniteTransition(label = "pista").animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "alfa",
    )
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedContent(texto, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "sub") { t ->
            val esPista = estado.subtitulo.isEmpty()
            Text(
                t,
                color = Color.White.copy(alpha = if (esPista) parpadeo else 1f),
                fontFamily = FontFamily.Serif,
                fontStyle = if (esPista) FontStyle.Italic else null,
                fontSize = if (estado.casa != null && t == "¡${estado.casa.nombre.uppercase()}!") 34.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
            )
        }
    }
}

@Composable
private fun PiePagina(estado: EstadoUi) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(estado.fase == Fase.REVELADO && !estado.hablando, enter = fadeIn(), exit = fadeOut()) {
            Text(
                "Toca para la siguiente persona",
                color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CASAS.forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(c.colorSecundario, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${estado.conteo[c.id] ?: 0}",
                        color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

/** Escudo sencillo con los colores de la casa y su inicial. */
@Composable
fun Escudo(casa: Casa, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val forma = Path().apply {
                moveTo(w * 0.1f, h * 0.08f)
                lineTo(w * 0.9f, h * 0.08f)
                lineTo(w * 0.9f, h * 0.5f)
                quadraticTo(w * 0.88f, h * 0.82f, w * 0.5f, h * 0.96f)
                quadraticTo(w * 0.12f, h * 0.82f, w * 0.1f, h * 0.5f)
                close()
            }
            drawPath(forma, Brush.verticalGradient(listOf(casa.colorPrincipal, lerpNegro(casa.colorPrincipal))))
            drawPath(forma, casa.colorSecundario, style = Stroke(width = w * 0.06f))
        }
        Text(
            casa.nombre.take(1),
            color = casa.colorSecundario, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black,
            fontSize = 44.sp, modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

/** Cielo con estrellas que titilan. */
@Composable
private fun Estrellas(modifier: Modifier, color: Color) {
    val estrellas = remember {
        List(70) { Triple(Offset(Random.nextFloat(), Random.nextFloat()), Random.nextFloat() * 2.2f + 0.8f, Random.nextFloat()) }
    }
    val t by rememberInfiniteTransition(label = "estrellas").animateFloat(
        0f, 1f, infiniteRepeatable(tween(4000), RepeatMode.Restart), label = "t",
    )
    Canvas(modifier) {
        estrellas.forEach { (pos, radio, desfase) ->
            val alfa = 0.25f + 0.6f * abs(sin((t + desfase) * 2f * PI.toFloat()))
            drawCircle(color.copy(alpha = alfa), radius = radio.dp.toPx() / 2f, center = Offset(pos.x * size.width, pos.y * size.height))
        }
    }
}

private fun lerpNegro(c: Color): Color = Color(c.red * 0.25f, c.green * 0.25f, c.blue * 0.25f, 1f)
