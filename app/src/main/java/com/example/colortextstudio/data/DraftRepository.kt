package com.example.colortextstudio.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.draftDataStore by preferencesDataStore(name = "draft_store")

data class DraftState(
    val text: String,
    val spans: List<ColorSpan>
)

/**
 * Persists the current draft (text + color spans) using a small manual encoding to avoid
 * pulling in a JSON library.
 *   payload = text + <SEP><SEP> + "start<F>end<F>colorArgb" records joined by <SEP>
 */
@Singleton
class DraftRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private val key = stringPreferencesKey("draft_v1")
    private val recordSep = "\u0001"
    private val fieldSep = "\u0002"

    suspend fun save(text: String, spans: List<ColorSpan>) {
        val spansPart = spans.joinToString(recordSep) { "${it.start}$fieldSep${it.end}$fieldSep${it.colorArgb}" }
        val payload = text + recordSep + recordSep + spansPart
        context.draftDataStore.edit { prefs -> prefs[key] = payload }
    }

    suspend fun load(): DraftState? {
        val raw = context.draftDataStore.data.first()[key] ?: return null
        val boundary = raw.indexOf(recordSep + recordSep)
        if (boundary < 0) return null
        val text = raw.substring(0, boundary)
        val spansRaw = raw.substring(boundary + 2)
        val spans = if (spansRaw.isBlank()) emptyList() else spansRaw.split(recordSep).mapNotNull { rec ->
            val parts = rec.split(fieldSep)
            if (parts.size != 3) return@mapNotNull null
            try {
                ColorSpan(parts[0].toInt(), parts[1].toInt(), parts[2].toLong())
            } catch (e: NumberFormatException) {
                null
            }
        }
        return DraftState(text, spans)
    }

    suspend fun clear() {
        context.draftDataStore.edit { prefs -> prefs.remove(key) }
    }
}
