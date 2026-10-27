package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.domain.DoorSwingGeometry
import com.zoran.aihomedesigner.domain.DoorSwingShape
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureIssue
import com.zoran.aihomedesigner.domain.FurnitureLayoutValidator
import com.zoran.aihomedesigner.domain.FurniturePassages
import com.zoran.aihomedesigner.domain.NarrowPassage
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.PlanImage
import com.zoran.aihomedesigner.domain.OpeningType
import com.zoran.aihomedesigner.domain.PlanIssue
import com.zoran.aihomedesigner.domain.PlanPoint
import com.zoran.aihomedesigner.domain.PlanValidator
import com.zoran.aihomedesigner.data.OpeningSaveResult
import com.zoran.aihomedesigner.data.RoomSaveResult
import com.zoran.aihomedesigner.domain.OpeningInput
import com.zoran.aihomedesigner.domain.WallPicking
import com.zoran.aihomedesigner.domain.WallTap
import com.zoran.aihomedesigner.domain.DrawnRect
import com.zoran.aihomedesigner.domain.RoomInput
import com.zoran.aihomedesigner.domain.MeasureAnchor
import com.zoran.aihomedesigner.domain.MeasureKind
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.RoomMeasureEdit
import com.zoran.aihomedesigner.domain.RoomDrawing
import com.zoran.aihomedesigner.domain.RoomGeometry
import com.zoran.aihomedesigner.domain.RoomGlueResult
import com.zoran.aihomedesigner.domain.RoomGroups
import com.zoran.aihomedesigner.domain.RoomMoveResult
import com.zoran.aihomedesigner.domain.RoomPlacement
import com.zoran.aihomedesigner.domain.SharedWall
import com.zoran.aihomedesigner.domain.SharedWalls
import com.zoran.aihomedesigner.domain.Stairs
import com.zoran.aihomedesigner.domain.StairsGeometry
import com.zoran.aihomedesigner.domain.StairsOnPlan
import com.zoran.aihomedesigner.domain.StairsPlanning
import com.zoran.aihomedesigner.domain.StairsRole
import com.zoran.aihomedesigner.domain.WallOrientation
import com.zoran.aihomedesigner.domain.WallSide
import com.zoran.aihomedesigner.domain.WallSnapping
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val MIN_SCALE = 8f   // najmanje piksela po metru
private const val MAX_SCALE = 600f // najviše piksela po metru
private const val FIT_MAX_SCALE = 300f
private const val MAX_ISSUES_SHOWN = 3
private val DoorColor = Color(0xFF8D6E63)
private val WindowColor = Color(0xFF1E88E5)

/**
 * 2D plan: sobe kao pravougaonici u razmeri.
 * Prikaz se dobija iz istih podataka (Room) koji se čuvaju u bazi, bez posebne kopije.
 * Prevlačenje sobe zaokružuje položaj na 5 cm, a dugmad sa strelicama pomeraju za tačan korak.
 */
@Composable
fun PlanScreen(
    rooms: List<Room>?,
    openings: List<Opening>,
    furniture: List<Furniture> = emptyList(),
    stairs: List<Stairs> = emptyList(),
    floorLevel: Int = 0,
    allRooms: List<Room> = emptyList(),
    planImage: PlanImage? = null,
    planBitmap: ImageBitmap? = null,
    onBack: () -> Unit,
    onMoveRoom: (String, Double, Double) -> Unit,
    onPlaceUnplaced: () -> Unit,
    onMoveFurniture: (String, Double, Double, Int) -> Unit = { _, _, _, _ -> },
    onMoveStairs: (String, Double, Double) -> Unit = { _, _, _ -> },
    onCreateDrawnRoom: (RoomInput, PlanPoint, (RoomSaveResult) -> Unit) -> Unit = { _, _, _ -> },
    onUpdateRoom: (String, RoomInput, (RoomSaveResult) -> Unit) -> Unit = { _, _, _ -> },
    onCreateOpening: (String, OpeningInput, (OpeningSaveResult) -> Unit) -> Unit = { _, _, _ -> },
    onMovePlanImage: (Double, Double) -> Unit = { _, _ -> },
    onSetRoomLocked: (String, Boolean) -> Unit = { _, _ -> },
    onGlueRoom: (String, (RoomGlueResult) -> Unit) -> Unit = { _, _ -> },
    onUnglueRoom: (String) -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(60f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var fitted by remember { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFurnitureId by rememberSaveable { mutableStateOf<String?>(null) }
    var dragRoomId by remember { mutableStateOf<String?>(null) }
    var dragPoint by remember { mutableStateOf<PlanPoint?>(null) }
    var dragFurnitureId by remember { mutableStateOf<String?>(null) }
    var dragFurniturePoint by remember { mutableStateOf<PlanPoint?>(null) }
    var panelVisible by remember { mutableStateOf(true) }
    var openingMode by remember { mutableStateOf(false) }
    var pendingWall by remember { mutableStateOf<WallTap?>(null) }
    var wallMiss by remember { mutableStateOf(false) }
    var measureEdit by remember { mutableStateOf<MeasureEdit?>(null) }
    var drawMode by remember { mutableStateOf(false) }
    var drawStartPoint by remember { mutableStateOf<PlanPoint?>(null) }
    var drawEndPoint by remember { mutableStateOf<PlanPoint?>(null) }
    var drawnRect by remember { mutableStateOf<DrawnRect?>(null) }
    var drawTooSmall by remember { mutableStateOf(false) }
    var dragStairsId by remember { mutableStateOf<String?>(null) }
    var dragStairsPoint by remember { mutableStateOf<PlanPoint?>(null) }
    var moveImageMode by remember { mutableStateOf(false) }
    var dragImagePoint by remember { mutableStateOf<PlanPoint?>(null) }
    var stepM by rememberSaveable { mutableStateOf(0.05) }
    var snapFeedback by remember { mutableStateOf<SnapFeedback?>(null) }
    var roomNote by remember { mutableStateOf<RoomNote?>(null) }

    val point = dragPoint
    val placedRooms = rooms.orEmpty().filter { it.isPlaced }
    // Dok se soba prevlači, sobe zalepljene za nju se pomeraju zajedno sa njom
    val draggedId = dragRoomId
    val dragPositions: Map<String, PlanPoint> =
        if (draggedId != null && point != null) {
            (RoomGroups.planMove(placedRooms, draggedId, point.xM, point.yM) as? RoomMoveResult.Moved)
                ?.positions ?: emptyMap()
        } else {
            emptyMap()
        }
    val displayRooms = placedRooms.map { room ->
        val moved = dragPositions[room.id]
        if (moved != null) room.copy(xM = moved.xM, yM = moved.yM) else room
    }
    val overlapping = RoomGeometry.overlappingRoomIds(displayRooms)
    val walls = SharedWalls.find(displayRooms)
    val issues = PlanValidator.validate(displayRooms)
    val wallsWithIssues: Set<SharedWall> = issues.mapNotNull {
        when (it) {
            is PlanIssue.WallTooThin -> it.wall
            is PlanIssue.WallThinnerThanEntered -> it.wall
            is PlanIssue.RoomsOverlap -> null
        }
    }.toSet()
    val selectedRoom = displayRooms.firstOrNull { it.id == selectedId }
    val furniturePoint = dragFurniturePoint
    val displayFurniture = furniture.map { item ->
        if (item.id == dragFurnitureId && furniturePoint != null) {
            item.copy(xM = furniturePoint.xM, yM = furniturePoint.yM)
        } else {
            item
        }
    }
    // Dok se stepenice prevlače, prikazuju se na privremenom položaju
    val stairsPoint = dragStairsPoint
    val displayStairs = stairs.map { item ->
        if (item.id == dragStairsId && stairsPoint != null) {
            item.copy(xM = stairsPoint.xM, yM = stairsPoint.yM)
        } else {
            item
        }
    }
    val selectedFurniture = displayFurniture.firstOrNull { item ->
        item.id == selectedFurnitureId && displayRooms.any { it.id == item.roomId }
    }

    // Provera rasporeda nameštaja, soba po soba (izlazak iz sobe, visina, preklapanje, blokiranje vrata)
    val furnitureIssues: List<FurnitureIssue> = displayRooms.flatMap { room ->
        FurnitureLayoutValidator.validate(
            room,
            displayFurniture.filter { it.roomId == room.id },
            openings.filter { it.roomId == room.id }
        )
    }
    // Preporuke o uskim prolazima između nameštaja (nisu greške)
    val furnitureWarnings: List<NarrowPassage> = displayRooms.flatMap { room ->
        FurniturePassages.find(room, displayFurniture.filter { it.roomId == room.id })
    }
    val furnitureWithIssues: Set<String> = furnitureIssues.flatMap { issue ->
        when (issue) {
            is FurnitureIssue.OutsideRoom -> listOf(issue.furnitureId)
            is FurnitureIssue.TooTall -> listOf(issue.furnitureId)
            is FurnitureIssue.Overlap -> listOf(issue.furnitureAId, issue.furnitureBId)
            is FurnitureIssue.BlocksDoor -> listOf(issue.furnitureId)
        }
    }.toSet()

    val currentRooms by rememberUpdatedState(displayRooms)
    val currentFurniture by rememberUpdatedState(displayFurniture)
    val currentOnMoveFurniture by rememberUpdatedState(onMoveFurniture)
    val currentStairs by rememberUpdatedState(displayStairs)
    val currentFloorLevel by rememberUpdatedState(floorLevel)
    val currentOnMoveStairs by rememberUpdatedState(onMoveStairs)
    val currentDrawMode by rememberUpdatedState(drawMode)
    val currentOpeningMode by rememberUpdatedState(openingMode)
    val currentSelectedId by rememberUpdatedState(selectedId)
    val currentOnMoveImage by rememberUpdatedState(onMovePlanImage)
    val currentPlanImage by rememberUpdatedState(planImage)
    val currentOnMove by rememberUpdatedState(onMoveRoom)

    LaunchedEffect(selectedId) {
        snapFeedback = null
        roomNote = null
    }

    // Kad stigne novi položaj slike iz baze, privremeni položaj prevlačenja više nije potreban.
    LaunchedEffect(planImage) { dragImagePoint = null }

    // Kad stigne novi spisak nameštaja iz baze, privremeni položaj prevlačenja više nije potreban.
    LaunchedEffect(furniture) {
        dragFurnitureId = null
        dragFurniturePoint = null
    }

    // Kad stigne novi spisak stepenica iz baze, privremeni položaj prevlačenja više nije potreban.
    LaunchedEffect(stairs) {
        dragStairsId = null
        dragStairsPoint = null
    }

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
        // Podloga (slika plana) ulazi u prikaz, da se vidi cela zajedno sa sobama
        val image = planImage
        val minX = min(placed.minOf { it.xM!! }, image?.originXM ?: Double.MAX_VALUE)
        val minY = min(placed.minOf { it.yM!! }, image?.originYM ?: Double.MAX_VALUE)
        val maxX = max(placed.maxOf { it.xM!! + it.lengthM }, image?.let { it.originXM + it.widthM } ?: -Double.MAX_VALUE)
        val maxY = max(placed.maxOf { it.yM!! + it.widthM }, image?.let { it.originYM + it.heightM } ?: -Double.MAX_VALUE)
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

    fun roomIsLocked(roomId: String): Boolean =
        RoomGroups.members(displayRooms, roomId).any { it.positionLocked }

    fun nudge(dx: Double, dy: Double) {
        val room = selectedRoom ?: return
        if (roomIsLocked(room.id)) {
            roomNote = RoomNote.LOCKED
            return
        }
        onMoveRoom(
            room.id,
            RoomPlacement.roundToMm(room.xM!! + dx),
            RoomPlacement.roundToMm(room.yM!! + dy)
        )
    }

    fun nudgeFurniture(dx: Double, dy: Double) {
        val item = selectedFurniture ?: return
        val room = displayRooms.firstOrNull { it.id == item.roomId } ?: return
        val maxX = max(0.0, room.lengthM - item.footprintLengthM)
        val maxY = max(0.0, room.widthM - item.footprintWidthM)
        onMoveFurniture(
            item.id,
            RoomPlacement.roundToMm((item.xM + dx).coerceIn(0.0, maxX)),
            RoomPlacement.roundToMm((item.yM + dy).coerceIn(0.0, maxY)),
            item.rotationDeg
        )
    }

    fun rotateFurniture() {
        val item = selectedFurniture ?: return
        val room = displayRooms.firstOrNull { it.id == item.roomId } ?: return
        val newRotation = (item.rotationDeg + 90) % 360
        val turned = newRotation % 180 != 0
        val newLength = if (turned) item.widthM else item.lengthM
        val newWidth = if (turned) item.lengthM else item.widthM
        val maxX = max(0.0, room.lengthM - newLength)
        val maxY = max(0.0, room.widthM - newWidth)
        onMoveFurniture(
            item.id,
            RoomPlacement.roundToMm(item.xM.coerceIn(0.0, maxX)),
            RoomPlacement.roundToMm(item.yM.coerceIn(0.0, maxY)),
            newRotation
        )
    }

    fun snapSelected() {
        val room = selectedRoom ?: return
        if (roomIsLocked(room.id)) {
            roomNote = RoomNote.LOCKED
            return
        }
        when (val result = WallSnapping.snap(displayRooms, room.id)) {
            is WallSnapping.Result.Moved -> {
                onMoveRoom(room.id, result.position.xM, result.position.yM)
                val neighborName = displayRooms.firstOrNull { it.id == result.neighborId }?.name ?: "?"
                snapFeedback = SnapFeedback.Moved(neighborName, result.thicknessM)
            }

            WallSnapping.Result.NoNeighborNearby -> snapFeedback = SnapFeedback.NoNeighbor
            WallSnapping.Result.NeedsThickness -> snapFeedback = SnapFeedback.NeedsThickness
        }
    }

    // Prozor za sobu nacrtanu preko slike: ime i visina se upisuju, a dužina i širina se predlažu sa slike
    drawnRect?.let { rect ->
        DrawRoomDialog(
            rect = rect,
            onSubmit = { input, onResult -> onCreateDrawnRoom(input, PlanPoint(rect.xM, rect.yM), onResult) },
            onDone = { drawnRect = null },
            onCancel = { drawnRect = null }
        )
    }

    // Forma za vrata ili prozor na zidu koji je dodirnut na planu (plan ostaje otvoren ispod)
    pendingWall?.let { tap ->
        displayRooms.firstOrNull { it.id == tap.roomId }?.let { room ->
            Dialog(
                onDismissRequest = { pendingWall = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OpeningFormScreen(
                        room = room,
                        initialOpening = null,
                        onSubmit = { input, onResult -> onCreateOpening(room.id, input, onResult) },
                        onDone = { pendingWall = null },
                        onCancel = { pendingWall = null },
                        initialSide = tap.side,
                        tapCenterM = tap.alongM
                    )
                }
            }
        }
    }

    // Prozor za izmenu jedne mere sobe dodirnute na planu
    measureEdit?.let { edit ->
        displayRooms.firstOrNull { it.id == edit.roomId }?.let { room ->
            RoomMeasureDialog(
                room = room,
                kind = edit.kind,
                onSubmit = { input, onResult -> onUpdateRoom(room.id, input, onResult) },
                onDone = { measureEdit = null },
                onCancel = { measureEdit = null }
            )
        }
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
                TextButton(onClick = { panelVisible = !panelVisible }) {
                    Text(stringResource(if (panelVisible) R.string.plan_panel_hide else R.string.plan_panel_show))
                }
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
                            // Crtanje sobe preko slike: prevlačenje pravi pravougaonik, ništa se ne pomera
                            val drawing = currentDrawMode
                            // Dodavanje vrata ili prozora: dodir bira zid, a prevlačenje samo pomera prikaz
                            val pickingWall = currentOpeningMode
                            val hit = currentRooms.lastOrNull { room ->
                                val left = startOffset.x + room.xM!! * startScale
                                val top = startOffset.y + room.yM!! * startScale
                                startPos.x >= left && startPos.x <= left + room.lengthM * startScale &&
                                    startPos.y >= top && startPos.y <= top + room.widthM * startScale
                            }
                            // Nameštaj je iznad sobe, pa ima prednost pri prevlačenju.
                            val hitItem = currentFurniture.lastOrNull { item ->
                                val itemRoom = currentRooms.firstOrNull { it.id == item.roomId }
                                    ?: return@lastOrNull false
                                val left = startOffset.x + (itemRoom.xM!! + item.xM) * startScale
                                val top = startOffset.y + (itemRoom.yM!! + item.yM) * startScale
                                startPos.x >= left &&
                                    startPos.x <= left + item.footprintLengthM * startScale &&
                                    startPos.y >= top &&
                                    startPos.y <= top + item.footprintWidthM * startScale
                            }
                            val hitItemRoom = hitItem?.let { f -> currentRooms.firstOrNull { it.id == f.roomId } }
                            var draggingFurnitureId: String? =
                                if (!drawing && !pickingWall && hitItem != null && hitItemRoom != null) hitItem.id else null
                            // Stepenice su iznad sobe, a ispod nameštaja: nameštaj ima prednost, pa one, pa soba.
                            val hitStairs = if (drawing || pickingWall || hitItem != null) null else currentStairs.lastOrNull { s ->
                                StairsOnPlan.role(s, currentFloorLevel) != null &&
                                    StairsOnPlan.contains(
                                        s,
                                        ((startPos.x - startOffset.x) / startScale).toDouble(),
                                        ((startPos.y - startOffset.y) / startScale).toDouble()
                                    )
                            }
                            var draggingStairsId: String? = hitStairs?.id
                            val startX = hit?.xM ?: 0.0
                            val startY = hit?.yM ?: 0.0
                            val hitIsLocked = hit != null &&
                                RoomGroups.members(currentRooms, hit.id).any { it.positionLocked }
                            var draggingId: String? =
                                if (!drawing && !pickingWall && draggingFurnitureId == null && draggingStairsId == null && !hitIsLocked) hit?.id else null
                            var moved = false
                            var multiTouch = false
                            var draggingImage = false
                            val startImageOrigin = currentPlanImage?.let { PlanPoint(it.originXM, it.originYM) }

                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount >= 2) {
                                    if (!multiTouch) {
                                        multiTouch = true
                                        draggingId = null
                                        dragRoomId = null
                                        dragPoint = null
                                        draggingFurnitureId = null
                                        dragFurnitureId = null
                                        dragFurniturePoint = null
                                        draggingStairsId = null
                                        dragStairsId = null
                                        dragStairsPoint = null
                                        drawStartPoint = null
                                        drawEndPoint = null
                                        draggingImage = false
                                        dragImagePoint = null
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
                                        val furnitureId = draggingFurnitureId
                                        val stairsId = draggingStairsId
                                        if (drawing) {
                                            drawStartPoint = PlanPoint(
                                                ((startPos.x - startOffset.x) / startScale).toDouble(),
                                                ((startPos.y - startOffset.y) / startScale).toDouble()
                                            )
                                            drawEndPoint = PlanPoint(
                                                ((change.position.x - startOffset.x) / startScale).toDouble(),
                                                ((change.position.y - startOffset.y) / startScale).toDouble()
                                            )
                                        } else if (furnitureId != null && hitItem != null && hitItemRoom != null) {
                                            val maxX = max(0.0, hitItemRoom.lengthM - hitItem.footprintLengthM)
                                            val maxY = max(0.0, hitItemRoom.widthM - hitItem.footprintWidthM)
                                            val nx = RoomPlacement.snapToGrid(hitItem.xM + total.x / startScale)
                                                .coerceIn(0.0, maxX)
                                            val ny = RoomPlacement.snapToGrid(hitItem.yM + total.y / startScale)
                                                .coerceIn(0.0, maxY)
                                            dragFurnitureId = furnitureId
                                            dragFurniturePoint = PlanPoint(nx, ny)
                                            selectedFurnitureId = furnitureId
                                            selectedId = null
                                        } else if (stairsId != null && hitStairs != null) {
                                            val nx = RoomPlacement.clampCoordinate(
                                                RoomPlacement.snapToGrid(hitStairs.xM + total.x / startScale)
                                            )
                                            val ny = RoomPlacement.clampCoordinate(
                                                RoomPlacement.snapToGrid(hitStairs.yM + total.y / startScale)
                                            )
                                            dragStairsId = stairsId
                                            dragStairsPoint = PlanPoint(nx, ny)
                                            selectedId = null
                                            selectedFurnitureId = null
                                        } else if (id != null) {
                                            val nx = RoomPlacement.clampCoordinate(
                                                RoomPlacement.snapToGrid(startX + total.x / startScale)
                                            )
                                            val ny = RoomPlacement.clampCoordinate(
                                                RoomPlacement.snapToGrid(startY + total.y / startScale)
                                            )
                                            dragRoomId = id
                                            dragPoint = PlanPoint(nx, ny)
                                            selectedId = id
                                            selectedFurnitureId = null
                                        } else if (moveImageMode && startImageOrigin != null) {
                                            val nx = RoomPlacement.clampCoordinate(
                                                RoomPlacement.snapToGrid(startImageOrigin.xM + total.x / startScale)
                                            )
                                            val ny = RoomPlacement.clampCoordinate(
                                                RoomPlacement.snapToGrid(startImageOrigin.yM + total.y / startScale)
                                            )
                                            draggingImage = true
                                            dragImagePoint = PlanPoint(nx, ny)
                                        } else {
                                            offset = startOffset + total
                                        }
                                        change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            if (!multiTouch) {
                                if (drawing) {
                                    val s = drawStartPoint
                                    val e = drawEndPoint
                                    drawStartPoint = null
                                    drawEndPoint = null
                                    if (moved && s != null && e != null) {
                                        val rect = RoomDrawing.rect(s, e)
                                        drawnRect = rect
                                        drawTooSmall = rect == null
                                    }
                                } else if (!moved) {
                                    // Dodir na meru uz zid izabrane sobe otvara izmenu te mere
                                    val chipRoom = currentRooms.firstOrNull { it.id == currentSelectedId }
                                    val chipHalfW = max(MeasureChipWidth.toPx(), 48.dp.toPx()) / 2f
                                    val chipHalfH = max(MeasureChipHeight.toPx(), 48.dp.toPx()) / 2f
                                    val tappedChip = chipRoom?.let { r ->
                                        RoomMeasureEdit.anchors(r).firstOrNull { a ->
                                            abs(startPos.x - (startOffset.x + (a.xM * startScale).toFloat())) <= chipHalfW &&
                                                abs(startPos.y - (startOffset.y + (a.yM * startScale).toFloat())) <= chipHalfH
                                        }
                                    }
                                    val hitFurniture = currentFurniture.lastOrNull { item ->
                                        val itemRoom = currentRooms.firstOrNull { it.id == item.roomId }
                                            ?: return@lastOrNull false
                                        val left = startOffset.x + (itemRoom.xM!! + item.xM) * startScale
                                        val top = startOffset.y + (itemRoom.yM!! + item.yM) * startScale
                                        startPos.x >= left &&
                                            startPos.x <= left + item.footprintLengthM * startScale &&
                                            startPos.y >= top &&
                                            startPos.y <= top + item.footprintWidthM * startScale
                                    }
                                    if (pickingWall) {
                                        val tapXm = ((startPos.x - startOffset.x) / startScale).toDouble()
                                        val tapYm = ((startPos.y - startOffset.y) / startScale).toDouble()
                                        val tolM = (24.dp.toPx() / startScale).toDouble()
                                        val wall = WallPicking.nearest(currentRooms, tapXm, tapYm, tolM)
                                        wallMiss = wall == null
                                        if (wall != null) pendingWall = wall
                                    } else if (tappedChip != null && chipRoom != null) {
                                        measureEdit = MeasureEdit(chipRoom.id, tappedChip.kind)
                                    } else if (hitFurniture != null) {
                                        selectedFurnitureId = hitFurniture.id
                                        selectedId = null
                                    } else {
                                        selectedFurnitureId = null
                                        selectedId = hit?.id
                                    }
                                } else {
                                    val id = draggingId
                                    val p = dragPoint
                                    if (id != null && p != null) {
                                        currentOnMove(id, p.xM, p.yM)
                                    }
                                    val imagePos = dragImagePoint
                                    if (draggingImage && imagePos != null) {
                                        currentOnMoveImage(imagePos.xM, imagePos.yM)
                                    }
                                    val furnitureId = draggingFurnitureId
                                    val furniturePos = dragFurniturePoint
                                    if (furnitureId != null && furniturePos != null && hitItem != null) {
                                        currentOnMoveFurniture(
                                            furnitureId,
                                            furniturePos.xM,
                                            furniturePos.yM,
                                            hitItem.rotationDeg
                                        )
                                    }
                                    val movedStairsId = draggingStairsId
                                    val stairsPos = dragStairsPoint
                                    if (movedStairsId != null && stairsPos != null) {
                                        currentOnMoveStairs(movedStairsId, stairsPos.xM, stairsPos.yM)
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Podloga: slika plana ispod soba, polu providna, u razmeri iz kalibracije
                val image = planImage
                if (image != null && planBitmap != null) {
                    val origin = dragImagePoint ?: PlanPoint(image.originXM, image.originYM)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = planBitmap,
                            dstOffset = IntOffset(
                                (offset.x + origin.xM * scale).roundToInt(),
                                (offset.y + origin.yM * scale).roundToInt()
                            ),
                            dstSize = IntSize(
                                (image.widthM * scale).roundToInt().coerceAtLeast(1),
                                (image.heightM * scale).roundToInt().coerceAtLeast(1)
                            ),
                            alpha = 0.55f
                        )
                    }
                    Text(
                        text = stringResource(R.string.plan_image_legend),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                }

                displayRooms.forEach { room ->
                    PlanRoom(
                        room = room,
                        scale = scale,
                        offset = offset,
                        selected = room.id == selectedId,
                        overlapping = room.id in overlapping,
                        locked = room.positionLocked,
                        glued = RoomGroups.isGlued(displayRooms, room.id)
                    )
                }

                walls.forEach { wall ->
                    PlanWall(
                        wall = wall,
                        scale = scale,
                        offset = offset,
                        hasIssue = wall in wallsWithIssues
                    )
                }

                // Stepenice sa ovog sprata (pune linije) i one koje stižu na ovaj sprat (isprekidane)
                displayStairs.forEach { item ->
                    val role = StairsOnPlan.role(item, floorLevel) ?: return@forEach
                    PlanStairs(
                        stairs = item,
                        role = role,
                        stepCount = StairsGeometry.stepCount(StairsPlanning.riseM(allRooms, item.lowerLevel)),
                        scale = scale,
                        offset = offset
                    )
                }

                val roomsById = displayRooms.associateBy { it.id }
                displayFurniture.forEach { item ->
                    val room = roomsById[item.roomId]
                    if (room != null) {
                        PlanFurniture(
                            furniture = item,
                            room = room,
                            scale = scale,
                            offset = offset,
                            selected = item.id == selectedFurnitureId,
                            hasIssue = item.id in furnitureWithIssues
                        )
                    }
                }

                openings.forEach { opening ->
                    val room = roomsById[opening.roomId]
                    if (room != null) {
                        PlanOpening(opening = opening, room = room, scale = scale, offset = offset)
                    }
                }

                // Luk otvaranja vrata (bez prozora i kliznih vrata)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    openings.forEach { opening ->
                        val room = roomsById[opening.roomId] ?: return@forEach
                        val shape = DoorSwingGeometry.shape(opening, room) ?: return@forEach
                        drawDoorSwing(shape, scale, offset, DoorColor)
                    }
                }

                if (openings.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.plan_legend_doors),
                            color = DoorColor,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.plan_legend_windows),
                            color = WindowColor,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
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
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )

                // Pravougaonik koji se trenutno crta preko slike, sa merama koje pokazuje slika
                val rubberStart = drawStartPoint
                val rubberEnd = drawEndPoint
                if (rubberStart != null && rubberEnd != null) {
                    val rx0 = min(rubberStart.xM, rubberEnd.xM)
                    val ry0 = min(rubberStart.yM, rubberEnd.yM)
                    val rw = abs(rubberEnd.xM - rubberStart.xM)
                    val rh = abs(rubberEnd.yM - rubberStart.yM)
                    val rubberColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val topLeft = Offset(offset.x + (rx0 * scale).toFloat(), offset.y + (ry0 * scale).toFloat())
                        val rubberSize = Size((rw * scale).toFloat(), (rh * scale).toFloat())
                        drawRect(color = rubberColor.copy(alpha = 0.2f), topLeft = topLeft, size = rubberSize)
                        drawRect(color = rubberColor, topLeft = topLeft, size = rubberSize, style = Stroke(width = 3f))
                    }
                    Text(
                        text = stringResource(R.string.plan_draw_room_size, formatMeters(rw), formatMeters(rh)),
                        style = MaterialTheme.typography.labelMedium,
                        color = rubberColor,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (offset.x + (rx0 * scale).toFloat()).roundToInt(),
                                    (offset.y + (ry0 * scale).toFloat()).roundToInt()
                                )
                            }
                            .padding(4.dp)
                    )
                }

                // Mere izabrane sobe uz zidove: dodir na meru je izmena (obrađuje se u gestu iznad)
                if (!drawMode && !openingMode) {
                    selectedRoom?.let { room ->
                        RoomMeasureEdit.anchors(room).forEach { anchor ->
                            MeasureChip(anchor, room, scale, offset)
                        }
                    }
                }

                // Uputstvo za aktivni režim ide na sam crtež (ispod oznake SEVER), a ne u donji panel
                val modeHintRes: Int? = when {
                    openingMode -> if (wallMiss) R.string.plan_opening_miss else R.string.plan_opening_hint
                    drawMode -> if (drawTooSmall) R.string.plan_draw_room_too_small else R.string.plan_draw_room_hint
                    else -> null
                }
                if (modeHintRes != null) {
                    Text(
                        text = stringResource(modeHintRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (wallMiss && openingMode || drawTooSmall && drawMode) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 32.dp, start = 40.dp, end = 40.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }

                // Strane sveta uz ivice prikaza, izvan crteža kuće ili stana.
                CompassLabel(
                    text = stringResource(R.string.compass_north),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                CompassLabel(
                    text = stringResource(R.string.compass_south),
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                CompassLabel(
                    text = stringResource(R.string.compass_west),
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                CompassLabel(
                    text = stringResource(R.string.compass_east),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            // Donji panel zauzima najviše 38% visine ekrana i može da se lista, da crtež uvek ostane vidljiv.
            val panelMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.38f).dp
            if (panelVisible) Column(
                modifier = Modifier
                    .heightIn(max = panelMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (selectedRoom != null && !drawMode && !openingMode) {
                    Text(
                        text = stringResource(R.string.plan_measure_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = {
                    openingMode = !openingMode
                    wallMiss = false
                    if (openingMode) {
                        drawMode = false
                        moveImageMode = false
                    }
                }) {
                    Text(
                        stringResource(
                            if (openingMode) R.string.plan_opening_on else R.string.plan_opening_off
                        )
                    )
                }
                if (planImage != null) {
                    TextButton(onClick = {
                        drawMode = !drawMode
                        drawTooSmall = false
                        if (drawMode) {
                            moveImageMode = false
                            openingMode = false
                        }
                    }) {
                        Text(
                            stringResource(
                                if (drawMode) R.string.plan_draw_room_on else R.string.plan_draw_room_off
                            )
                        )
                    }
                    TextButton(onClick = {
                        moveImageMode = !moveImageMode
                        if (moveImageMode) {
                            drawMode = false
                            openingMode = false
                        }
                    }) {
                        Text(
                            stringResource(
                                if (moveImageMode) R.string.plan_image_move_on else R.string.plan_image_move_off
                            )
                        )
                    }
                    if (moveImageMode) {
                        Text(
                            text = stringResource(R.string.plan_image_move_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                issues.take(MAX_ISSUES_SHOWN).forEach { issue ->
                    Text(
                        text = issueText(issue, displayRooms),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (issues.size > MAX_ISSUES_SHOWN) {
                    Text(
                        text = stringResource(R.string.plan_issue_more, issues.size - MAX_ISSUES_SHOWN),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                furnitureIssues.take(MAX_ISSUES_SHOWN).forEach { issue ->
                    Text(
                        text = furnitureIssueText(issue, displayFurniture),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (furnitureIssues.size > MAX_ISSUES_SHOWN) {
                    Text(
                        text = stringResource(R.string.plan_issue_more, furnitureIssues.size - MAX_ISSUES_SHOWN),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                furnitureWarnings.take(2).forEach { warning ->
                    Text(
                        text = narrowPassageText(warning, displayFurniture),
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (furnitureWarnings.size > 2) {
                    Text(
                        text = stringResource(R.string.plan_issue_more, furnitureWarnings.size - 2),
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (selectedFurniture != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = selectedFurniture.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(
                                    R.string.plan_selected_position,
                                    formatMeters(selectedFurniture.xM),
                                    formatMeters(selectedFurniture.yM)
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedButton(
                                onClick = { rotateFurniture() },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(stringResource(R.string.plan_furniture_rotate))
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                NudgeButton("←") { nudgeFurniture(-stepM, 0.0) }
                                NudgeButton("→") { nudgeFurniture(stepM, 0.0) }
                                NudgeButton("↑") { nudgeFurniture(0.0, -stepM) }
                                NudgeButton("↓") { nudgeFurniture(0.0, stepM) }
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
                } else if (selectedRoom != null) {
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
                            if (selectedRoom.positionLocked) {
                                Text(
                                    text = stringResource(R.string.plan_room_locked_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    onSetRoomLocked(selectedRoom.id, !selectedRoom.positionLocked)
                                    roomNote = null
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    stringResource(
                                        if (selectedRoom.positionLocked) R.string.plan_room_unlock else R.string.plan_room_lock
                                    )
                                )
                            }
                            val glued = RoomGroups.isGlued(displayRooms, selectedRoom.id)
                            OutlinedButton(
                                onClick = {
                                    if (glued) {
                                        onUnglueRoom(selectedRoom.id)
                                        roomNote = null
                                    } else {
                                        onGlueRoom(selectedRoom.id) { result ->
                                            roomNote = if (result is RoomGlueResult.Glued) {
                                                RoomNote.GLUED
                                            } else {
                                                RoomNote.NO_NEIGHBOURS
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    stringResource(if (glued) R.string.plan_room_unglue else R.string.plan_room_glue)
                                )
                            }
                            if (glued) {
                                val partners = RoomGroups.members(displayRooms, selectedRoom.id)
                                    .filter { it.id != selectedRoom.id }
                                    .joinToString { it.name }
                                Text(
                                    text = stringResource(R.string.plan_room_glued_with, partners),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            roomNote?.let { note ->
                                Text(
                                    text = roomNoteText(note),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = { snapSelected() },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(stringResource(R.string.plan_snap))
                            }
                            snapFeedback?.let { feedback ->
                                Text(
                                    text = snapFeedbackText(feedback),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.plan_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (displayRooms.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            if (issues.isEmpty()) R.string.plan_checked_ok else R.string.plan_checked
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
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
    overlapping: Boolean,
    locked: Boolean = false,
    glued: Boolean = false
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
                text = (if (locked) "🔒 " else "") + (if (glued) "🔗 " else "") + room.name,
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

/** Zid između dve susedne sobe: tamna traka na mestu razmaka, sa debljinom u centimetrima. */
@Composable
private fun PlanWall(wall: SharedWall, scale: Float, offset: Offset, hasIssue: Boolean) {
    val density = LocalDensity.current
    val rect = wall.rect
    val left = (offset.x + rect.x0 * scale).roundToInt()
    val top = (offset.y + rect.y0 * scale).roundToInt()
    // Tanak zid se crta bar 3 piksela široko da ostane vidljiv, iako je u modelu tanji.
    val widthPx = max(((rect.x1 - rect.x0) * scale).toFloat(), 3f)
    val heightPx = max(((rect.y1 - rect.y0) * scale).toFloat(), 3f)

    val colors = MaterialTheme.colorScheme
    val fill = if (hasIssue) colors.error else colors.onSurface.copy(alpha = 0.75f)
    val longSidePx = if (wall.orientation == WallOrientation.VERTICAL) heightPx else widthPx

    Box(
        modifier = Modifier
            .offset { IntOffset(left, top) }
            .requiredSize(
                with(density) { widthPx.toDp() },
                with(density) { heightPx.toDp() }
            )
            .background(fill),
        contentAlignment = Alignment.Center
    ) {
        if (longSidePx >= 70f) {
            Text(
                text = stringResource(R.string.plan_wall_label, formatCentimeters(wall.thicknessM)),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .wrapContentSize(unbounded = true)
                    .background(colors.surface.copy(alpha = 0.85f))
                    .padding(horizontal = 3.dp)
            )
        }
    }
}

@Composable
private fun issueText(issue: PlanIssue, rooms: List<Room>): String {
    fun name(id: String): String = rooms.firstOrNull { it.id == id }?.name ?: "?"
    return when (issue) {
        is PlanIssue.RoomsOverlap -> stringResource(
            R.string.plan_issue_overlap,
            name(issue.roomAId),
            name(issue.roomBId)
        )

        is PlanIssue.WallTooThin -> stringResource(
            R.string.plan_issue_wall_too_thin,
            name(issue.wall.roomAId),
            name(issue.wall.roomBId),
            formatCentimeters(PlanValidator.MIN_WALL_THICKNESS_M)
        )

        is PlanIssue.WallThinnerThanEntered -> stringResource(
            R.string.plan_issue_wall_thinner,
            name(issue.wall.roomAId),
            name(issue.wall.roomBId),
            formatCentimeters(issue.wall.thicknessM),
            name(issue.roomId),
            formatCentimeters(issue.enteredThicknessM)
        )
    }
}

/**
 * Komad nameštaja kao pravougaonik u razmeri, unutar sobe. Položaj se meri od gornjeg levog ugla
 * unutrašnjosti sobe, pa se nameštaj pomera zajedno sa sobom. Dužina i širina se menjaju za 90 i 270 stepeni.
 */
/**
 * Stepenice na planu: pravougaonik, poprečne crte gazišta i strelica ka gore.
 * Na donjem spratu su pune linije sa strelicom, a na gornjem isprekidane (stepenice stižu ovamo).
 * Broj crta je procena iz visine soba, a ne izmerena vrednost.
 */
@Composable
private fun PlanStairs(
    stairs: Stairs,
    role: StairsRole,
    stepCount: Int,
    scale: Float,
    offset: Offset
) {
    val density = LocalDensity.current
    val left = (offset.x + stairs.xM * scale).roundToInt()
    val top = (offset.y + stairs.yM * scale).roundToInt()
    val widthPx = (stairs.footprintLengthM * scale).toFloat()
    val heightPx = (stairs.footprintWidthM * scale).toFloat()

    val colors = MaterialTheme.colorScheme
    val lineColor = colors.secondary
    val fillColor = colors.secondaryContainer.copy(alpha = if (role == StairsRole.DEPARTS) 0.7f else 0.35f)
    val lines = remember(stairs, stepCount) { StairsOnPlan.stepLines(stairs, stepCount) }
    val arrow = remember(stairs) { StairsOnPlan.climbArrow(stairs) }
    val label = if (role == StairsRole.DEPARTS) {
        stringResource(R.string.plan_stairs_up, floorLabel(stairs.upperLevel))
    } else {
        stringResource(R.string.plan_stairs_from, floorLabel(stairs.lowerLevel))
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(left, top) }
            .requiredSize(
                with(density) { widthPx.toDp() },
                with(density) { heightPx.toDp() }
            )
            .clipToBounds()
            .drawBehind {
                fun px(p: PlanPoint) = Offset(
                    ((p.xM - stairs.xM) * scale).toFloat(),
                    ((p.yM - stairs.yM) * scale).toFloat()
                )

                drawRect(fillColor)
                val dash = if (role == StairsRole.ARRIVES) {
                    PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                } else {
                    null
                }
                drawRect(lineColor, style = Stroke(width = 2.dp.toPx(), pathEffect = dash))
                lines.forEach { line ->
                    drawLine(lineColor, px(line.from), px(line.to), strokeWidth = 1.dp.toPx(), pathEffect = dash)
                }

                if (role == StairsRole.DEPARTS) {
                    val tail = px(arrow.from)
                    val head = px(arrow.to)
                    val dx = head.x - tail.x
                    val dy = head.y - tail.y
                    val length = sqrt(dx * dx + dy * dy)
                    if (length > 0f) {
                        val ux = dx / length
                        val uy = dy / length
                        val headLength = min(10.dp.toPx(), length / 3f)
                        val bx = head.x - ux * headLength
                        val by = head.y - uy * headLength
                        val stroke = 2.dp.toPx()
                        drawLine(lineColor, tail, head, strokeWidth = stroke)
                        drawLine(
                            lineColor, head,
                            Offset(bx - uy * headLength * 0.5f, by + ux * headLength * 0.5f),
                            strokeWidth = stroke
                        )
                        drawLine(
                            lineColor, head,
                            Offset(bx + uy * headLength * 0.5f, by - ux * headLength * 0.5f),
                            strokeWidth = stroke
                        )
                    }
                }
            }
            .padding(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlanFurniture(
    furniture: Furniture,
    room: Room,
    scale: Float,
    offset: Offset,
    selected: Boolean,
    hasIssue: Boolean = false
) {
    val density = LocalDensity.current
    val left = (offset.x + (room.xM!! + furniture.xM) * scale).roundToInt()
    val top = (offset.y + (room.yM!! + furniture.yM) * scale).roundToInt()
    val widthPx = (furniture.footprintLengthM * scale).toFloat()
    val heightPx = (furniture.footprintWidthM * scale).toFloat()

    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .offset { IntOffset(left, top) }
            .requiredSize(
                with(density) { widthPx.toDp() },
                with(density) { heightPx.toDp() }
            )
            .clipToBounds()
            .background(
                if (hasIssue) colors.errorContainer.copy(alpha = 0.85f)
                else colors.tertiaryContainer.copy(alpha = 0.85f)
            )
            .border(
                if (selected) 3.dp else 1.5.dp,
                when {
                    hasIssue -> colors.error
                    selected -> colors.primary
                    else -> colors.tertiary
                }
            )
            .padding(2.dp)
    ) {
        Text(
            text = furniture.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Vrata (smeđa) i prozor (plava) kao traka na zidu sobe, na mestu koje je korisnik uneo.
 * Smer otvaranja vrata se za sada ne crta.
 */
@Composable
private fun PlanOpening(opening: Opening, room: Room, scale: Float, offset: Offset) {
    val density = LocalDensity.current
    val thickness = with(density) { 6.dp.toPx() }

    val roomLeft = offset.x + (room.xM!! * scale).toFloat()
    val roomTop = offset.y + (room.yM!! * scale).toFloat()
    val roomRight = roomLeft + (room.lengthM * scale).toFloat()
    val roomBottom = roomTop + (room.widthM * scale).toFloat()
    val start = (opening.offsetM * scale).toFloat()
    val length = max((opening.widthM * scale).toFloat(), 3f)

    // x, y, širina i visina trake u pikselima
    val box: List<Float> = when (opening.side) {
        WallSide.NORTH -> listOf(roomLeft + start, roomTop - thickness / 2, length, thickness)
        WallSide.SOUTH -> listOf(roomLeft + start, roomBottom - thickness / 2, length, thickness)
        WallSide.WEST -> listOf(roomLeft - thickness / 2, roomTop + start, thickness, length)
        WallSide.EAST -> listOf(roomRight - thickness / 2, roomTop + start, thickness, length)
    }
    val x = box[0]
    val y = box[1]
    val w = box[2]
    val h = box[3]

    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .requiredSize(
                with(density) { w.toDp() },
                with(density) { h.toDp() }
            )
            .background(if (opening.type == OpeningType.DOOR) DoorColor else WindowColor)
    )
}

/** Crta list vrata u otvorenom položaju i luk po kome se vrata otvaraju, oko tačke šarki. */
private fun DrawScope.drawDoorSwing(shape: DoorSwingShape, scale: Float, offset: Offset, color: Color) {
    fun px(p: PlanPoint) = Offset(
        offset.x + (p.xM * scale).toFloat(),
        offset.y + (p.yM * scale).toFloat()
    )

    val hinge = px(shape.hinge)
    val closed = px(shape.closedEnd)
    val open = px(shape.openEnd)
    val radius = (shape.radiusM * scale).toFloat()
    if (radius < 2f) return

    val startAngle = Math.toDegrees(
        atan2((closed.y - hinge.y).toDouble(), (closed.x - hinge.x).toDouble())
    ).toFloat()
    val endAngle = Math.toDegrees(
        atan2((open.y - hinge.y).toDouble(), (open.x - hinge.x).toDouble())
    ).toFloat()
    var sweep = endAngle - startAngle
    while (sweep > 180f) sweep -= 360f
    while (sweep <= -180f) sweep += 360f

    drawArc(
        color = color.copy(alpha = 0.8f),
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(hinge.x - radius, hinge.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 2f)
    )
    drawLine(color = color, start = hinge, end = open, strokeWidth = 3f)
}

@Composable
private fun furnitureIssueText(issue: FurnitureIssue, furniture: List<Furniture>): String {
    fun name(id: String): String = furniture.firstOrNull { it.id == id }?.name ?: "?"
    return when (issue) {
        is FurnitureIssue.OutsideRoom ->
            stringResource(R.string.furniture_issue_outside, name(issue.furnitureId))

        is FurnitureIssue.TooTall ->
            stringResource(R.string.furniture_issue_too_tall, name(issue.furnitureId))

        is FurnitureIssue.Overlap -> stringResource(
            R.string.furniture_issue_overlap,
            name(issue.furnitureAId),
            name(issue.furnitureBId)
        )

        is FurnitureIssue.BlocksDoor ->
            stringResource(R.string.furniture_issue_blocks_door, name(issue.furnitureId))
    }
}

/** Oznaka strane sveta, sa providnom pozadinom da ne zaklanja sobe. */
@Composable
private fun CompassLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = modifier
            .padding(4.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

private val MeasureChipWidth = 88.dp
private val MeasureChipHeight = 32.dp

/** Stanje izmene jedne mere sobe: koja soba i koja mera. */
private data class MeasureEdit(val roomId: String, val kind: MeasureKind)

/**
 * Natpis sa merom uz zid izabrane sobe. Samo se crta: dodir se prepoznaje u gestu plana, da dodir na meru
 * ne bi izabrao ili ukinuo izbor sobe.
 */
@Composable
private fun MeasureChip(anchor: MeasureAnchor, room: Room, scale: Float, offset: Offset) {
    val density = LocalDensity.current
    val halfW = with(density) { MeasureChipWidth.toPx() } / 2f
    val halfH = with(density) { MeasureChipHeight.toPx() } / 2f
    val value = RoomMeasureEdit.valueOf(room, anchor.kind)
    val text = if (anchor.kind == MeasureKind.HEIGHT) {
        stringResource(R.string.plan_measure_height, formatMeters(value))
    } else {
        stringResource(R.string.plan_measure_chip, formatMeters(value))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset {
                IntOffset(
                    (offset.x + (anchor.xM * scale).toFloat() - halfW).roundToInt(),
                    (offset.y + (anchor.yM * scale).toFloat() - halfH).roundToInt()
                )
            }
            .size(MeasureChipWidth, MeasureChipHeight)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** Poruka ispod dugmadi za zaključavanje i lepljenje sobe. */
private enum class RoomNote { LOCKED, NO_NEIGHBOURS, GLUED }

@Composable
private fun roomNoteText(note: RoomNote): String = when (note) {
    RoomNote.LOCKED -> stringResource(R.string.plan_note_locked)
    RoomNote.NO_NEIGHBOURS -> stringResource(R.string.plan_note_no_neighbours)
    RoomNote.GLUED -> stringResource(R.string.plan_note_glued)
}

/** Ishod poslednjeg pokušaja priljubljivanja, za poruku ispod dugmeta. */
private sealed interface SnapFeedback {
    data class Moved(val neighborName: String, val thicknessM: Double) : SnapFeedback
    data object NoNeighbor : SnapFeedback
    data object NeedsThickness : SnapFeedback
}

@Composable
private fun snapFeedbackText(feedback: SnapFeedback): String = when (feedback) {
    is SnapFeedback.Moved -> stringResource(
        R.string.plan_snap_done,
        feedback.neighborName,
        formatCentimeters(feedback.thicknessM)
    )

    SnapFeedback.NoNeighbor -> stringResource(R.string.plan_snap_none)
    SnapFeedback.NeedsThickness -> stringResource(R.string.plan_snap_needs_thickness)
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
