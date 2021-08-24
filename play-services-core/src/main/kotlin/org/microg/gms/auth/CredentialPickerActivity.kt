/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth

import android.app.Activity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.auth.api.credentials.HintRequest

fun <T> Parcelable.Creator<T>.createFromBytes(bytes: ByteArray): T {
    val parcel = Parcel.obtain()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)
    try {
        return createFromParcel(parcel)
    } finally {
        parcel.recycle()
    }
}

class CredentialPickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent.extras ?: Bundle()
        val callingPackage = callingActivity?.packageName?.takeIf { extras.getString("claimedCallingPackage", it) == it }
        val logSessionId = extras.getString("logSessionId")
        val credentialRequest = extras.getByteArray("credentialRequest")?.let { CredentialRequest.CREATOR.createFromBytes(it) }
        val hintRequest = extras.getByteArray("com.google.android.gms.credentials.HintRequest")?.let { HintRequest.CREATOR.createFromBytes(it) }
        Log.d("GmsCredentialPicker", "Not implemented. callingPackage=$callingPackage, logSessionId=$logSessionId, credentialsRequest=$credentialRequest, hintRequest=$hintRequest")
        finish()
    }
}
