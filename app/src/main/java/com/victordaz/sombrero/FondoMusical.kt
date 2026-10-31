package com.victordaz.sombrero

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Sonido de fondo continuo: la música mágica o, si está apagada, un "silencio" casi inaudible.
 * Mientras suena algo, las bocinas Bluetooth no se duermen y no se corta la primera palabra
 * del sombrero. Cuando el sombrero habla, la música baja sola.
 */
class FondoMusical(private val context: Context) {

    private var reproductor: MediaPlayer? = null
    private var archivo: String? = null
    private val manejador = Handler(Looper.getMainLooper())

    /** Volumen de la música (0 a 1), relativo al volumen multimedia del teléfono. */
    var volumen = 0.35f
        set(v) {
            field = v.coerceIn(0f, 1f)
            irA(objetivo())
        }

    private var atenuada = false
    private var actual = 0f

    /** Empieza (o cambia) el sonido de fondo: música si [conMusica], si no, el silencio. */
    fun sonar(conMusica: Boolean) {
        val nuevo = if (conMusica) MUSICA else SILENCIO
        if (nuevo == archivo && reproductor != null) {
            reproductor?.takeIf { !it.isPlaying }?.start()
            irA(objetivo())
            return
        }
        liberar()
        archivo = nuevo
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            context.assets.openFd(nuevo).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            mp.isLooping = true
            mp.prepare()
            actual = 0f
            mp.setVolume(0f, 0f)
            mp.start()
            reproductor = mp
            irA(objetivo())
        } catch (e: Exception) {
            Log.e("Sombrero", "No se pudo iniciar el sonido de fondo $nuevo", e)
            mp.release()
            archivo = null
        }
    }

    /** Baja la música mientras habla el sombrero (y la vuelve a subir al terminar). */
    fun atenuar(si: Boolean) {
        atenuada = si
        irA(objetivo())
    }

    fun pausar() {
        reproductor?.takeIf { it.isPlaying }?.pause()
    }

    fun reanudar() {
        reproductor?.takeIf { !it.isPlaying }?.start()
    }

    fun liberar() {
        manejador.removeCallbacksAndMessages(null)
        reproductor?.release()
        reproductor = null
        archivo = null
    }

    private fun objetivo(): Float = when {
        archivo == SILENCIO -> 1f
        atenuada -> volumen * 0.25f
        else -> volumen
    }

    /** Cambia el volumen poco a poco (unos 400 ms) para que no se note el salto. */
    private fun irA(destino: Float) {
        manejador.removeCallbacksAndMessages(null)
        val inicio = actual
        val pasos = 10
        for (i in 1..pasos) {
            manejador.postDelayed({
                actual = inicio + (destino - inicio) * i / pasos
                reproductor?.setVolume(actual, actual)
            }, i * 40L)
        }
    }

    private companion object {
        const val MUSICA = "musica/fondo.ogg"
        const val SILENCIO = "musica/silencio.ogg"
    }
}
