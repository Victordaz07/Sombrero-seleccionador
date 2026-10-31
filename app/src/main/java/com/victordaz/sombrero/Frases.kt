package com.victordaz.sombrero

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Una frase que dice el sombrero. [clave] es el nombre de su audio: `assets/voz/<clave>.mp3`. */
data class Linea(val clave: String, val texto: String)

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

/**
 * Todo lo que dice el sombrero, leído de `assets/frases.json`.
 * Si el archivo tiene un error, [error] lo describe y la app sigue funcionando con lo que pudo leer.
 *
 * Las claves se arman igual que en `herramientas/generar_voces.py`.
 */
class Frases(json: JSONObject, val error: String? = null) {

    private val prueba = Linea("prueba", json.optString("prueba", "Hmm... ¿me escuchas bien? Soy el sombrero seleccionador."))
    private val pruebaEn = Linea("prueba_en", json.optString("prueba_en", "Hmm... can you hear me? I am the Sorting Hat!"))

    fun prueba(idioma: Idioma) = if (idioma == Idioma.EN) pruebaEn else prueba

    /**
     * 4 discursos completos por casa en cada idioma.
     * Claves: `gryffindor_discurso_1` (español) y `gryffindor_discurso_1_en` (inglés).
     */
    val discursos: Map<Idioma, Map<String, List<Discurso>>> = mapOf(
        Idioma.ES to leerDiscursos(json, "discursos", "anuncio", ""),
        Idioma.EN to leerDiscursos(json, "discursos_en", "anuncio_en", "_en"),
    )

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
                Linea("invitacion_${publico.id}_${i + 1}$sufijo", sinEtiquetas(lista.optString(i)))
            }
        }
    }

    private fun versiones(campo: String, clave: String, porDefecto: String): List<Linea> {
        val lista = textosIdioma.optJSONArray(campo) ?: JSONArray().put(porDefecto)
        return List(lista.length()) { i ->
            Linea(if (i == 0) clave else "${clave}_${i + 1}", sinEtiquetas(lista.optString(i)))
        }
    }

    companion object {
        /** Marca, dentro de un discurso, el momento en que grita el nombre de la casa. */
        const val ANUNCIO = "{ANUNCIO}"

        fun cargar(context: Context): Frases {
            val texto = runCatching {
                context.assets.open("frases.json").bufferedReader().use { it.readText() }
            }.getOrElse { return Frases(JSONObject(), "No se encontró frases.json") }
            return try {
                Frases(JSONObject(texto))
            } catch (e: Exception) {
                Frases(JSONObject(), "frases.json tiene un error: ${e.message}")
            }
        }

        /** Quita las etiquetas de emoción de ElevenLabs ([whispers], [chuckles]...) para los subtítulos. */
        fun sinEtiquetas(texto: String) = texto.replace(Regex("""\[[^\]]*]\s*"""), "").trim()

        private fun leerDiscursos(json: JSONObject, campo: String, campoAnuncio: String, sufijo: String) =
            CASAS.associate { casa ->
                val lista = json.optJSONObject(campo)?.optJSONArray(casa.id) ?: JSONArray()
                val anuncio = json.optJSONObject("casas")?.optJSONObject(casa.id)?.optString(campoAnuncio)
                    ?.takeIf { it.isNotBlank() } ?: casa.nombre
                val discursos = (0 until lista.length()).mapNotNull { i ->
                    val crudos = lista.optJSONArray(i) ?: return@mapNotNull null
                    val segmentos = List(crudos.length()) { crudos.optString(it) }
                    // Un discurso sin {ANUNCIO} no sirve: el sombrero nunca diría la casa.
                    if (ANUNCIO !in segmentos) return@mapNotNull null
                    Discurso(
                        clave = "${casa.id}_discurso_${i + 1}$sufijo",
                        segmentos = segmentos.map { if (it == ANUNCIO) anuncio else sinEtiquetas(it) },
                        indiceAnuncio = segmentos.indexOf(ANUNCIO),
                    )
                }
                casa.id to discursos
            }
    }
}
