package com.victordaz.sombrero

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FrasesTest {

    private val frases = Frases(JSONObject(File("src/main/assets/frases.json").readText()))

    @Test
    fun frasesJsonSeLeeSinErrores() {
        assertNull(frases.error)
    }

    @Test
    fun cadaCasaTiene4DiscursosEnCadaIdiomaYTodosGritanLaCasa() {
        for (idioma in Idioma.entries) {
            for (casa in CASAS) {
                val discursos = frases.discursos.getValue(idioma).getValue(casa.id)
                assertEquals("$idioma ${casa.id}", 4, discursos.size)
                discursos.forEach { assertTrue(it.clave, it.indiceAnuncio in it.segmentos.indices) }
            }
        }
    }

    @Test
    fun losSubtitulosNoMuestranEtiquetasDeEmocion() {
        val textos = frases.discursos.values.flatMap { it.values }.flatten().flatMap { it.segmentos } +
            frases.invitaciones.values.flatMap { it.values }.flatten().map { it.texto }
        textos.forEach { assertFalse(it, it.contains("[")) }
    }

    @Test
    fun cadaTipoDePersonaTieneInvitacionEnLosDosIdiomas() {
        for (idioma in Idioma.entries) {
            for (publico in Publico.entries) {
                assertTrue("$idioma $publico", frases.invitaciones.getValue(idioma).getValue(publico).isNotEmpty())
            }
        }
    }

    @Test
    fun unArchivoVacioNoRompeLaApp() {
        val vacio = Frases(JSONObject())
        assertTrue(vacio.discursos.getValue(Idioma.ES).getValue("gryffindor").isEmpty())
        assertTrue(vacio.invitaciones.getValue(Idioma.EN).getValue(Publico.TODOS).isEmpty())
    }
}
