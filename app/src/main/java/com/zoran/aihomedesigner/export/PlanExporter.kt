package com.zoran.aihomedesigner.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.PlanExportLayout
import com.zoran.aihomedesigner.domain.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object PlanExporter {

    enum class Format { PNG, PDF }

    /**
     * Pravi sliku (PNG) ili PDF plana u kešu aplikacije i vraća Uri za deljenje preko FileProvider-a.
     * Vraća null ako nema soba sa položajem na planu ili ako upis ne uspe.
     * Stariji izvezeni fajlovi se brišu pri svakom izvozu.
     */
    suspend fun export(
        context: Context,
        projectName: String,
        northLabel: String,
        footerLines: List<String>,
        rooms: List<Room>,
        openings: List<Opening>,
        furniture: List<Furniture>,
        format: Format,
        stairsLayer: PlanStairsLayer? = null
    ): Uri? = withContext(Dispatchers.IO) {
        val bounds = PlanExportLayout.bounds(rooms) ?: return@withContext null
        val landscape = (bounds.x1 - bounds.x0) >= (bounds.y1 - bounds.y0)
        val (width, height) = when (format) {
            Format.PNG -> if (landscape) 2000 to 1400 else 1400 to 2000
            Format.PDF -> if (landscape) 842 to 595 else 595 to 842
        }

        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }
        val safeName = projectName.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("").take(40)
        val extension = if (format == Format.PNG) "png" else "pdf"
        val file = File(directory, "plan_$safeName.$extension")

        try {
            when (format) {
                Format.PNG -> {
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    PlanRenderer.draw(
                        Canvas(bitmap), width, height, projectName, northLabel, footerLines,
                        rooms, openings, furniture, stairsLayer
                    )
                    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }

                Format.PDF -> {
                    val document = PdfDocument()
                    val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, 1).create())
                    PlanRenderer.draw(
                        page.canvas, width, height, projectName, northLabel, footerLines,
                        rooms, openings, furniture, stairsLayer
                    )
                    document.finishPage(page)
                    file.outputStream().use { document.writeTo(it) }
                    document.close()
                }
            }
        } catch (e: IOException) {
            file.delete()
            return@withContext null
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
