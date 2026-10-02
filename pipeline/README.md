# Daily episode pipeline

Every morning a GitHub Actions job ([`daily-episode.yml`](../.github/workflows/daily-episode.yml))
writes, voices and publishes a new ~10 minute, two-host episode. The app downloads it from
`releases/latest/download/feed.json`.

```
profile.json ──▶ generate.py ──▶ episode.json ──▶ render_audio.py ──▶ seg-N.mp3 ─┐
 (who, topics,    Claude +         (script,         Kokoro TTS,         feed.json ├─▶ publish.py ──▶ GitHub Release
  adjacent        web search       visuals,         two voices,         ledger.json┘     ep-YYYY-MM-DD (latest)
  fields, mix)                     sources)         line timings)
```

- **generate.py** asks Claude for five segments: 2 from your core topics, 2 from adjacent
  fields (aviation safety, medicine, military history, behavioral economics, …) and 1 "fresh"
  item from the last 60 days found with web search. Each segment includes the two-host
  script, an infographic spec, sources, deeper questions and further reading. Output is
  validated against [`schema.py`](schema.py).
- **ledger.json** travels with every release and lists everything covered so far, so the
  generator never repeats an idea or source.
- **render_audio.py** voices the script with [Kokoro](https://github.com/thewh1teagle/kokoro-onnx),
  an open-weights neural TTS model that runs on CPU (no per-use cost), normalises loudness to
  podcast level, and records when each line starts so the app can highlight the transcript.
- **publish.py** creates the release and uploads the MP3s, `feed.json` and `ledger.json`.

## One-time setup

Add a repository secret **`ANTHROPIC_API_KEY`** (Settings → Secrets and variables → Actions).
The job runs at 09:30 UTC; change the `cron` line to move it. Run it by hand from the
Actions tab ("Daily episode" → Run workflow).

Cost: one Claude Opus 5.5 request with a few web searches per day, typically well under a
dollar. The voices are free.

## Running locally

```bash
python -m venv .venv && .venv/bin/pip install -r requirements.txt
# Kokoro model files (~350 MB) into ~/tools/kokoro, or set KOKORO_DIR
.venv/bin/python daily.py --repo OWNER/REPO                         # generate with Claude
.venv/bin/python daily.py --repo OWNER/REPO --script episodes/2026-10-01.json  # voice a hand-written script
GITHUB_TOKEN=... .venv/bin/python publish.py --repo OWNER/REPO
```

## Tuning

- **Topics and mix**: `profile.json` (`coreTopics`, `adjacentFields`, `mix`).
- **Voices**: `profile.json` → `voices`. Kokoro voices include `af_heart`, `af_bella`,
  `bf_emma` (female) and `am_michael`, `am_fenrir`, `bm_george` (male).
- **Length**: the script asks for ~300 spoken words per segment (about two minutes).
