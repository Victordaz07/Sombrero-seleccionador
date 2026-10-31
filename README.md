# 🎩 Sombrero Seleccionador

App de Android para eventos de la iglesia: una persona se pone el sombrero, el sombrero "piensa" en voz alta, dice sus virtudes y **grita a qué casa pertenece**, mientras la pantalla se pinta con los colores de esa casa.

Funciona **sin internet**, en **español e inglés**, con voces grabadas con IA (ElevenLabs) y música de fondo.

## Instalar

1. En GitHub, pestaña **Actions** → la última ejecución de **Compilar APK** → descarga `sombrero-seleccionador-apk` (es un `.zip` con el `.apk`).
2. Pásalo al teléfono, ábrelo y acepta "instalar apps de origen desconocido".

Cada vez que se sube un cambio, GitHub revisa `frases.json`, corre las pruebas y compila el APK solo.

## Cómo se usa

### En una fiesta
1. Eliges el idioma arriba a la izquierda (🇺🇸 EN / 🇲🇽 ES).
2. La persona se sienta y tocas la pantalla (o *Comenzar selección*, o el botón de un control Bluetooth).
3. Dice uno de sus discursos (4 por casa y por idioma, sin repetirse), **grita la casa** y suena una **fanfarria con aplausos** 🎺👏.
4. Tocas para la siguiente persona.

### En la calle 🚶
- **Arriba a la izquierda** eliges el idioma: 🇺🇸 EN / 🇲🇽 ES.
- **"El sombrero invita a…"**: tocas a quién va pasando (niño, niña, jovencito, jovencita, señor, señora o cualquiera) y el sombrero lo invita. Hay varias invitaciones por tipo y se van alternando.
- Si acepta, tocas *Comenzar*: la ceremonia empieza en ese idioma.
- **Batería** arriba al centro (se pone roja al 20%).

### Botones de arriba a la derecha
- 🎵 / 🔇 — prende o apaga la **música de fondo** (un toque).
- ⚙ — **Ajustes** (hay que **mantener presionado**, para que nadie los abra sin querer).

### Elegir tú la casa
Mantén presionado uno de los 4 escudos de abajo: el sombrero hace la ceremonia completa y termina en esa casa (para repetir una selección y grabarla). Por defecto no suma al conteo.

## Ajustes (⚙, mantener presionado)

| Ajuste | Para qué |
|---|---|
| Idioma | Español o inglés |
| Volumen + *Probar sonido* | Volumen multimedia (bocina del teléfono o Bluetooth) |
| Fanfarria con aplausos | Suena justo después de que el sombrero grita la casa. Hay 6 (`assets/efectos/fanfarria_1.mp3`…`_6.mp3`, hechas en ElevenLabs) y se alternan sin repetir |
| Música de fondo | Prender/apagar y su volumen. Baja sola cuando habla el sombrero |
| **Fijar / Soltar** | Fija la app: nadie puede salir con Atrás o Inicio |
| Modo calle | Muestra los botones de invitación |
| Repartir parejo | Manda a cada persona a la casa con menos gente |
| Sumar elegidas a mano | Si las casas elegidas con el escudo cuentan |
| Casas participantes / Reiniciar conteo | Para empezar de cero antes de cada evento |

## Sonido

- Todo suena por el canal **multimedia**: bocina del teléfono o **bocina Bluetooth** (escóndela dentro del sombrero).
- La app no cambia el volumen ni el brillo por su cuenta. Si el volumen está muy bajo aparece un aviso rojo; al tocarlo se abre el control de volumen del teléfono.
- **Música de fondo**: ambiente mágico original (libre de derechos), en bucle. Con la música apagada suena un "silencio" inaudible: así la bocina Bluetooth **no se duerme** y no se corta la primera palabra.
- Si entra una llamada o sales de la app, el sombrero se calla y la música se pausa.
- Si falta algún audio, esa parte la dice la voz del teléfono.

## Voces y textos

Todo lo que dice el sombrero está en **`app/src/main/assets/frases.json`**:

| Sección | Qué es | Audios |
|---|---|---|
| `discursos` / `discursos_en` | 4 discursos por casa. `{ANUNCIO}` marca dónde grita la casa | `<casa>_discurso_<n>.mp3` / `_en.mp3` |
| `invitaciones` | Modo calle, por idioma y tipo de persona | `invitacion_<tipo>_<n>_en.mp3` / `.mp3` |

Las etiquetas entre corchetes (`[excited]`, `[laughs]`, `[shouting]`…) son para que ElevenLabs actúe la emoción; en pantalla no se muestran. La lista completa de textos para grabar está en [`herramientas/GUION_GRABACION.md`](herramientas/GUION_GRABACION.md).

### Herramientas

```bash
python3 herramientas/validar_frases.py        # revisa frases.json y dice qué audios faltan
python3 herramientas/generar_voces.py --solo-lista   # actualiza GUION_GRABACION.md

# Generar audios con la API de ElevenLabs (o OpenAI con --proveedor openai)
export ELEVENLABS_API_KEY="..."
python3 herramientas/generar_voces.py --proveedor elevenlabs --voz <voice_id> --discursos --simular

# Importar audios hechos a mano en la página de ElevenLabs: los reconoce aunque se llamen
# distinto y sincroniza el grito con la pantalla (necesita: pip install faster-whisper)
python3 herramientas/importar_voces.py --casa gryffindor audios/*.mp3              # discursos en español
python3 herramientas/importar_voces.py --idioma en --casa gryffindor audios/*.mp3  # discursos en inglés
python3 herramientas/importar_voces.py --especiales audios/*.mp3                   # invitaciones

python3 herramientas/componer_musica.py       # vuelve a crear la música de fondo (necesita numpy)
```

## Para programadores

```
app/src/main/java/com/victordaz/sombrero/
├── MainActivity.kt        Pantalla completa, control remoto, pausa al salir
├── PantallaSombrero.kt    Interfaz (Jetpack Compose)
├── AjustesDialogo.kt      Ventana de ajustes
├── SombreroViewModel.kt   La ceremonia paso a paso
├── Reparto.kt             Reparto parejo entre casas y rotación sin repetir (con pruebas)
├── Frases.kt              Lee frases.json (si tiene un error, avisa en vez de cerrarse)
├── VozGrabada.kt          Reproduce los audios y sincroniza subtítulos y grito
├── VozDelSombrero.kt      Voz del teléfono (respaldo)
├── FondoMusical.kt        Música de fondo / silencio para el Bluetooth
├── Volumen.kt             Volumen multimedia y foco de audio
├── Ajustes.kt             Preferencias y conteo guardados
├── Casas.kt · Idioma.kt · Publico.kt   Datos de casas, textos de pantalla y tipos de persona
└── SombreroDibujo.kt      Dibujo del sombrero con boca animada
app/src/test/              Pruebas: ./gradlew testReleaseUnitTest
app/src/main/assets/       frases.json, voz/ (audios) y musica/
herramientas/              Scripts de voces, música y validación
arte/                      Ícono original
```

---
*Proyecto de fans para eventos privados y sin fines de lucro. Harry Potter y los nombres de las casas son marcas de sus respectivos dueños.*
