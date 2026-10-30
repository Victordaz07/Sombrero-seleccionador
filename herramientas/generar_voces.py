#!/usr/bin/env python3
"""
Genera los audios del sombrero con una voz de IA a partir de app/src/main/assets/frases.json.

Los audios quedan en app/src/main/assets/voz/<clave>.mp3 y la app los usa automáticamente.
Además escribe herramientas/GUION_GRABACION.md, la lista de frases por si prefieres grabarlas
con una persona o con otra herramienta (solo respeta los nombres de archivo).

Uso (desde la carpeta del proyecto):

    # OpenAI (gpt-4o-mini-tts), permite indicar el "estilo" de actuación
    export OPENAI_API_KEY="sk-..."
    python3 herramientas/generar_voces.py --proveedor openai --voz onyx

    # ElevenLabs (voces muy expresivas; busca un "old wizard" en su Voice Library)
    export ELEVENLABS_API_KEY="..."
    python3 herramientas/generar_voces.py --proveedor elevenlabs --voz <voice_id>

    # Solo actualizar la lista de frases, sin generar audio
    python3 herramientas/generar_voces.py --solo-lista

Opciones útiles:
    --simular        muestra qué se generaría y cuántos caracteres gasta, sin gastar nada
    --limite 10      genera como máximo 10 audios (empieza por los anuncios de casa)
    --rehacer        vuelve a generar audios que ya existen (por ejemplo, si cambiaste una frase)
    --solo prueba    genera solo las claves indicadas (para probar voces antes de gastar)
    --efecto         usa ffmpeg para hacer la voz un poco más grave y con eco de salón
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
FRASES = RAIZ / "app/src/main/assets/frases.json"
CARPETA_VOZ = RAIZ / "app/src/main/assets/voz"
GUION = RAIZ / "herramientas/GUION_GRABACION.md"

# Cómo debe "actuar" la voz (solo OpenAI lo usa).
ESTILO = (
    "Habla en español latinoamericano neutro. Eres un sombrero mágico muy antiguo y sabio, "
    "con cientos de años de edad. Voz grave, un poco rasposa y aterciopelada, pausada y misteriosa, "
    "pero cálida y amable, con un toque teatral. Respeta los puntos suspensivos con pausas reales, "
    "como si estuvieras pensando. Los 'Mmm' y 'Hmm' son murmullos pensativos."
)
ESTILO_ANUNCIO = (
    "Habla en español latinoamericano. Eres un sombrero mágico antiguo anunciando el resultado "
    "frente a todo un salón lleno de gente: grita el nombre con mucha emoción, fuerza y orgullo."
)


def claves_y_textos(datos):
    """Misma lógica de claves que Frases.kt. Si cambias una, cambia la otra."""
    frases = [("prueba", datos["prueba"])]
    for campo, prefijo in [
        ("aperturas", "apertura"),
        ("decisiones", "decision"),
        ("preanuncios", "preanuncio"),
        ("cierres", "cierre"),
    ]:
        frases += [(f"{prefijo}_{i + 1}", t) for i, t in enumerate(datos[campo])]
    for casa, c in datos["casas"].items():
        frases += [(f"{casa}_virtud_{i + 1}", t) for i, t in enumerate(c["virtudes"])]
        frases += [(f"{casa}_duda_{i + 1}", t) for i, t in enumerate(c["dudas"])]
        frases.append((f"{casa}_anuncio", c["anuncio"]))
    return frases


def prioridad(frase):
    """Primero lo que más se luce: el anuncio de la casa y el suspenso antes del anuncio."""
    clave = frase[0]
    if clave.endswith("_anuncio"):
        return 0
    if clave.startswith("preanuncio"):
        return 1
    if clave == "prueba":
        return 3
    return 2


def escribir_guion(frases):
    lineas = [
        "# Guion de grabación del sombrero",
        "",
        "Cada frase va en `app/src/main/assets/voz/<archivo>` en formato **MP3**.",
        "Si una frase no tiene audio, la app usa la voz del teléfono para esa frase.",
        "",
        "Este archivo se genera solo con `python3 herramientas/generar_voces.py --solo-lista`.",
        "",
        "| Archivo | Frase |",
        "|---|---|",
    ]
    lineas += [f"| `{clave}.mp3` | {texto} |" for clave, texto in frases]
    GUION.write_text("\n".join(lineas) + "\n", encoding="utf-8")


def pedir(url, cuerpo, cabeceras):
    req = urllib.request.Request(url, data=json.dumps(cuerpo).encode("utf-8"), method="POST")
    for k, v in cabeceras.items():
        req.add_header(k, v)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.read()
    except urllib.error.HTTPError as e:
        sys.exit(f"Error {e.code} del servicio de voz: {e.read().decode('utf-8', 'replace')}")


def generar_openai(texto, voz, es_anuncio):
    clave = os.environ.get("OPENAI_API_KEY") or sys.exit("Falta la variable OPENAI_API_KEY")
    return pedir(
        "https://api.openai.com/v1/audio/speech",
        {
            "model": "gpt-4o-mini-tts",
            "voice": voz or "onyx",
            "input": texto,
            "instructions": ESTILO_ANUNCIO if es_anuncio else ESTILO,
            "response_format": "mp3",
        },
        {"Authorization": f"Bearer {clave}"},
    )


def generar_elevenlabs(texto, voz, es_anuncio):
    clave = os.environ.get("ELEVENLABS_API_KEY") or sys.exit("Falta la variable ELEVENLABS_API_KEY")
    if not voz:
        sys.exit("Con ElevenLabs indica el id de la voz: --voz <voice_id>")
    return pedir(
        f"https://api.elevenlabs.io/v1/text-to-speech/{voz}?output_format=mp3_44100_128",
        {
            "text": texto,
            "model_id": "eleven_multilingual_v2",
            # Menos estabilidad = más expresivo; en el anuncio, más dramático todavía.
            "voice_settings": {
                "stability": 0.3 if es_anuncio else 0.45,
                "similarity_boost": 0.8,
                "style": 0.6 if es_anuncio else 0.35,
            },
        },
        {"xi-api-key": clave},
    )


def aplicar_efecto(ruta):
    """Baja un poco el tono y agrega un eco suave, como de salón grande."""
    if not shutil.which("ffmpeg"):
        sys.exit("--efecto necesita ffmpeg instalado")
    with tempfile.NamedTemporaryFile(suffix=".mp3", delete=False) as tmp:
        salida = Path(tmp.name)
    filtro = "asetrate=44100*0.92,aresample=44100,atempo=1.087,aecho=0.8:0.6:60|120:0.25|0.15"
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", str(ruta), "-af", filtro, "-b:a", "128k", str(salida)],
        check=True,
    )
    shutil.move(salida, ruta)


def main():
    p = argparse.ArgumentParser(description="Genera las voces del sombrero.")
    p.add_argument("--proveedor", choices=["openai", "elevenlabs"], default="openai")
    p.add_argument("--voz", help="OpenAI: onyx, ash, ballad, echo, fable, sage... / ElevenLabs: voice_id")
    p.add_argument("--rehacer", action="store_true", help="regenera también los audios que ya existen")
    p.add_argument("--solo", nargs="+", metavar="CLAVE", help="genera solo estas claves (ej. prueba)")
    p.add_argument("--limite", type=int, metavar="N", help="genera como máximo N audios")
    p.add_argument("--simular", action="store_true", help="muestra qué se generaría, sin gastar créditos")
    p.add_argument("--efecto", action="store_true", help="voz más grave + eco (requiere ffmpeg)")
    p.add_argument("--solo-lista", action="store_true", help="solo actualiza GUION_GRABACION.md")
    args = p.parse_args()

    frases = claves_y_textos(json.loads(FRASES.read_text(encoding="utf-8")))
    escribir_guion(frases)
    print(f"Guion actualizado: {GUION.relative_to(RAIZ)} ({len(frases)} frases)")
    if args.solo_lista:
        return

    pendientes = [
        (clave, texto) for clave, texto in sorted(frases, key=prioridad)
        if (not args.solo or clave in args.solo)
        and (args.rehacer or not (CARPETA_VOZ / f"{clave}.mp3").exists())
    ][: args.limite]
    caracteres = sum(len(t) for _, t in pendientes)
    print(f"Por generar: {len(pendientes)} audios, {caracteres} caracteres")
    if args.simular:
        for clave, texto in pendientes:
            print(f"   {clave}: {texto}")
        return

    generar = generar_openai if args.proveedor == "openai" else generar_elevenlabs
    CARPETA_VOZ.mkdir(parents=True, exist_ok=True)
    hechas = 0
    for clave, texto in pendientes:
        ruta = CARPETA_VOZ / f"{clave}.mp3"
        print(f"🎙  {clave}: {texto}")
        ruta.write_bytes(generar(texto, args.voz, clave.endswith("_anuncio")))
        if args.efecto:
            aplicar_efecto(ruta)
        hechas += 1
    print(f"Listo: {hechas} audios nuevos en {CARPETA_VOZ.relative_to(RAIZ)}")


if __name__ == "__main__":
    main()
