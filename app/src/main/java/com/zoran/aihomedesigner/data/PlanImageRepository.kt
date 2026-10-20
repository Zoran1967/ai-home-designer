package com.zoran.aihomedesigner.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.ImageSampling
import com.zoran.aihomedesigner.domain.PlanImage
import com.zoran.aihomedesigner.domain.RoomPlacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/** Slika plana koja je kopirana u privatni prostor aplikacije, ali još nema razmeru (nije kalibrisana). */
data class ImportedImage(val fileName: String, val widthPx: Int, val heightPx: Int)

class PlanImageRepository(
    private val context: Context,
    private val dao: PlanImageDao
) {

    private companion object {
        const val MAX_FILE_BYTES = 40L * 1024 * 1024
        const val MAX_SIDE_PX = 12_000
    }

    private val directory: File
        get() = File(context.filesDir, "plan_images").also { it.mkdirs() }

    fun observe(projectId: String): Flow<PlanImage?> =
        dao.observeByProject(projectId).map { it?.toDomainOrNull() }

    /**
     * Kopira izabranu sliku u privatni prostor aplikacije i čita njene dimenzije u pikselima.
     * Vraća null ako se slika ne može pročitati, prevelika je ili nije slika.
     */
    suspend fun copyImage(projectId: String, uri: Uri): ImportedImage? = withContext(Dispatchers.IO) {
        val name = "plan_${projectId}_${System.currentTimeMillis()}.img"
        val target = File(directory, name)
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            input.use { source -> target.outputStream().use { out -> source.copyTo(out) } }

            if (target.length() > MAX_FILE_BYTES) {
                target.delete()
                return@withContext null
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                max(bounds.outWidth, bounds.outHeight) > MAX_SIDE_PX
            ) {
                target.delete()
                return@withContext null
            }
            ImportedImage(name, bounds.outWidth, bounds.outHeight)
        } catch (e: java.io.IOException) {
            target.delete()
            null
        } catch (e: SecurityException) {
            target.delete()
            null
        }
    }

    /** Čuva sliku sa razmerom (posle kalibracije). Prethodna slika projekta se briše. */
    suspend fun saveCalibrated(
        projectId: String,
        image: ImportedImage,
        metersPerPixel: Double,
        nowMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val old = dao.getByProject(projectId)
        dao.upsert(
            PlanImage(
                projectId = projectId,
                fileName = image.fileName,
                widthPx = image.widthPx,
                heightPx = image.heightPx,
                metersPerPixel = metersPerPixel,
                originXM = 0.0,
                originYM = 0.0,
                dimensionSource = DimensionSource.IMPORTED_CALIBRATED,
                createdAtMillis = nowMillis
            ).toEntity()
        )
        if (old != null && old.fileName != image.fileName) deleteFile(old.fileName)
    }

    /** Briše kopiju slike koja nije kalibrisana (npr. korisnik je odustao). */
    suspend fun discard(image: ImportedImage) = withContext(Dispatchers.IO) { deleteFile(image.fileName) }

    /** Pomera sliku na planu. Neispravne koordinate se odbacuju. */
    suspend fun moveImage(projectId: String, xM: Double, yM: Double) {
        if (!RoomPlacement.isValidCoordinate(xM) || !RoomPlacement.isValidCoordinate(yM)) return
        dao.updateOrigin(projectId, xM, yM)
    }

    /** Briše sliku plana projekta (red u bazi i fajl). */
    suspend fun deleteImage(projectId: String) = withContext(Dispatchers.IO) {
        val old = dao.getByProject(projectId) ?: return@withContext
        dao.deleteByProject(projectId)
        deleteFile(old.fileName)
    }

    /** Učitava sliku za prikaz, smanjenu tako da duža strana ne pređe [maxSidePx]. */
    suspend fun loadBitmap(fileName: String, maxSidePx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(directory, fileName)
        if (!file.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageSampling.sampleSize(bounds.outWidth, bounds.outHeight, maxSidePx)
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun deleteFile(fileName: String) {
        File(directory, fileName).delete()
    }
}
