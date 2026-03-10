/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads

import android.os.Parcel
import androidx.annotation.Keep
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AdManager"

@Keep
class AdManagerCreatorImpl : AdManagerCreator.Stub() {
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
