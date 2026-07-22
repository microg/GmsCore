/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object SignatureVerifier {
    const val TAG = "GmsGuardSigVerify"
    const val PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxW77dCKJ8mhEIfXXdeidi7/7LMNM/fzwI+wj1Ed8xIKgTYWCnekRko3JxQb4Cv/gEL5hEA8e9lFs3V67VUL6hCo1FxysXj7Q8n3Kp7hARDkbiZ0mdk8bSanqrPAXTPx6pEL2ZOzfFCHEtJdhz5Ozp2C4XTKF1SBv/YbpsqSUJwdhG7ZPGjyCMRloMww6ITpGdVQ8lChklkCek0WPbz2UrY5RC1qIJKmmcB6KNxxE776Dn6QoYbhN5jPeVBp7lDD3UxjfVzTxKKDAome6fUVBop3dpcLM6rq3+nNT2YArgqTD1qtsVM9vHlcLaAYaPg82vtIN80iDUseMlVHgK+nf6wIDAQAB"

    fun verifySignature(data: ByteArray, signature: ByteArray): Boolean {
        try {
            val keyFactory = KeyFactory.getInstance("RSA") ?: return false
            val sig = Signature.getInstance("SHA256withRSA") ?: return false
            val keySpec = X509EncodedKeySpec(Base64.decode(PUBLIC_KEY, 0))
            sig.initVerify(keyFactory.generatePublic(keySpec))
            sig.update(data)
            return sig.verify(signature)
        } catch (e: Exception) {
            Log.w(TAG, e)
            return false
        }
    }
}
