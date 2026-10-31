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

/** IDIOMA = el sombrero preguntó en qué idioma hablar y espera la respuesta. */
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
    /** Idiomas que ofrece la pregunta (botones). */
    val idiomasPregunta: List<Idioma> = listOf(Idioma.EN, Idioma.ES),
    /** El volumen multimedia está tan bajo que casi no se escucharía. */
    val volumenBajo: Boolean = false,
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
    val idiomasPregunta: List<Idioma>,
    val contarManuales: Boolean,
    val volumen: Float,
    val usarGrabaciones: Boolean,
    val frasesGrabadas: Int,
    val frasesTotales: Int,
    val discursosGrabados: Int,
    val discursosTotales: Int,
    val casasActivas: Set<String>,
)

class SombreroViewModel(app: Application) : AndroidViewModel(app) {

    private val ajustes = Ajustes(app)
    private val frases = Frases.cargar(app)
    private val grabada = VozGrabada(app)
    private val volumen = Volumen(app).also {
        // Para la fiesta: si al abrir la app el volumen multimedia está bajo, se sube.
        if (it.nivel < 0.5f) it.poner(0.8f, mostrarEnPantalla = false)
    }
    private var trabajo: Job? = null

    /** Discursos pendientes por casa: se reparten todos antes de repetir alguno. */
    private val colaDiscursos = mutableMapOf<String, ArrayDeque<Discurso>>()
    private val ultimoDiscurso = mutableMapOf<String, String>()

    /** Última versión dicha de cada frase que tiene variantes, para no repetirla seguida. */
    private val ultimaVersion = mutableMapOf<String, String>()

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
        EstadoUi(conteo = ajustes.conteo(), idioma = ajustes.idioma, volumenBajo = volumen.nivel < 0.25f),
    )
        private set

    var estadoAjustes by mutableStateOf(leerAjustes())
        private set

    val controlRemoto get() = ajustes.controlRemoto

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
                trabajo?.cancel()
                voz.callar()
                estado = estado.copy(fase = Fase.ESPERANDO, casa = null, subtitulo = "", hablando = false)
            }
        }
    }

    /** El sombrero pregunta en qué idioma hablar; la respuesta llega con [elegirIdioma]. */
    private fun preguntarIdioma() {
        trabajo = lanzar {
            val idiomas = ajustes.idiomasPregunta
            val pregunta = if (Idioma.TL in idiomas) {
                elegirVersion("pregunta-tri", frases.preguntasTrilingues)
            } else {
                elegirVersion("pregunta", frases.preguntasIdioma)
            }
            estado = estado.copy(
                fase = Fase.IDIOMA, casa = null, subtitulo = pregunta.texto, hablando = true, idiomasPregunta = idiomas,
            )
            decir(pregunta)
            estado = estado.copy(hablando = false)
        }
    }

    /** La persona eligió idioma: el sombrero responde y empieza la selección en ese idioma. */
    fun elegirIdioma(idioma: Idioma) {
        if (estado.fase != Fase.IDIOMA) return
        trabajo?.cancel()
        voz.callar()
        aplicarIdioma(idioma)
        seleccionar(introduccion = elegirVersion("respuesta-${idioma.codigo}", frases.respuestasIdioma(idioma)))
    }

    private fun empezarCeremonia() {
        if (ajustes.preguntarIdioma) preguntarIdioma() else seleccionar()
    }

    /**
     * El operador eligió la casa tocando su escudo (por ejemplo, para repetir una selección y grabarla).
     * La ceremonia es igual que siempre, pero termina en esa casa.
     */
    fun elegirCasaManual(casa: Casa) {
        if (estado.fase == Fase.PENSANDO || estado.fase == Fase.IDIOMA) return
        trabajo?.cancel()
        voz.callar()
        estado = estado.copy(fase = Fase.ESPERANDO, casa = null, subtitulo = "", hablando = false)
        casaManual = casa
        empezarCeremonia()
    }

    private fun seleccionar(introduccion: Linea? = null) {
        val activas = CASAS.filter { it.id in ajustes.casasActivas }.ifEmpty { CASAS }
        val manual = casaManual
        casaManual = null
        sumarAlConteo = manual == null || ajustes.contarManuales
        val casa = manual ?: elegirCasa(activas)
        val guion = frases.guion(casa, activas - casa)

        trabajo = lanzar {
            estado = estado.copy(fase = Fase.PENSANDO, casa = null, subtitulo = "")
            if (introduccion != null) {
                estado = estado.copy(subtitulo = introduccion.texto, hablando = true)
                decir(introduccion)
                estado = estado.copy(hablando = false)
                delay(200)
            } else {
                delay(400)
            }
            val discurso = if (ajustes.usarGrabaciones) siguienteDiscurso(casa) else null
            when {
                discurso != null -> decirDiscurso(casa, discurso)
                // En inglés y tagalo no hay frases sueltas: la voz del teléfono lee un discurso completo.
                estado.idioma != Idioma.ES -> decirDiscursoConVoz(casa, discursosDe(casa).random())
                else -> decirGuion(casa, guion)
            }
        }
    }

    private fun revelar(casa: Casa) {
        if (estado.fase == Fase.REVELADO) return
        if (sumarAlConteo) ajustes.sumar(casa.id)
        estado = estado.copy(fase = Fase.REVELADO, casa = casa, conteo = ajustes.conteo())
    }

    /** Respaldo: arma el discurso con frases sueltas (audio grabado o voz del teléfono). */
    private suspend fun decirGuion(casa: Casa, guion: List<Linea>) {
        for (linea in guion) {
            if (linea.esAnuncio) revelar(casa)
            estado = estado.copy(
                subtitulo = if (linea.esAnuncio) anuncio(casa) else linea.texto,
                hablando = true,
            )
            decir(linea)
            estado = estado.copy(hablando = false)
            delay(linea.pausaDespues)
        }
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

    /** Sin audio grabado: la voz del teléfono lee el discurso frase por frase. */
    private suspend fun decirDiscursoConVoz(casa: Casa, discurso: Discurso) {
        discurso.segmentos.forEachIndexed { i, texto ->
            val esAnuncio = i == discurso.indiceAnuncio
            if (esAnuncio) revelar(casa)
            estado = estado.copy(subtitulo = if (esAnuncio) anuncio(casa) else texto, hablando = true)
            voz.hablar(texto, enfasis = esAnuncio)
            estado = estado.copy(hablando = false)
            delay(if (i == discurso.indiceAnuncio - 1) 900 else 250)
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

    /** Elige un discurso grabado de la casa sin repetir hasta haber usado todos. */
    private fun siguienteDiscurso(casa: Casa): Discurso? {
        val grabados = discursosDe(casa).filter { grabada.tiene(it.clave) }
        if (grabados.isEmpty()) return null
        val clave = "${estado.idioma.codigo}-${casa.id}"
        val cola = colaDiscursos.getOrPut(clave) { ArrayDeque() }
        if (cola.isEmpty()) {
            val mezcla = grabados.shuffled().toMutableList()
            // Que el primero de la nueva vuelta no sea el mismo que acaba de sonar.
            if (mezcla.size > 1 && mezcla.first().clave == ultimoDiscurso[clave]) {
                mezcla.add(mezcla.removeAt(0))
            }
            cola.addAll(mezcla)
        }
        return cola.removeFirst().also { ultimoDiscurso[clave] = it.clave }
    }

    /** Usa el audio grabado si existe; si no, la voz del teléfono. */
    private suspend fun decir(linea: Linea) {
        val sono = ajustes.usarGrabaciones && grabada.tiene(linea.clave) && grabada.reproducir(linea.clave)
        if (!sono) voz.hablar(linea.texto, enfasis = linea.esAnuncio)
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
        trabajo = lanzar {
            estado = estado.copy(hablando = true)
            // Si hay audio grabado de la respuesta de idioma, se prueba con ese (es el mismo camino que la ceremonia).
            val respuesta = frases.respuestasIdioma(estado.idioma).first()
            decir(if (grabada.tiene(respuesta.clave)) respuesta else frases.prueba(estado.idioma))
            estado = estado.copy(hablando = false)
        }
    }

    /** Cambio de idioma desde Ajustes: detiene lo que esté pasando y vuelve al inicio. */
    fun cambiarIdioma(nuevo: Idioma) {
        if (nuevo == ajustes.idioma) return
        trabajo?.cancel()
        voz.callar()
        casaManual = null
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

    fun cambiarContarManuales(v: Boolean) {
        ajustes.contarManuales = v; refrescarAjustes()
    }

    /** Agrega o quita un idioma de la pregunta (siempre quedan al menos dos). */
    fun alternarIdiomaPregunta(idioma: Idioma) {
        val actuales = ajustes.idiomasPregunta
        val nuevos = if (idioma in actuales) actuales - idioma else actuales + idioma
        if (nuevos.size >= 2) ajustes.idiomasPregunta = nuevos
        refrescarAjustes()
    }

    /** Control remoto: el "otro" idioma de la pregunta, siguiendo el orden de los botones. */
    fun siguienteIdiomaPregunta(): Idioma {
        val idiomas = estado.idiomasPregunta
        return idiomas[(idiomas.indexOf(estado.idioma) + 1).mod(idiomas.size)]
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
        idiomasPregunta = ajustes.idiomasPregunta,
        contarManuales = ajustes.contarManuales,
        volumen = volumen.nivel,
        usarGrabaciones = ajustes.usarGrabaciones,
        frasesGrabadas = frases.todas.count { grabada.tiene(it.clave) },
        frasesTotales = frases.todas.size,
        discursosGrabados = frases.discursos.getValue(ajustes.idioma).values.flatten().count { grabada.tiene(it.clave) },
        discursosTotales = frases.discursos.getValue(ajustes.idioma).values.sumOf { it.size },
        casasActivas = ajustes.casasActivas,
    )

    private fun refrescarAjustes() {
        estadoAjustes = leerAjustes()
    }

    /** Corre una parte de la ceremonia con el foco de audio, para que nada más suene encima. */
    private fun lanzar(bloque: suspend () -> Unit) = viewModelScope.launch {
        refrescarVolumen()
        volumen.pedirFoco()
        try {
            bloque()
        } finally {
            volumen.soltarFoco()
        }
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
    }
}
