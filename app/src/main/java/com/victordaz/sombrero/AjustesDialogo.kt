package com.victordaz.sombrero

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AjustesDialogo(vm: SombreroViewModel, alCerrar: () -> Unit) {
    val a = vm.estadoAjustes
    val conteo = vm.estado.conteo
    val actividad = LocalContext.current as? Activity
    var confirmarReinicio by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Dorado, onPrimary = Tinta, surface = Color(0xFF171A24),
        onSurface = Pergamino, onSurfaceVariant = Color(0xFFD1C5B3),
    )) {
        AlertDialog(
            onDismissRequest = alCerrar,
            confirmButton = { TextButton(onClick = alCerrar) { Text("Listo") } },
            title = { Text("Ajustes del sombrero", fontFamily = FontFamily.Serif, color = Dorado) },
            containerColor = Color(0xFF171A24),
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Seccion("Idioma / Language")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Idioma.entries.forEach { idioma ->
                            OpcionRadio(idioma.nombre, a.idioma == idioma, Modifier.weight(1f)) {
                                vm.cambiarIdioma(idioma)
                            }
                        }
                    }

                    Seccion("Volumen")
                    Text("Volumen: ${(a.volumen * 100).toInt()}% (bocina del teléfono o Bluetooth)", fontSize = 14.sp)
                    Slider(a.volumen, onValueChange = vm::cambiarVolumen, valueRange = 0f..1f)
                    OutlinedButton(onClick = vm::probarVoz, modifier = Modifier.fillMaxWidth()) { Text("🔊 Probar sonido") }

                    Seccion("Música de fondo")
                    FilaSwitch("Fanfarria con aplausos al revelar la casa", a.fanfarria, vm::cambiarFanfarria)
                    FilaSwitch("Música mágica de fondo", a.musica, vm::cambiarMusica)
                    if (a.musica) {
                        Text("Volumen de la música: ${(a.volumenMusica * 100).toInt()}% (baja sola cuando habla el sombrero)", fontSize = 14.sp)
                        Slider(a.volumenMusica, onValueChange = vm::cambiarVolumenMusica, valueRange = 0f..1f)
                    } else {
                        Text(
                            "Sin música suena un silencio que no se oye, para que la bocina Bluetooth no se duerma " +
                                "y no se corte la primera palabra del sombrero.",
                            fontSize = 12.sp, color = Color.Gray,
                        )
                    }

                    Seccion("Calle")
                    Text(
                        "Fijar la app: nadie puede salir de ella con los botones de Atrás o Inicio. " +
                            "Para soltarla, usa el botón de abajo o mantén presionados Atrás y Recientes a la vez.",
                        fontSize = 12.sp, color = Color.Gray,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { actividad?.startLockTask() }, modifier = Modifier.weight(1f)) { Text("📌 Fijar") }
                        OutlinedButton(onClick = { actividad?.let { runCatching { it.stopLockTask() } } }, modifier = Modifier.weight(1f)) {
                            Text("Soltar")
                        }
                    }

                    Seccion("Voz")
                    FilaSwitch(
                        "Usar voces grabadas",
                        a.usarGrabaciones, vm::cambiarUsarGrabaciones,
                    )
                    Text(
                        "Discursos con audio en ${a.idioma.nombre}: ${a.discursosGrabados} de ${a.discursosTotales}. " +
                            "Lo que no tenga audio lo dice la voz del teléfono:",
                        fontSize = 12.sp, color = Color.Gray,
                    )
                    Text("Tono: ${"%.2f".format(a.tono)} (más bajo = más grave)", fontSize = 14.sp)
                    Slider(a.tono, onValueChange = vm::cambiarTono, valueRange = 0.3f..1.5f)
                    Text("Velocidad: ${"%.2f".format(a.velocidad)}", fontSize = 14.sp)
                    Slider(a.velocidad, onValueChange = vm::cambiarVelocidad, valueRange = 0.5f..1.5f)
                    OutlinedButton(onClick = vm::probarVoz, modifier = Modifier.fillMaxWidth()) { Text("🔊 Probar voz") }

                    if (a.voces.isNotEmpty()) {
                        Spacer(Modifier.padding(4.dp))
                        Text("Elige la voz que más suene a sombrero:", fontSize = 14.sp)
                        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                            OpcionRadio("Automática", a.voz == null) { vm.cambiarVoz(null) }
                            a.voces.forEach { (nombre, etiqueta) ->
                                OpcionRadio(etiqueta, a.voz == nombre) { vm.cambiarVoz(nombre) }
                            }
                        }
                    }

                    Seccion("Selección")
                    FilaSwitch("Modo calle: botones para que el sombrero invite a la gente", a.modoCalle, vm::cambiarModoCalle)
                    FilaSwitch("Preguntar idioma a cada persona (English / Español)", a.preguntarIdioma, vm::cambiarPreguntarIdioma)
                    FilaSwitch("Repartir parejo entre casas", a.equilibrar, vm::cambiarEquilibrar)
                    Text(
                        "Para elegir tú la casa (por ejemplo, para repetir una selección y grabarla), mantén presionado " +
                            "su escudo abajo en la pantalla: el sombrero hace la ceremonia completa y termina en esa casa.",
                        fontSize = 12.sp, color = Color.Gray,
                    )
                    FilaSwitch("Sumar al conteo las casas elegidas a mano", a.contarManuales, vm::cambiarContarManuales)
                    FilaSwitch("Activar con botón de volumen / control Bluetooth", a.controlRemoto, vm::cambiarControlRemoto)
                    Spacer(Modifier.padding(4.dp))
                    Text("Casas participantes:", fontSize = 14.sp)
                    CASAS.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.alternarCasa(c.id) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(c.id in a.casasActivas, onCheckedChange = { vm.alternarCasa(c.id) })
                            Escudo(c, Modifier.width(28.dp), describir = false)
                            Spacer(Modifier.width(8.dp))
                            Text(c.nombre, Modifier.weight(1f))
                            Text("${conteo[c.id] ?: 0} personas", fontSize = 13.sp, color = Color(0xFFD1C5B3))
                        }
                    }

                    Seccion("Conteo")
                    if (!confirmarReinicio) {
                        OutlinedButton(onClick = { confirmarReinicio = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Reiniciar conteo de personas")
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { confirmarReinicio = false }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                            OutlinedButton(
                                onClick = { vm.reiniciarConteo(); confirmarReinicio = false },
                                modifier = Modifier.weight(1f),
                            ) { Text("Sí, reiniciar") }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun Seccion(titulo: String) {
    Spacer(Modifier.padding(6.dp))
    HorizontalDivider()
    Spacer(Modifier.padding(4.dp))
    Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFE8C15A))
    Spacer(Modifier.padding(2.dp))
}

@Composable
private fun OpcionRadio(texto: String, seleccionada: Boolean, modifier: Modifier = Modifier, alElegir: () -> Unit) {
    Row(modifier.fillMaxWidth().clickable(onClick = alElegir), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(seleccionada, onClick = alElegir)
        Text(texto, fontSize = 13.sp)
    }
}

@Composable
private fun FilaSwitch(texto: String, valor: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, Modifier.weight(1f), fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Switch(valor, onCheckedChange = alCambiar)
    }
}
