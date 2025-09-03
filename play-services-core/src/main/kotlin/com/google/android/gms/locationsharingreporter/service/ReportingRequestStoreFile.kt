/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter.service

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException

object ReportingRequestStoreFile {
    private const val TAG = "ReportingStoreFile"
    private const val REPORTING_REQUEST_STORE_FILE = "Reporting.RequestStore.pb"
    private const val FILE_PREFIX = "location_sharing_"


    fun setLocationSharingEnabled(context: Context, enabled: Boolean, accountName: String) {
        val file = File(getLocationSharingReporterDir(context), FILE_PREFIX + accountName)
        file.writeText(if (enabled) "true" else "false")
    }


    fun isLocationSharingEnabled(context: Context, accountName: String): Boolean {
        val file = File(getLocationSharingReporterDir(context), FILE_PREFIX + accountName)
        return if (file.exists()) {
            file.readText() == "true"
        } else {
            true
        }
    }

    private fun getLocationSharingReporterDir(context: Context) : File {
        val dir = File(context.filesDir, "locationsharingreporter")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getReportingRequestStoreFile(context: Context): File {
        return File(getLocationSharingReporterDir(context), REPORTING_REQUEST_STORE_FILE)
    }

    fun getReportingRequestStore(context: Context): ReportingRequestStore {
        synchronized(this) {
            try {
                val reportingRequestStoreFile = getReportingRequestStoreFile(context)
                if (!reportingRequestStoreFile.exists()) {
                    reportingRequestStoreFile.createNewFile()
                }

                return ReportingRequestStore.ADAPTER.decode(reportingRequestStoreFile.readBytes())
            } catch (e: IOException) {
                Log.w(TAG, "Returning empty ReportingRequestStore due to file access error", e)
                return ReportingRequestStore()
            }
        }
    }

    fun updateReportingRequestStore(context: Context, callback: (requestStore: ReportingRequestStore) -> ReportingRequestStore) {
        synchronized(this) {
            val currentStore = getReportingRequestStore(context)
            val newStore = callback(currentStore)

            if (currentStore != newStore) {
                try {
                    getReportingRequestStoreFile(context).writeBytes(newStore.encode())
                    Log.i(TAG, "ReportingRequestStore updated successfully")
                } catch (e: IOException) {
                    Log.w(TAG, "Failed to update ReportingRequestStore", e)
                }
            } else {
                Log.d(TAG, "No changes detected in ReportingRequestStore")
            }
        }
    }

    fun loadReportingRequestStore(
        context: Context,
        transform: (ReportingRequestStore) -> ReportingRequestStore = { it }
    ): ReportingRequestStore {
        return synchronized(this) {
            try {
                val file = getReportingRequestStoreFile(context)
                if (!file.exists()) {
                    file.createNewFile()
                    return@synchronized ReportingRequestStore()
                }

                val bytes = file.readBytes()
                if (bytes.isEmpty()) return@synchronized ReportingRequestStore()

                val reportingRequestStore = ReportingRequestStore.ADAPTER.decode(bytes)

                val newReportingRequestStore = transform(reportingRequestStore)

                if (reportingRequestStore != newReportingRequestStore) {
                    file.writeBytes(newReportingRequestStore.encode())
                }

                newReportingRequestStore
            } catch (e: IOException) {
                Log.w(TAG, "Returning empty ReportingRequestStore due to file access error", e)
                ReportingRequestStore()
            }
        }
    }
}