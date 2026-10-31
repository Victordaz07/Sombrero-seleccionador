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
- Invitaciones del modo calle (frases cortas).

Uso (desde la carpeta del proyecto):

    # ElevenLabs (recomendado). Los discursos usan Eleven v3, que entiende las etiquetas
    # de emoción como [whispers], [chuckles] o [shouting].
    export ELEVENLABS_API_KEY="..."
    python3 herramientas/generar_voces.py --proveedor elevenlabs --voz <voice_id> --discursos

    # OpenAI (gpt-4o-mini-tts), permite indicar el "estilo" de actuación
    export OPENAI_API_KEY="sk-..."
    python3 herramientas/generar_voces.py --proveedor openai --voz onyx --discursos

    # Solo actualizar la lista de frases, sin generar audio
    python3 herramientas/generar_voces.py --solo-lista

Opciones útiles:
    --discursos      genera solo los discursos completos
    --simular        muestra qué se generaría y cuántos caracteres gasta, sin gastar nada
    --limite 4       genera como máximo 4 audios (empieza por un discurso de cada casa)
    --rehacer        vuelve a generar audios que ya existen (por ejemplo, si cambiaste una frase)
    --solo prueba    genera solo las claves indicadas (para probar voces antes de gastar)
    --efecto         usa ffmpeg para hacer la voz un poco más grave y con eco de salón

Ajustes de actuación de los discursos (ElevenLabs):
    --modelo eleven_v3     o eleven_v4
    --velocidad 1.15       de 0.7 (lento) a 1.2 (rápido)
    --estabilidad 0.0      0.0 = Creativo (más expresivo), 0.5 = Natural, 1.0 = Robusto
    --pausa-maxima 0.3     recorta los silencios a este máximo (en segundos) para más ritmo
    --acelerar 1.1         acelera el audio terminado sin cambiar el tono (1.1 = 10% más rápido)
"""

import argparse
import base64
import json
import os
import re
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
ETIQUETA = re.compile(r"\[[^\]]*\]\s*")


def sin_etiquetas(texto):
    """Quita las etiquetas de emoción ([whispers], [chuckles]...) que solo entiende ElevenLabs v3."""
    return ETIQUETA.sub("", texto).strip()

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
ESTILO_DISCURSO_EN = (
    "Speak in English. You are a centuries-old, wise and mischievous magical talking hat. "
    "Lively, theatrical and full of energy; build suspense and SHOUT the house name at the end "
    "with huge excitement, as if announcing it to a whole hall full of people."
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
    def en_ingles(self):
        return self.clave.endswith("_en")

    @property
    def es_anuncio(self):
        return self.clave.endswith("_anuncio")


def cargar_frases(datos):
    """Misma lógica de claves que Frases.kt. Si cambias una, cambia la otra."""
    frases = []
    # Español: gryffindor_discurso_1 ... / Inglés: gryffindor_discurso_1_en ...
    for campo, campo_anuncio, sufijo in [("discursos", "anuncio", ""), ("discursos_en", "anuncio_en", "_en")]:
        for casa, lista in datos.get(campo, {}).items():
            anuncio = datos["casas"][casa][campo_anuncio]
            for i, crudos in enumerate(lista):
                if crudos.count(ANUNCIO) != 1:
                    sys.exit(f"El discurso {i + 1}{sufijo} de {casa} debe tener {ANUNCIO} exactamente una vez")
                # El anuncio se grita: "[shouting] ¡GRÍFINDOR!"
                segmentos = [f"[shouting] {anuncio.upper()}" if s == ANUNCIO else s for s in crudos]
                clave = f"{casa}_discurso_{i + 1}{sufijo}"
                frases.append(Frase(clave, " ".join(segmentos), segmentos, crudos.index(ANUNCIO)))

    # Invitaciones del modo calle: invitacion_<tipo>_<n>_en (inglés) e invitacion_<tipo>_<n> (español).
    for codigo, sufijo in [("en", "_en"), ("es", "")]:
        for tipo, lista in datos.get("invitaciones", {}).get(codigo, {}).items():
            frases += [Frase(f"invitacion_{tipo}_{i + 1}{sufijo}", t) for i, t in enumerate(lista)]

    for clave in ("prueba", "prueba_en"):
        if clave in datos:
            frases.append(Frase(clave, datos[clave]))
    return frases


def prioridad(frase):
    """Primero un discurso de cada casa, luego el resto de discursos, luego las demás frases."""
    if frase.es_discurso:
        return (0, int(frase.clave.split("_discurso_")[1].split("_")[0]))
    if frase.clave.startswith("prueba"):
        return (2, 0)
    return (1, 0)


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
        "4 por casa en español y 4 en inglés (los que terminan en `_en`). Cada uno se graba **de corrido**, "
        "en un solo audio. La app los va rotando para que no se repitan.",
        "Haz una pausa de suspenso antes del nombre de la casa (en **negritas**) y grítalo con emoción.",
        "",
        "| Archivo | Discurso |",
        "|---|---|",
    ]
    for f in frases:
        if f.es_discurso:
            partes = [f"**{s}**" if i == f.indice_anuncio else s for i, s in enumerate(f.segmentos)]
            lineas.append(f"| `{f.clave}.mp3` | {' '.join(partes)} |")
    especiales = [f for f in frases if f.clave.startswith("invitacion_")]
    if especiales:
        lineas += [
            "",
            "## Invitaciones (modo calle)",
            "",
            "El sombrero llama a quien va pasando, según el tipo de persona.",
            "",
            "| Archivo | Frase |",
            "|---|---|",
        ]
        lineas += [f"| `{f.clave}.mp3` | {f.texto} |" for f in especiales]
    otras = [f for f in frases if not f.es_discurso and f not in especiales]
    if otras:
        lineas += ["", "## Otras", "", "| Archivo | Frase |", "|---|---|"]
        lineas += [f"| `{f.clave}.mp3` | {f.texto} |" for f in otras]
    GUION.write_text("\n".join(lineas) + "\n", encoding="utf-8")


class ErrorServicio(Exception):
    pass


def pedir(url, cuerpo, cabeceras, salir_si_falla=True):
    req = urllib.request.Request(url, data=json.dumps(cuerpo).encode("utf-8"), method="POST")
    for k, v in cabeceras.items():
        req.add_header(k, v)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            return r.read()
    except urllib.error.HTTPError as e:
        mensaje = f"Error {e.code} del servicio de voz: {e.read().decode('utf-8', 'replace')}"
        if salir_si_falla:
            sys.exit(mensaje)
        raise ErrorServicio(mensaje)


def generar_openai(frase, voz):
    """Devuelve (audio_mp3, inicios). OpenAI no da tiempos: la app los estima."""
    clave = os.environ.get("OPENAI_API_KEY") or sys.exit("Falta la variable OPENAI_API_KEY")
    if frase.en_ingles:
        estilo = ESTILO_DISCURSO_EN
    else:
        estilo = ESTILO_DISCURSO if frase.es_discurso else ESTILO_ANUNCIO if frase.es_anuncio else ESTILO
    audio = pedir(
        "https://api.openai.com/v1/audio/speech",
        {
            "model": "gpt-4o-mini-tts",
            "voice": voz or "onyx",
            "input": sin_etiquetas(frase.texto),
            "instructions": estilo,
            "response_format": "mp3",
        },
        {"Authorization": f"Bearer {clave}"},
    )
    return audio, None


# Actuación de los discursos; se pueden cambiar con --modelo, --velocidad y --estabilidad.
ACTUACION = {"modelo": "eleven_v3", "velocidad": 1.15, "estabilidad": 0.0}


def generar_elevenlabs(frase, voz):
    """Devuelve (audio_mp3, inicios). En discursos pide los tiempos de cada letra."""
    clave = os.environ.get("ELEVENLABS_API_KEY") or sys.exit("Falta la variable ELEVENLABS_API_KEY")
    if not voz:
        sys.exit("Con ElevenLabs indica el id de la voz: --voz <voice_id>")

    base = f"https://api.elevenlabs.io/v1/text-to-speech/{voz}"
    formato = "?output_format=mp3_44100_128"

    if frase.es_discurso:
        # Eleven v3/v4 actúan las etiquetas de emoción ([excited], [laughs], [shouting]...).
        cuerpo = {
            "text": frase.texto,
            "model_id": ACTUACION["modelo"],
            "voice_settings": {"stability": ACTUACION["estabilidad"], "speed": ACTUACION["velocidad"]},
        }
        try:
            datos = json.loads(pedir(f"{base}/with-timestamps{formato}", cuerpo, {"xi-api-key": clave}, False))
        except ErrorServicio as e:
            # Si no da tiempos, igual generamos el audio: la app los estima.
            print(f"   (sin tiempos exactos: {e})")
            return pedir(f"{base}{formato}", cuerpo, {"xi-api-key": clave}), None
        audio = base64.b64decode(datos["audio_base64"])
        return audio, inicios_de_segmentos(frase.segmentos, datos.get("alignment"))

    # Frases sueltas: Multilingual v2. Menos estabilidad = más expresivo; en el anuncio, más todavía.
    ajustes = {
        "stability": 0.3 if frase.es_anuncio else 0.45,
        "similarity_boost": 0.8,
        "style": 0.6 if frase.es_anuncio else 0.35,
    }
    audio = pedir(
        f"{base}{formato}",
        {"text": sin_etiquetas(frase.texto), "model_id": "eleven_multilingual_v2", "voice_settings": ajustes},
        {"xi-api-key": clave},
    )
    return audio, None


def inicios_de_segmentos(segmentos, alineacion):
    """Busca cada frase dentro del texto alineado y toma el segundo en que empieza."""
    if not alineacion:
        return None
    letras = "".join(alineacion["characters"])
    tiempos = alineacion["character_start_times_seconds"]
    inicios, cursor = [], 0
    for s in segmentos:
        # La alineación puede traer las etiquetas de emoción o no: probamos de las dos formas.
        i = letras.find(s, cursor)
        if i < 0:
            s = sin_etiquetas(s)
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


def acortar_pausas(ruta, inicios, maximo):
    """Recorta los silencios más largos que `maximo` segundos y corrige los tiempos de cada frase."""
    if not shutil.which("ffmpeg"):
        sys.exit("--pausa-maxima necesita ffmpeg instalado")
    analisis = subprocess.run(
        ["ffmpeg", "-i", str(ruta), "-af", f"silencedetect=n=-40dB:d={maximo}", "-f", "null", "-"],
        capture_output=True, text=True,
    ).stderr
    comienzos = [float(x) for x in re.findall(r"silence_start: ([0-9.]+)", analisis)]
    finales = [float(x) for x in re.findall(r"silence_end: ([0-9.]+)", analisis)]
    # De cada silencio largo se quita el centro y se deja `maximo` (mitad a cada lado).
    cortes = [(a + maximo / 2, b - maximo / 2) for a, b in zip(comienzos, finales) if b - a > maximo]
    if not cortes:
        return inicios

    with tempfile.NamedTemporaryFile(suffix=".mp3", delete=False) as tmp:
        salida = Path(tmp.name)
    quitar = "+".join(f"between(t,{a:.3f},{b:.3f})" for a, b in cortes)
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", str(ruta),
         "-af", f"aselect='not({quitar})',asetpts=N/SR/TB", "-b:a", "128k", str(salida)],
        check=True,
    )
    shutil.move(salida, ruta)

    def nuevo_tiempo(t):
        return t - sum(min(b, max(a, t)) - a for a, b in cortes)

    return [round(nuevo_tiempo(t), 3) for t in inicios] if inicios else inicios


def acelerar(ruta, inicios, factor):
    """Acelera el audio sin cambiar el tono de la voz y ajusta los tiempos de cada frase."""
    if not shutil.which("ffmpeg"):
        sys.exit("--acelerar necesita ffmpeg instalado")
    with tempfile.NamedTemporaryFile(suffix=".mp3", delete=False) as tmp:
        salida = Path(tmp.name)
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", str(ruta), "-af", f"atempo={factor}", "-b:a", "128k", str(salida)],
        check=True,
    )
    shutil.move(salida, ruta)
    return [round(t / factor, 3) for t in inicios] if inicios else inicios


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
    p.add_argument("--modelo", default=ACTUACION["modelo"], help="modelo de ElevenLabs para discursos")
    p.add_argument("--velocidad", type=float, default=ACTUACION["velocidad"], help="0.7 a 1.2")
    p.add_argument("--estabilidad", type=float, default=ACTUACION["estabilidad"], help="0.0, 0.5 o 1.0")
    p.add_argument("--salida", type=Path, help="carpeta donde guardar (por defecto, la de la app)")
    p.add_argument("--pausa-maxima", type=float, metavar="SEG", help="recorta silencios a este máximo (ej. 0.3)")
    p.add_argument("--acelerar", type=float, metavar="X", help="acelera sin cambiar el tono (ej. 1.1)")
    args = p.parse_args()
    ACTUACION.update(modelo=args.modelo, velocidad=args.velocidad, estabilidad=args.estabilidad)
    carpeta = args.salida or CARPETA_VOZ

    frases = cargar_frases(json.loads(FRASES.read_text(encoding="utf-8")))
    escribir_guion(frases)
    discursos = sum(f.es_discurso for f in frases)
    print(f"Guion actualizado: {GUION.relative_to(RAIZ)} ({discursos} discursos, {len(frases) - discursos} frases cortas)")
    if args.solo_lista:
        return

    pendientes = [
        f for f in sorted(frases, key=prioridad)
        if (not args.solo or f.clave in args.solo)
        and (not args.discursos or f.es_discurso)
        and (args.rehacer or not (carpeta / f"{f.clave}.mp3").exists())
    ][: args.limite]
    caracteres = sum(len(f.texto) for f in pendientes)
    print(f"Por generar: {len(pendientes)} audios, {caracteres} caracteres")
    if args.simular:
        for f in pendientes:
            print(f"   {f.clave}: {f.texto}")
        return

    generar = generar_openai if args.proveedor == "openai" else generar_elevenlabs
    carpeta.mkdir(parents=True, exist_ok=True)
    for n, f in enumerate(pendientes, 1):
        print(f"🎙  [{n}/{len(pendientes)}] {f.clave}")
        audio, inicios = generar(f, args.voz)
        ruta = carpeta / f"{f.clave}.mp3"
        ruta.write_bytes(audio)
        if args.pausa_maxima:
            inicios = acortar_pausas(ruta, inicios, args.pausa_maxima)
        if args.acelerar:
            inicios = acelerar(ruta, inicios, args.acelerar)
        tiempos = carpeta / f"{f.clave}.json"
        if inicios:
            tiempos.write_text(json.dumps({"inicios": inicios}) + "\n", encoding="utf-8")
        elif tiempos.exists():
            tiempos.unlink()  # tiempos viejos de otra grabación: la app estimará
        if args.efecto:
            aplicar_efecto(ruta)
    print(f"Listo: {len(pendientes)} audios nuevos en {carpeta}")


if __name__ == "__main__":
    main()
