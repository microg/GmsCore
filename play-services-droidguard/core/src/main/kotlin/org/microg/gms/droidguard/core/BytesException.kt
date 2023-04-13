/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

class BytesException : Exception {
    val bytes: ByteArray

    constructor(bytes: ByteArray, message: String) : super(message) {
        this.bytes = bytes
    }

    constructor(bytes: ByteArray, cause: Throwable) : super(cause) {
        this.bytes = bytes
    }

    constructor(bytes: ByteArray, message: String, cause: Throwable) : super(message, cause) {
        this.bytes = bytes
    }
}
