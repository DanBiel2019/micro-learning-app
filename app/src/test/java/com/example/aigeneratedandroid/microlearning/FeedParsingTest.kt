package com.example.aigeneratedandroid.microlearning

import com.example.aigeneratedandroid.microlearning.model.Episode
import com.example.aigeneratedandroid.microlearning.model.VisualKinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The app must read exactly what the pipeline publishes. */
class FeedParsingTest {

    private val visualKinds = VisualKinds.ALL.toSet()

    @Test
    fun `published feed parses with audio, timings and visuals`() {
        val file = File("../pipeline/out/feed.json")
        if (!file.exists()) return // pipeline output not present in this checkout
        val ep = TestData.json.decodeFromString(Episode.serializer(), file.readText())
        assertTrue(ep.hasAudio)
        assertEquals(5, ep.segments.size)
        ep.segments.forEach { seg ->
            assertTrue(seg.lines.isNotEmpty())
            assertEquals(seg.lines.sortedBy { it.startMs }, seg.lines)
            assertTrue(seg.visual!!.kind in visualKinds)
            assertTrue((seg.durationMs ?: 0) > 60_000)
        }
    }

    @Test
    fun `every library visual uses a known kind`() {
        TestData.library.forEach { assertTrue(it.id, it.visual!!.kind in visualKinds) }
    }
}
