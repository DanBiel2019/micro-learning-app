package com.example.aigeneratedandroid.microlearning

import com.example.aigeneratedandroid.microlearning.model.Visual
import com.example.aigeneratedandroid.microlearning.model.VisualItem
import com.example.aigeneratedandroid.microlearning.model.VisualKinds
import com.example.aigeneratedandroid.microlearning.ui.infographic.GallerySamples
import com.example.aigeneratedandroid.microlearning.ui.infographic.describe
import com.example.aigeneratedandroid.microlearning.ui.infographic.resolveKind
import com.example.aigeneratedandroid.microlearning.ui.infographic.waffleShares
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every visual the app ships or demos uses a kind the renderer knows, in a shape that fits it. */
class InfographicSpecTest {

    private val known = VisualKinds.ALL.toSet()

    @Test
    fun `every library visual uses a known kind and draws as that kind`() {
        TestData.library.forEach { seg ->
            val v = seg.visual!!
            assertTrue("${seg.id}: unknown kind ${v.kind}", v.kind in known)
            // A library visual should never need the fallback: its shape must fit its kind.
            val drawn = resolveKind(v)
            assertTrue("${seg.id}: ${v.kind} falls back to $drawn", drawn == v.kind || (v.kind == "stats" && drawn == "big_number"))
            assertTrue("${seg.id}: at most one punchline", v.items.count { it.emphasis } <= 1)
        }
    }

    @Test
    fun `gallery has one sample per kind and each draws as its own kind`() {
        val kinds = GallerySamples.guides.map { it.kind }
        assertEquals("one guide per kind", VisualKinds.ALL.sorted(), kinds.sorted())
        GallerySamples.guides.forEach { g ->
            assertTrue(g.kind in known)
            assertEquals(g.kind, g.sample.kind)
            assertEquals("${g.kind} sample falls back", g.kind, resolveKind(g.sample))
            assertTrue(g.whenToUse.isNotBlank())
            assertTrue(describe(g.sample).isNotBlank())
        }
    }

    @Test
    fun `gallery fallbacks degrade to a known kind`() {
        GallerySamples.fallbacks.forEach { (note, v) ->
            val drawn = resolveKind(v)
            assertTrue(note, drawn in known)
            assertNotEquals(note, v.kind, drawn)
        }
    }

    @Test
    fun `unknown and malformed specs never crash the rules`() {
        val empty = Visual("mystery", "", emptyList())
        assertEquals("flow", resolveKind(empty))
        assertTrue(describe(empty).isEmpty() || describe(empty).isNotBlank())
        assertEquals("compare", resolveKind(Visual("matrix", "t", List(3) { VisualItem("x") })))
        assertEquals("compare", resolveKind(Visual("before_after", "t", List(3) { VisualItem("x") })))
        assertEquals("quote", resolveKind(Visual("big_number", "t", listOf(VisualItem("no number")))))
    }

    @Test
    fun `waffle shares accept percents, fractions and labels and never exceed 100`() {
        assertEquals(listOf(70, 30), waffleShares(listOf(VisualItem("a", value = 70.0), VisualItem("b", value = 30.0))).map { it.second })
        assertEquals(listOf(25), waffleShares(listOf(VisualItem("a", value = 0.25))).map { it.second })
        assertEquals(listOf(40), waffleShares(listOf(VisualItem("a", valueLabel = "~40%"))).map { it.second })
        assertTrue(waffleShares(listOf(VisualItem("a", value = 80.0), VisualItem("b", value = 80.0))).sumOf { it.second } <= 100)
    }

    @Test
    fun `pipeline schema lists the same kinds as the app`() {
        val schema = File("../pipeline/schema.py")
        if (!schema.exists()) return
        val text = schema.readText()
        val block = Regex("""VisualKind = Literal\[(.*?)]""", RegexOption.DOT_MATCHES_ALL).find(text)!!.groupValues[1]
        val kinds = Regex("\"([a-z_]+)\"").findAll(block).map { it.groupValues[1] }.toList()
        assertEquals(VisualKinds.ALL, kinds)
    }
}
