/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.model

class StandardIntegrityException : Exception {
    val code: Int

    constructor(code: Int, message: String) : super(message) {
        this.code = code
    }

    constructor(cause: String?) : super(cause) {
        this.code = IntegrityErrorCode.INTERNAL_ERROR
    }
}