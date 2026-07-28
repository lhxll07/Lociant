package io.lociant.data.session

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.lociant.core.config.RuntimeDefaults

@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class,
        EventEntity::class,
    ],
    version = 1,
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
                ).build().also { instance = it }
            }
        }
    }
}
