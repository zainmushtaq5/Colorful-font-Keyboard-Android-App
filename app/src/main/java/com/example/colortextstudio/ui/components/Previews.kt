package com.example.colortextstudio.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
private fun ColorPaletteRowPreview() {
    ColorPaletteRow(selectedColor = Color.Red, onColorSelected = {}, onCustomClick = {})
}

@Preview(showBackground = true)
@Composable
private fun EditorToolbarPreview() {
    EditorToolbar(
        canUndo = true,
        canRedo = false,
        fontSizeSp = 32f,
        fontFamily = FontFamily.Default,
        textAlign = TextAlign.Left,
        hasSelection = true,
        onUndo = {},
        onRedo = {},
        onClearAll = {},
        onFontSizeChange = {},
        onFontFamilyChange = {},
        onTextAlignChange = {},
        onBoldToggle = {},
        onItalicToggle = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun ExportDialogPreview() {
    ExportDialog(onDismiss = {}, onConfirm = {})
}
