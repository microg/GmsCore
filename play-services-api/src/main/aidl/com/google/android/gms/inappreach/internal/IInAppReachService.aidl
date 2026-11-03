/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.inappreach.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.inappreach.internal.IOnAccountHealthAlertsListener;
import com.google.android.gms.inappreach.internal.IOnAccountMessagesListener;
import com.google.android.gms.inappreach.internal.IOnAccountDataResponseListener;

interface IInAppReachService {
    void registerAccountHealthAlerts(IStatusCallback callback, String clientPackageName, IOnAccountHealthAlertsListener listener, in ApiMetadata apiMetadata) = 0;
    void unregisterAccountHealthAlerts(IStatusCallback callback, String clientPackageName, in ApiMetadata apiMetadata) = 1;
    void registerAccountMessages(IStatusCallback callback, String clientPackageName, IOnAccountMessagesListener listener, in ApiMetadata apiMetadata) = 6;
    void unregisterAccountMessages(IStatusCallback callback, String clientPackageName, in ApiMetadata apiMetadata) = 7;
    void registerAccountDataResponse(IStatusCallback callback, String clientPackageName, IOnAccountDataResponseListener listener, in ApiMetadata apiMetadata) = 10;
    void unregisterAccountDataResponse(IStatusCallback callback, String clientPackageName, in ApiMetadata apiMetadata) = 11;
    void registerAccountDataResponseV2(IStatusCallback callback, String clientPackageName, IOnAccountDataResponseListener listener, in ApiMetadata apiMetadata) = 14;
    void unregisterAccountDataResponseV2(IStatusCallback callback, String clientPackageName, in ApiMetadata apiMetadata) = 15;
}
