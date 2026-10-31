#!/usr/bin/env python3
"""
Importa audios de discursos generados a mano (por ejemplo, en la página de ElevenLabs).

Para cada MP3:
1. Lo transcribe con Whisper (en tu computadora, no gasta créditos).
2. Reconoce cuál de los 16 discursos de frases.json es, aunque el archivo se llame distinto.
3. Calcula el segundo en que empieza cada frase (y el grito de la casa) para sincronizar
   subtítulos y revelación, y guarda <clave>.mp3 y <clave>.json en assets/voz.

Necesita: pip install faster-whisper   (y ffmpeg)

Uso:
    python3 herramientas/importar_voces.py audio1.mp3 audio2.mp3 ...
    python3 herramientas/importar_voces.py --casa hufflepuff *.mp3     # solo busca entre esa casa
    python3 herramientas/importar_voces.py --idioma en *.mp3            # discursos en inglés
    python3 herramientas/importar_voces.py --especiales *.mp3           # pregunta de idioma e invitaciones
    python3 herramientas/importar_voces.py --simular *.mp3              # muestra qué haría
"""

import argparse
import difflib
import json
import re
import shutil
import subprocess
import sys
import unicodedata
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generar_voces import CARPETA_VOZ, FRASES, Frase, cargar_frases, sin_etiquetas  # noqa: E402


def normalizar(texto):
    """minúsculas, sin acentos ni signos: 'Ya SÉ!' -> 'ya se'."""
    texto = unicodedata.normalize("NFD", sin_etiquetas(texto).lower())
    texto = "".join(c for c in texto if unicodedata.category(c) != "Mn")
    return re.findall(r"[a-zñ0-9]+", texto)


def leer_audio(ruta):
    """Decodifica con ffmpeg a 16 kHz mono (lo que espera Whisper)."""
    import numpy as np

    crudo = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", str(ruta), "-ac", "1", "-ar", "16000", "-f", "s16le", "-"],
        capture_output=True, check=True,
    ).stdout
    return np.frombuffer(crudo, np.int16).astype(np.float32) / 32768.0


def transcribir(modelo, ruta, idioma):
    segmentos, _ = modelo.transcribe(leer_audio(ruta), language=idioma, word_timestamps=True, vad_filter=False)
    palabras = []
    for s in segmentos:
        for p in s.words or []:
            for n in normalizar(p.word):
                palabras.append((n, p.start, p.end))
    return palabras


def inicios_de(discurso, palabras):
    """Alinea las palabras del texto con las transcritas y toma el inicio de cada frase."""
    esperadas = []  # (palabra, índice de frase)
    for i, s in enumerate(discurso.segmentos):
        esperadas += [(n, i) for n in normalizar(s)]
    oidas = [p[0] for p in palabras]
    pares = {}
    bloques = difflib.SequenceMatcher(None, [e[0] for e in esperadas], oidas, autojunk=False)
    for b in bloques.get_matching_blocks():
        for k in range(b.size):
            pares[b.a + k] = b.b + k

    inicios, ultimo_fin = [], 0.0
    for i in range(len(discurso.segmentos)):
        indices = [j for j, (_, seg) in enumerate(esperadas) if seg == i]
        oidos = [pares[j] for j in indices if j in pares]
        if oidos:
            inicios.append(round(palabras[min(oidos)][1], 3))
            ultimo_fin = palabras[max(oidos)][2]
        else:
            # No se reconoció (p. ej. el nombre de la casa): primera palabra después de la frase anterior.
            siguiente = next((p for p in palabras if p[1] >= ultimo_fin - 0.05), None)
            inicios.append(round(siguiente[1] if siguiente else ultimo_fin, 3))
            if siguiente:
                ultimo_fin = siguiente[2]
    inicios[0] = 0.0
    # Que nunca vayan hacia atrás.
    for i in range(1, len(inicios)):
        inicios[i] = max(inicios[i], inicios[i - 1])
    return inicios


def parecido(discurso, palabras):
    return difflib.SequenceMatcher(
        None, normalizar(" ".join(discurso.segmentos[: discurso.indice_anuncio])), [p[0] for p in palabras]
    ).ratio()


def main():
    p = argparse.ArgumentParser(description="Importa audios de discursos hechos a mano.")
    p.add_argument("audios", nargs="+", type=Path)
    p.add_argument("--casa", help="solo compara con los discursos de esta casa")
    p.add_argument("--idioma", choices=["es", "en"], default="es", help="idioma de los audios")
    p.add_argument("--especiales", action="store_true", help="pregunta de idioma y sus respuestas")
    p.add_argument("--modelo", default="small", help="modelo de Whisper: tiny, base, small, medium")
    p.add_argument("--simular", action="store_true", help="muestra qué haría, sin copiar nada")
    args = p.parse_args()

    from faster_whisper import WhisperModel

    todas = cargar_frases(json.loads(FRASES.read_text(encoding="utf-8")))
    if args.especiales:
        # Frases de un solo tramo: se tratan como un "discurso" sin anuncio.
        discursos = [
            Frase(f.clave, f.texto, [f.texto], 1) for f in todas
            if f.clave.startswith(("pregunta_idioma", "respuesta_idioma", "invitacion_"))
        ]
    else:
        discursos = [f for f in todas if f.es_discurso and f.en_ingles == (args.idioma == "en")]
    if args.casa:
        discursos = [d for d in discursos if d.clave.startswith(f"{args.casa}_")]
    modelo = WhisperModel(args.modelo, device="cpu", compute_type="int8")

    usados = {}
    for ruta in args.audios:
        # La pregunta mezcla inglés y español: que Whisper detecte el idioma solo.
        palabras = transcribir(modelo, ruta, None if args.especiales else args.idioma)
        puntajes = sorted(((parecido(d, palabras), d) for d in discursos), key=lambda x: -x[0])
        puntaje, discurso = puntajes[0]
        oido = " ".join(p[0] for p in palabras)
        print(f"\n{ruta.name}\n   oído: {oido}\n   → {discurso.clave} (parecido {puntaje:.0%})")
        if puntaje < 0.5:
            print("   ⚠️  Parecido bajo: revisa que el texto sea el de frases.json")
        if discurso.clave in usados:
            print(f"   ⚠️  {discurso.clave} ya lo tenía {usados[discurso.clave]}; se reemplaza")
        usados[discurso.clave] = ruta.name

        inicios = inicios_de(discurso, palabras)
        for t, s in zip(inicios, discurso.segmentos):
            print(f"   {t:6.2f}s  {sin_etiquetas(s)}")
        if args.simular:
            continue
        CARPETA_VOZ.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ruta, CARPETA_VOZ / f"{discurso.clave}.mp3")
        (CARPETA_VOZ / f"{discurso.clave}.json").write_text(json.dumps({"inicios": inicios}) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
