package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.domain.Camera3D
import com.zoran.aihomedesigner.domain.DepthOrder
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.Projection
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.SceneBox
import com.zoran.aihomedesigner.domain.SceneBuilder
import com.zoran.aihomedesigner.domain.SceneKind
import com.zoran.aihomedesigner.domain.Stairs
import com.zoran.aihomedesigner.domain.Vec3
import kotlin.math.max

private const val DEFAULT_YAW = 30f
private const val DEFAULT_PITCH = 35f

/**
 * Interaktivni 3D prikaz cele nekretnine, napravljen iz istih podataka kao 2D plan.
 * Crta se bez dodatnih biblioteka (perspektivna projekcija i crtanje kvadara od daljih ka bližim, vidi [DepthOrder]).
 */
@Composable
fun Scene3DScreen(
    rooms: List<Room>?,
    openings: List<Opening>,
    furniture: List<Furniture>,
    onBack: () -> Unit,
    stairs: List<Stairs> = emptyList()
) {
    val placedRooms = rooms.orEmpty().filter { it.isPlaced }
    val boxes = SceneBuilder.build(placedRooms, openings, furniture, stairs)

    // Središte i veličina scene, da kamera počne tamo gde su sobe
    val minX = boxes.minOfOrNull { it.min.x } ?: 0.0
    val maxX = boxes.maxOfOrNull { it.max.x } ?: 0.0
    val minY = boxes.minOfOrNull { it.min.y } ?: 0.0
    val maxY = boxes.maxOfOrNull { it.max.y } ?: 0.0
    val maxZ = boxes.maxOfOrNull { it.max.z } ?: 0.0
    val minZ = boxes.minOfOrNull { it.min.z } ?: 0.0
    val target = Vec3((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2)
    val extent = max(max(max(maxX - minX, maxY - minY), maxZ - minZ), 3.0)
    val initialDistance = (extent * 1.8 + 3.0).toFloat()

    val sceneKey = Triple(placedRooms.size, (extent * 10).toInt(), boxes.size)
    var yaw by remember(sceneKey) { mutableFloatStateOf(DEFAULT_YAW) }
    var pitch by remember(sceneKey) { mutableFloatStateOf(DEFAULT_PITCH) }
    var distance by remember(sceneKey) { mutableFloatStateOf(initialDistance) }

    val usesDefaultWall = placedRooms.any { it.wallThicknessM == null }
    val hasStairs = boxes.any { it.kind == SceneKind.STAIRS }

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
                    yaw = DEFAULT_YAW
                    pitch = DEFAULT_PITCH
                    distance = initialDistance
                }) { Text(stringResource(R.string.scene3d_reset)) }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                yaw -= pan.x * 0.4f
                                pitch = (pitch + pan.y * 0.4f).coerceIn(5f, 89f)
                                distance = (distance / zoom).coerceIn(2f, 300f)
                            }
                        }
                ) {
                    val camera = Camera3D(
                        yawDeg = yaw.toDouble(),
                        pitchDeg = pitch.toDouble(),
                        distanceM = distance.toDouble(),
                        target = target
                    )
                    drawScene(boxes, camera)
                }
                if (boxes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.scene3d_empty),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.scene3d_hint),
                    style = MaterialTheme.typography.bodySmall
                )
                if (usesDefaultWall) {
                    Text(
                        text = stringResource(R.string.scene3d_note_default_wall),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (hasStairs) {
                    Text(
                        text = stringResource(R.string.scene3d_note_stairs),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.scene3d_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun baseColor(kind: SceneKind): Color = when (kind) {
    SceneKind.FLOOR -> Color(0xFFEDE7DC)
    SceneKind.WALL -> Color(0xFFB0B0B0)
    SceneKind.DOOR -> Color(0xFF8D6E63)
    SceneKind.WINDOW -> Color(0xFF64B5F6)
    SceneKind.FURNITURE -> Color(0xFFFFB74D)
    SceneKind.STAIRS -> Color(0xFFD7B98E)
}

private fun shade(color: Color, factor: Float) =
    Color(color.red * factor, color.green * factor, color.blue * factor, color.alpha)

/** Crta kvadre od najdaljih ka najbližim (redosled određuje [DepthOrder]), a od svakog samo stranice okrenute ka kameri. */
private fun DrawScope.drawScene(boxes: List<SceneBox>, camera: Camera3D) {
    val w = size.width.toDouble()
    val h = size.height.toDouble()
    val eye = Projection.eye(camera)

    for (box in DepthOrder.farToNear(boxes, eye)) {
        val x0 = box.min.x; val x1 = box.max.x
        val y0 = box.min.y; val y1 = box.max.y
        val z0 = box.min.z; val z1 = box.max.z
        // Stranica: četiri ugla, spoljašnja normala i faktor osvetljenosti
        val sides = listOf(
            Triple(listOf(Vec3(x0, y0, z1), Vec3(x1, y0, z1), Vec3(x1, y1, z1), Vec3(x0, y1, z1)), Vec3(0.0, 0.0, 1.0), 1.0f),
            Triple(listOf(Vec3(x0, y0, z0), Vec3(x1, y0, z0), Vec3(x1, y0, z1), Vec3(x0, y0, z1)), Vec3(0.0, -1.0, 0.0), 0.82f),
            Triple(listOf(Vec3(x0, y1, z0), Vec3(x1, y1, z0), Vec3(x1, y1, z1), Vec3(x0, y1, z1)), Vec3(0.0, 1.0, 0.0), 0.82f),
            Triple(listOf(Vec3(x0, y0, z0), Vec3(x0, y1, z0), Vec3(x0, y1, z1), Vec3(x0, y0, z1)), Vec3(-1.0, 0.0, 0.0), 0.68f),
            Triple(listOf(Vec3(x1, y0, z0), Vec3(x1, y1, z0), Vec3(x1, y1, z1), Vec3(x1, y0, z1)), Vec3(1.0, 0.0, 0.0), 0.68f)
        )
        for ((corners, normal, light) in sides) {
            val centre = Vec3(
                corners.sumOf { it.x } / 4, corners.sumOf { it.y } / 4, corners.sumOf { it.z } / 4
            )
            val towardEye = (eye.x - centre.x) * normal.x + (eye.y - centre.y) * normal.y + (eye.z - centre.z) * normal.z
            if (towardEye <= 0.0) continue // okrenuta od kamere

            val projected = corners.map { Projection.project(it, camera, w, h) }
            if (projected.any { it == null }) continue
            val points = projected.map { Offset(it!!.x.toFloat(), it.y.toFloat()) }

            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                close()
            }
            drawPath(path, shade(baseColor(box.kind), light))
            drawPath(path, Color(0x55000000), style = Stroke(width = 1f))
        }
    }
}
