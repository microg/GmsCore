package org.microg.vending.enterprise

import android.app.PendingIntent

internal sealed interface InstallProgress

internal data class Downloading(
    val bytesDownloaded: Long,
    val bytesTotal: Long
) : InstallProgress

internal data class CommitingSession(val installIntent: PendingIntent? = null, val deleteIntent: PendingIntent? = null) : InstallProgress
internal data object InstallComplete : InstallProgress
internal data class InstallError(
    val errorMessage: String
) : InstallProgress