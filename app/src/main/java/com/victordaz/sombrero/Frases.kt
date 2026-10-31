package com.victordaz.sombrero

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Una frase que dice el sombrero.
 *
 * - [clave] es el nombre de su audio grabado: `assets/voz/<clave>.mp3`.
 * - [pausaDespues] es cuánto espera (en milisegundos) antes de la siguiente.
 */
data class Linea(
    val clave: String,
    val texto: String,
    val pausaDespues: Long = 500,
    val esAnuncio: Boolean = false,
)

/**
 * Una actuación completa del sombrero para una casa, grabada en un solo audio.
 * [segmentos] son las frases (para los subtítulos) y [indiceAnuncio] es donde grita la casa.
 */
class Discurso(val clave: String, val segmentos: List<String>, val indiceAnuncio: Int) {

    /** Qué segmento se está diciendo en [posicionMs], con los tiempos exactos o estimando por largo del texto. */
    fun segmentoEn(posicionMs: Int, duracionMs: Int, inicios: List<Double>?): Int {
        val segundos = posicionMs / 1000.0
        if (inicios != null && inicios.size == segmentos.size) {
            return inicios.indexOfLast { it <= segundos }.coerceAtLeast(0)
        }
        if (duracionMs <= 0) return 0
        val total = segmentos.sumOf { it.length + 1 }.toDouble()
        var acumulado = 0.0
        segmentos.forEachIndexed { i, s ->
            acumulado += s.length + 1
            if (posicionMs < duracionMs * acumulado / total) return i
        }
        return segmentos.lastIndex
    }
}

/** Frases de una casa. */
class FrasesCasa(val virtudes: List<Linea>, val dudas: List<Linea>, val anuncio: Linea)

/**
 * Todas las frases del sombrero, leídas de `assets/frases.json`.
 *
 * Las claves se arman igual que en `herramientas/generar_voces.py`:
 * `apertura_1`, `decision_2`, `gryffindor_virtud_3`, `ravenclaw_duda_1`, `slytherin_anuncio`...
 */
class Frases(json: JSONObject) {

    private val aperturas = lista(json.getJSONArray("aperturas"), "apertura")
    private val decisiones = lista(json.getJSONArray("decisiones"), "decision")
    private val preanuncios = lista(json.getJSONArray("preanuncios"), "preanuncio")
    private val cierres = lista(json.getJSONArray("cierres"), "cierre")
    val prueba = Linea("prueba", json.getString("prueba"))
    val pruebaEn = Linea("prueba_en", json.optString("prueba_en", "Hmm... can you hear me? I am the Sorting Hat!"))

    private val casas: Map<String, FrasesCasa> = CASAS.associate { casa ->
        val c = json.getJSONObject("casas").getJSONObject(casa.id)
        casa.id to FrasesCasa(
            virtudes = lista(c.getJSONArray("virtudes"), "${casa.id}_virtud"),
            dudas = lista(c.getJSONArray("dudas"), "${casa.id}_duda"),
            anuncio = Linea("${casa.id}_anuncio", c.getString("anuncio"), esAnuncio = true),
        )
    }

    /**
     * 4 discursos completos por casa en cada idioma.
     * Claves: `gryffindor_discurso_1` (español) y `gryffindor_discurso_1_en` (inglés).
     */
    val discursos: Map<Idioma, Map<String, List<Discurso>>> = mapOf(
        Idioma.ES to leerDiscursos(json, "discursos", "anuncio", ""),
        Idioma.EN to leerDiscursos(json, "discursos_en", "anuncio_en", "_en"),
    )

    fun prueba(idioma: Idioma) = if (idioma == Idioma.EN) pruebaEn else prueba

    /**
     * Versiones de la pregunta bilingüe y de sus respuestas; el sombrero las va alternando.
     * Claves: `pregunta_idioma`, `pregunta_idioma_2`... y `respuesta_idioma_es`, `respuesta_idioma_es_2`...
     */
    private val textosIdioma = json.optJSONObject("idioma") ?: JSONObject()
    val preguntasIdioma = versiones("preguntas", "pregunta_idioma", "Do you prefer English... ¿o prefieres español?")
    private val respuestasEs = versiones("respuestas_es", "respuesta_idioma_es", "¡Perfecto!")
    private val respuestasEn = versiones("respuestas_en", "respuesta_idioma_en", "Perfect!")

    fun respuestasIdioma(idioma: Idioma) = if (idioma == Idioma.EN) respuestasEn else respuestasEs

    /**
     * Invitaciones del modo calle, por idioma y tipo de persona.
     * Claves: `invitacion_nino_1_en` (inglés) e `invitacion_nino_1` (español).
     */
    val invitaciones: Map<Idioma, Map<Publico, List<Linea>>> = Idioma.entries.associateWith { idioma ->
        val porTipo = json.optJSONObject("invitaciones")?.optJSONObject(idioma.codigo) ?: JSONObject()
        val sufijo = if (idioma == Idioma.EN) "_en" else ""
        Publico.entries.associateWith { publico ->
            val lista = porTipo.optJSONArray(publico.id) ?: JSONArray()
            List(lista.length()) { i ->
                Linea("invitacion_${publico.id}_${i + 1}$sufijo", sinEtiquetas(lista.getString(i)))
            }
        }
    }

    private fun versiones(campo: String, clave: String, porDefecto: String): List<Linea> {
        val lista = textosIdioma.optJSONArray(campo) ?: JSONArray().put(porDefecto)
        return List(lista.length()) { i ->
            Linea(if (i == 0) clave else "${clave}_${i + 1}", sinEtiquetas(lista.getString(i)))
        }
    }

    /** Todas las frases posibles (sirve para contar cuántas tienen audio grabado). */
    val todas: List<Linea> =
        listOf(prueba) + aperturas + decisiones + preanuncios + cierres +
            casas.values.flatMap { it.virtudes + it.dudas + it.anuncio }

    /** Arma el guion completo que dirá el sombrero para la casa ya elegida. */
    fun guion(casa: Casa, otras: List<Casa>): List<Linea> {
        val frases = casas.getValue(casa.id)
        val virtudes = frases.virtudes.shuffled()
        val lineas = mutableListOf<Linea>()

        lineas += aperturas.random().copy(pausaDespues = 900)
        lineas += virtudes[0].copy(pausaDespues = 600)

        // A veces el sombrero "duda" con una virtud de otra casa, para dar suspenso.
        if (otras.isNotEmpty() && Math.random() < 0.6) {
            lineas += casas.getValue(otras.random().id).dudas.random().copy(pausaDespues = 900)
        }

        lineas += virtudes[1].copy(pausaDespues = 800)
        lineas += decisiones.random().copy(pausaDespues = 700)
        lineas += preanuncios.random().copy(pausaDespues = 1400)
        lineas += frases.anuncio.copy(pausaDespues = 900)
        lineas += cierres.random().copy(pausaDespues = 0)
        return lineas
    }

    companion object {
        /** Marca, dentro de un discurso, el momento en que grita el nombre de la casa. */
        const val ANUNCIO = "{ANUNCIO}"

        fun cargar(context: Context): Frases =
            Frases(JSONObject(context.assets.open("frases.json").bufferedReader().use { it.readText() }))

        /** Quita las etiquetas de emoción de ElevenLabs ([whispers], [chuckles]...) para los subtítulos. */
        private fun sinEtiquetas(texto: String) = texto.replace(Regex("""\[[^\]]*]\s*"""), "").trim()

        private fun leerDiscursos(json: JSONObject, campo: String, campoAnuncio: String, sufijo: String) =
            CASAS.associate { casa ->
                val lista = json.optJSONObject(campo)?.optJSONArray(casa.id) ?: JSONArray()
                val anuncio = json.getJSONObject("casas").getJSONObject(casa.id).optString(campoAnuncio, casa.nombre)
                casa.id to List(lista.length()) { i ->
                    val crudos = lista.getJSONArray(i)
                    val segmentos = List(crudos.length()) { crudos.getString(it) }
                    Discurso(
                        clave = "${casa.id}_discurso_${i + 1}$sufijo",
                        segmentos = segmentos.map { if (it == ANUNCIO) anuncio else sinEtiquetas(it) },
                        indiceAnuncio = segmentos.indexOf(ANUNCIO),
                    )
                }
            }

        private fun lista(arr: JSONArray, prefijo: String): List<Linea> =
            List(arr.length()) { i -> Linea("${prefijo}_${i + 1}", arr.getString(i)) }
    }
}
