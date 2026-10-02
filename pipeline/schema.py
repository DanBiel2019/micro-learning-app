"""Episode schema shared by the generator, the audio renderer and the Android app.

Field names are camelCase because the JSON is read directly by the app
(app/src/main/java/.../microlearning/model/Episode.kt mirrors these classes).
"""
from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel, ConfigDict


class _Strict(BaseModel):
    # additionalProperties: false on every object, required by structured outputs.
    model_config = ConfigDict(extra="forbid")


VisualKind = Literal["flow", "cycle", "compare", "stats", "bars", "venn", "ladder", "timeline", "quote"]


class VisualItem(_Strict):
    label: str
    detail: str
    emoji: str
    # Used by "bars" (bar length) and "stats" (the big number). null elsewhere.
    value: Optional[float]
    # Display text for the value, e.g. "10%" or "1.8M mi". null elsewhere.
    valueLabel: Optional[str]
    # Highlight this item (the punchline, the winner, the real cause).
    emphasis: bool


class Visual(_Strict):
    """An infographic the app draws natively. Keep labels short (<= 4 words)."""
    kind: VisualKind
    title: str
    items: list[VisualItem]
    caption: str


class Source(_Strict):
    title: str
    author: str
    year: Optional[int]
    url: Optional[str]
    format: Literal["book", "paper", "article", "podcast", "talk", "news", "case"]


class Line(_Strict):
    speaker: Literal["host", "cohost"]
    text: str


class Reading(_Strict):
    title: str
    url: Optional[str]
    note: str


class Segment(_Strict):
    id: str
    topic: str
    # True for ideas outside the listener's stated core topics.
    adjacent: bool
    style: Literal["story", "counterintuitive", "practical", "big_picture", "data"]
    kicker: str
    title: str
    summary: str
    keyPoints: list[str]
    takeaway: str
    challenge: str
    source: Source
    visual: Visual
    script: list[Line]
    deeperQuestions: list[str]
    furtherReading: list[Reading]


class Episode(_Strict):
    date: str
    title: str
    theme: str
    segments: list[Segment]


# ---- Fields added after generation (not part of the model's output schema) ----

class TimedLine(BaseModel):
    speaker: str
    text: str
    startMs: int


class PublishedSegment(Segment):
    model_config = ConfigDict(extra="allow")
    audioUrl: Optional[str] = None
    durationMs: Optional[int] = None
    lines: list[TimedLine] = []


class PublishedEpisode(BaseModel):
    schemaVersion: int = 2
    id: str
    date: str
    title: str
    theme: str
    hosts: dict[str, str]
    segments: list[PublishedSegment]
    totalDurationMs: int
    previous: list[dict] = []
