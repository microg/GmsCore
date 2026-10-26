/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast

import org.microg.gms.cast.channel.CastDeviceSession

/**
 * Open device sessions by Cast device id, so code outside a client binding (the media route controller) can act on a
 * connected device, e.g. `CastChannelRegistry.get(deviceId)?.setVolume(level)`.
 */
object CastChannelRegistry {
    private val sessions = HashMap<String, MutableList<CastDeviceSession>>()

    @JvmStatic
    fun get(deviceId: String?): CastDeviceSession? = synchronized(sessions) {
        sessions[deviceId ?: return null]?.lastOrNull()
    }

    @JvmStatic
    fun register(deviceId: String, session: CastDeviceSession) = synchronized(sessions) {
        val list = sessions.getOrPut(deviceId) { ArrayList() }
        if (session !in list) list.add(session)
    }

    @JvmStatic
    fun unregister(deviceId: String, session: CastDeviceSession) = synchronized(sessions) {
        val list = sessions[deviceId] ?: return
        list.remove(session)
        if (list.isEmpty()) sessions.remove(deviceId)
    }
}
