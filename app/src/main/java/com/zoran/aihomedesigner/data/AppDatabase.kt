package com.zoran.aihomedesigner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        RoomEntity::class,
        OpeningEntity::class,
        FurnitureEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao

    abstract fun roomDao(): RoomDao

    abstract fun openingDao(): OpeningDao

    abstract fun furnitureDao(): FurnitureDao

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

        /** Verzija 3 -> 4: dodaje tabelu otvora (vrata i prozori). Postojeći podaci ostaju netaknuti. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `openings` (" +
                        "`id` TEXT NOT NULL, " +
                        "`roomId` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`side` TEXT NOT NULL, " +
                        "`offsetM` REAL NOT NULL, " +
                        "`widthM` REAL NOT NULL, " +
                        "`heightM` REAL NOT NULL, " +
                        "`sillHeightM` REAL NOT NULL, " +
                        "`swing` TEXT, " +
                        "`hinge` TEXT, " +
                        "`dimensionSource` TEXT NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`roomId`) REFERENCES `rooms`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_openings_roomId` ON `openings` (`roomId`)"
                )
            }
        }

        /** Verzija 4 -> 5: dodaje tabelu nameštaja. Postojeći podaci ostaju netaknuti. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `furniture` (" +
                        "`id` TEXT NOT NULL, " +
                        "`roomId` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`lengthM` REAL NOT NULL, " +
                        "`widthM` REAL NOT NULL, " +
                        "`heightM` REAL NOT NULL, " +
                        "`xM` REAL NOT NULL, " +
                        "`yM` REAL NOT NULL, " +
                        "`quarterTurns` INTEGER NOT NULL, " +
                        "`dimensionSource` TEXT NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`roomId`) REFERENCES `rooms`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_furniture_roomId` ON `furniture` (`roomId`)"
                )
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
