package com.mnnode.app.session

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [
        Index("sceneId"),
        Index("kind"),
        Index("updatedAt"),
    ],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val sceneId: String,
    val kind: String,
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
        Index("sceneId"),
        Index("type"),
        Index("createdAt"),
    ],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String?,
    val sceneId: String,
    val type: String,
    val level: String,
    val payloadJson: String,
    val createdAt: Long,
)

@Entity(
    tableName = "assets",
    indices = [
        Index("sessionId"),
        Index("kind"),
    ],
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val kind: String,
    val path: String,
    val mimeType: String,
    val createdAt: Long,
)

