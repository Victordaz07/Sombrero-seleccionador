#!/usr/bin/env python3
"""
Compone el efecto que suena cuando el sombrero revela la casa: una fanfarria de trompetas
original (libre de derechos) con timbal, seguida de aplausos de público.

Uso:  python3 herramientas/componer_efectos.py
Crea: app/src/main/assets/efectos/fanfarria.ogg   (necesita numpy y ffmpeg)
"""

import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np

RAIZ = Path(__file__).resolve().parent.parent
SALIDA = RAIZ / "app/src/main/assets/efectos/fanfarria.ogg"
SR = 44100
DURACION = 6.0
rng = np.random.default_rng(11)
total = np.zeros((int(DURACION * SR), 2))


def hz(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def poner(senal, inicio, pan=0.0, vol=1.0):
    i = max(0, int(inicio * SR))
    fin = min(i + len(senal), len(total))
    s = senal[: fin - i] * vol
    total[i:fin, 0] += s * np.sqrt((1 - pan) / 2)
    total[i:fin, 1] += s * np.sqrt((1 + pan) / 2)


def trompeta(nota, dur):
    """Metal sintetizado: armónicos que se abren en el ataque, con vibrato en las notas largas."""
    t = np.arange(int((dur + 0.25) * SR)) / SR
    f = hz(nota) * (1 + 0.006 * np.sin(2 * np.pi * 5.5 * t) * np.clip((t - 0.25) / 0.3, 0, 1))
    fase = 2 * np.pi * np.cumsum(f) / SR
    brillo = np.clip(t / 0.06, 0.25, 1)
    s = sum((brillo ** (n / 3)) * np.sin(n * fase) / n for n in range(1, 12))
    envolvente = np.minimum(t / 0.025, 1) * np.clip((dur + 0.25 - t) / 0.25, 0, 1)
    return s * envolvente * (0.9 + 0.1 * np.exp(-t / 0.1))


def timbal(nota, dur=1.6):
    t = np.arange(int(dur * SR)) / SR
    f = hz(nota) * (1 + 0.15 * np.exp(-t / 0.03))
    fase = 2 * np.pi * np.cumsum(f) / SR
    golpe = rng.normal(0, 1, len(t)) * np.exp(-t / 0.015) * 0.4
    return (np.sin(fase) + 0.4 * np.sin(1.5 * fase)) * np.exp(-t / 0.45) + golpe


def aplauso_individual():
    """Una palmada: ruido corto con un poco de cuerpo."""
    n = int(0.06 * SR)
    t = np.arange(n) / SR
    ruido = rng.normal(0, 1, n)
    ruido = np.diff(ruido, prepend=0)  # quita graves: suena más a palma
    return ruido * np.exp(-t / rng.uniform(0.006, 0.014))


# Fanfarria en re mayor: tresillo de repique, salto y nota larga con acorde.
TRESILLO = 0.13
melodia = [(74, 0.0, TRESILLO), (74, TRESILLO, TRESILLO), (74, 2 * TRESILLO, TRESILLO),
           (78, 3 * TRESILLO, 0.32), (76, 3 * TRESILLO + 0.34, 0.16), (78, 3 * TRESILLO + 0.52, 0.16),
           (81, 3 * TRESILLO + 0.70, 1.15)]
for nota, inicio, dur in melodia:
    poner(trompeta(nota, dur), inicio, pan=-0.2, vol=0.30)
    poner(trompeta(nota - 5 if nota != 74 else nota - 3, dur), inicio + 0.005, pan=0.25, vol=0.22)
final = 3 * TRESILLO + 0.70
for nota in (62, 66, 69):  # acorde de re mayor debajo de la nota larga
    poner(trompeta(nota, 1.15), final, pan=rng.uniform(-0.5, 0.5), vol=0.16)
poner(timbal(38), final, vol=0.55)
poner(timbal(45, 1.0), 3 * TRESILLO, vol=0.35)

# Aplausos: empiezan con la nota larga, crecen rápido y se van apagando.
inicio_aplauso = final + 0.15
t_aplauso = 4.2
for k in range(2600):
    momento = inicio_aplauso + t_aplauso * rng.uniform(0, 1)
    # Sube en 0.3 s, se sostiene y se apaga en el último segundo y medio.
    intensidad = np.clip((momento - inicio_aplauso) / 0.3, 0, 1) * np.clip((inicio_aplauso + t_aplauso - momento) / 1.5, 0, 1)
    poner(aplauso_individual(), momento, pan=rng.uniform(-0.9, 0.9), vol=0.30 * intensidad)
# Murmullo del público debajo de los aplausos.
n_murmullo = int(t_aplauso * SR)
tm = np.arange(n_murmullo) / SR
murmullo = np.convolve(rng.normal(0, 1, n_murmullo), np.ones(40) / 40, mode="same")
murmullo *= np.clip(tm / 0.4, 0, 1) * np.clip((t_aplauso - tm) / 1.5, 0, 1)
poner(murmullo, inicio_aplauso, vol=0.6)


def reverberacion(x, segundos=2.0, mezcla=0.28, semilla=0):
    r = np.random.default_rng(semilla)
    n = int(segundos * SR)
    t = np.arange(n) / SR
    ir = r.normal(0, 1, n) * np.exp(-t / (segundos / 5))
    ir /= np.sqrt(np.sum(ir ** 2))
    tam = 1 << (len(x) + n - 1).bit_length()
    humedo = np.fft.irfft(np.fft.rfft(x, tam) * np.fft.rfft(ir, tam), tam)[: len(x)]
    return x * (1 - mezcla) + humedo * mezcla * 2.0


for canal in (0, 1):
    total[:, canal] = reverberacion(total[:, canal], semilla=canal + 3)
total[-int(0.3 * SR):] *= np.linspace(1, 0, int(0.3 * SR))[:, None]
# Volumen: la parte fuerte (fanfarria) a unos -14 dB de promedio; un limitador suave (tanh)
# doma los picos de las palmadas para que no obliguen a bajar todo.
fuerte = total[: int(2.5 * SR)]
total *= 0.2 / np.sqrt(np.mean(fuerte ** 2))
total = np.tanh(total)
total *= 0.89 / np.max(np.abs(total))  # pico en -1 dB

SALIDA.parent.mkdir(parents=True, exist_ok=True)
with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
    ruta_wav = tmp.name
with wave.open(ruta_wav, "wb") as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes((total * 32767).astype("<i2").tobytes())
subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", ruta_wav, "-c:a", "libvorbis", "-q:a", "5", str(SALIDA)],
               check=True)
Path(ruta_wav).unlink()
print(f"Efecto listo: {SALIDA.relative_to(RAIZ)} ({DURACION:.1f} s, {SALIDA.stat().st_size // 1024} KB)")
