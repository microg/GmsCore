/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.notification

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.microg.gms.wearable.NotificationBridge
import org.microg.gms.wearable.WearableService
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "WearNotificationSvc"

/** Path used for forwarding bridged Android notifications to wearable peers. */
const val NOTIFICATION_PATH = "/wearable/notification"

/** Monotonic counter used to assign collision-free UIDs to bridged notifications. */
private val uidCounter = AtomicInteger(1)

/**
 * [NotificationListenerService] that bridges Android notifications to connected
 * Wear OS peers via the microG WearableImpl message transport.
 *
 * Filters out:
 * - Notifications originating from this GmsCore package itself.
 * - Ongoing notifications ([Notification.FLAG_ONGOING_EVENT]).
 * - Non-clearable notifications ([Notification.FLAG_NO_CLEAR]).
 */
class WearableNotificationService : NotificationListenerService() {

    /**
     * Maps the notification's stable key to the UID we assigned to it, so that we send
     * the same UID on removal.  The key is derived via [sbnKey] to remain safe on API 19.
     */
    private val keyToUid = HashMap<String, Int>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (shouldSkip(sbn)) return

        // Assign a stable, collision-free UID for this notification.
        val uid = keyToUid.getOrPut(sbnKey(sbn)) { uidCounter.getAndIncrement() }
        NotificationBridge.activeNotifications[uid] = sbn

        val payload = encodeNotification(uid, sbn) ?: return
        sendToWearable(payload)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val uid = keyToUid.remove(sbnKey(sbn)) ?: return
        NotificationBridge.activeNotifications.remove(uid)

        // Notify peers that this notification was dismissed
        val payload = encodeRemoval(uid, sbnKey(sbn)) ?: return
        sendToWearable(payload)
    }

    // -------------------------------------------------------------------------

    private fun shouldSkip(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) return true
        val flags = sbn.notification?.flags ?: return true
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return true
        if (flags and Notification.FLAG_NO_CLEAR != 0) return true
        return false
    }

    private fun sendToWearable(payload: ByteArray) {
        val wearable = WearableService.getInstance() ?: run {
            Log.d(TAG, "WearableService not running, skipping notification forward")
            return
        }
        val connectedNodes = wearable.allConnectedNodes
        if (connectedNodes.isEmpty()) {
            Log.d(TAG, "No connected wearable nodes, skipping notification forward")
            return
        }
        for (nodeId in connectedNodes) {
            val result = wearable.sendMessage(packageName, nodeId, NOTIFICATION_PATH, payload)
            if (result < 0) {
                Log.w(TAG, "sendMessage to $nodeId failed (result=$result)")
            }
        }
    }
}

// -------------------------------------------------------------------------
// Encoding helpers
// -------------------------------------------------------------------------

/**
 * Returns a stable string key for [sbn] that is safe on API 19+.
 *
 * [StatusBarNotification.getKey] was added in API 20 (KITKAT_WATCH). On API 19 we fall
 * back to a composite of `packageName + '|' + id + '|' + tag` which is sufficient for
 * distinguishing notifications within a single posting session.  Package names are
 * dot-separated identifiers and cannot contain '|', minimising the risk of collisions.
 */
internal fun sbnKey(sbn: StatusBarNotification): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
        sbn.key
    } else {
        "${sbn.packageName}|${sbn.id}|${sbn.tag ?: ""}"
    }
}

/**
 * Encodes a posted notification as:
 * - byte type = 1 (posted)
 * - int uid
 * - UTF packageName
 * - UTF key
 * - UTF title (empty string if absent)
 * - UTF text  (empty string if absent)
 * - long timestamp
 * - int actionCount
 * - UTF[] action titles
 */
internal fun encodeNotification(uid: Int, sbn: StatusBarNotification): ByteArray? {
    return try {
        val n = sbn.notification ?: return null
        val extras = n.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = (extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT))?.toString() ?: ""
        val actions: Array<Notification.Action> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            n.actions ?: emptyArray()
        } else {
            emptyArray()
        }

        ByteArrayOutputStream().also { baos ->
            DataOutputStream(baos).use { dos ->
                dos.writeByte(1) // type: posted
                dos.writeInt(uid)
                dos.writeUTF(sbn.packageName)
                dos.writeUTF(sbnKey(sbn))
                dos.writeUTF(title)
                dos.writeUTF(text)
                dos.writeLong(sbn.postTime)
                dos.writeInt(actions.size)
                for (action in actions) {
                    dos.writeUTF(action.title?.toString() ?: "")
                }
            }
        }.toByteArray()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to encode notification", e)
        null
    }
}

/**
 * Encodes a removal event as:
 * - byte type = 2 (removed)
 * - int uid
 * - UTF key
 */
internal fun encodeRemoval(uid: Int, key: String): ByteArray? {
    return try {
        ByteArrayOutputStream().also { baos ->
            DataOutputStream(baos).use { dos ->
                dos.writeByte(2) // type: removed
                dos.writeInt(uid)
                dos.writeUTF(key)
            }
        }.toByteArray()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to encode notification removal", e)
        null
    }
}
