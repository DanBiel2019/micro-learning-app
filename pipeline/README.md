# Daily episode pipeline

Every night a GitHub Actions job ([`daily-episode.yml`](../.github/workflows/daily-episode.yml))
writes, voices and publishes a new ~10 minute, two-host episode, ready by 7:00 AM US Central. The app downloads it from
`releases/download/daily-feed/feed.json` (a rolling release; each episode also gets a dated `ep-YYYY-MM-DD` archive release).

**No API keys and no running cost.** Everything runs on the free GitHub runner with
open-weights models:

| Job | Model | License |
|-----|-------|---------|
| Writing scripts, visuals, takeaways | [Gemma 4 26B-A4B](https://huggingface.co/google/gemma-4-26B-A4B-it) ([unsloth UD-IQ4_XS GGUF](https://huggingface.co/unsloth/gemma-4-26B-A4B-it-GGUF), 13.6 GB) via [llama.cpp](https://github.com/ggml-org/llama.cpp) | Apache-2.0 / MIT |
| Two-host voices | [Kokoro 82M](https://github.com/thewh1teagle/kokoro-onnx) | Apache-2.0 |

```
sources.json ─▶ pick ideas ─▶ research.py ─────────▶ generate_local.py ─▶ render_audio.py ─▶ publish.py
 (curated bank,  2 core,       Wikipedia article or   Gemma 4 writes each   Kokoro voices,     GitHub Release
  categories,    2 adjacent,   news article text      segment ONLY from    line timings       ep-YYYY-MM-DD
  news feeds)    1 fresh                              that source text
```

## How it stays accurate with a small model

A model this size invents details when writing from memory, so it never does: every
segment is written from a source document the pipeline fetches first, with instructions to
use only facts in that text. The source is cited in the app and linked under "Go deeper".

- **Evergreen ideas** come from Wikipedia. `sources.json` holds a curated bank of ~100
  ideas (survivorship bias, Braess's paradox, the Tenerife disaster, the Citicorp Center
  crisis, Conway's law, …), each with an angle and, where there is one, the classic book to
  credit. When the bank runs low, the model picks new articles from curated Wikipedia
  categories (engineering failures, cognitive biases, systems thinking, …), so coverage
  keeps widening on its own.
- **The fresh segment** comes from the last three weeks of engineering news feeds
  (Cloudflare's blog, The Register, Ars Technica, InfoQ, …). The model ranks candidates,
  favouring postmortems, outages and research findings over product launches.
- **ledger.json** lives on the rolling `daily-feed` release and records every episode and
  source used, so nothing repeats. Each run downloads it from there, not from a local `out/`
  folder; never publish a stale `out/` by hand, or the release ledger loses newer episodes.

## Running it

Nothing to set up. The job is scheduled for 02:17 UTC (9:17 PM CDT / 8:17 PM CST the evening
before), with a backup run at 05:17 UTC. The early start matters: GitHub has started scheduled
runs up to 5.5 hours late, and a run takes 1 to 3 hours on the free runner. Even then the
episode is out by about 5:45 AM Central. The episode is dated by the UTC day, which is
already the morning it is for. Each run first checks the ledger; if the day's episode is
already published (say the backup fires after a successful first run), it stops in seconds.

To run it by hand, open **Actions → Daily episode → Run workflow**. Untick *publish* for a
dry run; the episode script and audio are kept as a downloadable artifact either way.

Each run downloads the 13.6 GB writing model from Hugging Face (a few minutes; it is larger
than the 10 GB Actions cache allows) and restores the voice model and llama.cpp from the cache.

GitHub turns off scheduled workflows in a public repository after 60 days without commits.
It emails a warning first; re-enable the workflow under **Actions**, or push any commit.

## Why this model

The writer has to run on a free GitHub runner: 4 CPU cores, 16 GB RAM, no GPU. On CPU,
speed is set by the parameters used *per token*, while RAM has to hold *all* of them. A
mixture-of-experts model fits that well: Gemma 4 26B-A4B holds 25B parameters but uses
only ~4B per token, so it reads and writes several times faster than the dense Qwen3-14B
used before, while being a much stronger model. Compared in October 2026:

| Model (quant that fits 16 GB) | Active / total | Faithfulness: Vectara hallucination rate | EQ-Bench creative writing (Elo) | Notes |
|---|---|---|---|---|
| **Gemma 4 26B-A4B** (UD-IQ4_XS, 13.6 GB) | 3.8B / 25B | **5.2 %** | 1305 | chosen |
| Gemma 4 12B (QAT Q4_0, 7 GB) | 12B dense | n/a | 1289 | fallback: same family, small, but as slow as Qwen3-14B |
| Qwen3-14B (Q4_K_M, 9 GB) | 14.8B dense | 5.4 % | n/a | previous; ~7 tok/s reading, 2.5 tok/s writing |
| Qwen3.6-35B-A3B (UD-IQ3_XXS, 13.2 GB) | 3B / 35B | 10.5 % (Qwen3.5-35B-A3B) | n/a | strongest at code/maths, but only a 3-bit quant fits and the family invents more |
| Qwen3.8-27B (UD-IQ3_XXS, 10.9 GB) | 27B dense | 12.1 % (Qwen3.5-27B) | 1671 | best prose, but ~2x slower than Qwen3-14B and 3-bit; would not finish reliably in the 5 h job limit |
| gpt-oss-20b (MXFP4, 12.1 GB) | 3.6B / 21B | n/a | 666 | fast, but stiff, generic prose |

Sources: [Vectara hallucination leaderboard](https://github.com/vectara/hallucination-leaderboard),
[EQ-Bench creative writing v3](https://eqbench.com/creative_writing.html), the model cards.
Faithfulness matters most here (every fact must come from the fetched source), then natural
writing; Gemma 4 26B-A4B is the only candidate strong on both that fits.

### Locally

```bash
python -m venv .venv && .venv/bin/pip install -r requirements.txt
# models: gemma-4-26B-A4B-it-UD-IQ4_XS.gguf (or any GGUF) and Kokoro files; llama-server on PATH
LLM_MODEL=~/models/gemma-4-26B-A4B-it-UD-IQ4_XS.gguf KOKORO_DIR=~/kokoro \
  .venv/bin/python daily.py --repo OWNER/REPO
.venv/bin/python daily.py --repo OWNER/REPO --script episodes/2026-10-01.json   # voice a hand-written script
GITHUB_TOKEN=... .venv/bin/python publish.py --repo OWNER/REPO
```

Optional: `--engine claude` writes with the Anthropic API instead (needs `ANTHROPIC_API_KEY`).

## Tuning

- **Topics and mix**: `profile.json` (`listener`, `mix`), `sources.json` (`seeds`,
  `discovery.categories`, `freshFeeds`). Add a seed with a Wikipedia title, a topic and an
  angle.
- **Voices**: `profile.json` → `voices`. Kokoro voices include `af_heart`, `af_bella`,
  `bf_emma` (female) and `am_michael`, `am_fenrir`, `bm_george` (male).
- **A different model**: set `LLM_FILE` / `LLM_URL` in the workflow to any GGUF that
  llama.cpp supports. Sampling settings and chat-template quirks are picked per model
  family from the file name (`PROFILES` in `local_llm.py`: Gemma and Qwen3 are covered;
  anything else gets the Qwen3 settings). Keep the file under ~14 GB so it fits in RAM.
