/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import com.google.android.gms.common.api.Status
import com.google.android.gms.phenotype.internal.IGetStorageInfoCallbacks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhenotypeStorageInfoTest {
    @Test
    fun advertisesStorageInfoWithoutCommitV2() {
        val names = PHENOTYPE_FEATURES.map { it.name }
        assertEquals(listOf("get_storage_info_api"), names)
        assertTrue("commit_to_configuration_v2_api" !in names)
    }

    @Test
    fun returnsClientSupportedFallbackStatus() {
        var resultStatus: Status? = null
        var resultPayload: ByteArray? = byteArrayOf(1)
        val callbacks = object : IGetStorageInfoCallbacks.Default() {
            override fun onResult(status: Status?, storageInfo: ByteArray?) {
                resultStatus = status
                resultPayload = storageInfo
            }
        }

        deliverStorageInfoFallback(callbacks)

        assertEquals(29514, resultStatus?.statusCode)
        assertNull(resultPayload)
    }
}
