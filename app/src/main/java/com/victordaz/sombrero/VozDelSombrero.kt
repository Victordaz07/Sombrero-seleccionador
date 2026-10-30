package com.victordaz.sombrero

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * La voz del sombrero: usa el motor de texto a voz del teléfono en español,
 * con un tono grave y lento para que suene "antiguo".
 */
class VozDelSombrero(
    context: Context,
    private val alEstarLista: (Boolean) -> Unit,
) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)
    private val pendientes = ConcurrentHashMap<String, CancellableContinuation<Unit>>()

    var lista = false
        private set

    var tono = 0.65f
    var velocidad = 0.85f

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {}
                override fun onDone(utteranceId: String) = terminar(utteranceId)
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String) = terminar(utteranceId)
                override fun onError(utteranceId: String, errorCode: Int) = terminar(utteranceId)
                override fun onStop(utteranceId: String, interrupted: Boolean) = terminar(utteranceId)
            })
            lista = elegirEspanol()
        }
        // Avisamos siempre en el hilo principal y después de terminar de construir todo.
        Handler(Looper.getMainLooper()).post { alEstarLista(lista) }
    }

    private fun terminar(id: String) {
        val cont = pendientes.remove(id) ?: return
        if (cont.isActive) cont.resume(Unit)
    }

    private fun elegirEspanol(): Boolean {
        val candidatos = listOf(
            Locale("es", "US"), Locale("es", "MX"), Locale("es", "419"),
            Locale("es", "ES"), Locale("es"),
        )
        return candidatos.any { tts.setLanguage(it) >= TextToSpeech.LANG_AVAILABLE }
    }

    /** Voces en español instaladas en el teléfono. */
    fun vocesEnEspanol(): List<Voice> =
        runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
            .filter { it.locale.language == "es" }
            .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.locale.toString() }, { it.name }))

    fun usarVoz(nombre: String?) {
        if (!lista) return
        val voz = nombre?.let { n -> vocesEnEspanol().firstOrNull { it.name == n } }
        if (voz != null) tts.voice = voz else elegirEspanol()
    }

    /**
     * Dice el texto y espera (sin bloquear) hasta que termine de hablar.
     * Con [enfasis] lo dice más agudo y rápido, como un grito (para anunciar la casa).
     */
    suspend fun hablar(texto: String, enfasis: Boolean = false) {
        val estimado = 1500L + texto.length * 75L
        if (!lista) {
            // Sin voz disponible: al menos dejamos el subtítulo en pantalla un rato.
            delay(estimado)
            return
        }
        tts.setPitch(if (enfasis) tono * 1.2f else tono)
        tts.setSpeechRate(if (enfasis) velocidad * 1.1f else velocidad)
        val id = UUID.randomUUID().toString()
        // Algunos motores nunca avisan que terminaron; por eso el tiempo límite.
        withTimeoutOrNull(estimado * 3) {
            suspendCancellableCoroutine { cont ->
                pendientes[id] = cont
                cont.invokeOnCancellation {
                    pendientes.remove(id)
                    tts.stop()
                }
                if (tts.speak(texto, TextToSpeech.QUEUE_FLUSH, Bundle(), id) == TextToSpeech.ERROR) {
                    terminar(id)
                }
            }
        }
    }

    fun callar() {
        tts.stop()
    }

    fun apagar() {
        tts.stop()
        tts.shutdown()
    }
}
