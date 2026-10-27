/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.test.InstrumentationTestCase
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Synthetic cookie only, no network or Google authentication. */
@Suppress("DEPRECATION")
class SourceTransferWebIsolationTest : InstrumentationTestCase() {
    fun testCheckpointCookieCannotCrossProfilesOrDefaultStore() {
        val names = List(2) { "microg_direct_transfer_test_" + UUID.randomUUID() }
        val views = mutableListOf<WebView>()
        lateinit var first: CookieManager
        lateinit var second: CookieManager
        val ready = CountDownLatch(1)
        val fixture = "fixture_" + UUID.randomUUID().toString().replace("-", "")
        try {
            var supported = false
            instrumentation.runOnMainSync { supported = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) }
            // Assert on the instrumentation thread so an unmet runtime prerequisite is
            // reported as a test failure rather than terminating the app's main thread.
            assertTrue("The runtime must provide isolated WebView profiles", supported)
            instrumentation.runOnMainSync {
                for (name in names) {
                    val view = WebView(instrumentation.targetContext)
                    views.add(view)
                    WebViewCompat.setProfile(view, name)
                }
                first = WebViewCompat.getProfile(views[0]).cookieManager
                second = WebViewCompat.getProfile(views[1]).cookieManager
                first.setAcceptCookie(true)
                first.setCookie("https://accounts.google.com/", "GASC=$fixture; Secure; HttpOnly; Path=/") { ready.countDown() }
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            assertEquals(fixture, SourceTransferWebPolicy.checkpoint(first.getCookie("https://accounts.google.com/")))
            assertNull(SourceTransferWebPolicy.checkpoint(second.getCookie("https://accounts.google.com/")))
            assertFalse(CookieManager.getInstance().getCookie("https://accounts.google.com/").orEmpty().contains(fixture))
            val erased = CountDownLatch(1)
            instrumentation.runOnMainSync { first.removeAllCookies { erased.countDown() } }
            assertTrue(erased.await(10, TimeUnit.SECONDS))
            assertNull(SourceTransferWebPolicy.checkpoint(first.getCookie("https://accounts.google.com/")))
        } finally {
            instrumentation.runOnMainSync {
                views.forEach { WebViewCompat.getProfile(it).cookieManager.removeAllCookies(null); it.destroy() }
                names.forEach { runCatching { ProfileStore.getInstance().deleteProfile(it) } }
            }
        }
    }
}
