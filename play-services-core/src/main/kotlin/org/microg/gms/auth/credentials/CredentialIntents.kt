/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import org.microg.gms.auth.signin.ACTION_ASSISTED_SIGN_IN
import org.microg.gms.auth.signin.CLIENT_PACKAGE_NAME
import org.microg.gms.auth.signin.GOOGLE_SIGN_IN_OPTIONS
import org.microg.gms.common.GmsService
import org.microg.gms.fido.core.ui.ACTION_FIDO_AUTHENTICATE
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_CALLER
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_CREDENTIAL_ID
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_OPTIONS
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_SERVICE
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_SOURCE
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_TYPE

fun Context.buildFidoAuthenticateIntent(
    source: String,
    optionsBytes: ByteArray,
    callingPackage: String?,
    type: String,
    credentialIdString: String? = null,
): Intent = Intent(ACTION_FIDO_AUTHENTICATE).apply {
    `package` = packageName
    putExtra(KEY_SERVICE, GmsService.FIDO2_API.SERVICE_ID)
    putExtra(KEY_SOURCE, source)
    putExtra(KEY_TYPE, type)
    putExtra(KEY_OPTIONS, optionsBytes)
    callingPackage?.let { putExtra(KEY_CALLER, it) }
    credentialIdString?.let { putExtra(KEY_CREDENTIAL_ID, it) }
}

fun Context.buildAssistedSignInIntent(
    requestExtraKey: String,
    serializedRequest: ByteArray,
    googleSignInOptions: GoogleSignInOptions,
    callingPackage: String?,
): Intent = Intent(ACTION_ASSISTED_SIGN_IN).apply {
    `package` = packageName
    putExtra(requestExtraKey, serializedRequest)
    putExtra(GOOGLE_SIGN_IN_OPTIONS, SafeParcelableSerializer.serializeToBytes(googleSignInOptions))
    callingPackage?.let { putExtra(CLIENT_PACKAGE_NAME, it) }
}
