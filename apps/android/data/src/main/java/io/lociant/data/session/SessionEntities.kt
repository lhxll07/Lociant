package io.lociant.data.session

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [
        Index("updatedAt"),
    ],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val modelId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val metadataJson: String,
)

@Entity(
    tableName = "messages",
    indices = [
        Index("sessionId"),
        Index("createdAt"),
    ],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val role: String,
    val text: String,
    val contentJson: String,
    val status: String,
    val createdAt: Long,
)

@Entity(
    tableName = "events",
    indices = [
        Index("sessionId"),
        Index("type"),
        Index("createdAt"),
    ],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String?,
    val type: String,
    val level: String,
    val payloadJson: String,
    val createdAt: Long,
)
