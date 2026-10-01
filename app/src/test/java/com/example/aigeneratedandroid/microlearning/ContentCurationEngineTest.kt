package com.example.aigeneratedandroid.microlearning

import com.example.aigeneratedandroid.microlearning.curation.ContentCurationEngine
import com.example.aigeneratedandroid.microlearning.data.ContentBank
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
        val unknown = ContentBank.all.map { it.topic }.toSet() - profile.topics.toSet()
        assertTrue("Unmatched topics: $unknown", unknown.isEmpty())
        assertEquals(ContentBank.all.size, ContentBank.all.map { it.id }.toSet().size)
    }

    @Test
    fun `feed has the session size, no duplicates, and varied topics`() {
        val feed = ContentCurationEngine(ContentBank.all, InMemoryFeedbackStore()).buildFeed(profile, day)
        assertEquals(profile.cardsPerSession, feed.cards.size)
        assertEquals(feed.cards.size, feed.cards.map { it.id }.toSet().size)
        assertTrue(feed.cards.groupingBy { it.topic }.eachCount().values.all { it <= 2 })
        feed.cards.zipWithNext().forEach { (a, b) -> assertFalse(a.topic == b.topic) }
        assertTrue(feed.theme.isNotBlank())
        assertEquals(feed.cards.first().challenge, feed.challenge)
    }

    @Test
    fun `same day gives the same feed even after marking it shown`() {
        val store = InMemoryFeedbackStore()
        val engine = ContentCurationEngine(ContentBank.all, store)
        val first = engine.buildFeed(profile, day)
        store.markShown(first.cards.map { it.id }, day.toEpochDay())
        assertEquals(first.cards.map { it.id }, engine.buildFeed(profile, day).cards.map { it.id })
    }

    @Test
    fun `recently shown cards are mostly avoided the next day`() {
        val store = InMemoryFeedbackStore()
        val engine = ContentCurationEngine(ContentBank.all, store)
        val first = engine.buildFeed(profile, day)
        store.markShown(first.cards.map { it.id }, day.toEpochDay())
        val second = engine.buildFeed(profile, day.plusDays(1))
        val repeats = second.cards.map { it.id }.intersect(first.cards.map { it.id }.toSet())
        assertTrue("Too many repeats: $repeats", repeats.size <= 1)
    }

    @Test
    fun `heavily disliked cards are never served`() {
        val store = InMemoryFeedbackStore()
        repeat(3) { store.record("sys-01", Reaction.SKIPPED) }
        val engine = ContentCurationEngine(ContentBank.all, store)
        (0L until 30L).forEach { offset ->
            val feed = engine.buildFeed(profile, day.plusDays(offset))
            assertFalse(feed.cards.any { it.id == "sys-01" })
        }
    }

    @Test
    fun `liking a topic makes it show up more over a month`() {
        fun countLeadership(store: InMemoryFeedbackStore): Int {
            val engine = ContentCurationEngine(ContentBank.all, store)
            return (0L until 60L).sumOf { offset ->
                engine.buildFeed(profile, day.plusDays(offset)).cards.count { it.topic == ContentBank.TOPIC_LEADERSHIP }
            }
        }
        val neutral = countLeadership(InMemoryFeedbackStore())
        val fan = InMemoryFeedbackStore().apply {
            ContentBank.all.filter { it.topic == ContentBank.TOPIC_LEADERSHIP }.forEach { card ->
                repeat(3) { record(card.id, Reaction.LIKED) }
            }
        }
        assertTrue("neutral=$neutral, fan=${countLeadership(fan)}", countLeadership(fan) > neutral)
    }

    @Test
    fun `related prefers same author then same topic`() {
        val engine = ContentCurationEngine(ContentBank.all, InMemoryFeedbackStore())
        val crucial = ContentBank.byId("lead-01")!!
        assertEquals("lead-02", engine.related(crucial).first().id)
        assertTrue(engine.related(crucial, exclude = setOf("lead-02")).first().topic == crucial.topic)
    }
}
