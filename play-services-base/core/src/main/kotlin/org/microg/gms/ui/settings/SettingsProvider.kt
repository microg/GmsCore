/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.navigation.NavController

private const val TAG = "SettingsProvider"

interface SettingsProvider {
    fun getEntriesStatic(context: Context): List<Entry>
    suspend fun getEntriesDynamic(context: Context): List<Entry> = getEntriesStatic(context)

    fun preProcessSettingsIntent(intent: Intent)

    fun extendNavigation(navController: NavController)

    companion object {
        enum class Group {
            HEADER,
            GOOGLE,
            OTHER,
            FOOTER
        }

        data class Entry(
            val key: String,
            val group: Group,
            val navigationId: Int,
            val title: String,
            val summary: String? = null,
            val icon: Drawable? = null,
        )
    }
}

fun getAllSettingsProviders(context: Context): List<SettingsProvider> {
    val metaData = runCatching { context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData }.getOrNull() ?: Bundle.EMPTY
    return metaData.keySet().asSequence().filter {
        it.startsWith("org.microg.gms.ui.settings.entry:")
    }.mapNotNull {
        runCatching { metaData.getString(it) }.onFailure { Log.w(TAG, it) }.getOrNull()
    }.mapNotNull {
        runCatching { Class.forName(it) }.onFailure { Log.w(TAG, it) }.getOrNull()
    }.filter {
        SettingsProvider::class.java.isAssignableFrom(it)
    }.mapNotNull {
        runCatching { it.getDeclaredField("INSTANCE").get(null) as SettingsProvider }.onFailure { Log.w(TAG, it) }.getOrNull()
    }.toList()
}