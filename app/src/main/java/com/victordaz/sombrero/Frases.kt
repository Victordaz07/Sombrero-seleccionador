package com.victordaz.sombrero

/** Una frase que dice el sombrero y cuánto espera (en milisegundos) antes de la siguiente. */
data class Linea(
    val texto: String,
    val pausaDespues: Long = 500,
    val esAnuncio: Boolean = false,
)

object Frases {

    private val aperturas = listOf(
        "Mmm... veamos, veamos... ¿qué tenemos aquí?",
        "Ah... una cabeza nueva. No te muevas, que voy a mirar muy adentro.",
        "Interesante... muy interesante. Déjame pensar...",
        "Hmm... llevo cientos de años haciendo esto, y aun así me sorprendes.",
        "Bien, bien... no tengas miedo. Solo voy a echar un vistazo a tus pensamientos.",
        "Oh, vaya... ¡cuántas cosas hay en esta cabeza!",
    )

    private val dudas = listOf(
        "Aunque también veo %s... esto no será tan fácil.",
        "Pero espera... también hay %s. Hmm...",
        "Y por aquí encuentro %s... ¡qué difícil decisión!",
        "Aunque... no puedo ignorar %s.",
    )

    private val decisiones = listOf(
        "Sí... ahora lo veo con claridad.",
        "Ya no tengo ninguna duda.",
        "Lo he decidido.",
        "Mmm... sí, sí. Está muy claro.",
        "Muy bien... sé exactamente a dónde perteneces.",
    )

    private val preanuncios = listOf(
        "Tu lugar está en...",
        "Tu casa será...",
        "Te envío a...",
        "Perteneces a...",
    )

    private val cierres = listOf(
        "¡Que tengas una noche mágica!",
        "¡Tu nueva casa te recibe con los brazos abiertos!",
        "¡Ve con tu casa, que la fiesta te espera!",
        "¡Que la alegría te acompañe esta noche!",
        "¡Siguiente, por favor!",
    )

    /** Arma el guion completo que dirá el sombrero para la casa ya elegida. */
    fun guion(casa: Casa, otras: List<Casa>): List<Linea> {
        val virtudes = casa.virtudes.shuffled()
        val lineas = mutableListOf<Linea>()

        lineas += Linea(aperturas.random(), pausaDespues = 900)
        lineas += Linea(virtudes[0], pausaDespues = 600)

        // A veces el sombrero "duda" con un rasgo de otra casa, para dar suspenso.
        if (otras.isNotEmpty() && Math.random() < 0.6) {
            val rasgo = otras.random().rasgos.random()
            lineas += Linea(dudas.random().format(rasgo), pausaDespues = 900)
        }

        lineas += Linea(virtudes[1], pausaDespues = 800)
        lineas += Linea(decisiones.random(), pausaDespues = 700)
        lineas += Linea(preanuncios.random(), pausaDespues = 1400)
        lineas += Linea("¡${casa.nombreHablado}!", pausaDespues = 900, esAnuncio = true)
        lineas += Linea(cierres.random(), pausaDespues = 0)
        return lineas
    }

    const val PRUEBA = "Hmm... ¿me escuchas bien? Soy el sombrero seleccionador."
}
