package com.victordaz.sombrero

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Fase { ESPERANDO, PENSANDO, REVELADO }

data class EstadoUi(
    val fase: Fase = Fase.ESPERANDO,
    val subtitulo: String = "",
    val casa: Casa? = null,
    val hablando: Boolean = false,
    /** null = cargando, true = hay voz en español, false = no hay voz. */
    val vozLista: Boolean? = null,
    val conteo: Map<String, Int> = emptyMap(),
)

data class EstadoAjustes(
    val tono: Float,
    val velocidad: Float,
    val voz: String?,
    val voces: List<Pair<String, String>>, // nombre interno -> nombre para mostrar
    val equilibrar: Boolean,
    val controlRemoto: Boolean,
    val usarGrabaciones: Boolean,
    val frasesGrabadas: Int,
    val frasesTotales: Int,
    val casasActivas: Set<String>,
)

class SombreroViewModel(app: Application) : AndroidViewModel(app) {

    private val ajustes = Ajustes(app)
    private val frases = Frases.cargar(app)
    private val grabada = VozGrabada(app)
    private var trabajo: Job? = null

    private val voz = VozDelSombrero(app) { lista ->
        aplicarAjustesDeVoz()
        estado = estado.copy(vozLista = lista)
        refrescarAjustes()
    }

    var estado by mutableStateOf(EstadoUi(conteo = ajustes.conteo()))
        private set

    var estadoAjustes by mutableStateOf(leerAjustes())
        private set

    val controlRemoto get() = ajustes.controlRemoto

    /** Se llama al tocar la pantalla o al presionar el control remoto. */
    fun alTocar() {
        when (estado.fase) {
            Fase.ESPERANDO -> seleccionar()
            Fase.PENSANDO -> Unit // el sombrero está pensando, no lo interrumpas
            Fase.REVELADO -> {
                trabajo?.cancel()
                voz.callar()
                estado = estado.copy(fase = Fase.ESPERANDO, casa = null, subtitulo = "", hablando = false)
            }
        }
    }

    private fun seleccionar() {
        val activas = CASAS.filter { it.id in ajustes.casasActivas }.ifEmpty { CASAS }
        val casa = elegirCasa(activas)
        val guion = frases.guion(casa, activas - casa)

        trabajo = viewModelScope.launch {
            estado = estado.copy(fase = Fase.PENSANDO, casa = null, subtitulo = "")
            delay(400)
            for (linea in guion) {
                if (linea.esAnuncio) {
                    ajustes.sumar(casa.id)
                    estado = estado.copy(fase = Fase.REVELADO, casa = casa, conteo = ajustes.conteo())
                }
                estado = estado.copy(
                    subtitulo = if (linea.esAnuncio) "¡${casa.nombre.uppercase()}!" else linea.texto,
                    hablando = true,
                )
                decir(linea)
                estado = estado.copy(hablando = false)
                delay(linea.pausaDespues)
            }
        }
    }

    /** Usa el audio grabado si existe; si no, la voz del teléfono. */
    private suspend fun decir(linea: Linea) {
        if (ajustes.usarGrabaciones && grabada.tiene(linea.clave)) {
            grabada.reproducir(linea.clave)
        } else {
            voz.hablar(linea.texto, enfasis = linea.esAnuncio)
        }
    }

    private fun elegirCasa(activas: List<Casa>): Casa {
        if (!ajustes.equilibrar) return activas.random()
        // Elige al azar entre las casas que tienen menos personas, para que queden parejas.
        val conteo = ajustes.conteo()
        val minimo = activas.minOf { conteo[it.id] ?: 0 }
        return activas.filter { (conteo[it.id] ?: 0) == minimo }.random()
    }

    // ----- Ajustes -----

    fun probarVoz() {
        if (estado.fase != Fase.ESPERANDO) return
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            estado = estado.copy(hablando = true)
            decir(frases.prueba)
            estado = estado.copy(hablando = false)
        }
    }

    fun cambiarTono(v: Float) {
        ajustes.tono = v; aplicarAjustesDeVoz(); refrescarAjustes()
    }

    fun cambiarVelocidad(v: Float) {
        ajustes.velocidad = v; aplicarAjustesDeVoz(); refrescarAjustes()
    }

    fun cambiarVoz(nombre: String?) {
        ajustes.voz = nombre; voz.usarVoz(nombre); refrescarAjustes()
    }

    fun cambiarEquilibrar(v: Boolean) {
        ajustes.equilibrar = v; refrescarAjustes()
    }

    fun cambiarControlRemoto(v: Boolean) {
        ajustes.controlRemoto = v; refrescarAjustes()
    }

    fun cambiarUsarGrabaciones(v: Boolean) {
        ajustes.usarGrabaciones = v; refrescarAjustes()
    }

    fun alternarCasa(id: String) {
        val actuales = ajustes.casasActivas
        val nuevas = if (id in actuales) actuales - id else actuales + id
        if (nuevas.isNotEmpty()) ajustes.casasActivas = nuevas
        refrescarAjustes()
    }

    fun reiniciarConteo() {
        ajustes.reiniciarConteo()
        estado = estado.copy(conteo = ajustes.conteo())
    }

    private fun aplicarAjustesDeVoz() {
        voz.tono = ajustes.tono
        voz.velocidad = ajustes.velocidad
        voz.usarVoz(ajustes.voz)
    }

    private fun leerAjustes() = EstadoAjustes(
        tono = ajustes.tono,
        velocidad = ajustes.velocidad,
        voz = ajustes.voz,
        voces = voz.vocesEnEspanol().map { v ->
            val red = if (v.isNetworkConnectionRequired) " (internet)" else ""
            v.name to "${v.locale.displayName} · ${v.name}$red"
        },
        equilibrar = ajustes.equilibrar,
        controlRemoto = ajustes.controlRemoto,
        usarGrabaciones = ajustes.usarGrabaciones,
        frasesGrabadas = frases.todas.count { grabada.tiene(it.clave) },
        frasesTotales = frases.todas.size,
        casasActivas = ajustes.casasActivas,
    )

    private fun refrescarAjustes() {
        estadoAjustes = leerAjustes()
    }

    override fun onCleared() {
        voz.apagar()
    }
}
