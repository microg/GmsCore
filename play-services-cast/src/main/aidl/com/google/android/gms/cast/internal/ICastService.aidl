/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.cast.internal.IBundleCallback;
import com.google.android.gms.cast.RequestItem;

interface ICastService {
    oneway void broadcastPrecacheMessageLegacy(IStatusCallback callback, in String[] arg2, String precacheData) = 0;
    oneway void broadcastPrecacheMessage(IStatusCallback callback, in String[] arg2, String precacheData, in List<RequestItem> requestItems) = 1;
    oneway void getCxLessStatus(IStatusCallback callback) = 3;
    oneway void getFeatureFlags(IBundleCallback callback, in String[] flags) = 4;
    oneway void getCastStatusCodeDictionary(IBundleCallback callback, in String[] dictionaries) = 5;
}
