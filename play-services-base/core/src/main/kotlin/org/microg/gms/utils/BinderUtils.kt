/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Log

private const val TAG = "BinderUtils"

fun IBinder.warnOnTransactionIssues(code: Int, reply: Parcel?, flags: Int, tag: String = TAG, base: () -> Boolean): Boolean {
    if (base.invoke()) {
        if ((flags and Binder.FLAG_ONEWAY) > 0 && (reply?.dataSize() ?: 0) > 0) {
            Log.w(tag, "Method $code in $interfaceDescriptor is oneway, but returned data")
        }
        return true
    }
    Log.w(tag, "Unknown method $code in $interfaceDescriptor, skipping")
    return (flags and Binder.FLAG_ONEWAY) > 0 // Don't return false on oneway transaction to suppress warning
}
