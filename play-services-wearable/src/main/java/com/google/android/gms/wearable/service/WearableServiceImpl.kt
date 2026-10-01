package com.google.android.gms.wearable.service

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.internal.AddListenerRequest
import com.google.android.gms.wearable.internal.IWearableCallbacks
import com.google.android.gms.wearable.internal.IWearableService
import com.google.android.gms.wearable.internal.NodeParcelable
import com.google.android.gms.wearable.internal.PutDataResponse
import com.google.android.gms.wearable.internal.RemoveListenerRequest
import com.google.android.gms.wearable.internal.SendMessageResponse
import java.util.concurrent.atomic.AtomicInteger

class WearableServiceImpl(private val context: Context) : IWearableService.Stub() {
    private val messageCounter = AtomicInteger(1)

    override fun putData(callbacks: IWearableCallbacks?, request: PutDataRequest?) {
        Log.d(TAG, "putData request: ${request?.uri}")
        try {
            callbacks?.onPutDataResponse(PutDataResponse(0, null))
        } catch (e: Exception) {
            Log.w(TAG, "Error responding to putData", e)
        }
    }

    override fun sendMessage(callbacks: IWearableCallbacks?, nodeId: String?, path: String?, data: ByteArray?) {
        Log.d(TAG, "sendMessage to $nodeId, path: $path, data size: ${data?.size ?: 0}")
        val reqId = messageCounter.getAndIncrement()
        try {
            callbacks?.onSendMessageResponse(SendMessageResponse(0, reqId))
        } catch (e: Exception) {
            Log.w(TAG, "Error responding to sendMessage", e)
        }
    }

    override fun getLocalNode(callbacks: IWearableCallbacks?) {
        Log.d(TAG, "getLocalNode called")
        val localNode = NodeParcelable("local_node_id", "Local Device", 0, true)
        try {
            callbacks?.onGetLocalNodeResponse(localNode, Status.SUCCESS)
        } catch (e: Exception) {
            Log.w(TAG, "Error responding to getLocalNode", e)
        }
    }

    override fun getConnectedNodes(callbacks: IWearableCallbacks?) {
        Log.d(TAG, "getConnectedNodes called")
        try {
            callbacks?.onGetConnectedNodesResponse(emptyList())
        } catch (e: Exception) {
            Log.w(TAG, "Error responding to getConnectedNodes", e)
        }
    }

    override fun addListener(callbacks: IWearableCallbacks?, request: AddListenerRequest?) {
        Log.d(TAG, "addListener called")
        try {
            callbacks?.onStatus(Status.SUCCESS)
        } catch (e: Exception) {
            Log.w(TAG, "Error responding to addListener", e)
        }
    }

    override fun removeListener(callbacks: IWearableCallbacks?, request: RemoveListenerRequest?) {
        Log.d(TAG, "removeListener called")
        try {
            callbacks?.onStatus(Status.SUCCESS)
        } catch (e: Exception) {
            Log.w(TAG, "Error responding to removeListener", e)
        }
    }

    companion object {
        private const val TAG = "WearableServiceImpl"
    }
}
