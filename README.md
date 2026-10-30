# 🎩 Sombrero Seleccionador

App de Android para la fiesta de Halloween de la iglesia. Una persona se sienta con el sombrero puesto, alguien toca la pantalla (o un control Bluetooth) y el sombrero:

1. "Piensa" en voz alta con una voz grave y lenta.
2. Dice algunas **virtudes y fortalezas** de la persona.
3. A veces duda entre dos casas, para dar suspenso.
4. Anuncia la casa a gritos: **¡GRYFFINDOR!**, y la pantalla se pinta con los colores de esa casa.

Funciona **sin internet**: usa la voz de texto a voz que ya trae el teléfono.

## Funciones

| Función | Detalle |
|---|---|
| Voz en español | Texto a voz de Android, con tono y velocidad ajustables y opción de elegir entre las voces instaladas |
| Sombrero animado | Se balancea, abre la boca al hablar y levanta las cejas |
| Reparto parejo | Por defecto manda a cada persona a la casa con menos gente, para que los equipos queden balanceados |
| Contador | Abajo se ve cuántas personas hay en cada casa (se guarda aunque cierres la app) |
| Control remoto | El botón de **subir volumen**, o un control Bluetooth para selfies (de los baratos), activa el sombrero |
| Elegir casas | Puedes desactivar casas en Ajustes (por ejemplo, si solo quieres 3 equipos) |
| Pantalla siempre encendida | No se apaga durante la fiesta |

## Cómo conseguir el APK (sin instalar nada en tu computadora)

1. Sube los cambios a GitHub. El flujo **Compilar APK** se ejecuta solo.
2. En GitHub, entra a la pestaña **Actions**, abre la última ejecución de "Compilar APK" y descarga el artefacto `sombrero-seleccionador-apk` (es un `.zip`, adentro está el `.apk`).
3. Pasa el `.apk` al teléfono Android y ábrelo. Android te pedirá permiso para "instalar apps de origen desconocido": acéptalo.

## Cómo compilarlo en tu computadora

- **Android Studio:** abre la carpeta del proyecto y presiona ▶ Run.
- **Terminal:** `./gradlew assembleRelease` y el APK queda en `app/build/outputs/apk/release/`.

## 🎙 Voz natural (recomendado)

La voz del teléfono suena algo robótica. Para que el sombrero suene **natural y con personaje**, la app reproduce audios pregrabados: `app/src/main/assets/voz/<clave>.mp3`. Cada frase tiene su propio audio, y si a alguna le falta, esa frase se dice con la voz del teléfono. En ⚙ ves cuántas frases tienen audio.

Todo el guion son **61 frases y unos 3,200 caracteres**.

### Opción A: generarlas con IA (script incluido)

Desde la carpeta del proyecto (no hace falta instalar nada, solo Python 3):

```bash
# Ver qué se va a generar y cuántos caracteres gasta (no gasta créditos)
python3 herramientas/generar_voces.py --simular

# OpenAI: puedes dirigir la "actuación" (sombrero viejo, grave, misterioso)
export OPENAI_API_KEY="sk-..."
python3 herramientas/generar_voces.py --proveedor openai --voz onyx --solo prueba   # probar 1 frase
python3 herramientas/generar_voces.py --proveedor openai --voz onyx                # todas

# ElevenLabs: busca en su "Voice Library" una voz tipo "old wizard" y copia su Voice ID
export ELEVENLABS_API_KEY="..."
python3 herramientas/generar_voces.py --proveedor elevenlabs --voz <voice_id> --limite 10
```

| Opción | Para qué |
|---|---|
| `--simular` | Ver la lista y los caracteres, sin gastar |
| `--limite 10` | Generar solo 10 audios (empieza por los anuncios de casa, que son los que más se lucen). Puedes correrlo varias veces: no repite los que ya existen |
| `--solo prueba gryffindor_anuncio` | Generar solo esas frases |
| `--rehacer` | Volver a generar audios que ya existen (por ejemplo, si cambiaste el texto o la voz) |
| `--efecto` | Voz un poco más grave y con eco de salón (requiere `ffmpeg`) |

Después sube los `.mp3` al repo y compila de nuevo el APK.

### Opción B: grabar a una persona

Alguien de la iglesia con voz grave puede grabar las frases con el celular. La lista con el nombre exacto de cada archivo está en [`herramientas/GUION_GRABACION.md`](herramientas/GUION_GRABACION.md). Guárdalas en MP3 con esos nombres en `app/src/main/assets/voz/`.

### Cambiar lo que dice el sombrero

Todas las frases están en `app/src/main/assets/frases.json`. Si cambias el texto de una frase, vuelve a generar su audio con `--rehacer --solo <clave>`. Si agregas o quitas frases, cambian las claves: corre `--solo-lista` para actualizar el guion.

## Consejos para la fiesta

- **Voz del teléfono (respaldo):** si suena en inglés o no suena, ve a *Ajustes del teléfono → Sistema → Idioma → Salida de texto a voz*, elige "Servicios de voz de Google" e instala la voz en **español**. Después, en la app, toca ⚙ y prueba las voces hasta encontrar la que más suene a sombrero. Un **tono de 0.5 a 0.7** y una **velocidad de 0.8** funcionan bien.
- **Sonido:** conecta una **bocina Bluetooth** pequeña y escóndela dentro del sombrero. Así la voz sale del sombrero.
- **Efecto de magia:** compra un control Bluetooth para selfies. Deja el teléfono en una mesa (o escondido) y activa el sombrero desde lejos con el botón, sin que nadie vea.
- **Antes de empezar:** abre ⚙ → *Reiniciar conteo* para que el reparto empiece en cero.
- Toca la pantalla para pasar a la siguiente persona.

## Cambiar las frases, casas o colores

- `app/src/main/assets/frases.json`: todo lo que dice el sombrero (virtudes, dudas, anuncios). El anuncio está escrito como se pronuncia, por ejemplo "¡Jáfelpaf!" para Hufflepuff.
- `app/src/main/java/com/victordaz/sombrero/Casas.kt`: nombres, colores y lemas que se ven en pantalla.

## Estructura del proyecto

```
app/src/main/java/com/victordaz/sombrero/
├── MainActivity.kt        Pantalla completa, pantalla encendida, control remoto
├── PantallaSombrero.kt    Interfaz (Jetpack Compose): sombrero, estrellas, escudo, subtítulos
├── SombreroDibujo.kt      Dibujo del sombrero con boca animada
├── SombreroViewModel.kt   Lógica: elegir casa, decir el guion paso a paso
├── VozDelSombrero.kt      Texto a voz del teléfono (respaldo)
├── VozGrabada.kt          Reproduce los audios de assets/voz
├── Ajustes.kt             Preferencias y contador guardados en el teléfono
├── AjustesDialogo.kt      Ventana de ajustes
├── Casas.kt               Nombres y colores de las casas
└── Frases.kt              Lee frases.json y arma el guion
app/src/main/assets/
├── frases.json            Todo lo que dice el sombrero
└── voz/                   Audios .mp3 de cada frase
herramientas/
├── generar_voces.py       Genera los audios con OpenAI o ElevenLabs
└── GUION_GRABACION.md     Lista de frases y nombres de archivo
```

---
*Proyecto de fans para un evento privado y sin fines de lucro. Harry Potter y los nombres de las casas son marcas de sus respectivos dueños.*
