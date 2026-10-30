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

## Consejos para la fiesta

- **La voz:** si suena en inglés o no suena, ve a *Ajustes del teléfono → Sistema → Idioma → Salida de texto a voz*, elige "Servicios de voz de Google" e instala la voz en **español**. Después, en la app, toca ⚙ y prueba las voces hasta encontrar la que más suene a sombrero. Un **tono de 0.5 a 0.7** y una **velocidad de 0.8** funcionan bien.
- **Sonido:** conecta una **bocina Bluetooth** pequeña y escóndela dentro del sombrero. Así la voz sale del sombrero.
- **Efecto de magia:** compra un control Bluetooth para selfies. Deja el teléfono en una mesa (o escondido) y activa el sombrero desde lejos con el botón, sin que nadie vea.
- **Antes de empezar:** abre ⚙ → *Reiniciar conteo* para que el reparto empiece en cero.
- Toca la pantalla para pasar a la siguiente persona.

## Cambiar las frases, casas o colores

Todo está en dos archivos:

- `app/src/main/java/com/victordaz/sombrero/Casas.kt`: nombres, colores, lemas y virtudes de cada casa. `nombreHablado` es cómo lo pronuncia la voz (por ejemplo "Jáfelpaf" para Hufflepuff).
- `app/src/main/java/com/victordaz/sombrero/Frases.kt`: frases de apertura, dudas, suspenso y despedida.

## Estructura del proyecto

```
app/src/main/java/com/victordaz/sombrero/
├── MainActivity.kt        Pantalla completa, pantalla encendida, control remoto
├── PantallaSombrero.kt    Interfaz (Jetpack Compose): sombrero, estrellas, escudo, subtítulos
├── SombreroDibujo.kt      Dibujo del sombrero con boca animada
├── SombreroViewModel.kt   Lógica: elegir casa, decir el guion paso a paso
├── VozDelSombrero.kt      Texto a voz en español
├── Ajustes.kt             Preferencias y contador guardados en el teléfono
├── AjustesDialogo.kt      Ventana de ajustes
├── Casas.kt               Datos de las casas
└── Frases.kt              Frases del sombrero
```

---
*Proyecto de fans para un evento privado y sin fines de lucro. Harry Potter y los nombres de las casas son marcas de sus respectivos dueños.*
