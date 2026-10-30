package com.victordaz.sombrero

/** Idioma de la ceremonia: cambia los textos de pantalla, los discursos y la voz. */
enum class Idioma(val codigo: String, val nombre: String) {
    ES("es", "Español"),
    EN("en", "English"),
    ;

    companion object {
        fun desdeCodigo(codigo: String?) = entries.firstOrNull { it.codigo == codigo } ?: ES
    }
}

/** Textos de la pantalla principal en cada idioma. */
data class Textos(
    val etiquetaInicio: String,
    val etiquetaCasa: String,
    val titulo: String,
    val sinVoz: String,
    val pistaEsperando: String,
    val pistaPensando: String,
    val estadoHablando: String,
    val estadoPensando: String,
    val estadoRevelado: String,
    val estadoInicio: String,
    val botonEscuchar: String,
    val botonSiguiente: String,
    val botonComenzar: String,
    val lasCuatroCasas: String,
    val ajustes: String,
    val participantes: String,
    val noParticipa: String,
    val escudoDe: String,
) {
    /** "¡GRYFFINDOR!" o "GRYFFINDOR!" según el idioma. */
    fun anuncio(casa: Casa, idioma: Idioma) =
        if (idioma == Idioma.EN) "${casa.nombre.uppercase()}!" else "¡${casa.nombre.uppercase()}!"

    fun lema(casa: Casa, idioma: Idioma) = if (idioma == Idioma.EN) casa.lemaEn else casa.lema
}

fun textos(idioma: Idioma): Textos = when (idioma) {
    Idioma.ES -> Textos(
        etiquetaInicio = "LA CEREMONIA",
        etiquetaCasa = "TU CASA ES",
        titulo = "El Sombrero\nSeleccionador",
        sinVoz = "No se encontró voz en español. Instala Servicios de voz de Google y su paquete en español.",
        pistaEsperando = "Siéntate… y toca el sombrero",
        pistaPensando = "Hmm… veamos qué hay en ti…",
        estadoHablando = "EL SOMBRERO HABLA",
        estadoPensando = "EL SOMBRERO ESTÁ PENSANDO",
        estadoRevelado = "BIENVENIDO A TU CASA",
        estadoInicio = "TU HISTORIA COMIENZA AQUÍ",
        botonEscuchar = "Escucha al sombrero…",
        botonSiguiente = "Siguiente participante",
        botonComenzar = "Comenzar selección",
        lasCuatroCasas = "LAS CUATRO CASAS",
        ajustes = "Ajustes",
        participantes = "participantes",
        noParticipa = "no participa",
        escudoDe = "Escudo de",
    )
    Idioma.EN -> Textos(
        etiquetaInicio = "THE CEREMONY",
        etiquetaCasa = "YOUR HOUSE IS",
        titulo = "The Sorting\nHat",
        sinVoz = "No English voice found. Install Google Speech Services and its English voice pack.",
        pistaEsperando = "Have a seat… and touch the hat",
        pistaPensando = "Hmm… let's see what's inside you…",
        estadoHablando = "THE HAT IS SPEAKING",
        estadoPensando = "THE HAT IS THINKING",
        estadoRevelado = "WELCOME TO YOUR HOUSE",
        estadoInicio = "YOUR STORY BEGINS HERE",
        botonEscuchar = "Listen to the hat…",
        botonSiguiente = "Next participant",
        botonComenzar = "Start the sorting",
        lasCuatroCasas = "THE FOUR HOUSES",
        ajustes = "Settings",
        participantes = "participants",
        noParticipa = "not participating",
        escudoDe = "Crest of",
    )
}
