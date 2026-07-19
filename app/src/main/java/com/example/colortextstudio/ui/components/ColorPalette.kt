package com.example.colortextstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.colortextstudio.R
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.roundToInt

val PRESET_COLOR_LONGS = listOf(
    0xFF000000L, 0xFFFFFFFFL, 0xFFF44336L, 0xFFE91E63L, 0xFF9C27B0L, 0xFF3F51B5L,
    0xFF2196F3L, 0xFF03A9F4L, 0xFF009688L, 0xFF4CAF50L, 0xFFFFEB3BL, 0xFFFF9800L
)
val PRESET_COLORS = PRESET_COLOR_LONGS.map { Color(it) }

@Composable
fun ColorPaletteRow(
    selectedColor: Color?,
    onColorSelected: (Long) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(PRESET_COLOR_LONGS) { colorLong ->
            val color = Color(colorLong)
            Swatch(color = color, selected = color == selectedColor) { onColorSelected(colorLong) }
        }
        item {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush())
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onCustomClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.custom_color))
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun Brush(): androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.sweepGradient(
        listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
    )

@Composable
fun CustomColorDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initialHsv = remember {
        FloatArray(3).also {
            android.graphics.Color.RGBToHSV(
                (initialColor.red * 255).toInt(), (initialColor.green * 255).toInt(), (initialColor.blue * 255).toInt(), it
            )
        }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember { mutableStateOf(String.format("#%06X", 0xFFFFFF and initialColor.toArgb())) }

    val currentColor = remember(hue, saturation, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_color)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HsvWheel(
                    hue = hue,
                    saturation = saturation,
                    onHueSat = { h, s -> hue = h; saturation = s }
                )
                Spacer(Modifier.height(12.dp))
                Slider(value = value, onValueChange = { value = it })
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { text ->
                        hexText = text
                        val parsed = parseHex(text)
                        if (parsed != null) {
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed, hsv)
                            hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                        }
                    },
                    label = { Text(stringResource(R.string.hex_input_hint)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentColor.toArgb().toLong() and 0xFFFFFFFFL) }) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun parseHex(text: String): Int? = try {
    val clean = text.removePrefix("#")
    if (clean.length == 6) android.graphics.Color.parseColor("#$clean") else null
} catch (e: IllegalArgumentException) {
    null
}

@Composable
private fun HsvWheel(
    hue: Float,
    saturation: Float,
    onHueSat: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val diameterDp = 220.dp
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .size(diameterDp)
            .pointerInput(Unit) {
                detectTapGestures { offset -> updateFromOffset(offset, size.width, size.height, onHueSat) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> updateFromOffset(change.position, size.width, size.height, onHueSat) }
            }
    ) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val steps = 360
        for (i in 0 until steps) {
            val angle = i * (360f / steps)
            drawArc(
                color = Color(android.graphics.Color.HSVToColor(floatArrayOf(angle, 1f, 1f))),
                startAngle = angle,
                sweepAngle = 360f / steps + 1f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }
        // Desaturate toward white at center
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        // Selection marker
        val markerAngleRad = Math.toRadians(hue.toDouble())
        val markerRadius = radius * saturation
        val markerOffset = Offset(
            (center.x + markerRadius * kotlin.math.cos(markerAngleRad)).toFloat(),
            (center.y + markerRadius * kotlin.math.sin(markerAngleRad)).toFloat()
        )
        drawCircle(color = Color.Black, radius = 6.dp.toPx(), center = markerOffset, style = Stroke(width = 2.dp.toPx()))
    }
}

private fun updateFromOffset(offset: Offset, width: Int, height: Int, onHueSat: (Float, Float) -> Unit) {
    val center = Offset(width / 2f, height / 2f)
    val radius = min(width, height) / 2f
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
    var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    if (angle < 0) angle += 360f
    onHueSat(angle, (dist / radius).coerceIn(0f, 1f))
}
