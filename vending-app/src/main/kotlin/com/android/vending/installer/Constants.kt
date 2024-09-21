package com.android.vending.installer

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CompletableDeferred
import java.io.File

private const val FILE_SAVE_PATH = "phonesky-download-service"
internal const val TAG = "GmsPackageInstaller"

const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"


fun Context.packageDownloadLocation() = File(filesDir, FILE_SAVE_PATH).apply {
    if (!exists()) mkdir()
}
