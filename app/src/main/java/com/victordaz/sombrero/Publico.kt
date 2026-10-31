package com.victordaz.sombrero

/** A quién invita el sombrero en la calle (modo calle). [id] es la clave en frases.json. */
enum class Publico(val id: String, val emoji: String, private val es: String, private val en: String) {
    NINO("nino", "👦", "Niño", "Boy"),
    NINA("nina", "👧", "Niña", "Girl"),
    JOVEN_M("joven_m", "🙋‍♂️", "Jovencito", "Teen boy"),
    JOVEN_F("joven_f", "🙋‍♀️", "Jovencita", "Teen girl"),
    SENOR("senor", "👨", "Señor", "Man"),
    SENORA("senora", "👩", "Señora", "Woman"),
    TODOS("todos", "✨", "Cualquiera", "Anyone"),
    ;

    fun etiqueta(idioma: Idioma) = if (idioma == Idioma.EN) en else es
}
