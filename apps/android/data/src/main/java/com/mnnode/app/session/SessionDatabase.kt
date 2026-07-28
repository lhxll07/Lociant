package com.mnnode.app.session

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mnnode.app.config.RuntimeDefaults

@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class,
        EventEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var instance: SessionDatabase? = null

        fun get(context: Context): SessionDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SessionDatabase::class.java,
                    RuntimeDefaults.Sessions.DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS assets")
            }
        }
    }
}
