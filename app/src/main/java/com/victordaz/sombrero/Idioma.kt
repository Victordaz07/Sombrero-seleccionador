package com.victordaz.sombrero

/** Idioma de la ceremonia: cambia los textos de pantalla, los discursos y la voz. */
enum class Idioma(val codigo: String, val nombre: String, val boton: String) {
    ES("es", "Español", "🇲🇽  Español"),
    EN("en", "English", "🇺🇸  English"),
    TL("tl", "Tagalog", "🇵🇭  Tagalog"),
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
    val volumenBajo: String,
) {
    /** "¡GRYFFINDOR!" en español, "GRYFFINDOR!" en inglés y tagalo. */
    fun anuncio(casa: Casa, idioma: Idioma) =
        if (idioma == Idioma.ES) "¡${casa.nombre.uppercase()}!" else "${casa.nombre.uppercase()}!"

    fun lema(casa: Casa, idioma: Idioma) = when (idioma) {
        Idioma.ES -> casa.lema
        Idioma.EN -> casa.lemaEn
        Idioma.TL -> casa.lemaTl
    }
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
        volumenBajo = "🔇 El volumen está muy bajo · Toca aquí para subirlo",
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
        volumenBajo = "🔇 Volume is too low · Tap here to turn it up",
    )
    Idioma.TL -> Textos(
        etiquetaInicio = "ANG SEREMONYA",
        etiquetaCasa = "ANG BAHAY MO AY",
        titulo = "Ang Sorting\nHat",
        sinVoz = "Walang nakitang boses sa Filipino. I-install ang Google Speech Services at ang Filipino voice pack.",
        pistaEsperando = "Umupo ka… at hawakan ang sombrero",
        pistaPensando = "Hmm… tingnan natin kung ano ang nasa loob mo…",
        estadoHablando = "NAGSASALITA ANG SOMBRERO",
        estadoPensando = "NAG-IISIP ANG SOMBRERO",
        estadoRevelado = "MALIGAYANG PAGDATING SA IYONG BAHAY",
        estadoInicio = "DITO NAGSISIMULA ANG IYONG KUWENTO",
        botonEscuchar = "Makinig sa sombrero…",
        botonSiguiente = "Susunod na kalahok",
        botonComenzar = "Simulan ang pagpili",
        lasCuatroCasas = "ANG APAT NA BAHAY",
        ajustes = "Settings",
        participantes = "kalahok",
        noParticipa = "hindi kasali",
        escudoDe = "Sagisag ng",
        volumenBajo = "🔇 Masyadong mahina ang volume · I-tap dito para lakasan",
    )
}
