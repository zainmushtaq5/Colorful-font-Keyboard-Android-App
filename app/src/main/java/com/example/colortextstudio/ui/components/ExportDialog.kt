package com.example.colortextstudio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.colortextstudio.R

data class ExportOptions(
    val transparentBackground: Boolean,
    val scale: Int, // 1, 2, or 3
    val share: Boolean,
    val copy: Boolean,
    val addToCreations: Boolean
)

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExportOptions) -> Unit
) {
    var transparent by remember { mutableStateOf(false) }
    var scale by remember { mutableIntStateOf(2) }
    var share by remember { mutableStateOf(false) }
    var copy by remember { mutableStateOf(false) }
    var addToCreations by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_png)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = transparent, onCheckedChange = { transparent = it })
                    Text(stringResource(R.string.transparent_background))
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.resolution_scale))
                Row {
                    listOf(1, 2, 3).forEach { s ->
                        FilterChip(
                            selected = scale == s,
                            onClick = { scale = s },
                            label = { Text("${s}x") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = share, onCheckedChange = { share = it })
                    Text(stringResource(R.string.share))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = copy, onCheckedChange = { copy = it })
                    Text(stringResource(R.string.copy_image))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = addToCreations, onCheckedChange = { addToCreations = it })
                    Text(stringResource(R.string.my_creations))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(ExportOptions(transparent, scale, share, copy, addToCreations))
            }) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
