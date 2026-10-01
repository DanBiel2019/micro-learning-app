# micro-learning-app

A personal micro-learning feed for Android: each morning it curates five ideas from
business, technology/AI, leadership, creativity, and systems & measurement, and reads
them aloud podcast-style in ~1–2 minute chunks you can swipe, pause, or dig into.

## Download

Grab the latest APK from [`release/`](release/), or download it directly:
[micro-learning-v1.1-debug.apk](https://github.com/DanBiel2019/micro-learning-app/raw/main/release/micro-learning-v1.1-debug.apk).
Install steps are in [release/README.md](release/README.md).

## What's in it

```
app/src/main/java/com/example/aigeneratedandroid/
  MainActivity.kt                 today's feed: pager, play/pause, reactions, go-deeper
  microlearning/
    model/        IdeaCard, UserProfile (onboarding answers), DailyFeed
    data/         ContentBank (30 paraphrased, attributed ideas), FeedbackStore +
                  SharedPrefsFeedbackStore (likes/skips/completions), FeedCache
    curation/     ContentCurationEngine — weighted, date-seeded daily selection
    narration/    NarrationFormatter (card -> spoken segments), NarrationPlayer (TTS)
    ui/           CardPagerAdapter
```

**Curation.** A date-seeded weighted draw, so a day's feed is stable across relaunches.
Weights combine topic affinity (learned from feedback), preferred styles (story,
counterintuitive), followed authors, a 14-day cooldown on repeats, and per-card scores;
cards you thumbs-down repeatedly stop appearing. Each day has a rotating lead topic that
sets the theme; no topic appears twice in a row or more than twice a day.

**Narration.** Each card becomes one chunk: intro (first card only), the idea, a bridge
to network engineering / observability / resilience work, a reflection prompt matched to
the card's style, the challenge, and a hand-off to the next card. Played through Android
TextToSpeech one sentence at a time, so pause/resume continues mid-card. Finishing a card
auto-advances, so the whole set plays hands-free.

**Feedback loop.** 👍 / 👎 on each card, finishing a card, and "Go deeper" (which queues a
related card — same author, then topic — right after the current one) all feed back into
tomorrow's weights.

## Building

Requires JDK 17 and the Android SDK (platform 34). With `local.properties` pointing at
your SDK (`sdk.dir=...`):

```
./gradlew testDebugUnitTest   # curation + narration unit tests (JVM, no device)
./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Next ideas

- Onboarding/profile editing screen (profile is currently seeded in `UserProfile.default()`)
- Generated deep dives via an LLM for "Go deeper", instead of library-only related cards
- Media-session / lock-screen controls so narration keeps playing with the screen off
- Growing the content library beyond 30 cards
