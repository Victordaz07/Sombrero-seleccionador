package com.victordaz.sombrero

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class RotadorTest {

    private val opciones = listOf("a", "b", "c", "d")

    @Test
    fun usaTodasAntesDeRepetir() {
        val rotador = Rotador<String>(clave = { it }, azar = Random(1))
        repeat(5) {
            val vuelta = List(opciones.size) { rotador.siguiente("g", opciones)!! }
            assertEquals(opciones.toSet(), vuelta.toSet())
        }
    }

    @Test
    fun nuncaRepiteSeguidoAlCambiarDeVuelta() {
        val rotador = Rotador<String>(clave = { it }, azar = Random(3))
        var anterior: String? = null
        repeat(400) {
            val actual = rotador.siguiente("g", opciones)
            assertNotEquals(anterior, actual)
            anterior = actual
        }
    }

    @Test
    fun cadaGrupoLlevaSuPropiaCola() {
        val rotador = Rotador<String>(clave = { it }, azar = Random(5))
        val es = List(4) { rotador.siguiente("es-gryffindor", opciones) }
        val en = List(4) { rotador.siguiente("en-gryffindor", opciones) }
        assertEquals(opciones.toSet(), es.toSet())
        assertEquals(opciones.toSet(), en.toSet())
    }

    @Test
    fun sinOpcionesDevuelveNull() {
        assertNull(Rotador<String>(clave = { it }).siguiente("g", emptyList()))
    }
}
