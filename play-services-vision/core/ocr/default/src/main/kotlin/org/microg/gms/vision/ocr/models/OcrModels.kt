/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.ocr.models

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.microg.gms.vision.ocr.ui.OcrModelPrompt
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

object OcrModels {
    val packs = ocrModelFiles.keys

    @get:ChecksSdkIntAtLeast(api = 26)
    val supported: Boolean get() = android.os.Build.VERSION.SDK_INT >= 26

    private val lock = Any()

    private fun prefs(context: Context) = context.getSharedPreferences("ocr_models", Context.MODE_PRIVATE)
    private fun approved(pack: String) = "approved:$pack"
    private fun installedVersion(pack: String) = "installed-version:$pack"
    private fun legacyApprovalVersion(pack: String) = "approved-version:$pack"

    fun workName(pack: String) = "ocr-models:$pack"

    fun isReady(context: Context, pack: String): Boolean = synchronized(lock) {
        supported && pack in packs && isInstalled(context, pack) &&
                ocrModelFiles.getValue(pack).all { it.exists(context) }
    }

    fun remainingBytes(context: Context, pack: String): Long = synchronized(lock) {
        if (!supported || pack !in packs) return@synchronized 0L
        ocrModelFiles.getValue(pack).filterNot { it.exists(context) }.sumOf { it.bytes }
    }

    fun start(context: Context, pack: String, unmetered: Boolean): Unit = synchronized(lock) {
        require(supported && pack in packs)
        if (isReady(context, pack)) return@synchronized
        val token = UUID.randomUUID().toString()
        val state = prefs(context)
        check(state.edit().putString(approved(pack), token).remove(legacyApprovalVersion(pack)).commit())
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (unmetered) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<OcrModelDownloadWorker>()
            .setInputData(workDataOf("pack" to pack, "token" to token))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(pack), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, pack: String) = stop(context, pack, removeInstalled = false)
    fun delete(context: Context, pack: String) = stop(context, pack, removeInstalled = true)

    private fun stop(context: Context, pack: String, removeInstalled: Boolean) {
        require(supported && pack in packs)
        try {
            synchronized(lock) {
                val state = prefs(context)
                val edit = state.edit().remove(approved(pack)).remove(legacyApprovalVersion(pack))
                if (removeInstalled) edit.remove(installedVersion(pack)).remove("installed:$pack")
                check(edit.commit())
                WorkManager.getInstance(context).cancelUniqueWork(workName(pack))
                if (!reclaim(context, ocrModelFiles.getValue(pack))) throw IOException("Could not delete OCR model")
            }
        } finally {
            if (removeInstalled) changed(context)
        }
    }

    internal fun approvedFiles(context: Context, pack: String, token: String): List<OcrModelFile>? = synchronized(lock) {
        if (!isApproved(context, pack, token)) return@synchronized null
        ocrModelFiles.getValue(pack)
    }

    internal fun isApproved(context: Context, pack: String, token: String): Boolean = synchronized(lock) {
        val state = prefs(context)
        state.getString(approved(pack), null) == token && hasApprovedVersion(context, pack)
    }

    internal fun publish(
        context: Context, pack: String, token: String, model: OcrModelFile
    ): Boolean = synchronized(lock) {
        if (!isApproved(context, pack, token)) return@synchronized false
        if (!model.partial(context).renameTo(model.blob(context))) throw IOException("Could not publish OCR model")
        true
    }

    internal fun activate(context: Context, pack: String, token: String): Boolean {
        synchronized(lock) {
            if (!isApproved(context, pack, token)) return false
            if (!ocrModelFiles.getValue(pack).all { it.exists(context) }) return false
            val state = prefs(context)
            val previousVersion = state.getLong(installedVersion(pack), 0L)
            // SharedPreferences changes in-memory state before committing to disk.
            if (!state.edit().putLong(installedVersion(pack), MODEL_VERSION)
                    .remove(approved(pack)).remove(legacyApprovalVersion(pack)).commit()
            ) {
                val restore = state.edit().putString(approved(pack), token)
                if (previousVersion == 0L) restore.remove(installedVersion(pack))
                else restore.putLong(installedVersion(pack), previousVersion)
                restore.commit()
                return false
            }
        }
        OcrModelPrompt.downloadFinished(context, pack)
        changed(context)
        return true
    }

    internal fun fail(context: Context, pack: String, token: String) = synchronized(lock) {
        if (!isApproved(context, pack, token)) return@synchronized
        if (prefs(context).edit().remove(approved(pack)).remove(legacyApprovalVersion(pack)).commit()) {
            reclaim(context, ocrModelFiles.getValue(pack))
        }
    }

    internal fun open(context: Context, pack: String, version: Long, path: String): ParcelFileDescriptor = synchronized(lock) {
        if (!supported || pack !in packs) throw FileNotFoundException()
        if (version != MODEL_VERSION || !isInstalled(context, pack)) throw FileNotFoundException("OCR model is unavailable")
        val model = ocrModelFiles.getValue(pack).firstOrNull { it.assetPath == path && it.exists(context) }
            ?: throw FileNotFoundException()
        ParcelFileDescriptor.open(model.blob(context), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    internal fun retryUntil(context: Context): Long = prefs(context).getLong("retryUntil", 0L)

    internal fun postpone(context: Context, until: Long) = synchronized(lock) {
        if (until > retryUntil(context)) prefs(context).edit().putLong("retryUntil", until).apply()
    }

    private fun changed(context: Context) {
        val uri = Uri.Builder().scheme("content").authority("${context.packageName}.ocr.models").build()
        context.contentResolver.notifyChange(uri, null, false)
    }

    private fun isInstalled(context: Context, pack: String): Boolean {
        val state = prefs(context)
        val legacy = if (state.getBoolean("installed:$pack", false)) 1L else 0L
        return state.getLong(installedVersion(pack), legacy) == MODEL_VERSION
    }

    // WorkManager may resume requests approved before the model source became fixed.
    private fun hasApprovedVersion(context: Context, pack: String): Boolean =
        prefs(context).getLong(legacyApprovalVersion(pack), MODEL_VERSION) == MODEL_VERSION

    private fun reclaim(context: Context, discarded: List<OcrModelFile>): Boolean {
        val state = prefs(context)
        val used = ocrModelFiles.filterKeys { pack ->
            isInstalled(context, pack) || (state.contains(approved(pack)) && hasApprovedVersion(context, pack))
        }.values.flatten().mapTo(HashSet()) { it.sha256 }
        var complete = true
        for (file in discarded) {
            if (file.sha256 in used) continue
            val blob = file.blob(context)
            if (blob.exists() && !blob.delete()) complete = false
            file.partial(context).delete()
        }
        return complete
    }
}
