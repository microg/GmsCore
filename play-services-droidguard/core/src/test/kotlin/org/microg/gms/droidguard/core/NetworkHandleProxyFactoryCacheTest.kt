/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.droidguard.DG_CACHE_FOLDER_NAME
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkHandleProxyFactoryCacheTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun storeRow(factory: NetworkHandleProxyFactory, flow: String, vmKey: String) {
        DgDatabaseHelper(context).use {
            it.put(factory.databaseId(flow), 3600, vmKey, byteArrayOf(1), byteArrayOf(2))
        }
    }

    @Test
    fun readFromDatabase_skipsRowWhoseVmIsNotInCache() {
        val factory = NetworkHandleProxyFactory(context)
        // Row written by a build that used a lowercase key under the old cache_dg/ folder
        File(context.getDir("cache_dg", Context.MODE_PRIVATE), "ab12cd").apply {
            File(this, "opt").mkdirs()
            File(this, "the.apk").writeBytes(byteArrayOf(0))
        }
        storeRow(factory, "fast", "ab12cd")

        assertNull(factory.readFromDatabase("fast"))
    }

    @Test
    fun readFromDatabase_returnsRowWhoseVmIsCached() {
        val factory = NetworkHandleProxyFactory(context)
        File(context.getDir(DG_CACHE_FOLDER_NAME, Context.MODE_PRIVATE), "AB12CD").apply {
            File(this, "opt").mkdirs()
            File(this, "the.apk").writeBytes(byteArrayOf(0))
        }
        storeRow(factory, "fast", "AB12CD")

        assertEquals("AB12CD", factory.readFromDatabase("fast")?.first)
    }
}
