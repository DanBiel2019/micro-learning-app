package com.example.aigeneratedandroid.microlearning

import com.example.aigeneratedandroid.microlearning.curation.ContentCurationEngine
import com.example.aigeneratedandroid.microlearning.data.InMemoryFeedbackStore
import com.example.aigeneratedandroid.microlearning.data.Reaction
import com.example.aigeneratedandroid.microlearning.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ContentCurationEngineTest {

    private val profile = UserProfile.default()
    private val day = LocalDate.of(2026, 10, 1)

    @Test
    fun `library topics all match profile topics`() {
        val unknown = TestData.library.map { it.topic }.toSet() - profile.topics.toSet()
        assertTrue("Unmatched topics: $unknown", unknown.isEmpty())
        assertEquals(TestData.library.size, TestData.library.map { it.id }.toSet().size)
    }

    @Test
    fun `feed has the session size, no duplicates, and varied topics`() {
        val feed = ContentCurationEngine(TestData.library, InMemoryFeedbackStore()).buildEpisode(profile, day)
        assertEquals(profile.segmentsPerSession, feed.segments.size)
        assertEquals(feed.segments.size, feed.segments.map { it.id }.toSet().size)
        assertTrue(feed.segments.groupingBy { it.topic }.eachCount().values.all { it <= 2 })
        feed.segments.zipWithNext().forEach { (a, b) -> assertFalse(a.topic == b.topic) }
        assertTrue(feed.theme.isNotBlank())
        assertTrue(feed.offline)
    }

    @Test
    fun `same day gives the same feed even after marking it shown`() {
        val store = InMemoryFeedbackStore()
        val engine = ContentCurationEngine(TestData.library, store)
        val first = engine.buildEpisode(profile, day)
        store.markShown(first.segments.map { it.id }, day.toEpochDay())
        assertEquals(first.segments.map { it.id }, engine.buildEpisode(profile, day).segments.map { it.id })
    }

    @Test
    fun `recently shown cards are mostly avoided the next day`() {
        val store = InMemoryFeedbackStore()
        val engine = ContentCurationEngine(TestData.library, store)
        val first = engine.buildEpisode(profile, day)
        store.markShown(first.segments.map { it.id }, day.toEpochDay())
        val second = engine.buildEpisode(profile, day.plusDays(1))
        val repeats = second.segments.map { it.id }.intersect(first.segments.map { it.id }.toSet())
        assertTrue("Too many repeats: $repeats", repeats.size <= 1)
    }

    @Test
    fun `heavily disliked cards are never served`() {
        val store = InMemoryFeedbackStore()
        repeat(3) { store.record("sys-01", Reaction.SKIPPED) }
        val engine = ContentCurationEngine(TestData.library, store)
        (0L until 30L).forEach { offset ->
            val feed = engine.buildEpisode(profile, day.plusDays(offset))
            assertFalse(feed.segments.any { it.id == "sys-01" })
        }
    }

    @Test
    fun `liking a topic makes it show up more over a month`() {
        fun countLeadership(store: InMemoryFeedbackStore): Int {
            val engine = ContentCurationEngine(TestData.library, store)
            return (0L until 60L).sumOf { offset ->
                engine.buildEpisode(profile, day.plusDays(offset)).segments.count { it.topic == "Leadership" }
            }
        }
        val neutral = countLeadership(InMemoryFeedbackStore())
        val fan = InMemoryFeedbackStore().apply {
            TestData.library.filter { it.topic == "Leadership" }.forEach { card ->
                repeat(3) { record(card.id, Reaction.LIKED) }
            }
        }
        assertTrue("neutral=$neutral, fan=${countLeadership(fan)}", countLeadership(fan) > neutral)
    }

    @Test
    fun `related prefers same author then same topic`() {
        val engine = ContentCurationEngine(TestData.library, InMemoryFeedbackStore())
        val crucial = TestData.byId("lead-01")
        assertEquals("lead-02", engine.related(crucial).first().id)
        assertTrue(engine.related(crucial, exclude = setOf("lead-02")).first().topic == crucial.topic)
    }
}
