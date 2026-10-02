"""Writes one day's episode with a local open-weights model (no API key, no per-use cost).

Steps:
  1. pick today's ideas: 2 core + 2 adjacent from the curated bank in sources.json (or,
     once that runs dry, discovered from Wikipedia categories), + 1 fresh news item
  2. fetch each idea's source text (research.py)
  3. have the model write each segment from that text only (script, visual, takeaways)
  4. have it write the episode title/theme and the hand-offs that join the segments
"""
from __future__ import annotations

import json
import random
from datetime import date
from pathlib import Path
from typing import Literal, Optional

from pydantic import BaseModel, ConfigDict, Field

import research
from local_llm import LocalLLM
from schema import Episode, Line, Reading, Segment, Source, Visual

HERE = Path(__file__).parent
MIN_WORDS = 230


class _Strict(BaseModel):
    model_config = ConfigDict(extra="forbid")


# ---- What the model writes for one segment (we fill in ids, topic, sources ourselves) ----

class DraftItem(_Strict):
    label: str = Field(description="At most 4 words")
    detail: str = Field(description="At most 12 words")
    emoji: str
    value: Optional[float]
    valueLabel: Optional[str]
    emphasis: bool


class DraftVisual(_Strict):
    kind: Literal["flow", "cycle", "compare", "stats", "bars", "venn", "ladder", "timeline", "quote"]
    title: str
    items: list[DraftItem] = Field(min_length=2, max_length=6)
    caption: str


class DraftLine(_Strict):
    speaker: Literal["host", "cohost"]
    text: str


class DraftSegment(_Strict):
    kicker: str = Field(description="2-4 word hook")
    title: str
    style: Literal["story", "counterintuitive", "practical", "big_picture", "data"]
    summary: str = Field(description="2-3 sentences")
    keyPoints: list[str] = Field(min_length=3, max_length=3)
    takeaway: str
    challenge: str
    visual: DraftVisual
    script: list[DraftLine] = Field(min_length=12, max_length=24)
    deeperQuestions: list[str] = Field(min_length=3, max_length=3)


class Framing(_Strict):
    title: str = Field(description="Episode title, 2-6 words, intriguing")
    theme: str = Field(description="One line naming the thread that connects the segments")
    welcome: list[DraftLine] = Field(min_length=2, max_length=2)
    handoffs: list[str]
    signoff: list[DraftLine] = Field(min_length=2, max_length=3)


class Pick(_Strict):
    ranking: list[int] = Field(min_length=1, max_length=3)


SEGMENT_SYSTEM = """You write one segment of a personal two-host morning podcast.

Listener: {listener}

Hosts: {host} (speaker "host") tells the story. {cohost} (speaker "cohost") asks the questions a sharp engineer would ask, pushes back, and draws the link to the listener's world: networks, observability, incident response, automation, and leading engineers.

Accuracy rules, the most important part:
- Use ONLY facts stated in the SOURCE text. Do not add names, dates, numbers, quotes or events from memory.
- If the source says something is disputed, debated or possibly a myth, the hosts say so.
- If the source lacks a detail, leave it out rather than guess.

Shape:
- Open on the concrete story: who, when, what happened, what surprised people or went wrong.
- Then how people figured it out and what was learned.
- Then the transferable lesson, and a concrete bridge to the listener's work.
- Intermediate-to-advanced depth. No hype, no generic self-help.

Script: 14-20 lines, 280-340 spoken words in total. Natural conversation with genuine back-and-forth. Short spoken sentences, contractions. No greetings, no sign-off, no mention of "this episode" or other segments. No markdown, emoji, stage directions or sound effects. Spell out symbols and say numbers the way people say them.

Visual: one infographic that makes the core idea click. Pick the kind that fits:
flow (3-5 sequential steps), cycle (3-5 steps in a loop), compare (2-3 side-by-side options), stats (2-4 big numbers; put the number in valueLabel), bars (3-5 comparable quantities; set value and valueLabel), venn (2-3 overlapping ideas, the LAST item is the overlap), ladder (3-5 levels, weakest first), timeline (3-6 dated events; year in valueLabel), quote (one item: quote in label, speaker in detail).
Labels at most 4 words, details at most 12 words, one fitting emoji per item, emphasis=true on the single punchline item. value and valueLabel are null when unused. Only numbers that appear in the SOURCE.

Other fields: kicker is a 2-4 word hook; summary is 2-3 tight sentences; keyPoints are 3 short bullets; takeaway is one memorable sentence; challenge is one small action the listener can do today; deeperQuestions are 3 questions worth exploring further."""

SEGMENT_USER = """TOPIC: {topic}
ANGLE TO EXPLORE: {angle}
SOURCE: "{title}" ({publisher}{published})

SOURCE TEXT:
{text}

Write the segment as JSON."""

FRAMING_SYSTEM = """You produce the connective tissue for a two-host morning podcast. Hosts: {host} (speaker "host") and {cohost} (speaker "cohost"). Write naturally, briefly, in spoken English, with no markdown or emoji."""

FRAMING_USER = """Today's {n} segments, in order:
{listing}

Return:
- title: a short intriguing episode title (2-6 words)
- theme: one line naming the thread that genuinely connects the segments
- welcome: two lines; {host} says good morning and introduces herself, {cohost} introduces himself and states the theme
- handoffs: exactly {handoffs} one-sentence lines, one between each pair of consecutive segments, teasing the next segment without spoiling it
- signoff: two or three lines recapping the day's ideas in a few words each, then a warm goodbye"""

PICK_SYSTEM = "You are the producer of a morning podcast for a senior network engineer who owns observability and resilience. Choose material that teaches a transferable lesson."

PICK_FRESH_USER = """Recent articles:
{listing}

Rank the 3 best choices for a two-minute segment. Strongly prefer incident write-ups and postmortems, outages and their root causes, research findings, and hard-won lessons about AI, networking, reliability or engineering teams. Avoid product launches, funding news, opinion pieces and anything that needs heavy jargon. Return their numbers in order."""

PICK_DISCOVERY_USER = """Candidate Wikipedia articles from the category "{category}":
{listing}

Rank up to 3 that would make the most surprising, story-rich segment with a lesson about systems, measurement, decision making or leading teams. Return their numbers in order."""


def _remaining(sources: dict, covered: set[str]) -> list[dict]:
    return [s for s in sources["seeds"] if s["wiki"] not in covered]


def pick_ideas(sources: dict, covered: set[str], rng: random.Random, core: int, adjacent: int) -> list[dict]:
    pool = _remaining(sources, covered)
    core_pool = [s for s in pool if not s["adjacent"]]
    adj_pool = [s for s in pool if s["adjacent"]]
    rng.shuffle(core_pool)
    rng.shuffle(adj_pool)

    picks: list[dict] = []
    for s in core_pool:  # different core topics each day
        if len(picks) < core and all(p["topic"] != s["topic"] for p in picks):
            picks.append(s)
    adj = []
    for s in adj_pool:
        if len(adj) < adjacent and all(a["topic"] != s["topic"] for a in adj):
            adj.append(s)
    return picks + adj


def discover(llm: LocalLLM, sources: dict, covered: set[str], rng: random.Random) -> dict | None:
    """Finds a new idea from a Wikipedia category when the curated bank runs short."""
    categories = sources["discovery"]["categories"][:]
    rng.shuffle(categories)
    for category in categories[:3]:
        members = [m for m in research.category_members(category) if m.replace(" ", "_") not in covered]
        if not members:
            continue
        candidates = rng.sample(members, min(12, len(members)))
        listing = "\n".join(f"{i}. {t}" for i, t in enumerate(candidates))
        pick = llm.json(PICK_SYSTEM, PICK_DISCOVERY_USER.format(category=category, listing=listing),
                        Pick.model_json_schema(), max_tokens=60, temperature=0.2)
        for i in pick["ranking"]:
            if 0 <= i < len(candidates):
                title = candidates[i].replace(" ", "_")
                return {"wiki": title, "topic": category, "adjacent": True,
                        "angle": "What happened, what people learned, and the lesson for engineers"}
    return None


def pick_fresh(llm: LocalLLM, sources: dict, covered: set[str]) -> research.Document | None:
    items = [i for i in research.recent_items(sources["freshFeeds"]) if i.url not in covered]
    if not items:
        return None
    items = items[:45]
    listing = "\n".join(f"{n}. [{i.feed}] {i.title}: {i.summary[:140]}" for n, i in enumerate(items))
    pick = llm.json(PICK_SYSTEM, PICK_FRESH_USER.format(listing=listing), Pick.model_json_schema(),
                    max_tokens=60, temperature=0.2)
    for n in pick["ranking"]:
        if 0 <= n < len(items):
            try:
                doc = research.article(items[n])
            except Exception as e:
                print(f"  couldn't fetch {items[n].url}: {e}")
                continue
            if len(doc.text) > 1500:
                return doc
    return None


def write_segment(llm: LocalLLM, profile: dict, topic: str, angle: str, doc: research.Document) -> DraftSegment:
    v = profile["voices"]
    system = SEGMENT_SYSTEM.format(listener=profile["listener"], host=v["hostName"], cohost=v["cohostName"])
    user = SEGMENT_USER.format(topic=topic, angle=angle, title=doc.title, publisher=doc.publisher,
                               published=f", {doc.published}" if doc.published else "", text=doc.text)
    best = None
    for attempt in range(2):
        try:
            draft = DraftSegment.model_validate(llm.json(system, user, DraftSegment.model_json_schema(), max_tokens=3500))
        except Exception as e:
            print(f"    attempt {attempt + 1} failed: {e}")
            if attempt == 1 and best is None:
                raise
            continue
        words = sum(len(l.text.split()) for l in draft.script)
        print(f"    script: {len(draft.script)} lines, {words} words, visual={draft.visual.kind}")
        if best is None or words > best[0]:
            best = (words, draft)
        if words >= MIN_WORDS:
            break
        user += "\n\nThat script was too short. Write a fuller conversation of 280-340 words."
    return best[1]


def generate_episode(profile: dict, covered: list[str], today: date) -> tuple[Episode, list[str]]:
    """Returns the episode and, per segment, the reference the ledger records (wiki title or URL)."""
    sources = json.loads((HERE / "sources.json").read_text())
    done = set(covered) | set(sources.get("alreadyCovered", []))
    rng = random.Random(today.toordinal())
    mix = profile["mix"]
    v = profile["voices"]

    with LocalLLM() as llm:
        print(f"  model: {llm.model_name}")
        ideas = pick_ideas(sources, done, rng, mix["core"], mix["adjacent"])
        while sum(1 for i in ideas if i["adjacent"]) < mix["adjacent"]:
            found = discover(llm, sources, done | {i["wiki"] for i in ideas}, rng)
            if not found:
                break
            ideas.append(found)

        segments: list[Segment] = []
        refs: list[str] = []
        for idea in ideas:
            print(f"  researching {idea['wiki']}")
            try:
                doc = research.wikipedia(idea["wiki"])
            except Exception as e:
                print(f"  skipped {idea['wiki']}: {e}")
                continue
            try:
                draft = write_segment(llm, profile, idea["topic"], idea["angle"], doc)
            except Exception as e:  # one bad generation shouldn't sink the episode
                print(f"  couldn't write {idea['wiki']}: {e}")
                continue
            book = idea.get("book")
            source = (Source(title=book["title"], author=book["author"], year=book.get("year"), url=None, format="book")
                      if book else Source(title=doc.title, author="Wikipedia", year=None, url=doc.url, format="article"))
            reading = [Reading(title=f"{doc.title} (Wikipedia)", url=doc.url, note="The source this segment was written from")]
            if book:
                reading.append(Reading(title=f"{book['title']}, by {book['author']}", url=None, note="The classic book on this idea"))
            segments.append(_to_segment(draft, idea["topic"], idea["adjacent"], source, reading))
            refs.append(idea["wiki"])

        if mix.get("fresh"):
            print("  finding a fresh story")
            try:
                doc = pick_fresh(llm, sources, done)
            except Exception as e:
                print(f"  no fresh story today: {e}")
                doc = None
            draft = None
            if doc:
                print(f"  fresh: {doc.title} ({doc.url})")
                try:
                    draft = write_segment(llm, profile, "Fresh: Engineering & AI", "What happened, why it matters, and the lesson for engineers", doc)
                except Exception as e:
                    print(f"  couldn't write the fresh segment: {e}")
            if doc and draft:
                source = Source(title=doc.title, author=doc.publisher, year=int(doc.published[:4]) if doc.published else None,
                                url=doc.url, format="news")
                segments.append(_to_segment(draft, "Fresh: Engineering & AI", True, source,
                                            [Reading(title=doc.title, url=doc.url, note=f"Original article, {doc.publisher}")]))
                refs.append(doc.url)

        if len(segments) < 3:
            raise RuntimeError(f"Only {len(segments)} segments could be written")

        listing = "\n".join(f"{i + 1}. {s.title}: {s.takeaway}" for i, s in enumerate(segments))
        framing = Framing.model_validate(llm.json(
            FRAMING_SYSTEM.format(host=v["hostName"], cohost=v["cohostName"]),
            FRAMING_USER.format(n=len(segments), listing=listing, host=v["hostName"], cohost=v["cohostName"],
                                handoffs=len(segments) - 1),
            Framing.model_json_schema(), max_tokens=1200,
        ))

    _apply_framing(segments, framing)
    for i, seg in enumerate(segments, 1):
        seg.id = f"s{i}"
    return Episode(date=today.isoformat(), title=framing.title, theme=framing.theme, segments=segments), refs


def _to_segment(d: DraftSegment, topic: str, adjacent: bool, source: Source, reading: list[Reading]) -> Segment:
    return Segment(
        id="s0", topic=topic, adjacent=adjacent, style=d.style, kicker=d.kicker, title=d.title,
        summary=d.summary, keyPoints=d.keyPoints, takeaway=d.takeaway, challenge=d.challenge, source=source,
        visual=Visual.model_validate(d.visual.model_dump()),
        script=[Line(speaker=l.speaker, text=l.text) for l in d.script],
        deeperQuestions=d.deeperQuestions, furtherReading=reading,
    )


def _apply_framing(segments: list[Segment], f: Framing) -> None:
    segments[0].script[:0] = [Line(speaker=l.speaker, text=l.text) for l in f.welcome]
    for i, seg in enumerate(segments[:-1]):
        if i < len(f.handoffs):
            # Whoever didn't speak last delivers the hand-off, so it sounds like a conversation.
            last = seg.script[-1].speaker
            seg.script.append(Line(speaker="cohost" if last == "host" else "host", text=f.handoffs[i]))
    segments[-1].script.extend(Line(speaker=l.speaker, text=l.text) for l in f.signoff)

