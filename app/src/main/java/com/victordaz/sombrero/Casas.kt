package com.victordaz.sombrero

import androidx.compose.ui.graphics.Color

/**
 * Una casa a la que el sombrero puede enviar a alguien.
 *
 * - [nombre] es como se muestra en pantalla.
 * - [nombreHablado] es como lo debe pronunciar la voz en español
 *   (escrito "a la española" para que suene bien).
 * - [rasgos] son palabras cortas que el sombrero usa para "dudar" entre casas.
 * - [virtudes] son frases completas que el sombrero dice sobre la persona.
 *
 * Si quieres cambiar nombres, colores o frases, este es el único archivo que debes tocar.
 */
data class Casa(
    val id: String,
    val nombre: String,
    val nombreHablado: String,
    val colorPrincipal: Color,
    val colorSecundario: Color,
    val lema: String,
    val rasgos: List<String>,
    val virtudes: List<String>,
)

val CASAS = listOf(
    Casa(
        id = "gryffindor",
        nombre = "Gryffindor",
        nombreHablado = "Grífindor",
        colorPrincipal = Color(0xFF7F0909),
        colorSecundario = Color(0xFFE8B730),
        lema = "Valentía · Coraje · Corazón noble",
        rasgos = listOf("valentía", "un coraje enorme", "un espíritu aventurero"),
        virtudes = listOf(
            "Veo un valor que no se rinde, ni siquiera cuando tienes miedo.",
            "Tienes un corazón noble: defiendes a los demás aunque nadie te lo pida.",
            "Te gustan los retos... y cuando todos dan un paso atrás, tú das uno al frente.",
            "Hay fuego en tu interior. Un fuego que contagia valor a quienes te rodean.",
            "Dices la verdad aunque cueste. Eso es verdadera valentía.",
            "Veo una gran capacidad para levantarte cada vez que caes.",
        ),
    ),
    Casa(
        id = "hufflepuff",
        nombre = "Hufflepuff",
        nombreHablado = "Jáfelpaf",
        colorPrincipal = Color(0xFFB8860B),
        colorSecundario = Color(0xFF2B2420),
        lema = "Lealtad · Bondad · Trabajo constante",
        rasgos = listOf("una lealtad a toda prueba", "mucha bondad", "un gran corazón"),
        virtudes = listOf(
            "Veo una lealtad enorme. Tus amigos pueden contar contigo siempre.",
            "Tienes un corazón amable y paciente. Tratas a todos con justicia.",
            "No buscas aplausos: trabajas duro y nunca abandonas lo que empiezas.",
            "Compartes lo que tienes sin esperar nada a cambio. Eso es un tesoro.",
            "Haces sentir a los demás en casa. Es un don muy especial.",
            "Veo humildad y constancia, dos virtudes que valen más que el oro.",
        ),
    ),
    Casa(
        id = "ravenclaw",
        nombre = "Ravenclaw",
        nombreHablado = "Réivenclo",
        colorPrincipal = Color(0xFF0E2A6B),
        colorSecundario = Color(0xFFC0894A),
        lema = "Sabiduría · Ingenio · Creatividad",
        rasgos = listOf("una mente brillante", "mucha curiosidad", "una creatividad sorprendente"),
        virtudes = listOf(
            "Qué mente tan curiosa... siempre preguntando el porqué de las cosas.",
            "Veo ingenio y creatividad. Encuentras soluciones donde otros ven problemas.",
            "Te encanta aprender. Cada libro y cada pregunta es una aventura para ti.",
            "Tienes sabiduría para escuchar antes de hablar. Eso es muy poco común.",
            "Tu imaginación no tiene límites. Ves el mundo de una manera única.",
            "Piensas con calma y con claridad, incluso cuando todos se apresuran.",
        ),
    ),
    Casa(
        id = "slytherin",
        nombre = "Slytherin",
        nombreHablado = "Slíderin",
        colorPrincipal = Color(0xFF1A5C36),
        colorSecundario = Color(0xFFC8CCD0),
        lema = "Determinación · Liderazgo · Ambición",
        rasgos = listOf("mucha determinación", "madera de líder", "grandes metas"),
        virtudes = listOf(
            "Veo una determinación de hierro. Cuando te propones algo, lo consigues.",
            "Tienes madera de líder: los demás te siguen sin que se los pidas.",
            "Sueñas en grande y trabajas con astucia para alcanzar tus metas.",
            "Tienes ingenio para salir de cualquier aprieto. Nada te detiene.",
            "Veo ambición, sí... la buena ambición de querer ser cada día mejor.",
            "Sabes planear cada paso con cuidado. Eso te llevará muy lejos.",
        ),
    ),
)

fun casaPorId(id: String): Casa? = CASAS.firstOrNull { it.id == id }
