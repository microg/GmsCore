/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.net;

import com.google.android.gms.dynamic.IObjectWrapper;

interface ISocketFactoryCreator {
    IObjectWrapper newSocketFactory(in IObjectWrapper context, in IObjectWrapper keyManagers, in IObjectWrapper trustManagers, boolean useCache) = 1;
    IObjectWrapper newSocketFactoryWithCacheDir(in IObjectWrapper context, in IObjectWrapper keyManagers, in IObjectWrapper trustManagers, String cacheDir) = 2;
}
