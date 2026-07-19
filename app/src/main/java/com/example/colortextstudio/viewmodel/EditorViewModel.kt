package com.example.colortextstudio.viewmodel

import android.graphics.Bitmap
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.colortextstudio.data.ColorSpan
import com.example.colortextstudio.data.CreationRepository
import com.example.colortextstudio.data.DraftRepository
import com.example.colortextstudio.data.SpanOps
import com.example.colortextstudio.data.StyleSpan
import com.example.colortextstudio.util.ImageExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val text: String = "",
    val spans: List<ColorSpan> = emptyList(),
    val styleSpans: List<StyleSpan> = emptyList(),
    val selection: TextRange = TextRange.Zero,
    val pendingColor: Long? = null, // color to apply to next typed char if no selection
    val fontSizeSp: Float = 32f,
    val fontFamily: FontFamily = FontFamily.Default,
    val textAlign: TextAlign = TextAlign.Left,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val exportMessage: String? = null
)

private const val MAX_HISTORY = 20

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val draftRepository: DraftRepository,
    private val creationRepository: CreationRepository,
    private val imageExporter: ImageExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // History stacks store (text, spans) snapshots.
    private val undoStack = ArrayDeque<Pair<String, List<ColorSpan>>>()
    private val redoStack = ArrayDeque<Pair<String, List<ColorSpan>>>()

    val creations = creationRepository.observeAll()

    init {
        viewModelScope.launch {
            draftRepository.load()?.let { draft ->
                _uiState.value = _uiState.value.copy(
                    text = draft.text,
                    spans = SpanOps.normalize(draft.spans, draft.text.length)
                )
            }
        }
    }

    private fun pushHistory() {
        val s = _uiState.value
        undoStack.addLast(s.text to s.spans)
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        updateHistoryFlags()
    }

    private fun updateHistoryFlags() {
        _uiState.value = _uiState.value.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())
    }

    fun undo() {
        val current = _uiState.value
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(current.text to current.spans)
        _uiState.value = current.copy(text = prev.first, spans = prev.second)
        updateHistoryFlags()
        persistDraft()
    }

    fun redo() {
        val current = _uiState.value
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(current.text to current.spans)
        _uiState.value = current.copy(text = next.first, spans = next.second)
        updateHistoryFlags()
        persistDraft()
    }

    /** Called from the BasicTextField's onValueChange, comparing old vs new text/selection. */
    fun onTextChange(newText: String, newSelection: TextRange) {
        val old = _uiState.value
        if (newText == old.text) {
            _uiState.value = old.copy(selection = newSelection)
            return
        }
        pushHistory()

        val oldText = old.text
        var spans = old.spans

        // Simple diff: find common prefix/suffix to locate the edited region.
        val prefixLen = commonPrefixLen(oldText, newText)
        val oldSuffixLen = commonSuffixLen(oldText, newText, prefixLen)
        val newSuffixLen = oldSuffixLen
        val deleteStart = prefixLen
        val deleteCount = (oldText.length - newSuffixLen) - prefixLen
        val insertCount = (newText.length - newSuffixLen) - prefixLen

        if (deleteCount > 0) {
            spans = SpanOps.onDelete(spans, deleteStart, deleteCount)
        }
        if (insertCount > 0) {
            val colorForInsert = old.pendingColor ?: colorAt(spans, deleteStart) ?: 0xFF000000
            spans = SpanOps.onInsert(spans, deleteStart, insertCount, colorForInsert)
            spans = SpanOps.applyColor(spans, newText.length, deleteStart, deleteStart + insertCount, colorForInsert)
        }
        spans = SpanOps.normalize(spans, newText.length)

        _uiState.value = old.copy(
            text = newText,
            spans = spans,
            selection = newSelection,
            pendingColor = if (insertCount > 0) null else old.pendingColor
        )
        persistDraft()
    }

    fun onSelectionChange(selection: TextRange) {
        _uiState.value = _uiState.value.copy(selection = selection)
    }

    /** Apply [colorArgb] to current selection, or set it as the pending color for next input. */
    fun applyColor(colorArgb: Long) {
        val s = _uiState.value
        if (!s.selection.collapsed) {
            pushHistory()
            val newSpans = SpanOps.applyColor(
                s.spans, s.text.length,
                s.selection.min, s.selection.max, colorArgb
            )
            _uiState.value = s.copy(spans = newSpans)
            persistDraft()
        } else {
            _uiState.value = s.copy(pendingColor = colorArgb)
        }
    }

    fun toggleBold() = toggleStyle(bold = true)
    fun toggleItalic() = toggleStyle(bold = false)

    private fun toggleStyle(bold: Boolean) {
        val s = _uiState.value
        if (s.selection.collapsed) return
        val start = s.selection.min
        val end = s.selection.max
        val existing = s.styleSpans.filter { it.start < end && it.end > start }
        val currentlyOn = existing.any { if (bold) it.bold else it.italic }
        val remaining = s.styleSpans.filterNot { it.start < end && it.end > start }
        val updated = if (currentlyOn) {
            remaining
        } else {
            remaining + StyleSpan(start, end, bold = bold, italic = !bold)
        }
        _uiState.value = s.copy(styleSpans = updated)
    }

    fun setFontSize(sp: Float) {
        _uiState.value = _uiState.value.copy(fontSizeSp = sp)
    }

    fun setFontFamily(family: FontFamily) {
        _uiState.value = _uiState.value.copy(fontFamily = family)
    }

    fun setTextAlign(align: TextAlign) {
        _uiState.value = _uiState.value.copy(textAlign = align)
    }

    fun clearAll() {
        pushHistory()
        _uiState.value = _uiState.value.copy(text = "", spans = emptyList(), selection = TextRange.Zero, pendingColor = null)
        persistDraft()
    }

    private fun persistDraft() {
        val s = _uiState.value
        viewModelScope.launch { draftRepository.save(s.text, s.spans) }
    }

    private fun colorAt(spans: List<ColorSpan>, index: Int): Long? =
        spans.firstOrNull { index in it.start until it.end }?.colorArgb
            ?: spans.firstOrNull { index == it.end }?.colorArgb

    private fun commonPrefixLen(a: String, b: String): Int {
        val max = minOf(a.length, b.length)
        var i = 0
        while (i < max && a[i] == b[i]) i++
        return i
    }

    private fun commonSuffixLen(a: String, b: String, prefixLimit: Int): Int {
        var i = 0
        val maxA = a.length - prefixLimit
        val maxB = b.length - prefixLimit
        val max = minOf(maxA, maxB)
        while (i < max && a[a.length - 1 - i] == b[b.length - 1 - i]) i++
        return i
    }

    // ---- Export ----

    fun exportImage(bitmap: Bitmap, alsoShare: Boolean, alsoCopy: Boolean, saveToGallery: Boolean, addToCreations: Boolean) {
        viewModelScope.launch {
            try {
                val fileName = "colortext_${System.currentTimeMillis()}.png"
                if (saveToGallery) {
                    imageExporter.saveToGallery(bitmap, fileName)
                }
                if (alsoShare || alsoCopy) {
                    val uri = imageExporter.cacheAndGetContentUri(bitmap, fileName)
                    if (alsoShare) imageExporter.shareImage(uri)
                    if (alsoCopy) imageExporter.copyToClipboard(uri)
                }
                if (addToCreations) {
                    val thumbUri = imageExporter.saveThumbnail(bitmap, "thumb_$fileName")
                    creationRepository.add(thumbUri)
                }
                _uiState.value = _uiState.value.copy(exportMessage = "success")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(exportMessage = "failure")
            }
        }
    }

    fun consumeExportMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }
}
