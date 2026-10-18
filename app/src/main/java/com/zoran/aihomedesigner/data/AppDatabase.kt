package com.zoran.aihomedesigner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProjectEntity::class, RoomEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao

    abstract fun roomDao(): RoomDao

    companion object {
        /** Verzija 1 -> 2: dodaje tabelu soba. Postojeći projekti ostaju netaknuti. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `rooms` (" +
                        "`id` TEXT NOT NULL, " +
                        "`projectId` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`lengthM` REAL NOT NULL, " +
                        "`widthM` REAL NOT NULL, " +
                        "`heightM` REAL NOT NULL, " +
                        "`wallThicknessM` REAL, " +
                        "`dimensionSource` TEXT NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_rooms_projectId` ON `rooms` (`projectId`)"
                )
            }
        }

        /**
         * Verzija 2 -> 3: dodaje položaj sobe na planu (xM, yM). Postojeće sobe dobijaju NULL,
         * što znači "još nema položaj", a ne položaj (0, 0).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `rooms` ADD COLUMN `xM` REAL")
                db.execSQL("ALTER TABLE `rooms` ADD COLUMN `yM` REAL")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_home_designer.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
