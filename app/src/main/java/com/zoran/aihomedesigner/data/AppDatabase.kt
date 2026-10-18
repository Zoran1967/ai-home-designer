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
         * Verzija 2 -> 3: dodaje položaj sobe na planu (originXM, originYM, u metrima).
         * Postojeće sobe se slažu jedna pored druge duž x ose, istim redom kojim su pravljene,
         * sa razmakom od 0,5 m, da se ne preklapaju na planu.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `rooms` ADD COLUMN `originXM` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `rooms` ADD COLUMN `originYM` REAL NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE `rooms` SET `originXM` = COALESCE(" +
                        "(SELECT SUM(r2.`lengthM` + 0.5) FROM `rooms` AS r2 " +
                        "WHERE r2.`projectId` = `rooms`.`projectId` " +
                        "AND (r2.`createdAtMillis` < `rooms`.`createdAtMillis` " +
                        "OR (r2.`createdAtMillis` = `rooms`.`createdAtMillis` " +
                        "AND r2.`id` < `rooms`.`id`))), 0)"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
