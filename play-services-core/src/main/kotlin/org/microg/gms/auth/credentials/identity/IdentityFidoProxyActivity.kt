/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.identity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.fido.Fido.FIDO2_KEY_CREDENTIAL_EXTRA
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import org.microg.gms.auth.AuthConstants
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_CALLER

private const val REQUEST_CODE = 1586077619

class IdentityFidoProxyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivityForResult(Intent("org.microg.gms.fido.AUTHENTICATE").apply {
            `package` = packageName
            putExtras(intent.extras ?: Bundle())
            putExtra(KEY_CALLER, callingActivity?.packageName)
        }, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val publicKeyCredential = PublicKeyCredential.deserializeFromBytes(data?.getByteArrayExtra(FIDO2_KEY_CREDENTIAL_EXTRA))
                if (publicKeyCredential.response is AuthenticatorErrorResponse) {
                    val errorResponse = publicKeyCredential.response as AuthenticatorErrorResponse
                    setResult(RESULT_OK, Intent().apply {
                        putExtra(AuthConstants.STATUS, SafeParcelableSerializer.serializeToBytes(Status(CommonStatusCodes.ERROR, errorResponse.errorMessage)))
                    })
                } else {
                    setResult(RESULT_OK, Intent().apply {
                        putExtra(
                            AuthConstants.SIGN_IN_CREDENTIAL, SafeParcelableSerializer.serializeToBytes(
                                SignInCredential(
                                    publicKeyCredential.id,
                                    null, null, null, null, null, null, null,
                                    publicKeyCredential
                                )
                            )
                        )
                        putExtra(AuthConstants.STATUS, SafeParcelableSerializer.serializeToBytes(Status.SUCCESS))
                    })
                }
            } else {
                setResult(resultCode)
            }
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}