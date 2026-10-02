"""Writes one day's episode with Claude: picks ideas, researches a fresh one, scripts the dialogue.

Requires ANTHROPIC_API_KEY (or another credential the anthropic SDK can resolve).
"""
from __future__ import annotations

import json
from datetime import date

import anthropic

from schema import Episode

MODEL = "claude-opus-5-5"
MAX_CONTINUATIONS = 6

SYSTEM = """You are the producer and writer of a personal daily micro-learning podcast for one listener.

Listener profile:
{profile}

Each episode has exactly {count} segments:
- {core} from the listener's core topics: {core_topics}
- {adjacent} from adjacent fields that broaden their thinking (pick fresh ones; vary across days): {adjacent_fields}
- {fresh} "fresh" segment about something that happened or was published in the last 60 days, relevant to AI, networking, observability, reliability engineering or how technical teams work. Use web search to find it and cite the real URL.

Order the segments so they flow, and give the episode a theme that genuinely connects them.

Quality bar:
- Every claim must be accurate and attributable to a real, verifiable source (book, paper, documented incident, reputable article). If a famous story is partly apocryphal, say so in the script. Never invent quotes, numbers, or sources. Prefer round, well-documented figures.
- Lead with a concrete story or case: who, when, what went wrong or surprised people, what they learned, how they found out. Then the transferable lesson. Then a bridge to the listener's world (networks, observability, incident response, automation, leading engineers).
- Intermediate-to-advanced depth. No filler, no hype, no generic self-help.

Script format (field `script`): a natural conversation between two hosts, {host_name} (speaker "host") and {cohost_name} (speaker "cohost"). Around 280-330 spoken words per segment (about two minutes). Short spoken sentences, contractions, genuine back-and-forth (questions, pushback, "wait, really?"), no stage directions, no sound effects, no markdown, no emoji, spell out symbols. Numbers as you'd say them. Segment 1 opens with a one-line welcome and the theme; the last segment ends with a short sign-off. Other segments end with a natural hand-off.

Visual (field `visual`): one infographic that makes the core idea click at a glance. Choose the kind that fits:
flow (3-5 sequential steps), cycle (3-5 steps in a loop), compare (2-3 side-by-side options), stats (2-4 big numbers), bars (3-6 comparable quantities; set value), venn (2-3 overlapping ideas; last item is the overlap), ladder (3-5 levels, weakest first), timeline (3-6 dated events; put the year in valueLabel), quote (one item, the quote in label, the speaker in detail).
Labels at most 4 words, details at most 12 words, one relevant emoji per item, mark the punchline item with emphasis=true. value/valueLabel are null when not used.

Other fields: kicker is a 2-4 word hook ("The wrong number"); summary is 2-3 tight sentences; keyPoints are 3 short bullets; takeaway is one memorable sentence; challenge is one small action for today; deeperQuestions are 3 questions worth exploring further; furtherReading is 1-3 real resources with URLs when you have them. Set adjacent=true for adjacent and fresh segments. Segment ids are "s1".."s{count}".
"""

USER = """Today is {today}. Produce today's episode.

Do not repeat any of these previously covered ideas or sources:
{covered}
"""


def generate_episode(profile: dict, covered: list[str], today: date) -> Episode:
    mix = profile["mix"]
    system = SYSTEM.format(
        profile=json.dumps({k: profile[k] for k in ("listener", "lovedBooks", "styles", "format")}, indent=2),
        count=sum(mix.values()),
        core=mix["core"],
        adjacent=mix["adjacent"],
        fresh=mix["fresh"],
        core_topics=", ".join(profile["coreTopics"]),
        adjacent_fields=", ".join(profile["adjacentFields"]),
        host_name=profile["voices"]["hostName"],
        cohost_name=profile["voices"]["cohostName"],
    )
    user = USER.format(today=today.isoformat(), covered="\n".join(f"- {c}" for c in covered[-200:]) or "- (nothing yet)")

    client = anthropic.Anthropic()
    messages: list = [{"role": "user", "content": user}]

    for _ in range(MAX_CONTINUATIONS):
        with client.beta.messages.stream(
            model=MODEL,
            max_tokens=64000,
            system=system,
            messages=messages,
            tools=[{"type": "web_search_20260209", "name": "web_search", "max_uses": 6}],
            output_config={"effort": "high"},
            output_format=Episode,
            betas=["server-side-fallback-2026-07-01"],
            fallbacks="default",
        ) as stream:
            message = stream.get_final_message()

        if message.stop_reason == "pause_turn":
            # Server-side web search hit its iteration limit; resend so it resumes.
            messages = [messages[0], {"role": "assistant", "content": message.content}]
            continue
        if message.stop_reason == "refusal":
            raise RuntimeError(f"Generation refused: {message.stop_details}")
        if message.stop_reason == "max_tokens":
            raise RuntimeError("Generation hit max_tokens before finishing the episode")

        episode = message.parsed_output
        if episode is None:
            raise RuntimeError("Model returned no parseable episode")
        print(f"  generated by {message.model}: in={message.usage.input_tokens} out={message.usage.output_tokens}")
        return episode.model_copy(update={"date": today.isoformat()})

    raise RuntimeError("Generation did not finish after repeated pause_turn continuations")
