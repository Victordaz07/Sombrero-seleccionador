#!/usr/bin/env python3
"""
Genera los audios del sombrero con una voz de IA a partir de app/src/main/assets/frases.json.

Los audios quedan en app/src/main/assets/voz/<clave>.mp3 y la app los usa automáticamente.
Además escribe herramientas/GUION_GRABACION.md, la lista de frases por si prefieres grabarlas
con una persona o con otra herramienta (solo respeta los nombres de archivo).

Hay dos tipos de audio:
- Discursos: 4 actuaciones completas por casa (16 audios). Es lo recomendado: suenan
  más naturales porque se dicen de corrido. Con ElevenLabs también se guarda
  <clave>.json con el segundo exacto de cada frase, para sincronizar subtítulos y la revelación.
- Frases sueltas: respaldo para armar el discurso por partes si falta algún discurso.

Uso (desde la carpeta del proyecto):

    # ElevenLabs (recomendado)
    export ELEVENLABS_API_KEY="..."
    python3 herramientas/generar_voces.py --proveedor elevenlabs --voz <voice_id> --discursos

    # OpenAI (gpt-4o-mini-tts), permite indicar el "estilo" de actuación
    export OPENAI_API_KEY="sk-..."
    python3 herramientas/generar_voces.py --proveedor openai --voz onyx --discursos

    # Solo actualizar la lista de frases, sin generar audio
    python3 herramientas/generar_voces.py --solo-lista

Opciones útiles:
    --discursos      genera solo los 16 discursos completos (sin las frases sueltas)
    --simular        muestra qué se generaría y cuántos caracteres gasta, sin gastar nada
    --limite 4       genera como máximo 4 audios (empieza por un discurso de cada casa)
    --rehacer        vuelve a generar audios que ya existen (por ejemplo, si cambiaste una frase)
    --solo prueba    genera solo las claves indicadas (para probar voces antes de gastar)
    --efecto         usa ffmpeg para hacer la voz un poco más grave y con eco de salón
"""

import argparse
import base64
import json
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
FRASES = RAIZ / "app/src/main/assets/frases.json"
CARPETA_VOZ = RAIZ / "app/src/main/assets/voz"
GUION = RAIZ / "herramientas/GUION_GRABACION.md"
ANUNCIO = "{ANUNCIO}"

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
ESTILO_DISCURSO = ESTILO + (
    " Haz una pausa de suspenso antes de decir el nombre de la casa, y grita ese nombre "
    "con mucha emoción, como anunciándolo a todo un salón lleno de gente."
)


@dataclass
class Frase:
    clave: str
    texto: str
    segmentos: list = None  # solo en discursos
    indice_anuncio: int = -1

    @property
    def es_discurso(self):
        return self.segmentos is not None

    @property
    def es_anuncio(self):
        return self.clave.endswith("_anuncio")


def cargar_frases(datos):
    """Misma lógica de claves que Frases.kt. Si cambias una, cambia la otra."""
    frases = []
    for casa, lista in datos.get("discursos", {}).items():
        anuncio = datos["casas"][casa]["anuncio"]
        for i, crudos in enumerate(lista):
            if crudos.count(ANUNCIO) != 1:
                sys.exit(f"El discurso {i + 1} de {casa} debe tener {ANUNCIO} exactamente una vez")
            segmentos = [anuncio if s == ANUNCIO else s for s in crudos]
            frases.append(Frase(f"{casa}_discurso_{i + 1}", " ".join(segmentos), segmentos, crudos.index(ANUNCIO)))

    frases.append(Frase("prueba", datos["prueba"]))
    for campo, prefijo in [
        ("aperturas", "apertura"),
        ("decisiones", "decision"),
        ("preanuncios", "preanuncio"),
        ("cierres", "cierre"),
    ]:
        frases += [Frase(f"{prefijo}_{i + 1}", t) for i, t in enumerate(datos[campo])]
    for casa, c in datos["casas"].items():
        frases += [Frase(f"{casa}_virtud_{i + 1}", t) for i, t in enumerate(c["virtudes"])]
        frases += [Frase(f"{casa}_duda_{i + 1}", t) for i, t in enumerate(c["dudas"])]
        frases.append(Frase(f"{casa}_anuncio", c["anuncio"]))
    return frases


def prioridad(frase):
    """Primero un discurso de cada casa, luego el resto de discursos, luego frases sueltas."""
    if frase.es_discurso:
        return (0, int(frase.clave.rsplit("_", 1)[1]))
    if frase.es_anuncio:
        return (1, 0)
    if frase.clave.startswith("preanuncio"):
        return (2, 0)
    if frase.clave == "prueba":
        return (4, 0)
    return (3, 0)


def escribir_guion(frases):
    lineas = [
        "# Guion de grabación del sombrero",
        "",
        "Cada audio va en `app/src/main/assets/voz/<archivo>` en formato **MP3**.",
        "",
        "Este archivo se genera solo con `python3 herramientas/generar_voces.py --solo-lista`.",
        "",
        "## Discursos completos (recomendado)",
        "",
        "4 por casa. Cada uno se graba **de corrido**, en un solo audio. La app los va rotando para que no se repitan.",
        "Haz una pausa de suspenso antes del nombre de la casa (en **negritas**) y grítalo con emoción.",
        "",
        "| Archivo | Discurso |",
        "|---|---|",
    ]
    for f in frases:
        if f.es_discurso:
            partes = [f"**{s}**" if i == f.indice_anuncio else s for i, s in enumerate(f.segmentos)]
            lineas.append(f"| `{f.clave}.mp3` | {' '.join(partes)} |")
    lineas += [
        "",
        "## Frases sueltas (respaldo)",
        "",
        "Si una casa no tiene discursos grabados, el sombrero arma el discurso con estas frases.",
        "Si una frase no tiene audio, la app usa la voz del teléfono para esa frase.",
        "",
        "| Archivo | Frase |",
        "|---|---|",
    ]
    lineas += [f"| `{f.clave}.mp3` | {f.texto} |" for f in frases if not f.es_discurso]
    GUION.write_text("\n".join(lineas) + "\n", encoding="utf-8")


def pedir(url, cuerpo, cabeceras):
    req = urllib.request.Request(url, data=json.dumps(cuerpo).encode("utf-8"), method="POST")
    for k, v in cabeceras.items():
        req.add_header(k, v)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            return r.read()
    except urllib.error.HTTPError as e:
        sys.exit(f"Error {e.code} del servicio de voz: {e.read().decode('utf-8', 'replace')}")


def generar_openai(frase, voz):
    """Devuelve (audio_mp3, inicios). OpenAI no da tiempos: la app los estima."""
    clave = os.environ.get("OPENAI_API_KEY") or sys.exit("Falta la variable OPENAI_API_KEY")
    estilo = ESTILO_DISCURSO if frase.es_discurso else ESTILO_ANUNCIO if frase.es_anuncio else ESTILO
    audio = pedir(
        "https://api.openai.com/v1/audio/speech",
        {
            "model": "gpt-4o-mini-tts",
            "voice": voz or "onyx",
            "input": frase.texto,
            "instructions": estilo,
            "response_format": "mp3",
        },
        {"Authorization": f"Bearer {clave}"},
    )
    return audio, None


def generar_elevenlabs(frase, voz):
    """Devuelve (audio_mp3, inicios). En discursos pide los tiempos de cada letra."""
    clave = os.environ.get("ELEVENLABS_API_KEY") or sys.exit("Falta la variable ELEVENLABS_API_KEY")
    if not voz:
        sys.exit("Con ElevenLabs indica el id de la voz: --voz <voice_id>")

    if frase.es_discurso:
        # Pausa de suspenso justo antes de gritar la casa.
        partes = list(frase.segmentos)
        partes[frase.indice_anuncio] = '<break time="1.2s" /> ' + partes[frase.indice_anuncio]
        texto = " ".join(partes)
        ajustes = {"stability": 0.4, "similarity_boost": 0.8, "style": 0.45}
        url = f"https://api.elevenlabs.io/v1/text-to-speech/{voz}/with-timestamps?output_format=mp3_44100_128"
    else:
        texto = frase.texto
        # Menos estabilidad = más expresivo; en el anuncio, más dramático todavía.
        ajustes = {
            "stability": 0.3 if frase.es_anuncio else 0.45,
            "similarity_boost": 0.8,
            "style": 0.6 if frase.es_anuncio else 0.35,
        }
        url = f"https://api.elevenlabs.io/v1/text-to-speech/{voz}?output_format=mp3_44100_128"

    respuesta = pedir(
        url,
        {"text": texto, "model_id": "eleven_multilingual_v2", "voice_settings": ajustes},
        {"xi-api-key": clave},
    )
    if not frase.es_discurso:
        return respuesta, None

    datos = json.loads(respuesta)
    audio = base64.b64decode(datos["audio_base64"])
    return audio, inicios_de_segmentos(frase.segmentos, datos.get("alignment"))


def inicios_de_segmentos(segmentos, alineacion):
    """Busca cada frase dentro del texto alineado y toma el segundo en que empieza."""
    if not alineacion:
        return None
    letras = "".join(alineacion["characters"])
    tiempos = alineacion["character_start_times_seconds"]
    inicios, cursor = [], 0
    for s in segmentos:
        i = letras.find(s, cursor)
        if i < 0:
            inicios.append(None)
        else:
            inicios.append(round(tiempos[i], 3))
            cursor = i + len(s)
    if inicios[0] is None:
        inicios[0] = 0.0
    # Si alguna frase no se encontró, estima entre la anterior y la siguiente.
    for i, valor in enumerate(inicios):
        if valor is None:
            siguiente = next((v for v in inicios[i + 1:] if v is not None), tiempos[-1])
            inicios[i] = round((inicios[i - 1] + siguiente) / 2, 3)
    return inicios


def aplicar_efecto(ruta):
    """Baja un poco el tono y agrega un eco suave, como de salón grande (la duración no cambia)."""
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
    p.add_argument("--discursos", action="store_true", help="genera solo los discursos completos")
    p.add_argument("--rehacer", action="store_true", help="regenera también los audios que ya existen")
    p.add_argument("--solo", nargs="+", metavar="CLAVE", help="genera solo estas claves (ej. prueba)")
    p.add_argument("--limite", type=int, metavar="N", help="genera como máximo N audios")
    p.add_argument("--simular", action="store_true", help="muestra qué se generaría, sin gastar créditos")
    p.add_argument("--efecto", action="store_true", help="voz más grave + eco (requiere ffmpeg)")
    p.add_argument("--solo-lista", action="store_true", help="solo actualiza GUION_GRABACION.md")
    args = p.parse_args()

    frases = cargar_frases(json.loads(FRASES.read_text(encoding="utf-8")))
    escribir_guion(frases)
    discursos = sum(f.es_discurso for f in frases)
    print(f"Guion actualizado: {GUION.relative_to(RAIZ)} ({discursos} discursos, {len(frases) - discursos} frases sueltas)")
    if args.solo_lista:
        return

    pendientes = [
        f for f in sorted(frases, key=prioridad)
        if (not args.solo or f.clave in args.solo)
        and (not args.discursos or f.es_discurso)
        and (args.rehacer or not (CARPETA_VOZ / f"{f.clave}.mp3").exists())
    ][: args.limite]
    caracteres = sum(len(f.texto) for f in pendientes)
    print(f"Por generar: {len(pendientes)} audios, {caracteres} caracteres")
    if args.simular:
        for f in pendientes:
            print(f"   {f.clave}: {f.texto}")
        return

    generar = generar_openai if args.proveedor == "openai" else generar_elevenlabs
    CARPETA_VOZ.mkdir(parents=True, exist_ok=True)
    for n, f in enumerate(pendientes, 1):
        print(f"🎙  [{n}/{len(pendientes)}] {f.clave}")
        audio, inicios = generar(f, args.voz)
        ruta = CARPETA_VOZ / f"{f.clave}.mp3"
        ruta.write_bytes(audio)
        tiempos = CARPETA_VOZ / f"{f.clave}.json"
        if inicios:
            tiempos.write_text(json.dumps({"inicios": inicios}) + "\n", encoding="utf-8")
        elif tiempos.exists():
            tiempos.unlink()  # tiempos viejos de otra grabación: la app estimará
        if args.efecto:
            aplicar_efecto(ruta)
    print(f"Listo: {len(pendientes)} audios nuevos en {CARPETA_VOZ.relative_to(RAIZ)}")


if __name__ == "__main__":
    main()
