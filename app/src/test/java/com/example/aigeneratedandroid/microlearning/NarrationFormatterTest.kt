package com.example.aigeneratedandroid.microlearning

import com.example.aigeneratedandroid.microlearning.curation.ContentCurationEngine
import com.example.aigeneratedandroid.microlearning.data.InMemoryFeedbackStore
import com.example.aigeneratedandroid.microlearning.model.Line
import com.example.aigeneratedandroid.microlearning.model.UserProfile
import com.example.aigeneratedandroid.microlearning.narration.NarrationFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NarrationFormatterTest {

    private val episode = ContentCurationEngine(TestData.library, InMemoryFeedbackStore())
        .buildEpisode(UserProfile.default(), LocalDate.of(2026, 10, 1))
    private val chunks = NarrationFormatter.format(episode)

    @Test
    fun `one chunk per segment, intro only on the first`() {
        assertEquals(episode.segments.size, chunks.size)
        assertTrue(chunks.first().segments.first().startsWith("Good morning"))
        chunks.drop(1).forEach { assertFalse(it.segments.first().startsWith("Good morning")) }
    }

    @Test
    fun `chunks hand off to the next segment and close the set`() {
        chunks.zip(episode.segments.drop(1)).forEach { (chunk, next) ->
            assertEquals("Next up: ${next.title}.", chunk.segments.last())
        }
        assertTrue(chunks.last().segments.last().startsWith("That's the set for today"))
    }

    @Test
    fun `every library segment narrates cleanly`() {
        TestData.library.forEachIndexed { i, seg ->
            val chunk = NarrationFormatter.chunkFor(seg, i, TestData.library.size, "theme", null)
            chunk.segments.forEach { s ->
                assertFalse("Raw symbol in: $s", s.contains(Regex("[\\[\\]{}<>*_#|∩]")))
                assertTrue(s.isNotBlank())
            }
            assertTrue("${seg.id}: ${chunk.estimatedSeconds}s", chunk.estimatedSeconds in 45..150)
        }
    }

    @Test
    fun `scripted segments are read from their script`() {
        val seg = TestData.library.first().copy(script = listOf(Line("host", "Hello there."), Line("cohost", "Hi & welcome.")))
        val chunk = NarrationFormatter.chunkFor(seg, 0, 1, "theme", null)
        assertEquals(listOf("Hello there.", "Hi and welcome."), chunk.segments)
    }

    @Test
    fun `sentence splitting keeps punctuation`() {
        assertEquals(listOf("One.", "Two?", "Three!"), NarrationFormatter.sentences("One. Two? Three!"))
    }
}
