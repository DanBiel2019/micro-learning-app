package com.example.aigeneratedandroid.microlearning.ui.infographic

import com.example.aigeneratedandroid.microlearning.model.Visual
import com.example.aigeneratedandroid.microlearning.model.VisualItem
import com.example.aigeneratedandroid.microlearning.model.VisualKinds

/** One entry in the infographic gallery: a kind, when to use it, and a sample spec. */
data class KindGuide(
    val kind: String,
    val name: String,
    val whenToUse: String,
    /** A topic name, only used to pick the sample's accent colour. */
    val topic: String,
    val sample: Visual
) {
    val isNew: Boolean get() = kind in VisualKinds.ADDED
}

private fun item(
    label: String,
    detail: String = "",
    emoji: String = "",
    value: Double? = null,
    valueLabel: String? = null,
    emphasis: Boolean = false
) = VisualItem(label, detail, emoji, value, valueLabel, emphasis)

/**
 * Sample data for every kind, used by the gallery screen (and checked by unit tests). The
 * when-to-use rules match the generator prompt in pipeline/generate_local.py.
 */
object GallerySamples {
    val guides: List<KindGuide> = listOf(
        KindGuide(
            "flow", "Flow", "3-5 steps in order, cause to effect. Emphasise the step where it goes right or wrong.",
            "Networking",
            Visual(
                "flow", "How a good incident unfolds",
                listOf(
                    item("Detect", "An alert fires on user-facing symptoms", "🚨"),
                    item("Mitigate", "Stop the bleeding before finding the cause", "🩹", emphasis = true),
                    item("Diagnose", "Now dig for the root cause", "🔍"),
                    item("Learn", "Blameless review, tracked actions", "📝")
                ),
                "Mitigate first. Understanding can wait an hour."
            )
        ),
        KindGuide(
            "cycle", "Cycle", "3-5 steps that loop back to the start. Emphasise the step that drives the loop.",
            "Leadership",
            Visual(
                "cycle", "Boyd's OODA loop",
                listOf(
                    item("Observe", "Take in what is happening", "👀"),
                    item("Orient", "Make sense of it; where most loops are won", "🧭", emphasis = true),
                    item("Decide", "Pick a course of action", "🤔"),
                    item("Act", "Do it, then observe again", "⚡")
                ),
                "Cycle faster than the situation changes."
            )
        ),
        KindGuide(
            "compare", "Compare", "2-4 options side by side. Emphasise the one the story recommends.",
            "Technology & AI",
            Visual(
                "compare", "Two ways to ship a change",
                listOf(
                    item("Blue-green", "Switch all traffic to a full copy", "🔀", valueLabel = "All at once"),
                    item("Canary", "Send a small slice first, watch, widen", "🐤", valueLabel = "A few %", emphasis = true)
                ),
                "Let a small group find the bug for everyone."
            )
        ),
        KindGuide(
            "before_after", "Before / after", "Exactly 2 items: the old way, then the new way. Numbers optional.",
            "Technology & AI",
            Visual(
                "before_after", "Smaller batches, calmer releases",
                listOf(
                    item("Big-bang release", "Hundreds of changes, hard to debug", "📦", valueLabel = "Monthly"),
                    item("Small batches", "One change at a time, easy to roll back", "🧩", valueLabel = "Daily", emphasis = true)
                ),
                "Batch size is a lever on speed and stability."
            )
        ),
        KindGuide(
            "matrix", "2x2 matrix", "Exactly 4 items for two yes/no dimensions, ordered top-left, top-right, bottom-left, bottom-right. Name the dimensions in xAxis and yAxis; top-right is high on both.",
            "Business",
            Visual(
                "matrix", "The Eisenhower matrix",
                listOf(
                    item("Schedule", "Important, not urgent: where progress lives", "📅", emphasis = true),
                    item("Do now", "Important and urgent", "🔥"),
                    item("Drop", "Neither: stop doing it", "🗑️"),
                    item("Delegate", "Urgent but not important", "🤝")
                ),
                "Protect the top-left before it turns into fires.",
                xAxis = "Urgent",
                yAxis = "Important"
            )
        ),
        KindGuide(
            "iceberg", "Iceberg", "First item is what everyone sees; 2-4 more items are the hidden causes underneath. Emphasise the biggest hidden one.",
            "Systems & Measurement",
            Visual(
                "iceberg", "What a missed deadline hides",
                listOf(
                    item("Missed deadline", "The part everyone notices", "⏰"),
                    item("Manual deploys", "Every release takes a day", "🖐️"),
                    item("Flaky tests", "Nobody trusts a green build", "🎲"),
                    item("Knowledge in one head", "Work waits for one person", "🧠", emphasis = true)
                ),
                "Fix what's under the waterline, not the date."
            )
        ),
        KindGuide(
            "ladder", "Ladder", "3-5 levels, weakest first; the renderer stacks the strongest on top. Name what increases going up in yAxis.",
            "Systems & Measurement",
            Visual(
                "ladder", "The hierarchy of controls",
                listOf(
                    item("Protective gear", "Relies on people every time", "🦺"),
                    item("Procedures", "Change how people work", "📋"),
                    item("Engineering", "Isolate people from the hazard", "🛠️"),
                    item("Substitution", "Swap in something safer", "🔁"),
                    item("Elimination", "Remove the hazard entirely", "🚫", emphasis = true)
                ),
                "Design the hazard out before you train around it.",
                yAxis = "More effective"
            )
        ),
        KindGuide(
            "funnel", "Funnel", "3-5 stages that narrow, widest first. Put counts in value when the source gives them; width then follows the number.",
            "Networking",
            Visual(
                "funnel", "From alert noise to one page",
                listOf(
                    item("Alerts fired", "Every threshold crossed", "🔔", 1000.0, "1,000"),
                    item("Unique issues", "After de-duplication", "🧹", 220.0, "220"),
                    item("Actionable", "Someone must do something", "✋", 40.0, "40"),
                    item("Woke a human", "Paged out of hours", "📟", 6.0, "6", emphasis = true)
                ),
                "Illustrative numbers. Page only on what needs a human now."
            )
        ),
        KindGuide(
            "spectrum", "Spectrum", "3-5 positions between two extremes, in order. Name the scale in xAxis; emphasise the sweet spot.",
            "Technology & AI",
            Visual(
                "spectrum", "How fresh is a replica's answer?",
                listOf(
                    item("Strong", "Every read sees the latest write", "🔒"),
                    item("Causal", "Effects never appear before causes", "🔗", emphasis = true),
                    item("Eventual", "Replicas agree, eventually", "🌊")
                ),
                "Most apps need less than strong, more than eventual.",
                xAxis = "Faster and more available"
            )
        ),
        KindGuide(
            "timeline", "Timeline", "3-6 dated events in order, the date in valueLabel. Emphasise the turning point.",
            "History",
            Visual(
                "timeline", "How the internet got its language",
                listOf(
                    item("ARPANET's first link", "UCLA to SRI, two nodes talk", "📡", valueLabel = "1969"),
                    item("TCP/IP flag day", "Every host switches protocols", "🚩", valueLabel = "1983", emphasis = true),
                    item("The Web is proposed", "Tim Berners-Lee at CERN", "🌐", valueLabel = "1989")
                ),
                "One shared protocol let separate networks become one."
            )
        ),
        KindGuide(
            "stats", "Stats", "2-4 headline numbers, each in valueLabel with what it counts in label.",
            "Reliability",
            Visual(
                "stats", "What each extra nine costs you",
                listOf(
                    item("99.9% uptime", "Allowed downtime per 30 days", "⏱️", valueLabel = "43 min"),
                    item("99.99% uptime", "Allowed downtime per 30 days", "⏱️", valueLabel = "4.3 min", emphasis = true),
                    item("99.999% uptime", "Allowed downtime per 30 days", "⏱️", valueLabel = "26 s")
                ),
                "Each nine cuts the budget tenfold."
            )
        ),
        KindGuide(
            "big_number", "Big number", "One striking number (valueLabel) with what it counts in label; 1-2 more items add context.",
            "Space",
            Visual(
                "big_number", "One unit mix-up",
                listOf(
                    item("Mars Climate Orbiter lost", "One team used pound-seconds, the other newton-seconds", "🛰️", valueLabel = "$125M", emphasis = true),
                    item("Year it was lost", "On arrival at Mars", valueLabel = "1999")
                ),
                "Interfaces fail at the seams nobody owns."
            )
        ),
        KindGuide(
            "bars", "Bars", "3-5 quantities in the same unit; set value (bar length) and valueLabel (the text).",
            "Reliability",
            Visual(
                "bars", "Downtime allowed per 30 days",
                listOf(
                    item("99%", "", "", 432.0, "7.2 h"),
                    item("99.5%", "", "", 216.0, "3.6 h"),
                    item("99.9%", "The usual default", "", 43.2, "43 min", emphasis = true),
                    item("99.95%", "", "", 21.6, "22 min")
                ),
                "Agree the number before the outage, not during it."
            )
        ),
        KindGuide(
            "waffle", "Waffle", "A share of a whole: 1-3 parts with value as a percent (0-100). One square is 1%.",
            "Reliability",
            Visual(
                "waffle", "Where outages come from",
                listOf(
                    item("Changes to a live system", "Per Google's SRE book, roughly", "🔧", 70.0, "~70%", emphasis = true),
                    item("Everything else", "Hardware, load, dependencies", "", 30.0, "~30%")
                ),
                "Make change safe and most outages never happen."
            )
        ),
        KindGuide(
            "venn", "Venn", "2-3 ideas plus a LAST item that names their overlap.",
            "Technology & AI",
            Visual(
                "venn", "What data science needs",
                listOf(
                    item("Hacking skills", "", "💻"),
                    item("Math & statistics", "", "📐"),
                    item("Domain expertise", "", "🏥"),
                    item("Data science", "Useful only where all three meet", "✨", emphasis = true)
                ),
                "Two out of three is how bad analyses happen."
            )
        ),
        KindGuide(
            "quote", "Quote", "One item: the quote in label, who said it in detail.",
            "Systems & Measurement",
            Visual(
                "quote", "On models",
                listOf(item("All models are wrong, but some are useful.", "George Box, statistician")),
                "Ask what a model is useful for, not whether it's true."
            )
        )
    )

    /**
     * Specs that do not fit their kind (or name a kind this app version doesn't know), shown
     * in the gallery so the graceful fallbacks can be reviewed too. Pairs of (note, spec).
     */
    val fallbacks: List<Pair<String, Visual>> = listOf(
        "Unknown kind \"radar\" (e.g. from a newer feed) draws as a flow" to Visual(
            "radar", "A kind this version doesn't know",
            listOf(item("First", "Still readable", "1️⃣"), item("Second", "", "2️⃣"), item("Third", "", "3️⃣", emphasis = true)),
            "Unknown kinds degrade to steps instead of disappearing."
        ),
        "A matrix with 3 items (needs 4) draws as compare cards" to Visual(
            "matrix", "A matrix missing a quadrant",
            listOf(item("Do now", "Urgent and important", "🔥", emphasis = true), item("Schedule", "Important, not urgent", "📅"), item("Delegate", "Urgent, not important", "🤝")),
            "Bad shapes fall back to the closest kind that fits.",
            xAxis = "Urgent", yAxis = "Important"
        ),
        "A waffle without any numbers draws as compare cards" to Visual(
            "waffle", "A share with no numbers",
            listOf(item("Most of it", "", "🟧"), item("The rest", "", "⬜")),
            ""
        ),
        "Bars without values draw as a flow" to Visual(
            "bars", "Bars with nothing to measure",
            listOf(item("Option A"), item("Option B"), item("Option C")),
            ""
        )
    )
}
