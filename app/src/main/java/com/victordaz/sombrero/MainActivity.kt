package com.victordaz.sombrero

import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private val vm: SombreroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // La pantalla no se apaga durante la fiesta y el volumen controla la voz.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        volumeControlStream = AudioManager.STREAM_MUSIC

        // Pantalla completa (desliza desde el borde para ver las barras del sistema).
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent { PantallaSombrero(vm) }
    }

    /**
     * Los controles Bluetooth para selfies envían "subir volumen" o "Enter".
     * Así alguien puede activar el sombrero a distancia sin tocar el teléfono.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (vm.controlRemoto && keyCode in TECLAS_DISPARADOR) {
            if (event.repeatCount == 0) vm.alTocar()
            return true
        }
        // Mientras el sombrero pregunta el idioma, bajar volumen elige el otro idioma.
        if (vm.controlRemoto && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && vm.estado.fase == Fase.IDIOMA) {
            if (event.repeatCount == 0) vm.elegirIdioma(if (vm.estado.idioma == Idioma.ES) Idioma.EN else Idioma.ES)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private companion object {
        val TECLAS_DISPARADOR = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_CAMERA,
        )
    }
}
