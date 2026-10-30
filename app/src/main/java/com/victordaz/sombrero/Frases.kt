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

    private val casas: Map<String, FrasesCasa> = CASAS.associate { casa ->
        val c = json.getJSONObject("casas").getJSONObject(casa.id)
        casa.id to FrasesCasa(
            virtudes = lista(c.getJSONArray("virtudes"), "${casa.id}_virtud"),
            dudas = lista(c.getJSONArray("dudas"), "${casa.id}_duda"),
            anuncio = Linea("${casa.id}_anuncio", c.getString("anuncio"), esAnuncio = true),
        )
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
        fun cargar(context: Context): Frases =
            Frases(JSONObject(context.assets.open("frases.json").bufferedReader().use { it.readText() }))

        private fun lista(arr: JSONArray, prefijo: String): List<Linea> =
            List(arr.length()) { i -> Linea("${prefijo}_${i + 1}", arr.getString(i)) }
    }
}
