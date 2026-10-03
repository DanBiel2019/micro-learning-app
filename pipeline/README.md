# Daily episode pipeline

Every morning a GitHub Actions job ([`daily-episode.yml`](../.github/workflows/daily-episode.yml))
writes, voices and publishes a new ~10 minute, two-host episode. The app downloads it from
`releases/download/daily-feed/feed.json` (a rolling release; each episode also gets a dated `ep-YYYY-MM-DD` archive release).

**No API keys and no running cost.** Everything runs on the free GitHub runner with
open-weights models:

| Job | Model | License |
|-----|-------|---------|
| Writing scripts, visuals, takeaways | [Qwen3-14B](https://huggingface.co/Qwen/Qwen3-14B-GGUF) (Q4_K_M) via [llama.cpp](https://github.com/ggml-org/llama.cpp) | Apache-2.0 / MIT |
| Two-host voices | [Kokoro 82M](https://github.com/thewh1teagle/kokoro-onnx) | Apache-2.0 |

```
sources.json ─▶ pick ideas ─▶ research.py ─────────▶ generate_local.py ─▶ render_audio.py ─▶ publish.py
 (curated bank,  2 core,       Wikipedia article or   Qwen3 writes each    Kokoro voices,     GitHub Release
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
- **ledger.json** travels with every release and records every source used, so nothing
  repeats.

## Running it

Nothing to set up: the job runs daily at 08:00 UTC. To run it by hand, open
**Actions → Daily episode → Run workflow**. Untick *publish* for a dry run; the episode
script and audio are kept as a downloadable artifact either way.

The first run downloads ~9.5 GB of models (then cached); a run takes about two hours on the
free runner; later runs skip the download.

### Locally

```bash
python -m venv .venv && .venv/bin/pip install -r requirements.txt
# models: Qwen3-14B-Q4_K_M.gguf (or any GGUF) and Kokoro files; llama-server on PATH
LLM_MODEL=~/models/Qwen3-14B-Q4_K_M.gguf KOKORO_DIR=~/kokoro \
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
  llama.cpp supports.
