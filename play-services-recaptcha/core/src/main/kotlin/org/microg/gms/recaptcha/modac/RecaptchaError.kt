/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac

internal object RecaptchaErrorKind {
    const val INTERNAL = 1
    const val NETWORK = 3
    const val SERVER = 10
}

internal object RecaptchaErrorSubKind {
    const val RUNTIME_ERROR = 8
    const val NOT_FOUND = 17
    const val SERVICE_UNAVAILABLE = 47
    const val HTTP_ERROR = 48
    const val UNKNOWN = 64
    const val BAD_REQUEST = 91
    const val EMPTY_CHALLENGE_TOKEN = 121
    const val INVALID_SERVER_RESPONSE = 140
}

internal class RecaptchaError(
    val kindCode: Int,
    val subKindCode: Int,
    val label: String,
    val detail: String? = null,
    cause: Throwable? = null,
) : RuntimeException(
    "RecaptchaError(kind=$kindCode, subKind=$subKindCode) [$label]${detail?.let { ": $it" }.orEmpty()}",
    cause,
)
