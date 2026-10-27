package com.zoran.aihomedesigner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zoran.aihomedesigner.data.ImportedImage
import com.zoran.aihomedesigner.domain.Floors
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.export.PlanExporter
import com.zoran.aihomedesigner.export.PlanStairsLayer
import com.zoran.aihomedesigner.domain.FurnitureLayoutValidator
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.PlanImage
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.Stairs
import com.zoran.aihomedesigner.domain.StairsPlanning
import com.zoran.aihomedesigner.ui.CalibrationScreen
import com.zoran.aihomedesigner.ui.CrashScreen
import com.zoran.aihomedesigner.ui.CreateProjectScreen
import com.zoran.aihomedesigner.ui.FurnitureFormScreen
import com.zoran.aihomedesigner.ui.FurnitureListScreen
import com.zoran.aihomedesigner.ui.OpeningFormScreen
import com.zoran.aihomedesigner.ui.OpeningListScreen
import com.zoran.aihomedesigner.ui.PlanScreen
import com.zoran.aihomedesigner.ui.AiVisualScreen
import com.zoran.aihomedesigner.ui.Scene3DScreen
import com.zoran.aihomedesigner.ui.ProjectListScreen
import com.zoran.aihomedesigner.ui.ProjectsViewModel
import com.zoran.aihomedesigner.ui.RoomFormScreen
import com.zoran.aihomedesigner.ui.PdfPageDialog
import com.zoran.aihomedesigner.ui.RoomListScreen
import com.zoran.aihomedesigner.ui.StairsFormScreen
import com.zoran.aihomedesigner.ui.StairsListScreen
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private enum class Screen {
    PROJECTS,
    CREATE_PROJECT,
    ROOMS,
    CREATE_ROOM,
    EDIT_ROOM,
    PLAN,
    SCENE3D,
    CALIBRATE,
    OPENINGS,
    CREATE_OPENING,
    EDIT_OPENING,
    FURNITURE,
    CREATE_FURNITURE,
    EDIT_FURNITURE,
    STAIRS,
    CREATE_STAIRS,
    EDIT_STAIRS,
    AI_VISUAL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Ako se aplikacija prošli put neočekivano zatvorila, prvo prikaži opis greške.
                    var crashReport by remember { mutableStateOf(CrashReporter.readPending(applicationContext)) }
                    val report = crashReport
                    if (report != null) {
                        CrashScreen(
                            report = report,
                            onContinue = {
                                CrashReporter.clear(applicationContext)
                                crashReport = null
                            }
                        )
                    } else {
                        AppRoot()
                    }
                }
            }
        }
    }
}

/** Čuva sliku koja čeka kalibraciju, da preživi okretanje ekrana. */
private val ImportedImageSaver = listSaver<ImportedImage?, Any>(
    save = { image ->
        if (image == null) emptyList() else listOf(image.fileName, image.widthPx, image.heightPx)
    },
    restore = { list ->
        if (list.size == 3) ImportedImage(list[0] as String, list[1] as Int, list[2] as Int) else null
    }
)

@Composable
private fun AppRoot(viewModel: ProjectsViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf(Screen.PROJECTS) }
    var selectedProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedOpeningId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFurnitureId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStairsId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFloor by rememberSaveable { mutableIntStateOf(0) }

    val projects by viewModel.projects.collectAsState()
    val selectedProject = projects?.firstOrNull { it.id == selectedProjectId }

    val context = LocalContext.current
    var pendingImage by rememberSaveable(stateSaver = ImportedImageSaver) {
        mutableStateOf<ImportedImage?>(null)
    }
    val planImageFlow = remember(selectedProjectId) {
        selectedProjectId?.let { viewModel.observePlanImage(it) } ?: flowOf<PlanImage?>(null)
    }
    val planImage by planImageFlow.collectAsState(initial = null)

    // PDF sa više strana: korisnik bira stranu pre uvoza (uri i broj strana PDF-a)
    var pendingPdf by remember { mutableStateOf<Pair<Uri, Int>?>(null) }

    fun importPlan(projectId: String, uri: Uri, pageNumber: Int, announcePage: Boolean) {
        viewModel.importPlanImage(projectId, uri, pageNumber) { imported ->
            if (imported != null) {
                pendingImage = imported
                screen = Screen.CALIBRATE
                if (announcePage) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.plan_image_pdf_page_imported, pageNumber),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(context, R.string.plan_image_import_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Izbor slike ili PDF-a plana: datoteka se kopira u aplikaciju, pa ide na kalibraciju razmere.
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val projectId = selectedProjectId
        if (uri != null && projectId != null) {
            if (context.contentResolver.getType(uri) == "application/pdf") {
                viewModel.pdfPageCount(uri) { count ->
                    if (count != null && count > 1) {
                        pendingPdf = uri to count
                    } else {
                        importPlan(projectId, uri, 1, announcePage = false)
                    }
                }
            } else {
                importPlan(projectId, uri, 1, announcePage = false)
            }
        }
    }

    pendingPdf?.let { (pdfUri, pageCount) ->
        PdfPageDialog(
            pageCount = pageCount,
            onConfirm = { page ->
                pendingPdf = null
                selectedProjectId?.let { importPlan(it, pdfUri, page, announcePage = true) }
            },
            onCancel = { pendingPdf = null }
        )
    }

    val roomsFlow = remember(selectedProjectId) {
        selectedProjectId?.let { viewModel.observeRooms(it) } ?: flowOf<List<Room>?>(null)
    }
    val rooms by roomsFlow.collectAsState(initial = null)
    val selectedRoom = rooms?.firstOrNull { it.id == selectedRoomId }

    // Sobe izabranog sprata. Plan, 3D prikaz i izvoz rade sa spratom koji je izabran u listi soba.
    val floorRooms = rooms?.filter { it.floorLevel == selectedFloor }
    val floorLevels = (Floors.levels(rooms.orEmpty()) + selectedFloor).distinct().sorted()
    val nextFloorAbove = (floorLevels.max() + 1).takeIf { Floors.isValidLevel(it) }
    val nextFloorBelow = (floorLevels.min() - 1).takeIf { Floors.isValidLevel(it) }
    // Spratovi na koje se soba može prebaciti: postojeći, plus jedan novi iznad i jedan ispod
    val moveTargets = (floorLevels + listOfNotNull(nextFloorAbove, nextFloorBelow))
        .distinct().sorted().filter { it != selectedFloor }

    // Otvori izabrane sobe (za ekran vrata i prozora) i svi otvori projekta (za plan).
    val roomOpeningsFlow = remember(selectedRoomId) {
        selectedRoomId?.let { viewModel.observeOpenings(it) } ?: flowOf<List<Opening>?>(null)
    }
    val roomOpenings by roomOpeningsFlow.collectAsState(initial = null)
    val openingToEdit = roomOpenings?.firstOrNull { it.id == selectedOpeningId }

    val roomFurnitureFlow = remember(selectedRoomId) {
        selectedRoomId?.let { viewModel.observeFurniture(it) } ?: flowOf<List<Furniture>?>(null)
    }
    val roomFurniture by roomFurnitureFlow.collectAsState(initial = null)
    val furnitureToEdit = roomFurniture?.firstOrNull { it.id == selectedFurnitureId }

    val projectOpeningsFlow = remember(selectedProjectId) {
        selectedProjectId?.let { viewModel.observeProjectOpenings(it) } ?: flowOf<List<Opening>?>(null)
    }
    val projectOpenings by projectOpeningsFlow.collectAsState(initial = null)

    val projectFurnitureFlow = remember(selectedProjectId) {
        selectedProjectId?.let { viewModel.observeProjectFurniture(it) } ?: flowOf<List<Furniture>?>(null)
    }
    val projectFurniture by projectFurnitureFlow.collectAsState(initial = null)

    val projectStairsFlow = remember(selectedProjectId) {
        selectedProjectId?.let { viewModel.observeStairs(it) } ?: flowOf<List<Stairs>?>(null)
    }
    val projectStairs by projectStairsFlow.collectAsState(initial = null)
    val stairsToEdit = projectStairs?.firstOrNull { it.id == selectedStairsId }
    // Donji spratovi koje stepenice mogu da povezuju: postojeći spratovi i prvi ispod njih
    val stairsLevels = (floorLevels + listOfNotNull(nextFloorBelow))
        .filter { Floors.isValidLevel(it) && Floors.isValidLevel(it + 1) }
        .distinct().sorted()

    // Izvoz plana u sliku ili PDF i deljenje preko Android prozora za deljenje
    val scope = rememberCoroutineScope()
    fun exportPlan(format: PlanExporter.Format) {
        val project = selectedProject ?: return
        scope.launch {
            val uri = PlanExporter.export(
                context = context,
                projectName = project.name,
                northLabel = context.getString(R.string.compass_north),
                footerLines = listOf(
                    context.getString(R.string.export_note_layout),
                    context.getString(R.string.export_note_dimensions)
                ),
                rooms = floorRooms.orEmpty(),
                openings = projectOpenings.orEmpty(),
                furniture = projectFurniture.orEmpty(),
                format = format,
                stairsLayer = PlanStairsLayer(
                    stairs = projectStairs.orEmpty(),
                    floorLevel = selectedFloor,
                    allRooms = rooms.orEmpty(),
                    upLabel = context.getString(R.string.export_stairs_up),
                    fromLabel = context.getString(R.string.export_stairs_from)
                )
            )
            if (uri == null) {
                Toast.makeText(context, R.string.export_failed, Toast.LENGTH_LONG).show()
                return@launch
            }
            val mime = if (format == PlanExporter.Format.PNG) "image/png" else "application/pdf"
            val send = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, context.getString(R.string.export_share_title)))
        }
    }

    BackHandler(enabled = screen != Screen.PROJECTS) {
        screen = when (screen) {
            Screen.CREATE_ROOM, Screen.EDIT_ROOM, Screen.PLAN, Screen.SCENE3D, Screen.OPENINGS, Screen.FURNITURE,
            Screen.STAIRS, Screen.AI_VISUAL -> Screen.ROOMS
            Screen.CREATE_OPENING, Screen.EDIT_OPENING -> Screen.OPENINGS
            Screen.CREATE_FURNITURE, Screen.EDIT_FURNITURE -> Screen.FURNITURE
            Screen.CREATE_STAIRS, Screen.EDIT_STAIRS -> Screen.STAIRS
            else -> {
                selectedProjectId = null
                Screen.PROJECTS
            }
        }
    }

    // Odustajanje od kalibracije (dugme nazad) briše kopiju slike.
    BackHandler(enabled = screen == Screen.CALIBRATE) {
        pendingImage?.let { viewModel.discardImportedImage(it) }
        pendingImage = null
        screen = Screen.ROOMS
    }

    // Ako je izabrani projekat, soba ili otvor nestao (npr. obrisan), vrati se korak nazad.
    LaunchedEffect(
        projects, rooms, roomOpenings, roomFurniture, projectStairs, pendingImage,
        selectedProjectId, selectedRoomId, selectedOpeningId, selectedFurnitureId, selectedStairsId, screen
    ) {
        val needsProject = screen != Screen.PROJECTS && screen != Screen.CREATE_PROJECT
        val needsRoom = screen == Screen.EDIT_ROOM || screen == Screen.OPENINGS ||
            screen == Screen.CREATE_OPENING || screen == Screen.EDIT_OPENING ||
            screen == Screen.FURNITURE || screen == Screen.CREATE_FURNITURE || screen == Screen.EDIT_FURNITURE
        if (needsProject && projects != null && selectedProject == null) {
            selectedProjectId = null
            screen = Screen.PROJECTS
        } else if (needsRoom && rooms != null && selectedRoom == null) {
            screen = Screen.ROOMS
        } else if (screen == Screen.EDIT_OPENING && roomOpenings != null && openingToEdit == null) {
            screen = Screen.OPENINGS
        } else if (screen == Screen.EDIT_FURNITURE && roomFurniture != null && furnitureToEdit == null) {
            screen = Screen.FURNITURE
        } else if (screen == Screen.EDIT_STAIRS && projectStairs != null && stairsToEdit == null) {
            screen = Screen.STAIRS
        } else if (screen == Screen.CALIBRATE && pendingImage == null) {
            screen = Screen.ROOMS
        }
    }

    when (screen) {
        Screen.PROJECTS -> ProjectListScreen(
            projects = projects,
            onNewProject = { screen = Screen.CREATE_PROJECT },
            onOpenProject = {
                selectedProjectId = it.id
                selectedFloor = 0
                screen = Screen.ROOMS
            },
            onDeleteProject = { viewModel.deleteProject(it.id) }
        )

        Screen.CREATE_PROJECT -> CreateProjectScreen(
            onCreate = { name, type, onResult -> viewModel.createProject(name, type, onResult) },
            onDone = { screen = Screen.PROJECTS },
            onCancel = { screen = Screen.PROJECTS }
        )

        Screen.ROOMS -> selectedProject?.let { project ->
            RoomListScreen(
                projectName = project.name,
                rooms = floorRooms,
                floorLevels = floorLevels,
                selectedFloor = selectedFloor,
                onSelectFloor = { selectedFloor = it },
                onAddFloorAbove = nextFloorAbove?.let { level -> { selectedFloor = level } },
                onAddFloorBelow = nextFloorBelow?.let { level -> { selectedFloor = level } },
                onBack = {
                    selectedProjectId = null
                    screen = Screen.PROJECTS
                },
                onAddRoom = { screen = Screen.CREATE_ROOM },
                onOpenRoom = {
                    selectedRoomId = it.id
                    screen = Screen.EDIT_ROOM
                },
                onOpenPlan = { screen = Screen.PLAN },
                onOpenScene3D = { screen = Screen.SCENE3D },
                onOpenStairs = { screen = Screen.STAIRS },
                onOpenAiVisual = { screen = Screen.AI_VISUAL },
                planImage = planImage,
                onImportPlanImage = { pickImage.launch(arrayOf("image/*", "application/pdf")) },
                onRemovePlanImage = { selectedProjectId?.let { viewModel.deletePlanImage(it) } },
                onExportPng = { exportPlan(PlanExporter.Format.PNG) },
                onExportPdf = { exportPlan(PlanExporter.Format.PDF) },
                onOpenOpenings = {
                    selectedRoomId = it.id
                    screen = Screen.OPENINGS
                },
                onOpenFurniture = {
                    selectedRoomId = it.id
                    screen = Screen.FURNITURE
                },
                moveTargets = moveTargets,
                onMoveRoomToFloor = { room, level, onResult ->
                    viewModel.moveRoomToFloor(room.id, level, onResult)
                },
                onDeleteRoom = { viewModel.deleteRoom(it.id) }
            )
        }

        Screen.CREATE_ROOM -> selectedProject?.let { project ->
            RoomFormScreen(
                initialRoom = null,
                onSubmit = { input, onResult ->
                    viewModel.createRoom(project.id, input, onResult, floorLevel = selectedFloor)
                },
                onDone = { screen = Screen.ROOMS },
                onCancel = { screen = Screen.ROOMS }
            )
        }

        Screen.EDIT_ROOM -> selectedRoom?.let { room ->
            RoomFormScreen(
                initialRoom = room,
                onSubmit = { input, onResult -> viewModel.updateRoom(room.id, input, onResult) },
                onDone = { screen = Screen.ROOMS },
                onCancel = { screen = Screen.ROOMS }
            )
        }

        Screen.PLAN -> selectedProject?.let { project ->
            // Slika plana se učitava samo dok je plan otvoren, da ne zauzima memoriju bez potrebe
            val planBitmap by produceState<ImageBitmap?>(initialValue = null, planImage?.fileName) {
                value = planImage?.let { viewModel.loadPlanBitmap(it.fileName, 2048)?.asImageBitmap() }
            }
            PlanScreen(
                rooms = floorRooms,
                openings = projectOpenings.orEmpty(),
                furniture = projectFurniture.orEmpty(),
                stairs = projectStairs.orEmpty(),
                floorLevel = selectedFloor,
                allRooms = rooms.orEmpty(),
                planImage = planImage,
                planBitmap = planBitmap,
                onMovePlanImage = { x, y -> selectedProjectId?.let { viewModel.movePlanImage(it, x, y) } },
                onBack = { screen = Screen.ROOMS },
                onMoveRoom = { id, x, y -> viewModel.moveRoom(id, x, y) },
                onSetRoomLocked = { id, locked -> viewModel.setRoomLocked(id, locked) },
                onGlueRoom = { id, onResult -> viewModel.glueRoom(id, onResult) },
                onUnglueRoom = { id -> viewModel.unglueRoom(id) },
                onPlaceUnplaced = { viewModel.placeUnplacedRooms(project.id) },
                onMoveFurniture = { id, x, y, rotation -> viewModel.moveFurniture(id, x, y, rotation) },
                onMoveStairs = { id, x, y -> viewModel.moveStairs(id, x, y) },
                onCreateDrawnRoom = { input, position, onResult ->
                    viewModel.createRoom(project.id, input, onResult, floorLevel = selectedFloor, position = position)
                },
                onUpdateRoom = { roomId, input, onResult -> viewModel.updateRoom(roomId, input, onResult) },
                onCreateOpening = { roomId, input, onResult -> viewModel.createOpening(roomId, input, onResult) }
            )
        }

        Screen.SCENE3D -> selectedProject?.let {
            Scene3DScreen(
                rooms = rooms,
                openings = projectOpenings.orEmpty(),
                furniture = projectFurniture.orEmpty(),
                onBack = { screen = Screen.ROOMS },
                stairs = projectStairs.orEmpty()
            )
        }

        Screen.AI_VISUAL -> selectedProject?.let {
            AiVisualScreen(
                rooms = rooms,
                openings = projectOpenings.orEmpty(),
                furniture = projectFurniture.orEmpty(),
                stairs = projectStairs.orEmpty(),
                onBack = { screen = Screen.ROOMS }
            )
        }

        Screen.OPENINGS -> selectedRoom?.let { room ->
            OpeningListScreen(
                room = room,
                openings = roomOpenings,
                onBack = { screen = Screen.ROOMS },
                onAdd = { screen = Screen.CREATE_OPENING },
                onOpen = {
                    selectedOpeningId = it.id
                    screen = Screen.EDIT_OPENING
                },
                onDelete = { viewModel.deleteOpening(it.id) }
            )
        }

        Screen.CREATE_OPENING -> selectedRoom?.let { room ->
            OpeningFormScreen(
                room = room,
                initialOpening = null,
                onSubmit = { input, onResult -> viewModel.createOpening(room.id, input, onResult) },
                onDone = { screen = Screen.OPENINGS },
                onCancel = { screen = Screen.OPENINGS }
            )
        }

        Screen.EDIT_OPENING -> {
            val room = selectedRoom
            val opening = openingToEdit
            if (room != null && opening != null) {
                OpeningFormScreen(
                    room = room,
                    initialOpening = opening,
                    onSubmit = { input, onResult -> viewModel.updateOpening(opening.id, input, onResult) },
                    onDone = { screen = Screen.OPENINGS },
                    onCancel = { screen = Screen.OPENINGS }
                )
            }
        }

        Screen.FURNITURE -> selectedRoom?.let { room ->
            FurnitureListScreen(
                room = room,
                furniture = roomFurniture,
                issues = FurnitureLayoutValidator.validate(
                    room,
                    roomFurniture.orEmpty(),
                    roomOpenings.orEmpty()
                ),
                onBack = { screen = Screen.ROOMS },
                onAdd = { screen = Screen.CREATE_FURNITURE },
                onOpen = {
                    selectedFurnitureId = it.id
                    screen = Screen.EDIT_FURNITURE
                },
                onDelete = { viewModel.deleteFurniture(it.id) }
            )
        }

        Screen.CREATE_FURNITURE -> selectedRoom?.let { room ->
            FurnitureFormScreen(
                room = room,
                initialFurniture = null,
                onSubmit = { input, onResult -> viewModel.createFurniture(room.id, input, onResult) },
                onDone = { screen = Screen.FURNITURE },
                onCancel = { screen = Screen.FURNITURE }
            )
        }

        Screen.EDIT_FURNITURE -> {
            val room = selectedRoom
            val item = furnitureToEdit
            if (room != null && item != null) {
                FurnitureFormScreen(
                    room = room,
                    initialFurniture = item,
                    onSubmit = { input, onResult -> viewModel.updateFurniture(item.id, input, onResult) },
                    onDone = { screen = Screen.FURNITURE },
                    onCancel = { screen = Screen.FURNITURE }
                )
            }
        }

        Screen.STAIRS -> selectedProject?.let { project ->
            StairsListScreen(
                projectName = project.name,
                stairs = projectStairs,
                rooms = rooms.orEmpty(),
                onBack = { screen = Screen.ROOMS },
                onAdd = { screen = Screen.CREATE_STAIRS },
                onEdit = {
                    selectedStairsId = it.id
                    screen = Screen.EDIT_STAIRS
                },
                onDelete = { viewModel.deleteStairs(it.id) }
            )
        }

        Screen.CREATE_STAIRS -> selectedProject?.let { project ->
            StairsFormScreen(
                initialStairs = null,
                levels = stairsLevels,
                defaultLevel = selectedFloor,
                onSubmit = { input, onResult ->
                    val spot = StairsPlanning.defaultSpot(rooms.orEmpty(), input.lowerLevel)
                    viewModel.createStairs(project.id, input, spot.xM, spot.yM, onResult)
                },
                onDone = { screen = Screen.STAIRS },
                onCancel = { screen = Screen.STAIRS }
            )
        }

        Screen.EDIT_STAIRS -> {
            val item = stairsToEdit
            if (item != null) {
                StairsFormScreen(
                    initialStairs = item,
                    levels = stairsLevels,
                    defaultLevel = item.lowerLevel,
                    onSubmit = { input, onResult -> viewModel.updateStairs(item.id, input, onResult) },
                    onDone = { screen = Screen.STAIRS },
                    onCancel = { screen = Screen.STAIRS }
                )
            }
        }

        Screen.CALIBRATE -> {
            val image = pendingImage
            val projectId = selectedProjectId
            if (image != null && projectId != null) {
                CalibrationScreen(
                    image = image,
                    loadBitmap = { name, maxSide -> viewModel.loadPlanBitmap(name, maxSide) },
                    onSave = { metersPerPixel ->
                        viewModel.savePlanCalibration(projectId, image, metersPerPixel) {
                            pendingImage = null
                            screen = Screen.ROOMS
                        }
                    },
                    onCancel = {
                        viewModel.discardImportedImage(image)
                        pendingImage = null
                        screen = Screen.ROOMS
                    }
                )
            }
        }
    }

    // Upis u bazu nije uspeo (npr. pun telefon): jasna poruka umesto zatvaranja aplikacije
    val saveFailed by viewModel.saveFailed.collectAsState()
    if (saveFailed) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaveFailed() },
            title = { Text(stringResource(R.string.save_failed_title)) },
            text = { Text(stringResource(R.string.save_failed_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSaveFailed() }) {
                    Text(stringResource(R.string.save_failed_close))
                }
            }
        )
    }
}
