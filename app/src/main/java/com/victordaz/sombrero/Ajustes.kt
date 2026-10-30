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

    var voz: String?
        get() = prefs.getString("voz", null)
        set(v) = prefs.edit().putString("voz", v).apply()

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
