/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.app.PendingIntent

internal sealed interface InstallProgress

internal data class Downloading(
    val bytesDownloaded: Long,
    val bytesTotal: Long
) : InstallProgress, AppState
internal data class CommitingSession(val installIntent: PendingIntent? = null, val deleteIntent: PendingIntent? = null) : InstallProgress
internal data object InstallComplete : InstallProgress
internal data class InstallError(
    val errorMessage: String
) : InstallProgress