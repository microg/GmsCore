/*
 * SPDX-FileCopyrightText: 2010 The Android Open Source Project
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.licensing;

import com.android.vending.licensing.ILicenseResultListener;
import com.android.vending.licensing.ILicenseV2ResultListener;

interface ILicensingService {
    oneway void checkLicense(long nonce, String packageName, ILicenseResultListener listener);
    oneway void checkLicenseV2(String packageName, ILicenseV2ResultListener listener, in Bundle extraParams);

}