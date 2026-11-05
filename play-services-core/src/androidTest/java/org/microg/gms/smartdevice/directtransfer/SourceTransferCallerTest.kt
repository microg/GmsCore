/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.test.AndroidTestCase

@Suppress("DEPRECATION")
class SourceTransferCallerTest : AndroidTestCase() {
    fun testOwnProcessCannotClaimCompanionUid() {
        try {
            SourceTransferCaller.capture(context, SourceTransferCaller.PACKAGE)
            fail("Accepted the test process as the companion")
        } catch (_: Exception) { }
    }

    fun testCallerSuppliedOtherPackageIsRejected() {
        try {
            SourceTransferCaller.capture(context, "org.microg.test.invalid")
            fail("Accepted another package")
        } catch (_: IllegalArgumentException) { }
    }
}
