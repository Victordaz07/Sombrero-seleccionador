package com.victordaz.sombrero

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import kotlin.math.roundToInt

/**
 * Volumen multimedia (el que usan la bocina del teléfono y las bocinas Bluetooth)
 * y el "foco de audio", para que Android no silencie al sombrero mientras habla.
 */
class Volumen(context: Context, alPerderFoco: () -> Unit = {}) {

    private val audio = context.getSystemService(AudioManager::class.java)

    // Si otra app se adueña del audio para quedarse (por ejemplo, alguien pone música), el sombrero se calla.
    // Las pérdidas breves no cuentan: algunos motores de voz del teléfono las piden al hablar,
    // y las llamadas ya detienen todo porque la app pasa a segundo plano.
    private val foco = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(ATRIBUTOS)
        .setOnAudioFocusChangeListener({ cambio ->
            if (cambio == AudioManager.AUDIOFOCUS_LOSS) alPerderFoco()
        }, Handler(Looper.getMainLooper()))
        .build()

    /** De 0 (silencio) a 1 (máximo). */
    val nivel: Float
        get() {
            val maximo = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            return if (maximo <= 0) 0f else audio.getStreamVolume(AudioManager.STREAM_MUSIC) / maximo.toFloat()
        }

    fun poner(nivel: Float, mostrarEnPantalla: Boolean = true) {
        val maximo = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val valor = (nivel.coerceIn(0f, 1f) * maximo).roundToInt()
        runCatching {
            audio.setStreamVolume(
                AudioManager.STREAM_MUSIC, valor,
                if (mostrarEnPantalla) AudioManager.FLAG_SHOW_UI else 0,
            )
        }
    }

    fun pedirFoco() {
        runCatching { audio.requestAudioFocus(foco) }
    }

    fun soltarFoco() {
        runCatching { audio.abandonAudioFocusRequest(foco) }
    }

    companion object {
        /** Voz por el canal multimedia: sale por la bocina del teléfono o por Bluetooth. */
        val ATRIBUTOS: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }
}
