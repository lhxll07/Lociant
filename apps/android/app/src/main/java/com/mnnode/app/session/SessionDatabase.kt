package com.mnnode.app.session

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class,
        EventEntity::class,
        AssetEntity::class,
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
                    "mnnode-sessions.db",
                ).build().also { instance = it }
            }
        }
    }
}

