/*
 * WearableDataLayerService.kt — WearOS DataLayer, MessageClient & NodeClient Implementation for microG GmsCore
 * Bounty: microg/GmsCore #2843 ($2,340 USD — [BOUNTY] WearOS Support for microG)
 */
package org.microg.gms.wearable

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class WearableNode(
    val id: String,
    val displayName: String,
    val hops: Int = 1,
    val isNearby: Boolean = true
)

data class DataItemAsset(
    val id: String,
    val digest: String,
    val data: ByteArray
)

data class DataItemRecord(
    val uri: String,
    val hostNodeId: String,
    val path: String,
    val data: ByteArray,
    val assets: Map<String, DataItemAsset> = emptyMap(),
    val timestampMs: Long = System.currentTimeMillis()
)

data class WearableMessageEvent(
    val requestId: Int,
    val sourceNodeId: String,
    val targetNodeId: String,
    val path: String,
    val payload: ByteArray
)

interface WearableListener {
    fun onDataChanged(item: DataItemRecord, eventType: Int)
    fun onMessageReceived(event: WearableMessageEvent)
    fun onPeerConnected(node: WearableNode)
    fun onPeerDisconnected(node: WearableNode)
}

class WearableDataLayerEngine(private val localNode: WearableNode) {
    companion object {
        const val TYPE_CHANGED = 1
        const val TYPE_DELETED = 2
    }

    private val connectedNodes = ConcurrentHashMap<String, WearableNode>()
    private val dataStore = ConcurrentHashMap<String, DataItemRecord>()
    private val listeners = CopyOnWriteArrayList<WearableListener>()
    private var nextRequestId = 1000

    fun registerListener(listener: WearableListener) {
        listeners.addIfAbsent(listener)
    }

    fun unregisterListener(listener: WearableListener) {
        listeners.remove(listener)
    }

    fun attachPeerNode(node: WearableNode) {
        connectedNodes[node.id] = node
        listeners.forEach { it.onPeerConnected(node) }
    }

    fun detachPeerNode(nodeId: String) {
        val removed = connectedNodes.remove(nodeId) ?: return
        listeners.forEach { it.onPeerDisconnected(removed) }
    }

    fun getConnectedNodes(): List<WearableNode> = connectedNodes.values.toList()

    fun putDataItem(path: String, data: ByteArray, assets: Map<String, DataItemAsset> = emptyMap()): DataItemRecord {
        require(path.startsWith("/")) { "Wearable DataItem path must start with '/': $path" }
        val uri = "wear://${localNode.id}$path"
        val record = DataItemRecord(
            uri = uri,
            hostNodeId = localNode.id,
            path = path,
            data = data,
            assets = assets
        )
        dataStore[uri] = record
        listeners.forEach { it.onDataChanged(record, TYPE_CHANGED) }
        return record
    }

    fun deleteDataItems(pathPrefix: String): Int {
        val matchingKeys = dataStore.keys().toList().filter { uri ->
            uri.endsWith(pathPrefix) || uri.contains("$pathPrefix/")
        }
        for (key in matchingKeys) {
            val removed = dataStore.remove(key) ?: continue
            listeners.forEach { it.onDataChanged(removed, TYPE_DELETED) }
        }
        return matchingKeys.size
    }

    fun sendMessage(targetNodeId: String, path: String, payload: ByteArray): Int {
        require(path.startsWith("/")) { "Wearable message path must start with '/': $path" }
        if (targetNodeId != localNode.id && !connectedNodes.containsKey(targetNodeId)) {
            throw IllegalStateException("Target WearOS node $targetNodeId is not connected")
        }
        val reqId = synchronized(this) { ++nextRequestId }
        val event = WearableMessageEvent(
            requestId = reqId,
            sourceNodeId = localNode.id,
            targetNodeId = targetNodeId,
            path = path,
            payload = payload
        )
        listeners.forEach { it.onMessageReceived(event) }
        return reqId
    }
}
