package com.example.colortextstudio.data

/**
 * Represents a color applied to a half-open character range [start, end) of the text.
 * The list of spans held by the ViewModel always fully (and non-overlappingly) covers
 * [0, text.length) once normalized via [SpanOps.normalize].
 */
data class ColorSpan(
    val start: Int,
    val end: Int,
    val colorArgb: Long
) {
    val range: IntRange get() = start until end
}

/** Optional secondary styling (bold/italic) tracked alongside color spans, per range. */
data class StyleSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false
)

/**
 * Pure functions for maintaining a non-overlapping, fully-covering list of [ColorSpan]s.
 * Kept free of Android/Compose types so it is trivially unit-testable.
 */
object SpanOps {

    /** Returns a list of spans covering [0, length) — splitting/merging as needed — after
     * applying [colorArgb] to [applyStart, applyEnd). */
    fun applyColor(
        spans: List<ColorSpan>,
        length: Int,
        applyStart: Int,
        applyEnd: Int,
        colorArgb: Long
    ): List<ColorSpan> {
        if (applyStart >= applyEnd || length <= 0) return normalize(spans, length)
        val base = normalize(spans, length)
        val result = mutableListOf<ColorSpan>()
        for (s in base) {
            // Portion before the applied range
            if (s.start < applyStart) {
                result.add(s.copy(end = minOf(s.end, applyStart)))
            }
            // Portion after the applied range
            if (s.end > applyEnd) {
                result.add(s.copy(start = maxOf(s.start, applyEnd)))
            }
        }
        result.add(ColorSpan(applyStart, applyEnd, colorArgb))
        return mergeAdjacentSameColor(result.filter { it.start < it.end }.sortedBy { it.start })
    }

    /** Ensures spans fully cover [0, length) with no gaps/overlaps, filling gaps with
     * [defaultColor] (black) and clipping/dropping anything out of bounds. */
    fun normalize(spans: List<ColorSpan>, length: Int, defaultColor: Long = 0xFF000000): List<ColorSpan> {
        if (length <= 0) return emptyList()
        val clipped = spans
            .map { it.copy(start = it.start.coerceIn(0, length), end = it.end.coerceIn(0, length)) }
            .filter { it.start < it.end }
            .sortedBy { it.start }

        val filled = mutableListOf<ColorSpan>()
        var cursor = 0
        for (s in clipped) {
            if (s.start > cursor) filled.add(ColorSpan(cursor, s.start, defaultColor))
            if (s.start >= cursor) {
                filled.add(s)
                cursor = s.end
            }
        }
        if (cursor < length) filled.add(ColorSpan(cursor, length, defaultColor))
        return mergeAdjacentSameColor(filled)
    }

    private fun mergeAdjacentSameColor(spans: List<ColorSpan>): List<ColorSpan> {
        if (spans.isEmpty()) return spans
        val merged = mutableListOf(spans.first())
        for (i in 1 until spans.size) {
            val last = merged.last()
            val cur = spans[i]
            if (last.end == cur.start && last.colorArgb == cur.colorArgb) {
                merged[merged.lastIndex] = last.copy(end = cur.end)
            } else {
                merged.add(cur)
            }
        }
        return merged
    }

    /** Shifts/trims spans after a text insertion of [count] chars at [at]. */
    fun onInsert(spans: List<ColorSpan>, at: Int, count: Int, insertedColor: Long): List<ColorSpan> {
        if (count <= 0) return spans
        val shifted = spans.map { s ->
            when {
                s.end <= at -> s
                s.start >= at -> s.copy(start = s.start + count, end = s.end + count)
                else -> s.copy(end = s.end + count) // insertion happens inside this span
            }
        }
        return shifted // caller calls normalize() after mutating text length + applying insertedColor via applyColor
    }

    /** Shifts/trims spans after deleting [count] chars starting at [at]. */
    fun onDelete(spans: List<ColorSpan>, at: Int, count: Int): List<ColorSpan> {
        if (count <= 0) return spans
        val delEnd = at + count
        return spans.mapNotNull { s ->
            when {
                s.end <= at -> s
                s.start >= delEnd -> s.copy(start = s.start - count, end = s.end - count)
                else -> {
                    val newStart = s.start.coerceAtMost(at)
                    val overlapInSpan = (minOf(s.end, delEnd) - maxOf(s.start, at)).coerceAtLeast(0)
                    val newEnd = s.end - overlapInSpan
                    if (newStart < newEnd) s.copy(start = newStart, end = newEnd) else null
                }
            }
        }
    }
}
