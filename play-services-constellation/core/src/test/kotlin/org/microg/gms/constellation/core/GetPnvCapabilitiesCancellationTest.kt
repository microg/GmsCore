/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class GetPnvCapabilitiesCancellationTest {
    @Test
    fun cancellationDuringSubscriptionQuery_doesNotDeliverSuccessOrErrors() = runBlocking {
        val outcomes = listOf(null, SecurityException("permission revoked"), IllegalStateException("query failed"))
        for (failure in outcomes) {
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val context = Mockito.mock(Context::class.java)
            val telephony = Mockito.mock(TelephonyManager::class.java)
            val subscriptions = Mockito.mock(SubscriptionManager::class.java)
            val callbacks = Mockito.mock(IConstellationCallbacks::class.java)
            Mockito.`when`(context.getSystemService(TelephonyManager::class.java)).thenReturn(telephony)
            Mockito.`when`(context.getSystemService(SubscriptionManager::class.java)).thenReturn(subscriptions)
            Mockito.`when`(subscriptions.activeSubscriptionInfoList).thenAnswer {
                entered.countDown()
                check(release.await(10, TimeUnit.SECONDS)) { "subscription query was not released" }
                failure?.let { throw it }
                emptyList<SubscriptionInfo>()
            }

            val job = launch(Dispatchers.Default) {
                handleGetPnvCapabilities(
                    context, callbacks, GetPnvCapabilitiesRequest(null, emptyList(), emptyList())
                )
            }
            try {
                assertTrue("handler must reach the blocking query", entered.await(10, TimeUnit.SECONDS))
                job.cancel()
                release.countDown()
                job.join()

                assertTrue("the request must remain cancelled", job.isCancelled)
                Mockito.verifyNoInteractions(callbacks)
            } finally {
                release.countDown()
                job.cancelAndJoin()
            }
        }
    }
}
