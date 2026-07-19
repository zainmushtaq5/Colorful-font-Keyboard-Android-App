package com.example.colortextstudio

import com.example.colortextstudio.data.ColorSpan
import com.example.colortextstudio.data.SpanOps
import org.junit.Assert.assertEquals
import org.junit.Test

class SpanOpsTest {

    @Test
    fun `normalize fills gaps with default color`() {
        val spans = listOf(ColorSpan(2, 5, 0xFFFF0000))
        val result = SpanOps.normalize(spans, 10)
        assertEquals(0, result.first().start)
        assertEquals(10, result.last().end)
    }

    @Test
    fun `applyColor splits existing span`() {
        val spans = listOf(ColorSpan(0, 10, 0xFF000000))
        val result = SpanOps.applyColor(spans, 10, 3, 6, 0xFFFF0000)
        assertEquals(3, result.size)
        assertEquals(0xFFFF0000, result[1].colorArgb)
        assertEquals(3, result[1].start)
        assertEquals(6, result[1].end)
    }

    @Test
    fun `applyColor merges adjacent same color`() {
        val spans = listOf(ColorSpan(0, 5, 0xFFFF0000), ColorSpan(5, 10, 0xFF000000))
        val result = SpanOps.applyColor(spans, 10, 5, 10, 0xFFFF0000)
        assertEquals(1, result.size)
        assertEquals(0, result[0].start)
        assertEquals(10, result[0].end)
    }

    @Test
    fun `onInsert shifts spans after insertion point`() {
        val spans = listOf(ColorSpan(0, 5, 0xFFFF0000))
        val result = SpanOps.onInsert(spans, 5, 3, 0xFF000000)
        assertEquals(0, result[0].start)
        assertEquals(5, result[0].end)
    }

    @Test
    fun `onDelete trims overlapping spans`() {
        val spans = listOf(ColorSpan(0, 5, 0xFFFF0000), ColorSpan(5, 10, 0xFF0000FF))
        val result = SpanOps.onDelete(spans, 3, 4)
        assertEquals(2, result.size)
        assertEquals(3, result[0].end)
        assertEquals(3, result[1].start)
    }
}
