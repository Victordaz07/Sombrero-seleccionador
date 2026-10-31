package com.victordaz.sombrero

import kotlin.random.Random

/** Cómo elige el sombrero la casa. Separado del ViewModel para poder probarlo. */
object Reparto {

    /**
     * Con [equilibrar], elige al azar entre las casas que tienen menos personas (para que queden parejas);
     * sin él, cualquiera de las [activas].
     */
    fun elegir(activas: List<Casa>, conteo: Map<String, Int>, equilibrar: Boolean, azar: Random = Random.Default): Casa {
        require(activas.isNotEmpty()) { "Tiene que haber al menos una casa activa" }
        if (!equilibrar) return activas.random(azar)
        val minimo = activas.minOf { conteo[it.id] ?: 0 }
        return activas.filter { (conteo[it.id] ?: 0) == minimo }.random(azar)
    }
}

/**
 * Reparte opciones sin repetir: usa todas antes de volver a empezar, y la primera de cada
 * vuelta nunca es la última que sonó. Se lleva una cola por [grupo] (por ejemplo "en-gryffindor").
 */
class Rotador<T>(private val clave: (T) -> String, private val azar: Random = Random.Default) {

    private val colas = mutableMapOf<String, ArrayDeque<T>>()
    private val ultimo = mutableMapOf<String, String>()

    fun siguiente(grupo: String, opciones: List<T>): T? {
        if (opciones.isEmpty()) return null
        val cola = colas.getOrPut(grupo) { ArrayDeque() }
        // Si cambiaron las opciones (por ejemplo, llegó un audio nuevo), se arma una vuelta nueva.
        cola.removeAll { it !in opciones }
        if (cola.isEmpty()) {
            val mezcla = opciones.shuffled(azar).toMutableList()
            if (mezcla.size > 1 && clave(mezcla.first()) == ultimo[grupo]) mezcla.add(mezcla.removeAt(0))
            cola.addAll(mezcla)
        }
        return cola.removeFirst().also { ultimo[grupo] = clave(it) }
    }
}
