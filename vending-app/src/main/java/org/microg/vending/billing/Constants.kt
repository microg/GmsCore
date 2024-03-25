/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing

import com.google.android.gms.common.BuildConfig

const val TAG = "Billing"

// TODO: What versions to use?
const val VENDING_VERSION_CODE = 83061810L
const val VENDING_VERSION_NAME = "30.6.18-21 [0] [PR] 450795914"
const val VENDING_PACKAGE_NAME = "com.android.vending"
// TODO: Replace key name
const val KEY_IAP_SHEET_UI_PARAM = "key_iap_sheet_ui_param"
const val DEFAULT_ACCOUNT_TYPE = BuildConfig.BASE_PACKAGE_NAME
const val ADD_PAYMENT_METHOD_URL = "https://play.google.com/store/paymentmethods"