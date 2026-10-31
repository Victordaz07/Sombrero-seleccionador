package com.victordaz.sombrero

import android.content.Context

/** Guarda en el teléfono las preferencias y cuántas personas hay en cada casa. */
class Ajustes(context: Context) {

    private val prefs = context.getSharedPreferences("sombrero", Context.MODE_PRIVATE)

    var tono: Float
        get() = prefs.getFloat("tono", 0.65f)
        set(v) = prefs.edit().putFloat("tono", v).apply()

    var velocidad: Float
        get() = prefs.getFloat("velocidad", 0.85f)
        set(v) = prefs.edit().putFloat("velocidad", v).apply()

    var idioma: Idioma
        get() = Idioma.desdeCodigo(prefs.getString("idioma", null))
        set(v) = prefs.edit().putString("idioma", v.codigo).apply()

    /** Voz del teléfono elegida para el idioma actual (cada idioma recuerda la suya). */
    var voz: String?
        get() = prefs.getString(claveVoz(), null)
        set(v) = prefs.edit().putString(claveVoz(), v).apply()

    // En español se conserva la clave original "voz" para no perder lo que ya estaba elegido.
    private fun claveVoz() = if (idioma == Idioma.ES) "voz" else "voz_${idioma.codigo}"

    /** Si está activo, el sombrero pregunta "English o español" antes de cada persona. */
    var preguntarIdioma: Boolean
        get() = prefs.getBoolean("preguntar_idioma", true)
        set(v) = prefs.edit().putBoolean("preguntar_idioma", v).apply()

    /** Si está activo, las casas elegidas a mano (tocando un escudo) también suman al conteo. */
    var contarManuales: Boolean
        get() = prefs.getBoolean("contar_manuales", false)
        set(v) = prefs.edit().putBoolean("contar_manuales", v).apply()

    /** Idiomas que ofrece la pregunta (con tagalo usa la pregunta trilingüe). */
    var idiomasPregunta: List<Idioma>
        get() {
            val guardados = prefs.getStringSet("idiomas_pregunta", null) ?: setOf("en", "es")
            return Idioma.entries.filter { it.codigo in guardados }
                .sortedBy { listOf(Idioma.EN, Idioma.ES, Idioma.TL).indexOf(it) }
        }
        set(v) = prefs.edit().putStringSet("idiomas_pregunta", v.map { it.codigo }.toSet()).apply()

    /** Si está activo, el sombrero reparte a la gente de forma pareja entre las casas. */
    var equilibrar: Boolean
        get() = prefs.getBoolean("equilibrar", true)
        set(v) = prefs.edit().putBoolean("equilibrar", v).apply()

    /** Si está activo, el botón de subir volumen (o un control remoto Bluetooth) activa el sombrero. */
    var controlRemoto: Boolean
        get() = prefs.getBoolean("control_remoto", true)
        set(v) = prefs.edit().putBoolean("control_remoto", v).apply()

    /** Si está activo, usa los audios de assets/voz cuando existen. */
    var usarGrabaciones: Boolean
        get() = prefs.getBoolean("usar_grabaciones", true)
        set(v) = prefs.edit().putBoolean("usar_grabaciones", v).apply()

    var casasActivas: Set<String>
        get() = prefs.getStringSet("casas_activas", null) ?: CASAS.map { it.id }.toSet()
        set(v) = prefs.edit().putStringSet("casas_activas", v).apply()

    fun conteo(): Map<String, Int> = CASAS.associate { it.id to prefs.getInt("conteo_${it.id}", 0) }

    fun sumar(casaId: String) {
        prefs.edit().putInt("conteo_$casaId", prefs.getInt("conteo_$casaId", 0) + 1).apply()
    }

    fun reiniciarConteo() {
        prefs.edit().apply { CASAS.forEach { remove("conteo_${it.id}") } }.apply()
    }
}
