package com.zoran.aihomedesigner.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.data.ImportedImage
import com.zoran.aihomedesigner.domain.Calibration
import com.zoran.aihomedesigner.domain.CalibrationResult
import kotlin.math.min
import kotlin.math.roundToInt

private val PointAColor = Color(0xFFD32F2F)
private val PointBColor = Color(0xFF1976D2)

/**
 * Ekran za kalibraciju razmere slike plana: korisnik obeleži dve tačke i upiše njihovo pravo rastojanje.
 * Koordinate tačaka čuvaju se u pikselima prave slike, bez obzira na zum.
 */
@Composable
fun CalibrationScreen(
    image: ImportedImage,
    loadBitmap: suspend (String, Int) -> Bitmap?,
    onSave: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, image.fileName) {
        value = loadBitmap(image.fileName, 2048)?.asImageBitmap()
    }
    val imageW = image.widthPx.toFloat()
    val imageH = image.heightPx.toFloat()

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var fitScale by remember { mutableFloatStateOf(1f) }
    var topLeft by remember { mutableStateOf(Offset.Zero) }
    var initialised by remember { mutableStateOf(false) }

    var pointA by remember { mutableStateOf<Offset?>(null) }
    var pointB by remember { mutableStateOf<Offset?>(null) }
    var distanceText by rememberSaveable { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }

    // Slika se na početku uklapa u prostor za crtanje
    LaunchedEffect(canvasSize, bitmap) {
        if (!initialised && canvasSize.width > 0 && canvasSize.height > 0 && bitmap != null) {
            val fit = min(canvasSize.width / imageW, canvasSize.height / imageH)
            fitScale = fit
            scale = fit
            topLeft = Offset((canvasSize.width - imageW * fit) / 2f, (canvasSize.height - imageH * fit) / 2f)
            initialised = true
        }
    }

    val a = pointA
    val b = pointB
    val live: CalibrationResult? = if (a != null && b != null) {
        Calibration.calibrate(
            a.x.toDouble(), a.y.toDouble(), b.x.toDouble(), b.y.toDouble(),
            distanceText, image.widthPx, image.heightPx
        )
    } else {
        null
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                Text(
                    text = stringResource(R.string.calibration_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    pointA = null
                    pointB = null
                    showErrors = false
                }) { Text(stringResource(R.string.calibration_clear)) }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(fitScale * 0.5f, fitScale * 12f)
                            val factor = newScale / scale
                            topLeft = centroid - (centroid - topLeft) * factor + pan
                            scale = newScale
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            val px = (tap.x - topLeft.x) / scale
                            val py = (tap.y - topLeft.y) / scale
                            if (px in 0f..imageW && py in 0f..imageH) {
                                val point = Offset(px, py)
                                when {
                                    pointA == null -> pointA = point
                                    pointB == null -> pointB = point
                                    else -> {
                                        pointA = point
                                        pointB = null
                                    }
                                }
                                showErrors = false
                            }
                        }
                    }
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                        dstSize = IntSize((imageW * scale).roundToInt(), (imageH * scale).roundToInt())
                    )
                }
                fun toScreen(p: Offset) = Offset(topLeft.x + p.x * scale, topLeft.y + p.y * scale)
                val sa = a?.let { toScreen(it) }
                val sb = b?.let { toScreen(it) }
                if (sa != null && sb != null) {
                    drawLine(Color(0xFFFFEB3B), sa, sb, strokeWidth = 3f)
                }
                if (sa != null) {
                    drawCircle(PointAColor, radius = 12f, center = sa, style = Stroke(width = 4f))
                    drawCircle(PointAColor, radius = 3f, center = sa)
                }
                if (sb != null) {
                    drawCircle(PointBColor, radius = 12f, center = sb, style = Stroke(width = 4f))
                    drawCircle(PointBColor, radius = 3f, center = sb)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (bitmap == null) {
                    Text(
                        text = stringResource(R.string.calibration_load_failed),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = stringResource(R.string.calibration_instructions),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = {
                        distanceText = it
                        showErrors = false
                    },
                    label = { Text(stringResource(R.string.calibration_distance_label)) },
                    singleLine = true,
                    supportingText = {
                        val invalid = live as? CalibrationResult.Invalid
                        val error = if (showErrors) invalid?.distanceError else null
                        Text(
                            if (error != null) {
                                dimensionErrorText(
                                    error,
                                    Calibration.MIN_KNOWN_DISTANCE_M,
                                    Calibration.MAX_KNOWN_DISTANCE_M
                                )
                            } else {
                                stringResource(R.string.calibration_distance_hint)
                            }
                        )
                    },
                    isError = showErrors && (live as? CalibrationResult.Invalid)?.distanceError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                when {
                    live is CalibrationResult.Ok -> Text(
                        text = stringResource(
                            R.string.calibration_preview,
                            formatMeters(image.widthPx * live.metersPerPixel),
                            formatMeters(image.heightPx * live.metersPerPixel)
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    showErrors && a == null || showErrors && b == null -> Text(
                        text = stringResource(R.string.calibration_need_points),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    showErrors && live is CalibrationResult.Invalid && live.pointsTooClose -> Text(
                        text = stringResource(R.string.calibration_points_too_close),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    showErrors && live is CalibrationResult.Invalid && live.implausibleScale -> Text(
                        text = stringResource(R.string.calibration_implausible),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = stringResource(R.string.calibration_accuracy_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Button(
                    onClick = {
                        if (live is CalibrationResult.Ok) {
                            onSave(live.metersPerPixel)
                        } else {
                            showErrors = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.calibration_save)) }
            }
        }
    }
}
