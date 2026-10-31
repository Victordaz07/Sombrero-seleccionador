package com.victordaz.sombrero

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscursoTest {

    private val discurso = Discurso("prueba", listOf("Hola", "¿Qué tenemos aquí?", "¡GRÍFINDOR!"), indiceAnuncio = 2)

    @Test
    fun conTiemposExactosElGritoEmpiezaJustoEnSuSegundo() {
        val inicios = listOf(0.0, 2.0, 5.5)
        assertEquals(0, discurso.segmentoEn(1_000, 8_000, inicios))
        assertEquals(1, discurso.segmentoEn(5_499, 8_000, inicios))
        assertEquals(2, discurso.segmentoEn(5_500, 8_000, inicios))
    }

    @Test
    fun sinTiemposEstimaPorElLargoDelTexto() {
        assertEquals(0, discurso.segmentoEn(0, 10_000, null))
        assertEquals(2, discurso.segmentoEn(9_999, 10_000, null))
    }
}
