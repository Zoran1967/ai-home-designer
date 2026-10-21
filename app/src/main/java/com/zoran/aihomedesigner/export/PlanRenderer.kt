package com.zoran.aihomedesigner.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.zoran.aihomedesigner.domain.DoorSwingGeometry
import com.zoran.aihomedesigner.domain.ExportFrame
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.PlanExportLayout
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.SceneBox
import com.zoran.aihomedesigner.domain.SceneBuilder
import com.zoran.aihomedesigner.domain.SceneKind
import com.zoran.aihomedesigner.ui.formatMeters
import kotlin.math.atan2
import kotlin.math.min

/**
 * Crta plan odozgo na proizvoljnu površinu (slika ili PDF stranica), iz istih podataka kao ekran plana:
 * sobe, zidovi, vrata, prozori i nameštaj. Lenjir razmere i napomene su deo crteža.
 */
object PlanRenderer {

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        title: String,
        northLabel: String,
        footerLines: List<String>,
        rooms: List<Room>,
        openings: List<Opening>,
        furniture: List<Furniture>
    ) {
        canvas.drawColor(Color.WHITE)
        val w = width.toDouble()
        val h = height.toDouble()
        val unit = min(width, height) / 60f
        val margin = unit * 2.5f
        val lineHeight = unit * 1.4f

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        text.textSize = unit * 1.8f
        text.isFakeBoldText = true
        canvas.drawText(title, margin, margin + unit * 1.5f, text)
        text.textSize = unit * 1.1f
        text.textAlign = Paint.Align.RIGHT
        canvas.drawText(northLabel, width - margin, margin + unit * 1.5f, text)
        text.textAlign = Paint.Align.LEFT
        text.isFakeBoldText = false

        val bounds = PlanExportLayout.bounds(rooms) ?: return

        val footerTop = height - margin - footerLines.size * lineHeight
        val barY = footerTop - unit * 1.6f
        val frame = PlanExportLayout.frame(
            bounds = bounds,
            pageWidth = w,
            pageHeight = h,
            margin = margin.toDouble(),
            topReserved = (unit * 3.5f).toDouble(),
            bottomReserved = (height - barY + unit * 1.5f - margin).toDouble()
        )

        drawPlan(canvas, frame, unit, rooms, openings, furniture)
        drawScaleBar(canvas, frame, margin, barY, width * 0.25, unit)

        text.textSize = unit * 0.9f
        text.color = Color.DKGRAY
        footerLines.forEachIndexed { i, line ->
            canvas.drawText(line, margin, footerTop + (i + 1) * lineHeight - unit * 0.3f, text)
        }
    }

    private fun drawPlan(
        canvas: Canvas,
        frame: ExportFrame,
        unit: Float,
        rooms: List<Room>,
        openings: List<Opening>,
        furniture: List<Furniture>
    ) {
        val boxes = SceneBuilder.build(rooms, openings, furniture)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = unit * 0.08f
        }

        fun rect(box: SceneBox) = RectF(
            frame.x(box.min.x).toFloat(), frame.y(box.min.y).toFloat(),
            frame.x(box.max.x).toFloat(), frame.y(box.max.y).toFloat()
        )

        // Redosled crtanja: podovi, zidovi, nameštaj, pa vrata i prozori
        for (box in boxes.filter { it.kind == SceneKind.FLOOR }) {
            fill.color = Color.rgb(0xF5, 0xF1, 0xE8)
            canvas.drawRect(rect(box), fill)
        }
        for (box in boxes.filter { it.kind == SceneKind.WALL }) {
            fill.color = Color.rgb(0x55, 0x55, 0x55)
            canvas.drawRect(rect(box), fill)
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x4E, 0x34, 0x2E)
            textAlign = Paint.Align.CENTER
        }
        for (box in boxes.filter { it.kind == SceneKind.FURNITURE }) {
            val r = rect(box)
            fill.color = Color.rgb(0xFF, 0xCC, 0x80)
            canvas.drawRect(r, fill)
            stroke.color = Color.rgb(0xE6, 0x51, 0x00)
            canvas.drawRect(r, stroke)
            val name = box.label
            label.textSize = unit * 0.75f
            if (name != null && label.measureText(name) <= r.width() - unit * 0.3f && r.height() > unit) {
                canvas.drawText(name, r.centerX(), r.centerY() + label.textSize / 3f, label)
            }
        }
        for (box in boxes.filter { it.kind == SceneKind.DOOR || it.kind == SceneKind.WINDOW }) {
            fill.color = if (box.kind == SceneKind.DOOR) {
                Color.rgb(0x8D, 0x6E, 0x63)
            } else {
                Color.rgb(0x42, 0xA5, 0xF5)
            }
            canvas.drawRect(rect(box), fill)
        }

        drawDoorSwings(canvas, frame, unit, rooms, openings)
        drawRoomLabels(canvas, frame, unit, rooms)
    }

    private fun drawDoorSwings(
        canvas: Canvas,
        frame: ExportFrame,
        unit: Float,
        rooms: List<Room>,
        openings: List<Opening>
    ) {
        val roomsById = rooms.associateBy { it.id }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = unit * 0.1f
            color = Color.rgb(0x8D, 0x6E, 0x63)
        }
        for (opening in openings) {
            val room = roomsById[opening.roomId] ?: continue
            val shape = DoorSwingGeometry.shape(opening, room) ?: continue
            val hx = frame.x(shape.hinge.xM).toFloat()
            val hy = frame.y(shape.hinge.yM).toFloat()
            val radius = (shape.radiusM * frame.scale).toFloat()
            val startAngle = Math.toDegrees(
                atan2(frame.y(shape.closedEnd.yM) - hy, frame.x(shape.closedEnd.xM) - hx)
            ).toFloat()
            val endAngle = Math.toDegrees(
                atan2(frame.y(shape.openEnd.yM) - hy, frame.x(shape.openEnd.xM) - hx)
            ).toFloat()
            var sweep = endAngle - startAngle
            while (sweep > 180f) sweep -= 360f
            while (sweep <= -180f) sweep += 360f
            canvas.drawArc(RectF(hx - radius, hy - radius, hx + radius, hy + radius), startAngle, sweep, false, paint)
            canvas.drawLine(
                hx, hy,
                frame.x(shape.openEnd.xM).toFloat(), frame.y(shape.openEnd.yM).toFloat(),
                paint
            )
        }
    }

    private fun drawRoomLabels(canvas: Canvas, frame: ExportFrame, unit: Float, rooms: List<Room>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        for (room in rooms.filter { it.isPlaced }) {
            val left = frame.x(room.xM!!).toFloat()
            val top = frame.y(room.yM!!).toFloat()
            val roomWidthPx = (room.lengthM * frame.scale).toFloat()
            val cx = left + roomWidthPx / 2f
            val cy = top + (room.widthM * frame.scale).toFloat() / 2f

            paint.textSize = unit * 1.1f
            paint.isFakeBoldText = true
            if (paint.measureText(room.name) <= roomWidthPx) {
                canvas.drawText(room.name, cx, cy - unit * 0.3f, paint)
            }
            paint.textSize = unit * 0.9f
            paint.isFakeBoldText = false
            val size = "${formatMeters(room.lengthM)} × ${formatMeters(room.widthM)} m"
            if (paint.measureText(size) <= roomWidthPx) {
                canvas.drawText(size, cx, cy + unit * 1.0f, paint)
            }
        }
    }

    private fun drawScaleBar(canvas: Canvas, frame: ExportFrame, x: Float, y: Float, maxLengthPx: Double, unit: Float) {
        val meters = PlanExportLayout.scaleBarMeters(frame.scale, maxLengthPx)
        val length = (meters * frame.scale).toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = unit * 0.12f
        }
        canvas.drawLine(x, y, x + length, y, paint)
        canvas.drawLine(x, y - unit * 0.4f, x, y + unit * 0.4f, paint)
        canvas.drawLine(x + length, y - unit * 0.4f, x + length, y + unit * 0.4f, paint)
        paint.textSize = unit * 0.9f
        canvas.drawText("${formatMeters(meters)} m", x + length + unit * 0.6f, y + unit * 0.3f, paint)
    }
}
