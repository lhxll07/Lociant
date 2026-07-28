package io.lociant.data.session

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {
    private lateinit var database: SessionDatabase
    private lateinit var dao: SessionDao

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SessionDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.sessionDao()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun deletingSessionAlsoDeletesMessagesAndSessionEvents() {
        dao.upsertSession(SessionEntity("chat_test", "Test", "model", 1, 1, "{}"))
        dao.insertMessage(MessageEntity(sessionId = "chat_test", role = "user", text = "hello", contentJson = "{}", status = "ok", createdAt = 1))
        dao.insertEvent(EventEntity(sessionId = "chat_test", type = "chat.completed", level = "info", payloadJson = "{}", createdAt = 1))

        dao.deleteSession("chat_test")

        assertNull(dao.session("chat_test"))
        assertEquals(0, dao.messages("chat_test").size)
        assertEquals(0, dao.eventCountBySession("chat_test"))
    }
}
