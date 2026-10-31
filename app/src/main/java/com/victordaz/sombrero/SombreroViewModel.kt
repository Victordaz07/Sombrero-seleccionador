package com.victordaz.sombrero

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** IDIOMA = el sombrero preguntó "English o español" y espera la respuesta. */
enum class Fase { ESPERANDO, IDIOMA, PENSANDO, REVELADO }

data class EstadoUi(
    val fase: Fase = Fase.ESPERANDO,
    val subtitulo: String = "",
    val casa: Casa? = null,
    val hablando: Boolean = false,
    /** null = cargando, true = hay voz del teléfono en el idioma, false = no hay voz. */
    val vozLista: Boolean? = null,
    val conteo: Map<String, Int> = emptyMap(),
    val idioma: Idioma = Idioma.ES,
    /** El volumen multimedia está tan bajo que casi no se escucharía. */
    val volumenBajo: Boolean = false,
    val musica: Boolean = true,
    /** Problema al leer frases.json (la app sigue funcionando con lo que pudo leer). */
    val aviso: String? = null,
)

data class EstadoAjustes(
    val idioma: Idioma,
    val tono: Float,
    val velocidad: Float,
    val voz: String?,
    val voces: List<Pair<String, String>>, // nombre interno -> nombre para mostrar
    val equilibrar: Boolean,
    val controlRemoto: Boolean,
    val preguntarIdioma: Boolean,
    val modoCalle: Boolean,
    val contarManuales: Boolean,
    val volumen: Float,
    val musica: Boolean,
    val volumenMusica: Float,
    val brilloMaximo: Boolean,
    val fanfarria: Boolean,
    val usarGrabaciones: Boolean,
    val discursosGrabados: Int,
    val discursosTotales: Int,
    val casasActivas: Set<String>,
)

class SombreroViewModel(app: Application) : AndroidViewModel(app) {

    private val ajustes = Ajustes(app)
    private val frases = Frases.cargar(app)
    private val grabada = VozGrabada(app)
    private val fondo = FondoMusical(app).apply { volumen = ajustes.volumenMusica }
    private val volumen = Volumen(app, alPerderFoco = ::detener).also {
        // Para la fiesta: si al abrir la app el volumen multimedia está bajo, se sube.
        if (it.nivel < 0.5f) it.poner(0.8f, mostrarEnPantalla = false)
    }
    private var trabajo: Job? = null

    /** Discursos: se usan todos antes de repetir alguno. */
    private val rotador = Rotador<Discurso>(clave = { it.clave })

    /** Última versión dicha de cada frase que tiene variantes, para no repetirla seguida. */
    private val ultimaVersion = mutableMapOf<String, String>()

    /** Tras una invitación el idioma ya está elegido: la ceremonia no vuelve a preguntarlo. */
    private var invitacionReciente = false

    /** Casa elegida a mano para la próxima ceremonia (null = la elige el sombrero). */
    private var casaManual: Casa? = null

    /** Si la ceremonia actual suma al conteo (las elegidas a mano no, salvo que se active). */
    private var sumarAlConteo = true

    private val voz: VozDelSombrero = VozDelSombrero(app) {
        val lista = voz.cambiarIdioma(ajustes.idioma)
        aplicarAjustesDeVoz()
        estado = estado.copy(vozLista = lista)
        refrescarAjustes()
    }

    var estado by mutableStateOf(
        EstadoUi(
            conteo = ajustes.conteo(), idioma = ajustes.idioma, volumenBajo = volumen.nivel < 0.25f,
            musica = ajustes.musica, aviso = frases.error,
        ),
    )
        private set

    var estadoAjustes by mutableStateOf(leerAjustes())
        private set

    val controlRemoto get() = ajustes.controlRemoto

    init {
        fondo.sonar(ajustes.musica)
    }

    /** Se llama al tocar la pantalla o al presionar el control remoto. */
    fun alTocar() {
        refrescarVolumen()
        when (estado.fase) {
            Fase.ESPERANDO -> {
                casaManual = null
                empezarCeremonia()
            }
            // Con el control remoto se confirma el idioma actual.
            Fase.IDIOMA -> elegirIdioma(estado.idioma)
            Fase.PENSANDO -> Unit // el sombrero está pensando, no lo interrumpas
            Fase.REVELADO -> {
                callar()
                invitacionReciente = false
                estado = estado.copy(fase = Fase.ESPERANDO, casa = null, subtitulo = "", hablando = false)
            }
        }
    }

    /** El sombrero pregunta en qué idioma hablar; la respuesta llega con [elegirIdioma]. */
    private fun preguntarIdioma() {
        val pregunta = elegirVersion("pregunta", frases.preguntasIdioma)
        estado = estado.copy(fase = Fase.IDIOMA, casa = null, subtitulo = pregunta.texto, hablando = true)
        lanzar {
            decir(pregunta)
            estado = estado.copy(hablando = false)
        }
    }

    /** La persona eligió idioma: el sombrero responde y empieza la selección en ese idioma. */
    fun elegirIdioma(idioma: Idioma) {
        if (estado.fase != Fase.IDIOMA) return
        aplicarIdioma(idioma)
        seleccionar(introduccion = elegirVersion("respuesta-${idioma.codigo}", frases.respuestasIdioma(idioma)))
    }

    private fun empezarCeremonia() {
        val preguntar = ajustes.preguntarIdioma && !invitacionReciente
        invitacionReciente = false
        if (preguntar) preguntarIdioma() else seleccionar()
    }

    /** Modo calle: el sombrero invita a quien va pasando, en el idioma elegido en pantalla. */
    fun invitar(publico: Publico) {
        if (estado.fase == Fase.PENSANDO || estado.fase == Fase.IDIOMA) return
        val versiones = frases.invitaciones[estado.idioma]?.get(publico).orEmpty()
        if (versiones.isEmpty()) return
        val invitacion = elegirVersion("invitacion-${estado.idioma.codigo}-${publico.id}", versiones)
        invitacionReciente = true
        estado = estado.copy(fase = Fase.ESPERANDO, casa = null, subtitulo = invitacion.texto, hablando = true)
        lanzar {
            decir(invitacion)
            estado = estado.copy(hablando = false)
        }
    }

    /**
     * El operador eligió la casa (toque largo en su escudo), por ejemplo para repetir una selección y grabarla.
     * La ceremonia es igual que siempre, pero termina en esa casa.
     */
    fun elegirCasaManual(casa: Casa) {
        if (estado.fase == Fase.PENSANDO || estado.fase == Fase.IDIOMA) return
        estado = estado.copy(fase = Fase.ESPERANDO, casa = null, subtitulo = "", hablando = false)
        casaManual = casa
        empezarCeremonia()
    }

    private fun seleccionar(introduccion: Linea? = null) {
        val activas = CASAS.filter { it.id in ajustes.casasActivas }.ifEmpty { CASAS }
        val manual = casaManual
        casaManual = null
        sumarAlConteo = manual == null || ajustes.contarManuales
        val casa = manual ?: Reparto.elegir(activas, ajustes.conteo(), ajustes.equilibrar)

        estado = estado.copy(fase = Fase.PENSANDO, casa = null, subtitulo = introduccion?.texto ?: "")
        lanzar {
            if (introduccion != null) {
                estado = estado.copy(hablando = true)
                decir(introduccion)
                estado = estado.copy(hablando = false)
                delay(200)
            } else {
                delay(400)
            }
            val grabados = if (ajustes.usarGrabaciones) discursosDe(casa).filter { grabada.tiene(it.clave) } else emptyList()
            val discurso = rotador.siguiente("${estado.idioma.codigo}-${casa.id}", grabados)
            when {
                discurso != null -> decirDiscurso(casa, discurso)
                // Sin audio grabado, la voz del teléfono lee un discurso completo.
                else -> decirDiscursoConVoz(casa, discursosDe(casa).randomOrNull())
            }
            // ¡Ta-ra-rá! y aplausos. Si tocan para la siguiente persona, se corta.
            if (ajustes.fanfarria) grabada.reproducirEfecto("fanfarria.ogg")
        }
    }

    private fun revelar(casa: Casa) {
        if (estado.fase == Fase.REVELADO) return
        if (sumarAlConteo) ajustes.sumar(casa.id)
        estado = estado.copy(fase = Fase.REVELADO, casa = casa, conteo = ajustes.conteo())
    }

    /** Reproduce un discurso completo y sincroniza subtítulos y revelación con el audio. */
    private suspend fun decirDiscurso(casa: Casa, discurso: Discurso) {
        val inicios = grabada.tiempos(discurso.clave)
        var actual = -1
        fun mostrar(i: Int) {
            if (i <= actual) return
            actual = i
            if (i >= discurso.indiceAnuncio) revelar(casa)
            estado = estado.copy(
                subtitulo = if (i == discurso.indiceAnuncio) anuncio(casa) else discurso.segmentos[i],
            )
        }
        mostrar(0)
        estado = estado.copy(hablando = true)
        val sono = grabada.reproducir(discurso.clave) { posicion, duracion ->
            mostrar(discurso.segmentoEn(posicion, duracion, inicios))
        }
        estado = estado.copy(hablando = false)
        // Si el audio no se pudo reproducir, que al menos lo diga la voz del teléfono.
        if (!sono) decirDiscursoConVoz(casa, discurso)
        revelar(casa)
    }

    /** Sin audio grabado: la voz del teléfono lee el discurso frase por frase (o solo grita la casa). */
    private suspend fun decirDiscursoConVoz(casa: Casa, discurso: Discurso?) {
        val segmentos = discurso?.segmentos ?: listOf(anuncio(casa))
        val indiceAnuncio = discurso?.indiceAnuncio ?: 0
        segmentos.forEachIndexed { i, texto ->
            val esAnuncio = i == indiceAnuncio
            if (esAnuncio) revelar(casa)
            estado = estado.copy(subtitulo = if (esAnuncio) anuncio(casa) else texto, hablando = true)
            voz.hablar(texto, enfasis = esAnuncio)
            estado = estado.copy(hablando = false)
            delay(if (i == indiceAnuncio - 1) 900 else 250)
        }
        revelar(casa)
    }

    /** Elige una versión al azar, sin repetir la anterior y prefiriendo las que tienen audio grabado. */
    private fun elegirVersion(tipo: String, versiones: List<Linea>): Linea {
        val conAudio = versiones.filter { grabada.tiene(it.clave) }.ifEmpty { versiones }
        val opciones = conAudio.filter { it.clave != ultimaVersion[tipo] }.ifEmpty { conAudio }
        return opciones.random().also { ultimaVersion[tipo] = it.clave }
    }

    private fun discursosDe(casa: Casa) = frases.discursos.getValue(estado.idioma).getValue(casa.id)

    private fun anuncio(casa: Casa) = textos(estado.idioma).anuncio(casa, estado.idioma)

    /** Usa el audio grabado si existe; si no, la voz del teléfono. */
    private suspend fun decir(linea: Linea) {
        val sono = ajustes.usarGrabaciones && grabada.tiene(linea.clave) && grabada.reproducir(linea.clave)
        if (!sono) voz.hablar(linea.texto)
    }

    /**
     * Corre una parte de la ceremonia. Antes espera a que la anterior termine de callarse
     * (así nunca suenan dos audios encimados), pide el foco de audio y baja la música.
     */
    private fun lanzar(bloque: suspend () -> Unit) {
        val anterior = trabajo
        voz.callar()
        trabajo = viewModelScope.launch {
            anterior?.cancelAndJoin()
            refrescarVolumen()
            volumen.pedirFoco()
            fondo.atenuar(true)
            try {
                bloque()
            } finally {
                fondo.atenuar(false)
                volumen.soltarFoco()
            }
        }
    }

    private fun callar() {
        trabajo?.cancel()
        voz.callar()
    }

    /**
     * Se interrumpe todo: la app pasó a segundo plano o entró una llamada.
     * Si la casa ya se reveló se queda en pantalla; si no, vuelve al inicio.
     */
    fun detener() {
        callar()
        invitacionReciente = false
        estado = if (estado.fase == Fase.REVELADO) {
            estado.copy(hablando = false)
        } else {
            estado.copy(fase = Fase.ESPERANDO, casa = null, subtitulo = "", hablando = false)
        }
    }

    fun alSalir() {
        detener()
        fondo.pausar()
    }

    fun alVolver() {
        refrescarVolumen()
        fondo.reanudar()
    }

    // ----- Ajustes -----

    fun probarVoz() {
        if (estado.fase != Fase.ESPERANDO) return
        estado = estado.copy(hablando = true)
        lanzar {
            // Si hay audio grabado de la respuesta de idioma, se prueba con ese (es el mismo camino que la ceremonia).
            val respuesta = frases.respuestasIdioma(estado.idioma).first()
            decir(if (grabada.tiene(respuesta.clave)) respuesta else frases.prueba(estado.idioma))
            estado = estado.copy(hablando = false)
        }
    }

    /** Cambio de idioma (selector de la pantalla o Ajustes): detiene lo que esté pasando y vuelve al inicio. */
    fun cambiarIdioma(nuevo: Idioma) {
        if (nuevo == ajustes.idioma) return
        callar()
        casaManual = null
        invitacionReciente = false
        estado = estado.copy(fase = Fase.ESPERANDO, casa = null, subtitulo = "", hablando = false)
        aplicarIdioma(nuevo)
    }

    private fun aplicarIdioma(nuevo: Idioma) {
        if (nuevo == ajustes.idioma && nuevo == estado.idioma) return
        ajustes.idioma = nuevo
        val lista = voz.cambiarIdioma(nuevo)
        aplicarAjustesDeVoz()
        estado = estado.copy(idioma = nuevo, vozLista = if (estado.vozLista == null) null else lista)
        refrescarAjustes()
    }

    fun alternarMusica() = cambiarMusica(!ajustes.musica)

    fun cambiarMusica(v: Boolean) {
        ajustes.musica = v
        fondo.sonar(v)
        estado = estado.copy(musica = v)
        refrescarAjustes()
    }

    fun cambiarVolumenMusica(v: Float) {
        ajustes.volumenMusica = v
        fondo.volumen = v
        refrescarAjustes()
    }

    fun cambiarFanfarria(v: Boolean) {
        ajustes.fanfarria = v; refrescarAjustes()
    }

    fun cambiarBrilloMaximo(v: Boolean) {
        ajustes.brilloMaximo = v; refrescarAjustes()
    }

    fun cambiarModoCalle(v: Boolean) {
        ajustes.modoCalle = v; refrescarAjustes()
    }

    fun cambiarContarManuales(v: Boolean) {
        ajustes.contarManuales = v; refrescarAjustes()
    }

    fun cambiarPreguntarIdioma(v: Boolean) {
        ajustes.preguntarIdioma = v; refrescarAjustes()
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
        idioma = ajustes.idioma,
        tono = ajustes.tono,
        velocidad = ajustes.velocidad,
        voz = ajustes.voz,
        voces = voz.voces().map { v ->
            val red = if (v.isNetworkConnectionRequired) " (internet)" else ""
            v.name to "${v.locale.displayName} · ${v.name}$red"
        },
        equilibrar = ajustes.equilibrar,
        controlRemoto = ajustes.controlRemoto,
        preguntarIdioma = ajustes.preguntarIdioma,
        modoCalle = ajustes.modoCalle,
        contarManuales = ajustes.contarManuales,
        volumen = volumen.nivel,
        musica = ajustes.musica,
        volumenMusica = ajustes.volumenMusica,
        brilloMaximo = ajustes.brilloMaximo,
        fanfarria = ajustes.fanfarria,
        usarGrabaciones = ajustes.usarGrabaciones,
        discursosGrabados = frases.discursos.getValue(ajustes.idioma).values.flatten().count { grabada.tiene(it.clave) },
        discursosTotales = frases.discursos.getValue(ajustes.idioma).values.sumOf { it.size },
        casasActivas = ajustes.casasActivas,
    )

    private fun refrescarAjustes() {
        estadoAjustes = leerAjustes()
    }

    fun refrescarVolumen() {
        estado = estado.copy(volumenBajo = volumen.nivel < 0.25f)
        if (estadoAjustes.volumen != volumen.nivel) refrescarAjustes()
    }

    fun subirVolumen() {
        volumen.poner(1f)
        refrescarVolumen()
    }

    fun cambiarVolumen(nivel: Float) {
        volumen.poner(nivel, mostrarEnPantalla = false)
        refrescarVolumen()
    }

    override fun onCleared() {
        voz.apagar()
        fondo.liberar()
    }
}
