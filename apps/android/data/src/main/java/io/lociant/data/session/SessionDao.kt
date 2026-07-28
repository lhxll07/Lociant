package io.lociant.data.session

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSession(session: SessionEntity)

    @Insert
    fun insertMessage(message: MessageEntity): Long

    @Insert
    fun insertEvent(event: EventEntity): Long

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun session(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC LIMIT :limit")
    fun recentSessions(limit: Int): List<SessionEntity>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
    fun messages(sessionId: String): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId")
    fun messageCount(sessionId: String): Int

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY createdAt DESC, id DESC LIMIT 1")
    fun latestMessage(sessionId: String): MessageEntity?

    @Query("SELECT * FROM events WHERE type = :type ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun recentEventsByType(type: String, limit: Int): List<EventEntity>

    @Query("SELECT COUNT(*) FROM events WHERE type = :type")
    fun eventCountByType(type: String): Int

    @Query("SELECT COUNT(*) FROM events WHERE sessionId = :sessionId")
    fun eventCountBySession(sessionId: String): Int

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    fun deleteMessages(sessionId: String)

    @Query("DELETE FROM events WHERE sessionId = :sessionId")
    fun deleteEvents(sessionId: String)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    fun deleteSessionRow(sessionId: String)

    @Transaction
    fun deleteSession(sessionId: String) {
        deleteMessages(sessionId)
        deleteEvents(sessionId)
        deleteSessionRow(sessionId)
    }
}
