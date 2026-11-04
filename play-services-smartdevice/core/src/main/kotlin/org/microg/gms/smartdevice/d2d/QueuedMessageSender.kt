/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

internal class QueuedMessageSender<T> {
    private val pending = ArrayDeque<T>()
    private var receiver: ((T) -> Unit)? = null

    @Synchronized
    fun attach(receiver: (T) -> Unit) {
        while (pending.isNotEmpty()) receiver(pending.removeFirst())

        this.receiver = receiver
    }

    @Synchronized
    fun detach() {
        receiver = null
    }

    @Synchronized
    fun send(message: T) {
        receiver?.invoke(message) ?: pending.addLast(message)
    }

    @Synchronized
    fun isAttached(): Boolean = receiver != null

}
