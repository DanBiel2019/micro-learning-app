package com.example.aigeneratedandroid.microlearning

import com.example.aigeneratedandroid.microlearning.curation.ContentCurationEngine
import com.example.aigeneratedandroid.microlearning.data.ContentBank
import com.example.aigeneratedandroid.microlearning.data.InMemoryFeedbackStore
import com.example.aigeneratedandroid.microlearning.model.UserProfile
import com.example.aigeneratedandroid.microlearning.narration.NarrationFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NarrationFormatterTest {

    private val feed = ContentCurationEngine(ContentBank.all, InMemoryFeedbackStore())
        .buildFeed(UserProfile.default(), LocalDate.of(2026, 10, 1))
    private val chunks = NarrationFormatter.format(feed)

    @Test
    fun `one chunk per card, intro only on the first`() {
        assertEquals(feed.cards.size, chunks.size)
        assertTrue(chunks.first().segments.first().startsWith("Good morning"))
        chunks.drop(1).forEach { assertFalse(it.segments.first().startsWith("Good morning")) }
    }

    @Test
    fun `chunks hand off to the next card and close the set`() {
        chunks.zip(feed.cards.drop(1)).forEach { (chunk, next) ->
            assertEquals("Next up: ${next.title}.", chunk.segments.last())
        }
        assertTrue(chunks.last().segments.last().startsWith("That's the set for today"))
    }

    @Test
    fun `every card in the library narrates cleanly`() {
        ContentBank.all.forEachIndexed { i, card ->
            val chunk = NarrationFormatter.chunkFor(card, i, ContentBank.all.size, "theme", null)
            chunk.segments.forEach { seg ->
                assertFalse("Raw symbol in: $seg", seg.contains(Regex("[\\[\\]{}<>*_#|∩]")))
                assertTrue(seg.isNotBlank())
            }
            // Roughly a one-to-two minute listen per card.
            assertTrue("${card.id}: ${chunk.estimatedSeconds}s", chunk.estimatedSeconds in 45..150)
        }
    }

    @Test
    fun `sentence splitting keeps punctuation`() {
        assertEquals(listOf("One.", "Two?", "Three!"), NarrationFormatter.sentences("One. Two? Three!"))
    }
}
