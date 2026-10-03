# Shared context for micro-learning agents (not an agent; referenced by each agent)

Project: personal morning-podcast Android app (Kotlin, Jetpack Compose, Material 3, Media3)
plus a Python content pipeline that publishes one episode a day to GitHub Releases.

Key paths
- app/src/main/java/com/example/aigeneratedandroid/microlearning/
  - model/Episode.kt         feed data model (mirrors pipeline/schema.py, keep in sync)
  - data/EpisodeRepository   reads releases/download/daily-feed/feed.json, caches, downloads audio
  - playback/                PlaybackService (Media3 session) + EpisodePlayer (studio audio or device TTS)
  - ui/                      App.kt (shell/nav), HomeScreen, SegmentScreen, MiniPlayer, CoverArt,
                             Infographic.kt (visual renderer), theme/Theme.kt, MainViewModel
- app/src/main/assets/library.json   30 bundled segments (offline fallback) with visual specs
- pipeline/  schema.py, generate_local.py (local LLM writer), local_llm.py (llama.cpp),
             research.py, render_audio.py (Kokoro TTS), daily.py, publish.py, sources.json
- .github/workflows/daily-episode.yml   daily job on the GitHub runner

Machine limits (IMPORTANT): this is an old 4-core laptop with 5 GB RAM.
- Never start the Android emulator. Never run two Gradle builds at once.
- Build with: export JAVA_HOME=$HOME/tools/jdk17; ./gradlew --no-daemon -q compileDebugKotlin
  then ./gradlew --no-daemon -q testDebugUnitTest assembleDebug before you finish.
- A worktree needs local.properties containing: sdk.dir=/home/groot/tools/android-sdk
- Python tooling: ~/tools/pyenv/bin/python (pydantic, kokoro-onnx, anthropic installed).

Working rules
- Stay inside the files your brief says you own; if you must touch another file, keep the
  change minimal and say so in your report.
- Keep the feed format backward compatible: new fields need defaults; unknown values must
  degrade gracefully in the app.
- Commit your work to your current branch with clear messages ending with:
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
  Do not push unless your brief says so.
- Cite the sources behind design decisions in your final report (URLs).
