/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast.firstparty.internal;

import android.os.Bundle;
import com.google.android.gms.cast.firstparty.internal.ICastSettingsCallback;
import com.google.android.gms.common.api.ApiMetadata;

oneway interface ICastFirstPartyService {
    void getCastSettings(ICastSettingsCallback callback, in ApiMetadata metadata) = 2;
    void setBooleanSetting(String key, boolean value, in ApiMetadata metadata) = 3;
    void setActiveAccount(String accountName, in ApiMetadata metadata) = 8;
    void setBundleSettings(in List<String> keys, in List types, in Bundle values, in ApiMetadata metadata) = 11;
}
