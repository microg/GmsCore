package org.microg.gms.wearable

import android.content.Context
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Foundation for WearOS companion pairing in microG.
 * Tracks pairing lifecycle and exposes hooks for notification echo / media controls.
 * Full Bluetooth/Wear protocol is intentionally out of scope for this first slice.
 */
class WearableCompanionManager private constructor(private val context: Context) {

    enum class PairingState {
        DISCONNECTED,
        DISCOVERING,
        PAIRING,
        CONNECTED,
        ERROR
    }

    data class CompanionNode(
        val id: String,
        val displayName: String,
        val isNearby: Boolean = false
    )

    interface Listener {
        fun onStateChanged(state: PairingState) {}
        fun onNodeChanged(node: CompanionNode?) {}
        fun onNotificationBridge(packageName: String, title: String, text: String) {}
        fun onMediaCommand(command: String) {}
    }

    private val state = AtomicReference(PairingState.DISCONNECTED)
    private val activeNode = AtomicReference<CompanionNode?>(null)
    private val listeners = CopyOnWriteArrayList<Listener>()

    fun getState(): PairingState = state.get()
    fun getActiveNode(): CompanionNode? = activeNode.get()

    fun addListener(listener: Listener) {
        listeners.addIfAbsent(listener)
        listener.onStateChanged(state.get())
        listener.onNodeChanged(activeNode.get())
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun startDiscovery() {
        if (state.get() == PairingState.CONNECTED || state.get() == PairingState.PAIRING) return
        setState(PairingState.DISCOVERING)
        Log.i(TAG, "WearOS discovery started (protocol backend not yet implemented)")
    }

    fun stopDiscovery() {
        if (state.get() == PairingState.DISCOVERING) {
            setState(PairingState.DISCONNECTED)
        }
    }

    /**
     * Begin pairing with a discovered node. Real BLE/Wear handshake is a follow-up.
     */
    fun beginPairing(node: CompanionNode) {
        activeNode.set(node)
        listeners.forEach { it.onNodeChanged(node) }
        setState(PairingState.PAIRING)
        Log.i(TAG, "Pairing requested with ${node.displayName} (${node.id})")
        // Placeholder success path for integration tests / UI wiring.
        // Replace with real companion protocol when available.
        completePairing(success = false, reason = "Wear protocol backend missing")
    }

    fun completePairing(success: Boolean, reason: String? = null) {
        if (success) {
            setState(PairingState.CONNECTED)
            Log.i(TAG, "WearOS companion connected: ${activeNode.get()?.displayName}")
        } else {
            Log.w(TAG, "WearOS pairing incomplete: ${reason ?: "unknown"}")
            activeNode.set(null)
            listeners.forEach { it.onNodeChanged(null) }
            setState(PairingState.ERROR)
            setState(PairingState.DISCONNECTED)
        }
    }

    fun disconnect() {
        activeNode.set(null)
        listeners.forEach { it.onNodeChanged(null) }
        setState(PairingState.DISCONNECTED)
    }

    /** Bridge phone notification to watch (hook for future transport). */
    fun echoNotification(packageName: String, title: String, text: String) {
        if (state.get() != PairingState.CONNECTED) {
            Log.d(TAG, "Skip notification echo; not connected")
            return
        }
        listeners.forEach { it.onNotificationBridge(packageName, title, text) }
        Log.d(TAG, "Notification bridge: $packageName | $title")
    }

    /** Media control from watch → phone (hook). */
    fun dispatchMediaCommand(command: String) {
        if (state.get() != PairingState.CONNECTED) return
        listeners.forEach { it.onMediaCommand(command) }
        Log.d(TAG, "Media command: $command")
    }

    private fun setState(next: PairingState) {
        val prev = state.getAndSet(next)
        if (prev != next) {
            Log.i(TAG, "PairingState $prev -> $next")
            listeners.forEach { it.onStateChanged(next) }
        }
    }

    companion object {
        private const val TAG = "GmsWearCompanion"

        @Volatile private var instance: WearableCompanionManager? = null

        fun get(context: Context): WearableCompanionManager {
            return instance ?: synchronized(this) {
                instance ?: WearableCompanionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
