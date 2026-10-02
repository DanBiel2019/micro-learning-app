"""Turns an Episode's two-host scripts into one MP3 per segment using Kokoro TTS.

Kokoro (https://github.com/thewh1teagle/kokoro-onnx) is an open-weights neural TTS model
that runs on CPU, so this works on a laptop or a GitHub Actions runner with no API cost.
Each line's start time is recorded so the app can highlight the transcript while playing.
"""
from __future__ import annotations

import os
import re
import subprocess
import tempfile
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import soundfile as sf

from schema import Episode, Segment, TimedLine

SAMPLE_RATE = 24_000
GAP_SAME_SPEAKER_S = 0.28
GAP_TURN_S = 0.42
LEAD_IN_S = 0.35
TAIL_S = 0.9


@dataclass
class Voices:
    host: str = "af_heart"
    cohost: str = "am_michael"
    speed: float = 1.0


@dataclass
class RenderedSegment:
    path: Path
    duration_ms: int
    lines: list[TimedLine]


def load_kokoro():
    from kokoro_onnx import Kokoro

    model_dir = Path(os.environ.get("KOKORO_DIR", Path.home() / "tools" / "kokoro"))
    return Kokoro(str(model_dir / "kokoro-v1.0.onnx"), str(model_dir / "voices-v1.0.bin"))


def speakable(text: str) -> str:
    """Light cleanup so the phonemizer doesn't read symbols literally."""
    text = text.replace("&", " and ").replace("%", " percent").replace("~", "about ")
    text = text.replace("—", ", ").replace("–", ", ").replace("…", "...")
    text = re.sub(r"[\[\]{}<>*_#|]", "", text)
    return re.sub(r"\s+", " ", text).strip()


def chime(sr: int = SAMPLE_RATE) -> np.ndarray:
    """A soft two-note sting that opens the episode, synthesized so there's no asset licensing."""
    def note(freq: float, dur: float) -> np.ndarray:
        t = np.linspace(0, dur, int(sr * dur), endpoint=False)
        env = np.exp(-t * 5.0) * np.minimum(1.0, t * 80)
        tone = np.sin(2 * np.pi * freq * t) + 0.35 * np.sin(2 * np.pi * freq * 2 * t)
        return (tone * env * 0.18).astype(np.float32)

    a, b = note(659.25, 0.55), note(987.77, 0.9)  # E5 then B5
    out = np.zeros(int(sr * 1.25), dtype=np.float32)
    out[: len(a)] += a
    out[int(sr * 0.22): int(sr * 0.22) + len(b)] += b
    return out


def render_segment(kokoro, seg: Segment, voices: Voices, out_path: Path, with_chime: bool) -> RenderedSegment:
    chunks: list[np.ndarray] = []
    lines: list[TimedLine] = []
    cursor = 0

    def add(samples: np.ndarray):
        nonlocal cursor
        chunks.append(samples.astype(np.float32))
        cursor += len(samples)

    if with_chime:
        add(chime())
    add(np.zeros(int(SAMPLE_RATE * LEAD_IN_S), dtype=np.float32))

    previous = None
    for line in seg.script:
        if previous is not None:
            gap = GAP_SAME_SPEAKER_S if previous == line.speaker else GAP_TURN_S
            add(np.zeros(int(SAMPLE_RATE * gap), dtype=np.float32))
        voice = voices.host if line.speaker == "host" else voices.cohost
        lines.append(TimedLine(speaker=line.speaker, text=line.text, startMs=cursor * 1000 // SAMPLE_RATE))
        samples, sr = kokoro.create(speakable(line.text), voice=voice, speed=voices.speed, lang="en-us")
        assert sr == SAMPLE_RATE, sr
        add(samples)
        previous = line.speaker

    add(np.zeros(int(SAMPLE_RATE * TAIL_S), dtype=np.float32))
    audio = np.concatenate(chunks)

    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "seg.wav"
        sf.write(wav, audio, SAMPLE_RATE)
        # Loudness-normalise to podcast level (-16 LUFS) and encode mono MP3.
        subprocess.run(
            ["ffmpeg", "-loglevel", "error", "-y", "-i", str(wav),
             "-af", "loudnorm=I=-16:TP=-1.5:LRA=11", "-ar", "44100", "-ac", "1",
             "-codec:a", "libmp3lame", "-b:a", "64k", str(out_path)],
            check=True,
        )

    return RenderedSegment(out_path, len(audio) * 1000 // SAMPLE_RATE, lines)


def render_episode(episode: Episode, out_dir: Path, voices: Voices) -> list[RenderedSegment]:
    out_dir.mkdir(parents=True, exist_ok=True)
    kokoro = load_kokoro()
    rendered = []
    for i, seg in enumerate(episode.segments, start=1):
        path = out_dir / f"seg-{i}.mp3"
        print(f"  voicing segment {i}/{len(episode.segments)}: {seg.title}", flush=True)
        rendered.append(render_segment(kokoro, seg, voices, path, with_chime=(i == 1)))
    return rendered
