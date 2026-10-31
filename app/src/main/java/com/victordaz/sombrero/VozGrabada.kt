package com.victordaz.sombrero

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Reproduce los audios pregrabados de `assets/voz/<clave>.mp3`
 * (generados con IA o grabados por una persona). Suenan mucho más naturales
 * que la voz del teléfono, que queda solo como respaldo.
 */
class VozGrabada(private val context: Context) {

    private val archivos: Set<String> =
        runCatching { context.assets.list(CARPETA).orEmpty().toSet() }.getOrDefault(emptySet())

    fun tiene(clave: String) = "$clave.mp3" in archivos

    /**
     * Segundo en que empieza cada segmento de un discurso, si el generador guardó
     * `assets/voz/<clave>.json` (ElevenLabs lo da exacto). Si no existe, devuelve null.
     */
    fun tiempos(clave: String): List<Double>? {
        if ("$clave.json" !in archivos) return null
        return runCatching {
            val texto = context.assets.open("$CARPETA/$clave.json").bufferedReader().use { it.readText() }
            val inicios = JSONObject(texto).getJSONArray("inicios")
            List(inicios.length()) { inicios.getDouble(it) }
        }.getOrNull()
    }

    /**
     * Reproduce el audio y espera (sin bloquear) hasta que termine.
     * [alAvanzar] recibe la posición y la duración (en milisegundos) unas 10 veces por segundo.
     * Devuelve false si el audio no se pudo abrir (para usar la voz del teléfono en su lugar).
     */
    suspend fun reproducir(clave: String, alAvanzar: ((posicionMs: Int, duracionMs: Int) -> Unit)? = null): Boolean =
        reproducirArchivo("$CARPETA/$clave.mp3", alAvanzar)

    private val efectos: Set<String> =
        runCatching { context.assets.list("efectos").orEmpty().toSet() }.getOrDefault(emptySet())

    /** Efectos de `assets/efectos/` cuyo nombre empieza con [prefijo] (ej. fanfarria_1.mp3, fanfarria_2.mp3...). */
    fun efectos(prefijo: String): List<String> =
        efectos.filter { it.startsWith(prefijo) && (it.endsWith(".mp3") || it.endsWith(".ogg")) }.sorted()

    /** Reproduce un efecto de `assets/efectos/` y espera a que termine. */
    suspend fun reproducirEfecto(archivo: String): Boolean = reproducirArchivo("efectos/$archivo")

    private suspend fun reproducirArchivo(ruta: String, alAvanzar: ((Int, Int) -> Unit)? = null): Boolean {
        val reproductor = MediaPlayer()
        try {
            reproductor.setAudioAttributes(Volumen.ATRIBUTOS)
            context.assets.openFd(ruta).use {
                reproductor.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            reproductor.prepare()
        } catch (e: Exception) {
            Log.e("Sombrero", "No se pudo abrir el audio $ruta", e)
            reproductor.release()
            return false
        }

        try {
            coroutineScope {
                val seguimiento = alAvanzar?.let { avisar ->
                    launch {
                        while (true) {
                            avisar(reproductor.currentPosition, reproductor.duration)
                            delay(100)
                        }
                    }
                }
                suspendCancellableCoroutine { cont ->
                    reproductor.setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                    reproductor.setOnErrorListener { _, _, _ -> if (cont.isActive) cont.resume(Unit); true }
                    reproductor.start()
                }
                seguimiento?.cancel()
            }
        } finally {
            reproductor.release()
        }
        return true
    }

    private companion object {
        const val CARPETA = "voz"
    }
}
