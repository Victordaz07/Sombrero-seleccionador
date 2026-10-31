@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.victordaz.sombrero

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

internal val Dorado = Color(0xFFE8C15A)
internal val Pergamino = Color(0xFFFFEED2)
internal val Tinta = Color(0xFF0C101B)

@Composable
fun PantallaSombrero(vm: SombreroViewModel) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Dorado, onPrimary = Tinta,
        secondary = Pergamino, background = Tinta,
        surface = Color(0xFF171A24), onSurface = Pergamino,
    )) {
        ContenidoSombrero(vm)
    }
}

@Composable
private fun ContenidoSombrero(vm: SombreroViewModel) {
    val estado = vm.estado
    val casa = estado.casa
    var mostrarAjustes by rememberSaveable { mutableStateOf(false) }
    val haptico = LocalHapticFeedback.current
    LaunchedEffect(casa) {
        if (casa != null) haptico.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val tinte by animateColorAsState(
        casa?.colorPrincipal?.copy(alpha = 0.55f) ?: Color.Transparent,
        tween(1100), label = "color de la casa",
    )
    val t = textos(estado.idioma)
    val calle = vm.estadoAjustes.modoCalle
    // Pista breve para el operador cuando toca algo que necesita toque largo.
    var pista by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pista) {
        if (pista != null) {
            delay(2500)
            pista = null
        }
    }
    Box(Modifier.fillMaxSize().background(Tinta).clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null,
        enabled = estado.fase != Fase.PENSANDO,
        onClickLabel = if (estado.fase == Fase.REVELADO) t.botonSiguiente else t.botonComenzar,
        onClick = vm::alTocar,
    )) {
        Image(painterResource(R.drawable.fondo_noche), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(tinte))
        // Keep the hall visible while protecting text contrast in every house.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Tinta.copy(alpha = 0.76f), Tinta.copy(alpha = 0.15f), Tinta.copy(alpha = 0.92f),
        ))))
        Estrellas(Modifier.fillMaxSize(), color = acento(casa))

        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val horizontal = maxWidth > maxHeight && maxWidth >= 600.dp
            val alto = maxHeight
            val ancho = maxWidth
            if (horizontal) {
                Row(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Encabezado(estado)
                        SombreroAnimado(estado, (alto * 0.55f).coerceIn(160.dp, 350.dp))
                    }
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Subtitulo(estado)
                        if (vm.estadoAjustes.modoCalle) PanelInvitacion(estado, vm::invitar)
                        Accion(estado, vm::alTocar)
                        PiePagina(estado, vm.estadoAjustes.casasActivas, vm::elegirCasaManual, compacto = calle) {
                            pista = t.mantenerPresionado
                        }
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        .heightIn(min = alto).padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Encabezado(estado)
                    // En modo calle el sombrero es más chico para dejar espacio a los botones grandes.
                    val proporcion = when {
                        casa != null -> 0.32f
                        calle -> 0.26f
                        else -> 0.43f
                    }
                    SombreroAnimado(estado, minOf(ancho - 32.dp, (alto * proporcion).coerceIn(150.dp, 390.dp)))
                    Column(
                        Modifier.widthIn(max = 520.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Subtitulo(estado)
                        if (vm.estadoAjustes.modoCalle) PanelInvitacion(estado, vm::invitar)
                        Accion(estado, vm::alTocar)
                        PiePagina(estado, vm.estadoAjustes.casasActivas, vm::elegirCasaManual, compacto = calle) {
                            pista = t.mantenerPresionado
                        }
                    }
                }
            }
        }
        Row(
            Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Música: un toque la prende o la apaga.
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Tinta.copy(alpha = 0.7f))
                    .clickable(onClickLabel = t.musica, onClick = vm::alternarMusica),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (estado.musica) "🎵" else "🔇", fontSize = 20.sp)
            }
            // Ajustes: solo con toque largo, para que nadie los abra sin querer.
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Tinta.copy(alpha = 0.7f))
                    .combinedClickable(
                        enabled = estado.fase != Fase.PENSANDO,
                        onClick = { pista = t.mantenerPresionado },
                        onLongClickLabel = t.ajustes,
                        onLongClick = { mostrarAjustes = true },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Settings, t.ajustes, tint = Pergamino.copy(alpha = if (estado.fase == Fase.PENSANDO) 0.3f else 0.9f))
            }
        }
        IndicadorBateria(Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 14.dp))
        pista?.let {
            Text(
                it,
                Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                    .background(Tinta.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                    .border(1.dp, Dorado.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = Pergamino, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
        }
        estado.aviso?.let {
            Text(
                "⚠ $it",
                Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                    .background(Color(0xFFB3261E), RoundedCornerShape(16.dp)).padding(12.dp),
                color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center,
            )
        }
        SelectorIdioma(
            estado, vm::cambiarIdioma,
            Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(8.dp),
        )
        if (estado.volumenBajo) {
            Text(
                t.volumenBajo,
                Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 60.dp, start = 16.dp, end = 16.dp)
                    .background(Color(0xFFB3261E), RoundedCornerShape(20.dp))
                    .clickable(onClick = vm::mostrarControlVolumen)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            )
        }
    }
    if (mostrarAjustes) AjustesDialogo(vm, alCerrar = { mostrarAjustes = false })
}

@Composable
private fun Encabezado(estado: EstadoUi) {
    val t = textos(estado.idioma)
    AnimatedContent(
        targetState = estado.casa,
        transitionSpec = { (fadeIn(tween(650)) + scaleIn(initialScale = 0.85f, animationSpec = tween(650))) togetherWith fadeOut(tween(200)) },
        label = "revelación",
    ) { casa ->
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (casa == null) t.etiquetaInicio else t.etiquetaCasa, color = Pergamino.copy(alpha = 0.8f),
                fontSize = 11.sp, letterSpacing = 3.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            if (casa != null) Escudo(casa, Modifier.size(88.dp), idioma = estado.idioma)
            Text(
                casa?.nombre?.uppercase() ?: t.titulo,
                color = acento(casa), fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, fontSize = if (casa == null) 34.sp else 29.sp,
                lineHeight = 38.sp, textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 3f), 12f)),
            )
            Spacer(Modifier.height(8.dp))
            if (casa == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(Modifier.width(42.dp), color = Dorado.copy(alpha = 0.5f))
                    Text("✦", color = Dorado, fontSize = 12.sp)
                    HorizontalDivider(Modifier.width(42.dp), color = Dorado.copy(alpha = 0.5f))
                }
            } else {
                Text(t.lema(casa, estado.idioma), color = Pergamino, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            if (estado.vozLista == false) {
                Text(t.sinVoz,
                    Modifier.padding(top = 8.dp), color = Color(0xFFFFB4A0), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SombreroAnimado(estado: EstadoUi, tamano: Dp) {
    val infinita = rememberInfiniteTransition(label = "sombrero")
    val pensando = estado.fase == Fase.PENSANDO
    val fase by infinita.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "balanceo")
    val amplitud by animateFloatAsState(if (pensando) 5f else 1.8f, tween(600), label = "amplitud")
    // Same speaking signal and timing as the original app: no changes to TTS or ViewModel.
    val movBoca by infinita.animateFloat(0f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Restart), label = "boca")
    val abierta = if (estado.hablando) 0.2f + 0.8f * abs(sin(movBoca * 5f * PI.toFloat())) else 0f
    val boca by animateFloatAsState(abierta, tween(70), label = "bocaSuave")
    val cejas by animateFloatAsState(if (estado.hablando) 1f else 0f, tween(300), label = "cejas")
    val pulso by infinita.animateFloat(0.6f, 1f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulso")
    val brillo by animateFloatAsState(if (pensando || estado.casa != null) 1f else 0.4f, tween(800), label = "brillo")
    val color = acento(estado.casa)
    Box(Modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.35f * brillo * pulso), Color.Transparent),
                center = center, radius = size.minDimension / 2f))
        }
        SombreroDibujo(boca, cejas, Modifier.fillMaxSize().padding(10.dp).graphicsLayer {
            rotationZ = (fase - 0.5f) * 2f * amplitud
            translationY = (fase - 0.5f) * 8.dp.toPx()
            transformOrigin = TransformOrigin(0.5f, 0.85f)
        })
    }
}

@Composable
private fun Subtitulo(estado: EstadoUi) {
    val t = textos(estado.idioma)
    val texto = estado.subtitulo.ifEmpty {
        if (estado.fase == Fase.ESPERANDO) t.pistaEsperando else t.pistaPensando
    }
    Column(
        Modifier.fillMaxWidth().background(Tinta.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
            .border(1.dp, Dorado.copy(alpha = 0.22f), RoundedCornerShape(18.dp)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(when {
            estado.hablando -> t.estadoHablando
            estado.fase == Fase.PENSANDO -> t.estadoPensando
            estado.fase == Fase.REVELADO -> t.estadoRevelado
            else -> t.estadoInicio
        }, color = Dorado, fontSize = 10.sp, letterSpacing = 1.5.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        AnimatedContent(texto, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "subtitulo") { t ->
            Text(t, color = Pergamino, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic,
                fontSize = 20.sp, lineHeight = 27.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Accion(estado: EstadoUi, alTocar: () -> Unit) {
    val t = textos(estado.idioma)
    // Mientras invita (ESPERANDO + hablando) sí se puede empezar: la persona aceptó.
    val disponible = estado.fase != Fase.PENSANDO && !(estado.hablando && estado.fase != Fase.ESPERANDO)
    OutlinedButton(
        onClick = alTocar, enabled = disponible,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Dorado.copy(alpha = if (disponible) 0.85f else 0.25f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF35291B).copy(alpha = 0.8f), contentColor = Pergamino,
            disabledContainerColor = Tinta.copy(alpha = 0.6f), disabledContentColor = Pergamino.copy(alpha = 0.65f),
        ),
    ) {
        Text(when {
            estado.fase == Fase.ESPERANDO -> t.botonComenzar
            estado.fase == Fase.PENSANDO || estado.hablando -> t.botonEscuchar
            estado.fase == Fase.REVELADO -> t.botonSiguiente
            else -> t.botonComenzar
        }, fontFamily = FontFamily.Serif, fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}

/** Batería del teléfono: en la calle no hay dónde cargar. Se pone roja al 20% o menos. */
@Composable
private fun IndicadorBateria(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val bateria by produceState(initialValue = -1 to false) {
        while (true) {
            val intent = contexto.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val nivel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val escala = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val enchufado = (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
            value = (if (nivel >= 0 && escala > 0) nivel * 100 / escala else -1) to enchufado
            delay(30_000)
        }
    }
    val (porcentaje, cargando) = bateria
    if (porcentaje < 0) return
    val baja = porcentaje <= 20 && !cargando
    Text(
        "${if (cargando) "⚡" else "🔋"} $porcentaje%",
        modifier.background(if (baja) Color(0xFFB3261E) else Tinta.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = Color.White, fontSize = 12.sp, fontWeight = if (baja) FontWeight.Bold else FontWeight.Normal,
    )
}

/** Idioma de la ceremonia, siempre a la vista (arriba a la izquierda). */
@Composable
private fun SelectorIdioma(estado: EstadoUi, alCambiar: (Idioma) -> Unit, modifier: Modifier = Modifier) {
    val activo = estado.fase != Fase.PENSANDO
    Row(
        modifier.background(Tinta.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
            .border(1.dp, Dorado.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(3.dp),
    ) {
        listOf(Idioma.EN to "🇺🇸 EN", Idioma.ES to "🇲🇽 ES").forEach { (idioma, etiqueta) ->
            val elegido = estado.idioma == idioma
            Text(
                etiqueta,
                Modifier.clip(RoundedCornerShape(16.dp))
                    .background(if (elegido) Dorado else Color.Transparent)
                    .clickable(enabled = activo && !elegido) { alCambiar(idioma) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (elegido) Tinta else Pergamino.copy(alpha = if (activo) 0.9f else 0.4f),
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Modo calle: el operador toca a quién invitar y el sombrero lo llama. */
@Composable
private fun PanelInvitacion(estado: EstadoUi, alInvitar: (Publico) -> Unit) {
    if (estado.fase != Fase.ESPERANDO && estado.fase != Fase.REVELADO) return
    val t = textos(estado.idioma)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(t.invitarA, color = Dorado, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        listOf(Publico.entries.take(4), Publico.entries.drop(4)).forEach { fila ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                fila.forEach { publico ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF35291B).copy(alpha = 0.85f))
                            .border(1.dp, Dorado.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable { alInvitar(publico) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(publico.emoji, fontSize = 30.sp)
                        Text(publico.etiqueta(estado.idioma), color = Pergamino, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun PiePagina(
    estado: EstadoUi,
    activas: Set<String>,
    alElegirCasa: (Casa) -> Unit,
    compacto: Boolean = false,
    alTocarCorto: () -> Unit = {},
) {
    val t = textos(estado.idioma)
    // Tocar un escudo hace la ceremonia completa y termina en esa casa (para el operador).
    val sePuedeElegir = estado.fase == Fase.ESPERANDO || estado.fase == Fase.REVELADO
    val haptico = LocalHapticFeedback.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(t.lasCuatroCasas, color = Pergamino.copy(alpha = 0.65f), fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CASAS.forEach { c ->
                Column(
                    Modifier.weight(1f).graphicsLayer { alpha = if (c.id in activas) 1f else 0.4f }
                        .clip(RoundedCornerShape(12.dp))
                        // Toque largo = elegir esa casa a mano (un toque normal no hace nada, para evitar accidentes).
                        .combinedClickable(
                            enabled = sePuedeElegir,
                            onClick = alTocarCorto,
                            onLongClickLabel = c.nombre,
                            onLongClick = {
                                haptico.performHapticFeedback(HapticFeedbackType.LongPress)
                                alElegirCasa(c)
                            },
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${c.nombre}: ${estado.conteo[c.id] ?: 0} ${t.participantes}" +
                                if (c.id in activas) "" else ", ${t.noParticipa}"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Escudo(c, Modifier.size(if (compacto) 34.dp else 48.dp), describir = false, idioma = estado.idioma)
                    Text("${estado.conteo[c.id] ?: 0}", color = Pergamino, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun Escudo(casa: Casa, modifier: Modifier = Modifier, describir: Boolean = true, idioma: Idioma = Idioma.ES) {
    val recurso = when (casa.id) {
        "gryffindor" -> R.drawable.escudo_gryffindor
        "hufflepuff" -> R.drawable.escudo_hufflepuff
        "ravenclaw" -> R.drawable.escudo_ravenclaw
        "slytherin" -> R.drawable.escudo_slytherin
        else -> return
    }
    Image(painterResource(recurso), if (describir) "${textos(idioma).escudoDe} ${casa.nombre}" else null,
        modifier = modifier, contentScale = ContentScale.Fit)
}

// Hufflepuff's secondary color is near-black: use pale gold for readable text/light.
private fun acento(casa: Casa?): Color = when (casa?.id) {
    "hufflepuff" -> Color(0xFFFFDC83)
    "ravenclaw" -> Color(0xFFE6B981)
    else -> casa?.colorSecundario ?: Dorado
}

@Composable
private fun Estrellas(modifier: Modifier, color: Color) {
    val estrellas = remember {
        List(48) { Triple(Offset(Random.nextFloat(), Random.nextFloat()), Random.nextFloat() * 2.2f + 0.8f, Random.nextFloat()) }
    }
    val t by rememberInfiniteTransition(label = "estrellas").animateFloat(0f, 1f,
        infiniteRepeatable(tween(4000), RepeatMode.Restart), label = "destello")
    Canvas(modifier) {
        estrellas.forEach { (pos, radio, desfase) ->
            val alfa = 0.15f + 0.5f * abs(sin((t + desfase) * 2f * PI.toFloat()))
            drawCircle(color.copy(alpha = alfa), radio.dp.toPx() / 2f, Offset(pos.x * size.width, pos.y * size.height))
        }
    }
}
