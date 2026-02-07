/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsGroupChatManager - Group chat management
 */

package org.microg.gms.rcs.group

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RcsGroupChatManager(private val context: Context) {

    companion object {
        private const val TAG = "RcsGroupChat"
        private const val MAX_GROUP_SIZE = 100
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeGroups = ConcurrentHashMap<String, GroupChat>()
    private val listeners = mutableListOf<GroupChatListener>()

    suspend fun createGroup(
        name: String,
        participants: List<String>,
        iconUri: String? = null
    ): GroupChatResult {
        if (participants.size > MAX_GROUP_SIZE) {
            return GroupChatResult.failure("Group too large (max $MAX_GROUP_SIZE)")
        }
        
        val groupId = UUID.randomUUID().toString()
        
        val group = GroupChat(
            id = groupId,
            name = name,
            iconUri = iconUri,
            participants = participants.toMutableList(),
            createdAt = System.currentTimeMillis(),
            isActive = true
        )
        
        activeGroups[groupId] = group
        
        Log.d(TAG, "Created group $groupId with ${participants.size} participants")
        
        return GroupChatResult.success(groupId)
    }

    fun getGroup(groupId: String): GroupChat? {
        return activeGroups[groupId]
    }

    suspend fun addParticipant(groupId: String, phoneNumber: String): Boolean {
        val group = activeGroups[groupId] ?: return false
        
        if (group.participants.size >= MAX_GROUP_SIZE) {
            return false
        }
        
        if (group.participants.contains(phoneNumber)) {
            return true
        }
        
        group.participants.add(phoneNumber)
        notifyParticipantAdded(groupId, phoneNumber)
        
        return true
    }

    suspend fun removeParticipant(groupId: String, phoneNumber: String): Boolean {
        val group = activeGroups[groupId] ?: return false
        
        val removed = group.participants.remove(phoneNumber)
        if (removed) {
            notifyParticipantRemoved(groupId, phoneNumber)
        }
        
        return removed
    }

    suspend fun updateGroupName(groupId: String, newName: String): Boolean {
        val group = activeGroups[groupId] ?: return false
        group.name = newName
        notifyGroupUpdated(groupId)
        return true
    }

    suspend fun leaveGroup(groupId: String): Boolean {
        val group = activeGroups[groupId] ?: return false
        group.isActive = false
        notifyLeftGroup(groupId)
        return true
    }

    fun addListener(listener: GroupChatListener) {
        listeners.add(listener)
    }

    private fun notifyParticipantAdded(groupId: String, phoneNumber: String) {
        listeners.forEach { it.onParticipantAdded(groupId, phoneNumber) }
    }

    private fun notifyParticipantRemoved(groupId: String, phoneNumber: String) {
        listeners.forEach { it.onParticipantRemoved(groupId, phoneNumber) }
    }

    private fun notifyGroupUpdated(groupId: String) {
        listeners.forEach { it.onGroupUpdated(groupId) }
    }

    private fun notifyLeftGroup(groupId: String) {
        listeners.forEach { it.onLeftGroup(groupId) }
    }
}

data class GroupChat(
    val id: String,
    var name: String,
    var iconUri: String?,
    val participants: MutableList<String>,
    val createdAt: Long,
    var isActive: Boolean
)

data class GroupChatResult(
    val isSuccessful: Boolean,
    val groupId: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun success(groupId: String) = GroupChatResult(true, groupId)
        fun failure(error: String) = GroupChatResult(false, errorMessage = error)
    }
}

interface GroupChatListener {
    fun onParticipantAdded(groupId: String, phoneNumber: String)
    fun onParticipantRemoved(groupId: String, phoneNumber: String)
    fun onGroupUpdated(groupId: String)
    fun onLeftGroup(groupId: String)
}
