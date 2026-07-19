package com.example.colortextstudio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.colortextstudio.R

data class FontOption(val label: String, val family: FontFamily)

val FONT_OPTIONS = listOf(
    FontOption("Default", FontFamily.Default),
    FontOption("Serif", FontFamily.Serif),
    FontOption("Sans", FontFamily.SansSerif),
    FontOption("Monospace", FontFamily.Monospace),
    FontOption("Cursive", FontFamily.Cursive)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    fontSizeSp: Float,
    fontFamily: FontFamily,
    textAlign: TextAlign,
    hasSelection: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearAll: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (FontFamily) -> Unit,
    onTextAlignChange: (TextAlign) -> Unit,
    onBoldToggle: () -> Unit,
    onItalicToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fontMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(Icons.Filled.Undo, contentDescription = stringResource(R.string.undo))
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(Icons.Filled.Redo, contentDescription = stringResource(R.string.redo))
                }
                IconButton(onClick = onBoldToggle, enabled = hasSelection) {
                    Icon(Icons.Filled.FormatBold, contentDescription = stringResource(R.string.bold))
                }
                IconButton(onClick = onItalicToggle, enabled = hasSelection) {
                    Icon(Icons.Filled.FormatItalic, contentDescription = stringResource(R.string.italic))
                }
            }
            Row {
                IconButton(onClick = { onTextAlignChange(TextAlign.Left) }) {
                    Icon(Icons.Filled.FormatAlignLeft, contentDescription = stringResource(R.string.align_left))
                }
                IconButton(onClick = { onTextAlignChange(TextAlign.Center) }) {
                    Icon(Icons.Filled.FormatAlignCenter, contentDescription = stringResource(R.string.align_center))
                }
                IconButton(onClick = { onTextAlignChange(TextAlign.Right) }) {
                    Icon(Icons.Filled.FormatAlignRight, contentDescription = stringResource(R.string.align_right))
                }
            }
            IconButton(onClick = onClearAll) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.clear_all))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.font_size), style = MaterialTheme.typography.labelMedium)
            Slider(
                value = fontSizeSp,
                onValueChange = onFontSizeChange,
                valueRange = 16f..96f,
                modifier = Modifier.weight(1f)
            )
            Text("${fontSizeSp.toInt()}sp", style = MaterialTheme.typography.labelMedium)
        }

        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinedButton(onClick = { fontMenuExpanded = true }) {
                Text(FONT_OPTIONS.firstOrNull { it.family == fontFamily }?.label ?: stringResource(R.string.font_family))
            }
            DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) {
                FONT_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onFontFamilyChange(option.family)
                            fontMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}
