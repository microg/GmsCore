/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wallet

/**
 * BENDER3 widget intent — wires PmRootChimeraActivity (producer) to
 * GenericDelegatorChimeraActivityX (consumer). The 3DS2 / IAP secure-payments
 * inner activity is launched with this action and reads the extras below.
 */
const val ACTION_BENDER3 = "com.google.android.gms.firstparty.ACTION_BENDER3"

const val EXTRA_BENDER3_BUYFLOW_CONFIG = "buyflowConfig"
const val EXTRA_BENDER3_O2_ACTION_TOKEN = "o2ActionToken"
const val EXTRA_BENDER3_ENCRYPTED_PARAMS = "encryptedParams"
const val EXTRA_BENDER3_UNENCRYPTED_PARAMS = "unencryptedParams"

/**
 * OAuth2 scope used by the Wallet/IAP "Sierra" service to acquire a per-account
 * auth token before initializing a Bender3 payment flow.
 */
const val OAUTH_SCOPE_SIERRA = "oauth2:https://www.googleapis.com/auth/sierra"
