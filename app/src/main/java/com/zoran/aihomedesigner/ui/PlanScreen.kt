package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.domain.PlanPoint
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.RoomGeometry
import com.zoran.aihomedesigner.domain.RoomPlacement
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MIN_SCALE = 8f   // najmanje piksela po metru
private const val MAX_SCALE = 600f // najviše piksela po metru
private const val FIT_MAX_SCALE = 300f

/**
 * 2D plan: sobe kao pravougaonici u razmeri.
 * Prikaz se dobija iz istih podataka (Room) koji se čuvaju u bazi, bez posebne kopije.
 * Prevlačenje sobe zaokružuje položaj na 5 cm, a dugmad sa strelicama pomeraju za tačan korak.
 */
@Composable
fun PlanScreen(
    rooms: List<Room>?,
    onBack: () -> Unit,
    onMoveRoom: (String, Double, Double) -> Unit,
    onPlaceUnplaced: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(60f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var fitted by remember { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var dragRoomId by remember { mutableStateOf<String?>(null) }
    var dragPoint by remember { mutableStateOf<PlanPoint?>(null) }
    var stepM by rememberSaveable { mutableStateOf(0.05) }

    val point = dragPoint
    val displayRooms = rooms.orEmpty().filter { it.isPlaced }.map { room ->
        if (room.id == dragRoomId && point != null) {
            room.copy(xM = point.xM, yM = point.yM)
        } else {
            room
        }
    }
    val overlapping = RoomGeometry.overlappingRoomIds(displayRooms)
    val selectedRoom = displayRooms.firstOrNull { it.id == selectedId }

    val currentRooms by rememberUpdatedState(displayRooms)
    val currentOnMove by rememberUpdatedState(onMoveRoom)

    // Sobe koje još nemaju položaj (npr. starije sobe) dobijaju ga pri otvaranju plana.
    LaunchedEffect(rooms) {
        if (rooms != null && rooms.any { !it.isPlaced }) onPlaceUnplaced()
    }

    // Kad stigne novi spisak soba iz baze, prikaz više ne mora da pamti privremeni položaj prevlačenja.
    LaunchedEffect(rooms) {
        dragRoomId = null
        dragPoint = null
    }

    fun applyZoom(centroid: Offset, zoom: Float) {
        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val factor = newScale / scale
        offset = centroid - (centroid - offset) * factor
        scale = newScale
    }

    fun fitToView() {
        val placed = currentRooms
        if (viewSize.width <= 0 || viewSize.height <= 0 || placed.isEmpty()) return
        val minX = placed.minOf { it.xM!! }
        val minY = placed.minOf { it.yM!! }
        val maxX = placed.maxOf { it.xM!! + it.lengthM }
        val maxY = placed.maxOf { it.yM!! + it.widthM }
        val margin = 48f
        val boundsW = max((maxX - minX).toFloat(), 1f)
        val boundsH = max((maxY - minY).toFloat(), 1f)
        val w = viewSize.width.toFloat()
        val h = viewSize.height.toFloat()
        val newScale = min((w - 2 * margin) / boundsW, (h - 2 * margin) / boundsH)
            .coerceIn(MIN_SCALE, FIT_MAX_SCALE)
        scale = newScale
        offset = Offset(
            w / 2f - ((minX + maxX) / 2.0).toFloat() * newScale,
            h / 2f - ((minY + maxY) / 2.0).toFloat() * newScale
        )
    }

    // Prvo prilagođavanje prikaza, tek kad su sve sobe dobile položaj.
    LaunchedEffect(viewSize, rooms, fitted) {
        if (!fitted && viewSize.width > 0 && rooms != null &&
            rooms.isNotEmpty() && rooms.all { it.isPlaced }
        ) {
            fitToView()
            fitted = true
        }
    }

    fun nudge(dx: Double, dy: Double) {
        val room = selectedRoom ?: return
        onMoveRoom(
            room.id,
            RoomPlacement.roundToMm(room.xM!! + dx),
            RoomPlacement.roundToMm(room.yM!! + dy)
        )
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val gridStep = gridStepM(scale)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    applyZoom(Offset(viewSize.width / 2f, viewSize.height / 2f), 1f / 1.4f)
                }) { Text(stringResource(R.string.plan_zoom_out)) }
                TextButton(onClick = {
                    applyZoom(Offset(viewSize.width / 2f, viewSize.height / 2f), 1.4f)
                }) { Text(stringResource(R.string.plan_zoom_in)) }
                TextButton(onClick = { fitToView() }) { Text(stringResource(R.string.plan_fit)) }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .onSizeChanged { viewSize = it }
                    .drawBehind { drawGrid(scale, offset, gridStep, gridColor) }
                    .pointerInput(Unit) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPos = down.position
                            val startOffset = offset
                            val startScale = scale
                            val hit = currentRooms.lastOrNull { room ->
                                val left = startOffset.x + room.xM!! * startScale
                                val top = startOffset.y + room.yM!! * startScale
                                startPos.x >= left && startPos.x <= left + room.lengthM * startScale &&
                                    startPos.y >= top && startPos.y <= top + room.widthM * startScale
                            }
                            val startX = hit?.xM ?: 0.0
                            val startY = hit?.yM ?: 0.0
                            var draggingId: String? = hit?.id
                            var moved = false
                            var multiTouch = false

                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount >= 2) {
                                    if (!multiTouch) {
                                        multiTouch = true
                                        draggingId = null
                                        dragRoomId = null
                                        dragPoint = null
                                    }
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    if (centroid != Offset.Unspecified && zoom != 1f) {
                                        applyZoom(centroid, zoom)
                                    }
                                    offset += pan
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                } else if (!multiTouch) {
                                    val change = event.changes.firstOrNull { it.pressed }
                                        ?: event.changes.first()
                                    val total = change.position - startPos
                                    if (!moved && total.getDistance() > slop) moved = true
                                    if (moved) {
                                        val id = draggingId
                                        if (id != null) {
                                            val nx = RoomPlacement.clampCoordinate(
                                                RoomPlacement.snapToGrid(startX + total.x / startScale)
                                            )
                                            val ny = RoomPlacement.clampCoordinate(
                                                RoomPlacement.snapToGrid(startY + total.y / startScale)
                                            )
                                            dragRoomId = id
                                            dragPoint = PlanPoint(nx, ny)
                                            selectedId = id
                                        } else {
                                            offset = startOffset + total
                                        }
                                        change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            if (!multiTouch) {
                                if (!moved) {
                                    selectedId = hit?.id
                                } else {
                                    val id = draggingId
                                    val p = dragPoint
                                    if (id != null && p != null) {
                                        currentOnMove(id, p.xM, p.yM)
                                    }
                                }
                            }
                        }
                    }
            ) {
                displayRooms.forEach { room ->
                    PlanRoom(
                        room = room,
                        scale = scale,
                        offset = offset,
                        selected = room.id == selectedId,
                        overlapping = room.id in overlapping
                    )
                }

                if (rooms != null && rooms.isEmpty()) {
                    Text(
                        text = stringResource(R.string.plan_empty),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.plan_grid, formatMeters(gridStep.toDouble())),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (overlapping.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.plan_overlap_warning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (selectedRoom != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = selectedRoom.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(
                                    R.string.plan_selected_position,
                                    formatMeters(selectedRoom.xM!!),
                                    formatMeters(selectedRoom.yM!!)
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                NudgeButton("←") { nudge(-stepM, 0.0) }
                                NudgeButton("→") { nudge(stepM, 0.0) }
                                NudgeButton("↑") { nudge(0.0, -stepM) }
                                NudgeButton("↓") { nudge(0.0, stepM) }
                                TextButton(onClick = {
                                    stepM = if (stepM < 0.1) 0.5 else 0.05
                                }) {
                                    Text(
                                        stringResource(
                                            R.string.plan_step,
                                            (stepM * 100).roundToInt().toString()
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.plan_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun NudgeButton(symbol: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = Modifier
            .width(48.dp)
            .padding(end = 4.dp)
    ) {
        Text(symbol)
    }
}

@Composable
private fun PlanRoom(
    room: Room,
    scale: Float,
    offset: Offset,
    selected: Boolean,
    overlapping: Boolean
) {
    val density = LocalDensity.current
    val left = (offset.x + room.xM!! * scale).roundToInt()
    val top = (offset.y + room.yM!! * scale).roundToInt()
    val widthPx = (room.lengthM * scale).toFloat()
    val heightPx = (room.widthM * scale).toFloat()

    val colors = MaterialTheme.colorScheme
    val fill = if (overlapping) colors.errorContainer.copy(alpha = 0.6f)
    else colors.primaryContainer.copy(alpha = 0.6f)
    val border = if (overlapping) colors.error else colors.primary

    Box(
        modifier = Modifier
            .offset { IntOffset(left, top) }
            .requiredSize(
                with(density) { widthPx.toDp() },
                with(density) { heightPx.toDp() }
            )
            .background(fill)
            .border(if (selected) 3.dp else 1.5.dp, border)
            .padding(4.dp)
    ) {
        Column {
            Text(
                text = room.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.plan_room_dimensions,
                    formatMeters(room.lengthM),
                    formatMeters(room.widthM)
                ),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Razmak linija mreže u metrima: najmanji od ponuđenih koji je na ekranu bar oko 40 piksela. */
private fun gridStepM(scale: Float): Float {
    val candidates = floatArrayOf(0.1f, 0.5f, 1f, 5f, 10f)
    return candidates.firstOrNull { it * scale >= 40f } ?: 10f
}

private fun DrawScope.drawGrid(scale: Float, offset: Offset, stepM: Float, color: Color) {
    val stepPx = scale * stepM
    if (stepPx < 6f) return

    var x = ((offset.x % stepPx) + stepPx) % stepPx
    while (x <= size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += stepPx
    }
    var y = ((offset.y % stepPx) + stepPx) % stepPx
    while (y <= size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += stepPx
    }
}
