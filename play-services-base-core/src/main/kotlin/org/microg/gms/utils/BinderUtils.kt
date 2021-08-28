/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

import android.os.Binder
import android.os.Parcel
import android.util.Log

fun warnOnTransactionIssues(tag: String, code: Int, reply: Parcel?, flags: Int, base: () -> Boolean): Boolean {
    if (base.invoke()) {
        if ((flags and Binder.FLAG_ONEWAY) > 0 && (reply?.dataSize() ?: 0) > 0) {
            Log.w(tag, "onTransact[$code] is oneway, but returned data")
        }
        return true
    }
    Log.w(tag, "onTransact[$code] is not processed.")
    return (flags and Binder.FLAG_ONEWAY) > 0 // Don't return false on oneway transaction to suppress warning
}
