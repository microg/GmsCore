/*
 * SPDX-FileCopyrightText: 2010 The Android Open Source Project
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.licensing;

interface ILicenseV2ResultListener {
    oneway void verifyLicense(int responseCode, in Bundle responsePayload);
}