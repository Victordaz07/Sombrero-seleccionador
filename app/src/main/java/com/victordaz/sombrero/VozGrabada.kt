package com.victordaz.sombrero

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
     */
    suspend fun reproducir(clave: String, alAvanzar: ((posicionMs: Int, duracionMs: Int) -> Unit)? = null) {
        val reproductor = MediaPlayer()
        try {
            context.assets.openFd("$CARPETA/$clave.mp3").use {
                reproductor.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            reproductor.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            reproductor.prepare()
        } catch (e: Exception) {
            reproductor.release()
            return
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
    }

    private companion object {
        const val CARPETA = "voz"
    }
}
