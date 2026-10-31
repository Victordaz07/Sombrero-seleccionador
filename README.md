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

La voz del teléfono suena algo robótica. Para que el sombrero suene **natural y con personaje**, la app reproduce audios pregrabados en `app/src/main/assets/voz/`:

1. **Discursos completos (recomendado):** 4 actuaciones distintas por casa (16 audios, unos 4,600 caracteres). Cada una se dice de corrido, así que suena como una sola actuación. La app las rota y no repite ninguna hasta usar las 4. Los subtítulos y la revelación de la casa se sincronizan con el audio (con ElevenLabs, al segundo exacto).
2. **Frases sueltas (respaldo):** si una casa no tiene discursos, el sombrero arma uno con 61 frases sueltas. Las que no tienen audio usan la voz del teléfono.

En ⚙ ves cuántos discursos y frases tienen audio.

### Opción A: generarlas con IA (script incluido)

Desde la carpeta del proyecto (no hace falta instalar nada, solo Python 3):

```bash
# Ver qué se va a generar y cuántos caracteres gasta (no gasta créditos)
python3 herramientas/generar_voces.py --simular

# OpenAI: puedes dirigir la "actuación" (sombrero viejo, grave, misterioso)
export OPENAI_API_KEY="sk-..."
python3 herramientas/generar_voces.py --proveedor openai --voz onyx --solo prueba   # probar 1 frase
python3 herramientas/generar_voces.py --proveedor openai --voz onyx                # todas

# ElevenLabs (recomendado): crea la voz con Voice Design y copia su Voice ID
export ELEVENLABS_API_KEY="..."
python3 herramientas/generar_voces.py --proveedor elevenlabs --voz <voice_id> --discursos --limite 4  # 1 por casa
python3 herramientas/generar_voces.py --proveedor elevenlabs --voz <voice_id> --discursos            # los 16
```

| Opción | Para qué |
|---|---|
| `--discursos` | Generar solo los 16 discursos completos |
| `--simular` | Ver la lista y los caracteres, sin gastar |
| `--limite 4` | Generar solo 4 audios (empieza por un discurso de cada casa). Puedes correrlo varias veces: no repite los que ya existen |
| `--solo prueba gryffindor_anuncio` | Generar solo esas frases |
| `--rehacer` | Volver a generar audios que ya existen (por ejemplo, si cambiaste el texto o la voz) |
| `--efecto` | Voz un poco más grave y con eco de salón (requiere `ffmpeg`) |

Después sube los `.mp3` al repo y compila de nuevo el APK.

### Opción B: grabar a una persona

Alguien de la iglesia con voz grave puede grabar las frases con el celular. La lista con el nombre exacto de cada archivo está en [`herramientas/GUION_GRABACION.md`](herramientas/GUION_GRABACION.md). Guárdalas en MP3 con esos nombres en `app/src/main/assets/voz/`.

### Cambiar lo que dice el sombrero

Todo está en `app/src/main/assets/frases.json`. En `discursos`, cada casa tiene 4 listas de frases y `{ANUNCIO}` marca dónde grita el nombre de la casa. Los discursos se generan con **Eleven v3**, que actúa las etiquetas de emoción entre corchetes (`[whispers]`, `[chuckles]`, `[thoughtful]`, `[excited]`...). Las etiquetas no se muestran en los subtítulos. Si cambias el texto de una frase o discurso, vuelve a generar su audio con `--rehacer --solo <clave>` (por ejemplo `--solo gryffindor_discurso_2`). Si agregas o quitas frases, cambian las claves: corre `--solo-lista` para actualizar el guion.

## 🌎 Español / English

En ⚙ → **Idioma / Language** cambias toda la ceremonia a inglés: textos de pantalla, lemas de las casas, subtítulos y discursos. Hay 16 discursos en inglés (`discursos_en` en `frases.json`, archivos `<casa>_discurso_<n>_en.mp3`). Si un discurso en inglés no tiene audio, la voz del teléfono lo lee en inglés.

**Pregunta de idioma:** si está activo en ⚙ (*Preguntar idioma a cada persona*), al tocar el sombrero pregunta "…do you prefer ENGLISH… ¿o prefieres que te hable en ESPAÑOL?" y aparecen dos botones. Al elegir, responde en ese idioma y sigue la ceremonia. Con el control Bluetooth: subir volumen = idioma actual, bajar volumen = el otro. Audios: `pregunta_idioma.mp3`, `respuesta_idioma_es.mp3`, `respuesta_idioma_en.mp3` (se importan con `importar_voces.py --especiales`).

Para importar audios hechos a mano en ElevenLabs (los reconoce y sincroniza el grito aunque el archivo se llame distinto):

```bash
pip install faster-whisper
python3 herramientas/importar_voces.py --casa gryffindor audios/*.mp3              # español
python3 herramientas/importar_voces.py --idioma en --casa gryffindor audios/*.mp3  # inglés
```

## Consejos para la fiesta

- **Voz del teléfono (respaldo):** si suena en inglés o no suena, ve a *Ajustes del teléfono → Sistema → Idioma → Salida de texto a voz*, elige "Servicios de voz de Google" e instala la voz en **español**. Después, en la app, toca ⚙ y prueba las voces hasta encontrar la que más suene a sombrero. Un **tono de 0.5 a 0.7** y una **velocidad de 0.8** funcionan bien.
- **Sonido:** conecta una **bocina Bluetooth** pequeña y escóndela dentro del sombrero. Así la voz sale del sombrero.
- **Volumen:** la app usa el volumen *multimedia* (el mismo de la música), tanto en la bocina del teléfono como en Bluetooth. Si está muy bajo, al abrir la app se sube al 80% y aparece un aviso rojo; también se ajusta en ⚙ → *Volumen* (con el botón *Probar sonido*). Los botones de volumen del teléfono suben y bajan el volumen; solo un control Bluetooth externo activa el sombrero.
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
