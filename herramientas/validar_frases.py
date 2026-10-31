#!/usr/bin/env python3
"""
Revisa app/src/main/assets/frases.json antes de compilar.

- Error (sale con código 1): el JSON no se puede leer, o un discurso no tiene {ANUNCIO}
  exactamente una vez, o falta el nombre de una casa para el anuncio.
- Aviso (no detiene nada): frases sin audio grabado, y audios que ya no corresponden a ninguna frase.

Uso:  python3 herramientas/validar_frases.py
GitHub Actions lo corre solo en cada cambio, antes de compilar el APK.
"""

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generar_voces import ANUNCIO, CARPETA_VOZ, FRASES, cargar_frases  # noqa: E402

CASAS = ["gryffindor", "hufflepuff", "ravenclaw", "slytherin"]
errores, avisos = [], []

try:
    datos = json.loads(FRASES.read_text(encoding="utf-8"))
except json.JSONDecodeError as e:
    sys.exit(f"❌ frases.json no se puede leer (línea {e.lineno}, columna {e.colno}): {e.msg}")

for campo, campo_anuncio in [("discursos", "anuncio"), ("discursos_en", "anuncio_en")]:
    for casa in CASAS:
        lista = datos.get(campo, {}).get(casa, [])
        if not lista:
            avisos.append(f"{campo}.{casa}: no tiene discursos")
        if not datos.get("casas", {}).get(casa, {}).get(campo_anuncio):
            errores.append(f"casas.{casa}.{campo_anuncio}: falta el nombre que grita el sombrero")
        for i, segmentos in enumerate(lista, 1):
            if not isinstance(segmentos, list):
                errores.append(f"{campo}.{casa}[{i}]: debe ser una lista de frases")
            elif segmentos.count(ANUNCIO) != 1:
                errores.append(f"{campo}.{casa}[{i}]: debe tener {ANUNCIO} exactamente una vez")

if not errores:
    frases = cargar_frases(datos)
    claves = {f.clave for f in frases}
    sin_audio = [f.clave for f in frases if not (CARPETA_VOZ / f"{f.clave}.mp3").exists() and not f.clave.startswith("prueba")]
    sobrantes = sorted(p.stem for p in CARPETA_VOZ.glob("*.mp3") if p.stem not in claves)
    if sin_audio:
        avisos.append(f"{len(sin_audio)} frases sin audio (usarán la voz del teléfono): " + ", ".join(sin_audio))
    if sobrantes:
        avisos.append(f"{len(sobrantes)} audios que ya no se usan: " + ", ".join(sobrantes))

for a in avisos:
    print(f"⚠️  {a}")
for e in errores:
    print(f"❌ {e}")
if errores:
    sys.exit(1)
print("✅ frases.json está bien")
