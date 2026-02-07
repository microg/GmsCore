/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsConversationManager - Manages conversations and threads
 */

package org.microg.gms.rcs.conversation

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.microg.gms.rcs.storage.MessageDeliveryStatus
import org.microg.gms.rcs.storage.MessageDirection
import org.microg.gms.rcs.storage.StoredMessage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RcsConversationManager(context: Context) {

    companion object {
        private const val TAG = "RcsConversation"
    }

    private val dbHelper = ConversationDatabaseHelper(context)
    private val conversationCache = ConcurrentHashMap<String, Conversation>()

    fun getOrCreateConversation(participantPhone: String): Conversation {
        val conversationId = generateConversationId(listOf(participantPhone))
        
        conversationCache[conversationId]?.let { return it }
        
        var conversation = getConversationFromDb(conversationId)
        
        if (conversation == null) {
            conversation = Conversation(
                id = conversationId,
                participants = listOf(participantPhone),
                isGroupChat = false,
                createdAt = System.currentTimeMillis(),
                lastMessageTime = System.currentTimeMillis()
            )
            saveConversation(conversation)
        }
        
        conversationCache[conversationId] = conversation
        return conversation
    }

    fun getOrCreateGroupConversation(
        groupId: String,
        participants: List<String>,
        groupName: String?
    ): Conversation {
        conversationCache[groupId]?.let { return it }
        
        var conversation = getConversationFromDb(groupId)
        
        if (conversation == null) {
            conversation = Conversation(
                id = groupId,
                participants = participants,
                isGroupChat = true,
                groupName = groupName,
                createdAt = System.currentTimeMillis(),
                lastMessageTime = System.currentTimeMillis()
            )
            saveConversation(conversation)
        }
        
        conversationCache[groupId] = conversation
        return conversation
    }

    fun getConversation(conversationId: String): Conversation? {
        return conversationCache[conversationId] ?: getConversationFromDb(conversationId)
    }

    fun getAllConversations(): List<Conversation> {
        val conversations = mutableListOf<Conversation>()
        
        val cursor = dbHelper.readableDatabase.query(
            ConversationDatabaseHelper.TABLE_CONVERSATIONS,
            null, null, null, null, null,
            "last_message_time DESC"
        )
        
        cursor.use {
            while (it.moveToNext()) {
                cursorToConversation(it)?.let { conv -> conversations.add(conv) }
            }
        }
        
        return conversations
    }

    fun updateLastMessage(
        conversationId: String,
        messagePreview: String,
        messageTime: Long
    ) {
        val values = ContentValues().apply {
            put("last_message_preview", messagePreview)
            put("last_message_time", messageTime)
        }
        
        dbHelper.writableDatabase.update(
            ConversationDatabaseHelper.TABLE_CONVERSATIONS,
            values,
            "id = ?",
            arrayOf(conversationId)
        )
        
        conversationCache[conversationId]?.let { conv ->
            conversationCache[conversationId] = conv.copy(
                lastMessagePreview = messagePreview,
                lastMessageTime = messageTime
            )
        }
    }

    fun markConversationAsRead(conversationId: String) {
        val values = ContentValues().apply {
            put("unread_count", 0)
        }
        
        dbHelper.writableDatabase.update(
            ConversationDatabaseHelper.TABLE_CONVERSATIONS,
            values,
            "id = ?",
            arrayOf(conversationId)
        )
        
        conversationCache[conversationId]?.let { conv ->
            conversationCache[conversationId] = conv.copy(unreadCount = 0)
        }
    }

    fun incrementUnreadCount(conversationId: String) {
        dbHelper.writableDatabase.execSQL(
            "UPDATE ${ConversationDatabaseHelper.TABLE_CONVERSATIONS} SET unread_count = unread_count + 1 WHERE id = ?",
            arrayOf(conversationId)
        )
        
        conversationCache[conversationId]?.let { conv ->
            conversationCache[conversationId] = conv.copy(unreadCount = conv.unreadCount + 1)
        }
    }

    fun deleteConversation(conversationId: String) {
        dbHelper.writableDatabase.delete(
            ConversationDatabaseHelper.TABLE_CONVERSATIONS,
            "id = ?",
            arrayOf(conversationId)
        )
        
        conversationCache.remove(conversationId)
    }

    fun archiveConversation(conversationId: String) {
        val values = ContentValues().apply {
            put("is_archived", 1)
        }
        
        dbHelper.writableDatabase.update(
            ConversationDatabaseHelper.TABLE_CONVERSATIONS,
            values,
            "id = ?",
            arrayOf(conversationId)
        )
    }

    fun muteConversation(conversationId: String, muteUntil: Long) {
        val values = ContentValues().apply {
            put("mute_until", muteUntil)
        }
        
        dbHelper.writableDatabase.update(
            ConversationDatabaseHelper.TABLE_CONVERSATIONS,
            values,
            "id = ?",
            arrayOf(conversationId)
        )
    }

    private fun saveConversation(conversation: Conversation) {
        val values = ContentValues().apply {
            put("id", conversation.id)
            put("participants", conversation.participants.joinToString(","))
            put("is_group", if (conversation.isGroupChat) 1 else 0)
            put("group_name", conversation.groupName)
            put("group_icon", conversation.groupIcon)
            put("created_at", conversation.createdAt)
            put("last_message_time", conversation.lastMessageTime)
            put("last_message_preview", conversation.lastMessagePreview)
            put("unread_count", conversation.unreadCount)
            put("is_archived", if (conversation.isArchived) 1 else 0)
            put("mute_until", conversation.muteUntil)
        }
        
        dbHelper.writableDatabase.insertWithOnConflict(
            ConversationDatabaseHelper.TABLE_CONVERSATIONS,
            null, values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun getConversationFromDb(conversationId: String): Conversation? {
        val cursor = dbHelper.readableDatabase.query(
            ConversationDatabaseHelper.TABLE_CONVERSATIONS,
            null,
            "id = ?",
            arrayOf(conversationId),
            null, null, null
        )
        
        return cursor.use {
            if (it.moveToFirst()) cursorToConversation(it) else null
        }
    }

    private fun cursorToConversation(cursor: android.database.Cursor): Conversation? {
        return try {
            Conversation(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                participants = cursor.getString(cursor.getColumnIndexOrThrow("participants")).split(","),
                isGroupChat = cursor.getInt(cursor.getColumnIndexOrThrow("is_group")) == 1,
                groupName = cursor.getString(cursor.getColumnIndexOrThrow("group_name")),
                groupIcon = cursor.getString(cursor.getColumnIndexOrThrow("group_icon")),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                lastMessageTime = cursor.getLong(cursor.getColumnIndexOrThrow("last_message_time")),
                lastMessagePreview = cursor.getString(cursor.getColumnIndexOrThrow("last_message_preview")),
                unreadCount = cursor.getInt(cursor.getColumnIndexOrThrow("unread_count")),
                isArchived = cursor.getInt(cursor.getColumnIndexOrThrow("is_archived")) == 1,
                muteUntil = cursor.getLong(cursor.getColumnIndexOrThrow("mute_until"))
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun generateConversationId(participants: List<String>): String {
        return participants.sorted().joinToString("_").hashCode().toString()
    }

    fun close() {
        dbHelper.close()
    }
}

class ConversationDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "rcs_conversations.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_CONVERSATIONS = "conversations"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_CONVERSATIONS (
                id TEXT PRIMARY KEY,
                participants TEXT NOT NULL,
                is_group INTEGER DEFAULT 0,
                group_name TEXT,
                group_icon TEXT,
                created_at INTEGER NOT NULL,
                last_message_time INTEGER NOT NULL,
                last_message_preview TEXT,
                unread_count INTEGER DEFAULT 0,
                is_archived INTEGER DEFAULT 0,
                mute_until INTEGER DEFAULT 0
            )
        """)
        
        db.execSQL("CREATE INDEX idx_last_time ON $TABLE_CONVERSATIONS(last_message_time)")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONVERSATIONS")
        onCreate(db)
    }
}

data class Conversation(
    val id: String,
    val participants: List<String>,
    val isGroupChat: Boolean,
    val groupName: String? = null,
    val groupIcon: String? = null,
    val createdAt: Long,
    val lastMessageTime: Long,
    val lastMessagePreview: String? = null,
    val unreadCount: Int = 0,
    val isArchived: Boolean = false,
    val muteUntil: Long = 0
) {
    fun isMuted(): Boolean {
        return muteUntil > System.currentTimeMillis()
    }
    
    fun getDisplayName(): String {
        return when {
            isGroupChat && groupName != null -> groupName
            isGroupChat -> participants.take(3).joinToString(", ")
            else -> participants.firstOrNull() ?: "Unknown"
        }
    }
}
