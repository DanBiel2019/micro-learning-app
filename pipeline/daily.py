"""Builds one day's episode end to end: script -> audio -> feed.json + ledger.json.

  python daily.py --repo OWNER/REPO                 # generate with Claude (needs ANTHROPIC_API_KEY)
  python daily.py --repo OWNER/REPO --script ep.json  # voice a hand-written episode instead

Outputs land in --out (default ./out): seg-N.mp3, feed.json, ledger.json. publish.py then
uploads them as a GitHub Release, which is where the app reads today's feed from.
"""
from __future__ import annotations

import argparse
import json
import urllib.error
import urllib.request
from datetime import date
from pathlib import Path

from render_audio import Voices, render_episode
from schema import Episode, PublishedEpisode, PublishedSegment

HERE = Path(__file__).parent
KEEP_PREVIOUS = 30


def fetch_ledger(repo: str) -> list[dict]:
    """The ledger rides along with each release, so the newest release always has full history."""
    url = f"https://github.com/{repo}/releases/latest/download/ledger.json"
    try:
        with urllib.request.urlopen(url, timeout=30) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return []
        raise


def covered_items(ledger: list[dict]) -> list[str]:
    """Human-readable list for the Claude prompt."""
    return [f"{seg['title']} ({seg['source']})" for ep in ledger for seg in ep.get("segments", [])]


def covered_refs(ledger: list[dict]) -> list[str]:
    """Wikipedia titles / article URLs already used, for the local generator."""
    return [seg["ref"] for ep in ledger for seg in ep.get("segments", []) if seg.get("ref")]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True, help="owner/repo hosting the releases")
    ap.add_argument("--date", default=date.today().isoformat())
    ap.add_argument("--script", type=Path, help="hand-written Episode JSON; skips generation")
    ap.add_argument("--engine", choices=["local", "claude"], default="local",
                    help="local: open-weights model via llama.cpp (default); claude: Anthropic API")
    ap.add_argument("--out", type=Path, default=HERE / "out")
    args = ap.parse_args()

    profile = json.loads((HERE / "profile.json").read_text())
    today = date.fromisoformat(args.date)
    tag = f"ep-{today.isoformat()}"
    ledger = fetch_ledger(args.repo)
    if any(e["tag"] == tag for e in ledger):
        print(f"{tag} is already published; nothing to do.")
        return

    refs: list[str | None]
    if args.script:
        print(f"Loading script {args.script}")
        episode = Episode.model_validate_json(args.script.read_text())
        refs = [None] * len(episode.segments)
    elif args.engine == "local":
        from generate_local import generate_episode

        print(f"Generating episode for {today} with a local model ({len(ledger)} past episodes in ledger)")
        episode, refs = generate_episode(profile, covered_refs(ledger), today)
    else:
        from generate import generate_episode

        print(f"Generating episode for {today} with Claude ({len(ledger)} past episodes in ledger)")
        episode = generate_episode(profile, covered_items(ledger), today)
        refs = [s.source.url for s in episode.segments]

    args.out.mkdir(parents=True, exist_ok=True)
    (args.out / "episode.json").write_text(episode.model_dump_json(indent=2))

    v = profile["voices"]
    print("Voicing episode")
    rendered = render_episode(episode, args.out, Voices(host=v["host"], cohost=v["cohost"]))

    base = f"https://github.com/{args.repo}/releases/download/{tag}"
    segments = [
        PublishedSegment(
            **seg.model_dump(),
            audioUrl=f"{base}/{r.path.name}",
            durationMs=r.duration_ms,
            lines=r.lines,
        )
        for seg, r in zip(episode.segments, rendered)
    ]
    feed = PublishedEpisode(
        id=tag,
        date=today.isoformat(),
        title=episode.title,
        theme=episode.theme,
        hosts={"host": v["hostName"], "cohost": v["cohostName"]},
        segments=segments,
        totalDurationMs=sum(r.duration_ms for r in rendered),
        previous=[
            {"id": e["tag"], "date": e["date"], "title": e["title"],
             "feedUrl": f"https://github.com/{args.repo}/releases/download/{e['tag']}/feed.json"}
            for e in reversed(ledger[-KEEP_PREVIOUS:])
        ],
    )
    (args.out / "feed.json").write_text(feed.model_dump_json(indent=2))

    ledger.append({
        "tag": tag,
        "date": today.isoformat(),
        "title": episode.title,
        "segments": [{"title": s.title, "topic": s.topic, "source": s.source.title, "ref": r}
                     for s, r in zip(episode.segments, refs)],
    })
    (args.out / "ledger.json").write_text(json.dumps(ledger, indent=2))

    minutes = feed.totalDurationMs / 60000
    print(f"Done: {tag} '{episode.title}', {len(segments)} segments, {minutes:.1f} min -> {args.out}")


if __name__ == "__main__":
    main()
