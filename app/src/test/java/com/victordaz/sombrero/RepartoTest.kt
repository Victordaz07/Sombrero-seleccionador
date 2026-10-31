package com.victordaz.sombrero

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RepartoTest {

    @Test
    fun conEquilibrioEligeSiempreUnaDeLasCasasConMenosGente() {
        val conteo = mapOf("gryffindor" to 3, "hufflepuff" to 1, "ravenclaw" to 1, "slytherin" to 2)
        repeat(200) { semilla ->
            val casa = Reparto.elegir(CASAS, conteo, equilibrar = true, azar = Random(semilla))
            assertTrue(casa.id in setOf("hufflepuff", "ravenclaw"))
        }
    }

    @Test
    fun conEquilibrioLasCuatroCasasQuedanParejas() {
        val conteo = CASAS.associate { it.id to 0 }.toMutableMap()
        val azar = Random(7)
        repeat(40) {
            val casa = Reparto.elegir(CASAS, conteo, equilibrar = true, azar = azar)
            conteo[casa.id] = conteo.getValue(casa.id) + 1
        }
        assertEquals(setOf(10), conteo.values.toSet())
    }

    @Test
    fun soloEligeEntreLasCasasActivas() {
        val activas = CASAS.filter { it.id != "slytherin" }
        repeat(100) { semilla ->
            assertNotEquals("slytherin", Reparto.elegir(activas, emptyMap(), equilibrar = false, azar = Random(semilla)).id)
        }
    }
}
