# Rediseño visual — Sombrero Seleccionador

Integración sobre `ccr-e19635cf-i62oqk`, base `0dfd2ca`.

## Recursos

Arte original generado para esta app mediante generación de imágenes integrada; no se descargaron fotografías de películas. PNG locales en `app/src/main/res/drawable-nodpi/`, cada uno menor de 500.000 bytes. Los nombres de las casas y sus colores vienen de `Casas.kt`.

- `fondo_noche.png`: 1080 × 1920. Salón gótico nocturno, velas a los lados, centro oscuro para el personaje y los textos; sin letras ni personas. Paleta optimizada para limitar el peso.
- `escudo_gryffindor.png`: 512 × 512, RGBA. León dorado #E8B730 sobre rojo #7F0909.
- `escudo_hufflepuff.png`: 512 × 512, RGBA. Tejón oscuro #2B2420 sobre ocre #B8860B.
- `escudo_ravenclaw.png`: 512 × 512, RGBA. Águila de bronce #C0894A sobre azul #0E2A6B.
- `escudo_slytherin.png`: 512 × 512, RGBA. Serpiente plateada #C8CCD0 sobre verde #1A5C36.
- `sombrero.png`: 640 × 640, RGBA. Sombrero antiguo de cuero/tela, punta doblada, rostro formado por pliegues y boca cerrada; luz cálida desde arriba a la izquierda.

Dirección común de los prompts: pieza medieval cinematográfica en vista frontal, materiales con relieve, detalle legible en tamaño pequeño, sin texto, sin marcas de agua; fondo realmente transparente para los objetos. Todos los escudos son imágenes individuales, no recortes de una hoja.

## Interfaz

Fondo con `painterResource` y `ContentScale.Crop`; estrellas conservadas; velo de color animado al revelar cada casa. Panel de subtítulos con fondo oscuro, tipografía serif, botón visible de selección y escudos con conteo. Los colores claros de presentación mantienen legibles Hufflepuff y Ravenclaw sin modificar los colores del modelo.

Distribución vertical desplazable para teléfonos y texto ampliado. Distribución en dos columnas cuando el área es horizontal y tiene al menos 600 dp de ancho. Los ajustes mantienen todos sus controles y reciben la misma paleta.

## Boca animada

`SombreroDibujo` superpone un Canvas sobre la imagen usando el mismo cuadrado centrado. El pliegue está aproximadamente entre x=0,30 y 0,70, y=0,65 de la imagen. No recortar ni estirar el PNG de forma independiente. En reposo se ve la costura original; al hablar se dibuja la abertura. Imagen y boca comparten balanceo y transformación. El parámetro `cejas` produce un leve estiramiento expresivo del conjunto.

La señal `estado.hablando`, la oscilación de 900 ms y el suavizado de 70 ms se conservan. Esto es animación vinculada al estado de voz, no sincronización por fonemas.

## Alcance y verificación

No se modifican `VozDelSombrero.kt`, `SombreroViewModel.kt`, `Frases.kt`, `Ajustes.kt` ni `Casas.kt`. No se añaden bibliotecas ni permisos de red. La compilación usa el flujo existente `Compilar APK` (`./gradlew assembleRelease --no-daemon`).

Prueba manual en Android: comenzar; escuchar la frase y comprobar boca/balanceo; esperar revelación y conteo; pasar a siguiente participante; abrir ajustes, probar voz, cambiar casas activas; probar control Bluetooth y texto ampliado. Comprobar las cuatro casas, especialmente el contraste de Hufflepuff. La compilación por sí sola no valida voz, Bluetooth ni el resultado visual en un dispositivo.
