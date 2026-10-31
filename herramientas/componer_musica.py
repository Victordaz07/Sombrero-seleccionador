#!/usr/bin/env python3
"""
Compone la música de fondo del sombrero: un ambiente mágico original (libre de derechos),
en re menor a 72 BPM, que se repite en bucle sin cortes.

Capas: colchón de acordes, bajo, caja musical en arpegios y destellos de campanitas,
todo con reverberación de salón grande.

Uso:  python3 herramientas/componer_musica.py
Crea: app/src/main/assets/musica/fondo.ogg   (necesita numpy y ffmpeg)
"""

import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np

RAIZ = Path(__file__).resolve().parent.parent
SALIDA = RAIZ / "app/src/main/assets/musica/fondo.ogg"

SR = 44100
BPM = 72
PULSO = 60 / BPM
COMPAS = 4 * PULSO
ACORDES = {
    "Dm": [50, 53, 57], "Bb": [46, 50, 53], "Gm": [43, 46, 50], "A": [45, 49, 52],
    "F": [41, 45, 48], "C": [48, 52, 55],
}
PROGRESION = ["Dm", "Bb", "Gm", "A", "Dm", "F", "C", "A", "Dm", "Bb", "Gm", "A", "Bb", "C", "A", "A"]
ESCALA_DESTELLOS = [74, 76, 77, 79, 81, 82, 85, 86, 89, 93]  # re menor armónica, octava alta
COLA = 4.0  # segundos de reverberación que se envuelven al inicio para que el bucle no se corte

rng = np.random.default_rng(31)
DURACION = len(PROGRESION) * COMPAS
total = np.zeros((int((DURACION + COLA) * SR), 2))


def hz(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def poner(senal, inicio, pan=0.0, vol=1.0):
    """Suma una señal mono al estéreo en el segundo `inicio`, con paneo (-1 izq, 1 der)."""
    i = max(0, int(inicio * SR))
    fin = min(i + len(senal), len(total))
    s = senal[: fin - i] * vol
    total[i:fin, 0] += s * np.sqrt((1 - pan) / 2)
    total[i:fin, 1] += s * np.sqrt((1 + pan) / 2)


def colchon(notas, dur):
    t = np.arange(int((dur + 1.5) * SR)) / SR
    s = np.zeros_like(t)
    for n in notas + [notas[0] + 12, notas[2] + 12]:
        for desafino in (-0.004, 0.004):
            f = hz(n) * (1 + desafino)
            s += np.sin(2 * np.pi * f * t) + 0.25 * np.sin(4 * np.pi * f * t) + 0.08 * np.sin(6 * np.pi * f * t)
    entrada = np.minimum(t / 1.2, 1)
    salida = np.clip((dur + 1.5 - t) / 1.5, 0, 1)
    respiro = 0.85 + 0.15 * np.sin(2 * np.pi * t / dur)
    return s * entrada * salida * respiro / 14


def bajo(nota, dur):
    t = np.arange(int(dur * SR)) / SR
    f = hz(nota - 12)
    s = np.sin(2 * np.pi * f * t) + 0.3 * np.sin(4 * np.pi * f * t)
    return s * np.exp(-t / 2.2) * np.minimum(t / 0.03, 1) / 2.5


def caja_musical(nota, dur=2.0):
    t = np.arange(int(dur * SR)) / SR
    f = hz(nota)
    s = (np.sin(2 * np.pi * f * t) * np.exp(-t / 0.9)
         + 0.35 * np.sin(2 * np.pi * f * 3.01 * t) * np.exp(-t / 0.25)
         + 0.12 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t / 0.08))
    return s * np.minimum(t / 0.004, 1)


def campanita(nota, dur=3.0):
    t = np.arange(int(dur * SR)) / SR
    f = hz(nota)
    s = sum(a * np.sin(2 * np.pi * f * r * t) * np.exp(-t / d)
            for a, r, d in [(1, 1, 1.4), (0.5, 2.76, 0.5), (0.25, 5.4, 0.2), (0.12, 8.93, 0.1)])
    return s * np.minimum(t / 0.002, 1)


PATRON = [0, 1, 2, 3, 4, 3, 2, 1]  # sube y baja por las notas del acorde
for c, nombre in enumerate(PROGRESION):
    notas = ACORDES[nombre]
    inicio = c * COMPAS
    poner(colchon(notas, COMPAS), inicio, vol=0.9)
    poner(bajo(notas[0], COMPAS + 1), inicio, vol=0.9)
    # Arpegio de caja musical en corcheas, dos octavas arriba.
    arpegio = [n + 24 for n in notas] + [notas[0] + 36, notas[1] + 36]
    for k, idx in enumerate(PATRON):
        # En los compases pares, el arpegio entra a media frase para dar aire.
        if c % 4 == 3 and k >= 6:
            continue
        humano = rng.normal(0, 0.008)
        fuerza = 0.55 + 0.25 * (k == 0) + rng.uniform(-0.05, 0.05)
        poner(caja_musical(arpegio[idx]), inicio + k * PULSO / 2 + humano,
              pan=np.sin(k) * 0.4, vol=0.22 * fuerza)
    # Destellos de vez en cuando.
    if c % 2 == 1:
        for _ in range(rng.integers(1, 3)):
            poner(campanita(int(rng.choice(ESCALA_DESTELLOS))), inicio + rng.uniform(0.5, COMPAS - 0.5),
                  pan=rng.uniform(-0.8, 0.8), vol=0.07)


def reverberacion(x, segundos=2.8, mezcla=0.38, semilla=0):
    """Reverb de salón: convolución con ruido que decae (FFT con numpy)."""
    r = np.random.default_rng(semilla)
    n = int(segundos * SR)
    t = np.arange(n) / SR
    ir = r.normal(0, 1, n) * np.exp(-t / (segundos / 5))
    ir[: int(0.02 * SR)] *= np.linspace(0, 1, int(0.02 * SR))
    ir /= np.sqrt(np.sum(ir ** 2))
    largo = len(x) + n
    tam = 1 << (largo - 1).bit_length()
    humedo = np.fft.irfft(np.fft.rfft(x, tam) * np.fft.rfft(ir, tam), tam)[: len(x)]
    return x * (1 - mezcla) + humedo * mezcla * 2.2


for canal in (0, 1):
    total[:, canal] = reverberacion(total[:, canal], semilla=canal)

# Bucle sin costuras: la cola del final se suma al principio.
n_bucle = int(DURACION * SR)
bucle = total[:n_bucle].copy()
cola = total[n_bucle:]
bucle[: len(cola)] += cola
bucle *= 0.5 / np.max(np.abs(bucle))  # pico en -6 dB: la música va debajo de la voz

SALIDA.parent.mkdir(parents=True, exist_ok=True)
with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
    ruta_wav = tmp.name
with wave.open(ruta_wav, "wb") as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes((bucle * 32767).astype("<i2").tobytes())
subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", ruta_wav, "-c:a", "libvorbis", "-q:a", "4", str(SALIDA)], check=True)
Path(ruta_wav).unlink()
print(f"Música lista: {SALIDA.relative_to(RAIZ)} ({DURACION:.1f} s, {SALIDA.stat().st_size // 1024} KB)")
