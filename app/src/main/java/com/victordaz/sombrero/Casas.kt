package com.victordaz.sombrero

import androidx.compose.ui.graphics.Color

/**
 * Una casa a la que el sombrero puede enviar a alguien: nombre, colores y lema que se ven en pantalla.
 *
 * Lo que el sombrero DICE de cada casa (virtudes, dudas, anuncio) está en `assets/frases.json`.
 */
data class Casa(
    val id: String,
    val nombre: String,
    val colorPrincipal: Color,
    val colorSecundario: Color,
    val lema: String,
)

val CASAS = listOf(
    Casa(
        id = "gryffindor",
        nombre = "Gryffindor",
        colorPrincipal = Color(0xFF7F0909),
        colorSecundario = Color(0xFFE8B730),
        lema = "Valentía · Coraje · Corazón noble",
    ),
    Casa(
        id = "hufflepuff",
        nombre = "Hufflepuff",
        colorPrincipal = Color(0xFFB8860B),
        colorSecundario = Color(0xFF2B2420),
        lema = "Lealtad · Bondad · Trabajo constante",
    ),
    Casa(
        id = "ravenclaw",
        nombre = "Ravenclaw",
        colorPrincipal = Color(0xFF0E2A6B),
        colorSecundario = Color(0xFFC0894A),
        lema = "Sabiduría · Ingenio · Creatividad",
    ),
    Casa(
        id = "slytherin",
        nombre = "Slytherin",
        colorPrincipal = Color(0xFF1A5C36),
        colorSecundario = Color(0xFFC8CCD0),
        lema = "Determinación · Liderazgo · Ambición",
    ),
)

fun casaPorId(id: String): Casa? = CASAS.firstOrNull { it.id == id }
