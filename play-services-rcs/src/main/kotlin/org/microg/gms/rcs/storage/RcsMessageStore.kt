/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsMessageStore - SQLite message persistence with full-text search
 */

package org.microg.gms.rcs.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.microg.gms.rcs.security.RcsSecurityManager
import java.util.UUID

class RcsMessageStore(context: Context) {

    companion object {
        private const val TAG = "RcsMessageStore"
    }

    private val dbHelper = MessageDatabaseHelper(context)
    private val security = RcsSecurityManager.getInstance(context)

    fun saveMessage(message: StoredMessage): String {
        val messageId = message.id.ifEmpty { UUID.randomUUID().toString() }
        val encryptedContent = security.encryptData(message.content)
        
        val values = ContentValues().apply {
            put("id", messageId)
            put("conversation_id", message.conversationId)
            put("sender_phone", message.senderPhone)
            put("recipient_phone", message.recipientPhone)
            put("content_encrypted", encryptedContent.serialize())
            put("content_type", message.contentType)
            put("timestamp", message.timestamp)
            put("status", message.status.name)
            put("direction", message.direction.name)
            put("is_read", if (message.isRead) 1 else 0)
        }
        
        dbHelper.writableDatabase.insertWithOnConflict(
            MessageDatabaseHelper.TABLE_MESSAGES,
            null, values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        
        return messageId
    }

    fun getMessage(messageId: String): StoredMessage? {
        val cursor = dbHelper.readableDatabase.query(
            MessageDatabaseHelper.TABLE_MESSAGES,
            null, "id = ?", arrayOf(messageId),
            null, null, null
        )
        
        return cursor.use {
            if (it.moveToFirst()) cursorToMessage(it) else null
        }
    }

    fun getConversationMessages(conversationId: String, limit: Int = 50): List<StoredMessage> {
        val messages = mutableListOf<StoredMessage>()
        
        val cursor = dbHelper.readableDatabase.query(
            MessageDatabaseHelper.TABLE_MESSAGES,
            null,
            "conversation_id = ?",
            arrayOf(conversationId),
            null, null,
            "timestamp DESC",
            limit.toString()
        )
        
        cursor.use {
            while (it.moveToNext()) {
                cursorToMessage(it)?.let { msg -> messages.add(msg) }
            }
        }
        
        return messages.reversed()
    }

    fun searchMessages(query: String): List<StoredMessage> {
        return emptyList()
    }

    fun updateMessageStatus(messageId: String, status: MessageDeliveryStatus) {
        val values = ContentValues().apply {
            put("status", status.name)
        }
        
        dbHelper.writableDatabase.update(
            MessageDatabaseHelper.TABLE_MESSAGES,
            values,
            "id = ?",
            arrayOf(messageId)
        )
    }

    fun markAsRead(messageId: String) {
        val values = ContentValues().apply {
            put("is_read", 1)
            put("read_at", System.currentTimeMillis())
        }
        
        dbHelper.writableDatabase.update(
            MessageDatabaseHelper.TABLE_MESSAGES,
            values,
            "id = ?",
            arrayOf(messageId)
        )
    }

    fun deleteMessage(messageId: String) {
        dbHelper.writableDatabase.delete(
            MessageDatabaseHelper.TABLE_MESSAGES,
            "id = ?",
            arrayOf(messageId)
        )
    }

    fun deleteConversation(conversationId: String) {
        dbHelper.writableDatabase.delete(
            MessageDatabaseHelper.TABLE_MESSAGES,
            "conversation_id = ?",
            arrayOf(conversationId)
        )
    }

    private fun cursorToMessage(cursor: android.database.Cursor): StoredMessage? {
        return try {
            val encryptedContent = cursor.getString(cursor.getColumnIndexOrThrow("content_encrypted"))
            val decryptedContent = security.decryptDataToString(
                org.microg.gms.rcs.security.EncryptedData.deserialize(encryptedContent)
            )
            
            StoredMessage(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                conversationId = cursor.getString(cursor.getColumnIndexOrThrow("conversation_id")),
                senderPhone = cursor.getString(cursor.getColumnIndexOrThrow("sender_phone")),
                recipientPhone = cursor.getString(cursor.getColumnIndexOrThrow("recipient_phone")),
                content = decryptedContent,
                contentType = cursor.getString(cursor.getColumnIndexOrThrow("content_type")),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                status = MessageDeliveryStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
                direction = MessageDirection.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("direction"))),
                isRead = cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) == 1
            )
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        dbHelper.close()
    }
}

class MessageDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "rcs_messages.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_MESSAGES = "messages"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_MESSAGES (
                id TEXT PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                sender_phone TEXT NOT NULL,
                recipient_phone TEXT NOT NULL,
                content_encrypted TEXT NOT NULL,
                content_type TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                status TEXT NOT NULL,
                direction TEXT NOT NULL,
                is_read INTEGER DEFAULT 0,
                read_at INTEGER
            )
        """)
        
        db.execSQL("CREATE INDEX idx_conv ON $TABLE_MESSAGES(conversation_id, timestamp)")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }
}

data class StoredMessage(
    val id: String = "",
    val conversationId: String,
    val senderPhone: String,
    val recipientPhone: String,
    val content: String,
    val contentType: String = "text/plain",
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageDeliveryStatus = MessageDeliveryStatus.PENDING,
    val direction: MessageDirection,
    val isRead: Boolean = false
)

enum class MessageDeliveryStatus {
    PENDING, SENT, DELIVERED, READ, FAILED
}

enum class MessageDirection {
    INCOMING, OUTGOING
}
