package com.zoran.aihomedesigner.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.PlanImage
import kotlinx.coroutines.flow.Flow

/** Red u tabeli "plan_images": najviše jedna slika plana po projektu. Brisanjem projekta briše se i red. */
@Entity(
    tableName = "plan_images",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlanImageEntity(
    @PrimaryKey val projectId: String,
    val fileName: String,
    val widthPx: Int,
    val heightPx: Int,
    val metersPerPixel: Double,
    val originXM: Double,
    val originYM: Double,
    val dimensionSource: String,
    val createdAtMillis: Long
)

fun PlanImage.toEntity(): PlanImageEntity = PlanImageEntity(
    projectId = projectId,
    fileName = fileName,
    widthPx = widthPx,
    heightPx = heightPx,
    metersPerPixel = metersPerPixel,
    originXM = originXM,
    originYM = originYM,
    dimensionSource = dimensionSource.name,
    createdAtMillis = createdAtMillis
)

/** Vraća null ako je izvor mere u bazi nepoznat, da ne prikažemo pogrešne podatke. */
fun PlanImageEntity.toDomainOrNull(): PlanImage? {
    val source = DimensionSource.values().firstOrNull { it.name == dimensionSource } ?: return null
    return PlanImage(
        projectId = projectId,
        fileName = fileName,
        widthPx = widthPx,
        heightPx = heightPx,
        metersPerPixel = metersPerPixel,
        originXM = originXM,
        originYM = originYM,
        dimensionSource = source,
        createdAtMillis = createdAtMillis
    )
}

@Dao
interface PlanImageDao {

    @Query("SELECT * FROM plan_images WHERE projectId = :projectId")
    fun observeByProject(projectId: String): Flow<PlanImageEntity?>

    @Query("SELECT * FROM plan_images WHERE projectId = :projectId")
    suspend fun getByProject(projectId: String): PlanImageEntity?

    /** Nova slika zamenjuje staru u istom projektu. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(image: PlanImageEntity)

    /** Menja samo položaj slike na planu. */
    @Query("UPDATE plan_images SET originXM = :xM, originYM = :yM WHERE projectId = :projectId")
    suspend fun updateOrigin(projectId: String, xM: Double, yM: Double)

    @Query("DELETE FROM plan_images WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: String)
}
