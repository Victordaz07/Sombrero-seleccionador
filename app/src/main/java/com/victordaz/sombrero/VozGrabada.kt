package com.victordaz.sombrero

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Reproduce los audios pregrabados de `assets/voz/<clave>.mp3`
 * (generados con IA o grabados por una persona). Suenan mucho más naturales
 * que la voz del teléfono, que queda solo como respaldo.
 */
class VozGrabada(private val context: Context) {

    private val disponibles: Set<String> =
        runCatching { context.assets.list(CARPETA).orEmpty() }.getOrDefault(emptyArray())
            .filter { it.endsWith(EXTENSION) }
            .map { it.removeSuffix(EXTENSION) }
            .toSet()

    fun tiene(clave: String) = clave in disponibles

    /** Reproduce el audio y espera (sin bloquear) hasta que termine. */
    suspend fun reproducir(clave: String) = suspendCancellableCoroutine { cont ->
        val reproductor = MediaPlayer()
        val liberado = AtomicBoolean(false)
        fun terminar() {
            if (liberado.compareAndSet(false, true)) reproductor.release()
            if (cont.isActive) cont.resume(Unit)
        }
        cont.invokeOnCancellation { if (liberado.compareAndSet(false, true)) reproductor.release() }
        try {
            context.assets.openFd("$CARPETA/$clave$EXTENSION").use {
                reproductor.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            reproductor.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            reproductor.setOnCompletionListener { terminar() }
            reproductor.setOnErrorListener { _, _, _ -> terminar(); true }
            reproductor.prepare()
            reproductor.start()
        } catch (e: Exception) {
            terminar()
        }
    }

    private companion object {
        const val CARPETA = "voz"
        const val EXTENSION = ".mp3"
    }
}
