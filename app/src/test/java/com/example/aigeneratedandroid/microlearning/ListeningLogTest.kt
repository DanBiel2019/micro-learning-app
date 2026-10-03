package com.example.aigeneratedandroid.microlearning

import com.example.aigeneratedandroid.microlearning.data.ListeningLog
import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningLogTest {

    @Test
    fun `streak counts consecutive days ending today`() {
        assertEquals(3, ListeningLog.streak(setOf(8L, 9L, 10L), today = 10))
    }

    @Test
    fun `streak survives until the day after the last listen`() {
        assertEquals(2, ListeningLog.streak(setOf(8L, 9L), today = 10))
    }

    @Test
    fun `a missed day breaks the streak`() {
        assertEquals(0, ListeningLog.streak(setOf(7L, 8L), today = 10))
        assertEquals(1, ListeningLog.streak(setOf(7L, 10L), today = 10))
    }
}
