package com.example.colortextstudio.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.colortextstudio.R
import com.example.colortextstudio.ui.components.ColorPaletteRow
import com.example.colortextstudio.ui.components.CustomColorDialog
import com.example.colortextstudio.ui.components.EditorToolbar
import com.example.colortextstudio.ui.components.ExportDialog
import com.example.colortextstudio.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun EditorScreen(
    onOpenGallery: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    var showCustomColorDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportBackground by remember { mutableStateOf(Color.White) }

    LaunchedEffect(state.exportMessage) {
        when (state.exportMessage) {
            "success" -> Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
            "failure" -> Toast.makeText(context, context.getString(R.string.export_failure), Toast.LENGTH_SHORT).show()
        }
        if (state.exportMessage != null) viewModel.consumeExportMessage()
    }

    val annotatedText = remember(state.text, state.spans, state.styleSpans) {
        buildAnnotated(state.text, state.spans, state.styleSpans)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenGallery) {
                        Icon(Icons.Filled.CollectionsBookmark, contentDescription = stringResource(R.string.my_creations))
                    }
                    IconButton(onClick = { showExportDialog = true }, enabled = state.text.isNotEmpty()) {
                        Icon(Icons.Filled.IosShare, contentDescription = stringResource(R.string.export_png))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            EditorToolbar(
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                fontSizeSp = state.fontSizeSp,
                fontFamily = state.fontFamily,
                textAlign = state.textAlign,
                hasSelection = !state.selection.collapsed,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onClearAll = viewModel::clearAll,
                onFontSizeChange = viewModel::setFontSize,
                onFontFamilyChange = viewModel::setFontFamily,
                onTextAlignChange = viewModel::setTextAlign,
                onBoldToggle = viewModel::toggleBold,
                onItalicToggle = viewModel::toggleItalic
            )

            HorizontalDivider()

            ColorPaletteRow(
                selectedColor = state.pendingColor?.let { Color(it) },
                onColorSelected = viewModel::applyColor,
                onCustomClick = { showCustomColorDialog = true }
            )

            HorizontalDivider()

            // The captured region: constrained box we render to a GraphicsLayer for export.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(exportBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithLayer(graphicsLayer)
                ) {
                    if (state.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.editor_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = state.fontSizeSp.sp
                        )
                    }
                    BasicTextField(
                        value = TextFieldValue(state.text, state.selection),
                        onValueChange = { tfv -> viewModel.onTextChange(tfv.text, tfv.selection) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = state.fontSizeSp.sp,
                            fontFamily = state.fontFamily,
                            textAlign = state.textAlign
                        ),
                        decorationBox = { inner ->
                            Text(text = annotatedText, fontSize = state.fontSizeSp.sp, fontFamily = state.fontFamily, textAlign = state.textAlign)
                            Box(modifier = Modifier.matchParentSize()) { inner() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showCustomColorDialog) {
        CustomColorDialog(
            initialColor = state.pendingColor?.let { Color(it) } ?: Color.Black,
            onDismiss = { showCustomColorDialog = false },
            onConfirm = { colorLong ->
                viewModel.applyColor(colorLong)
                showCustomColorDialog = false
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = { options ->
                exportBackground = if (options.transparentBackground) Color.Transparent else Color.White
                showExportDialog = false
                scope.launch {
                    // allow recomposition with new background before capture
                    kotlinx.coroutines.delay(50)
                    val bitmap: Bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    val scaled = if (options.scale == 1) bitmap else Bitmap.createScaledBitmap(
                        bitmap, bitmap.width * options.scale, bitmap.height * options.scale, true
                    )
                    viewModel.exportImage(
                        bitmap = scaled,
                        alsoShare = options.share,
                        alsoCopy = options.copy,
                        saveToGallery = true,
                        addToCreations = options.addToCreations
                    )
                }
            }
        )
    }
}

/** Records draw operations into [layer] so it can be rasterized later via layer.toImageBitmap(). */
private fun Modifier.drawWithLayer(layer: GraphicsLayer): Modifier = this.then(
    Modifier.drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }
)

private fun buildAnnotated(
    text: String,
    spans: List<com.example.colortextstudio.data.ColorSpan>,
    styleSpans: List<com.example.colortextstudio.data.StyleSpan>
): AnnotatedString = AnnotatedString.Builder(text).apply {
    spans.forEach { s ->
        addStyle(SpanStyle(color = Color(s.colorArgb)), s.start, s.end)
    }
    styleSpans.forEach { s ->
        addStyle(
            SpanStyle(
                fontWeight = if (s.bold) FontWeight.Bold else null,
                fontStyle = if (s.italic) FontStyle.Italic else null
            ),
            s.start, s.end
        )
    }
}.toAnnotatedString()