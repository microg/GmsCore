/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.semanticlocation.internal;

import android.app.PendingIntent;
import android.os.IInterface;
import com.google.android.gms.semanticlocation.SemanticLocationEventRequest;
import com.google.android.gms.semanticlocation.internal.SemanticLocationParameters;
import com.google.android.gms.common.api.internal.IStatusCallback;

interface ISemanticLocationService {
    void registerSemanticLocationEvents(in SemanticLocationParameters params, IStatusCallback callback, in SemanticLocationEventRequest request, in PendingIntent pendingIntent) = 0;
    void unregisterSemanticLocationEvents(in SemanticLocationParameters params, IStatusCallback callback, in PendingIntent pendingIntent) = 1;

    void setIncognitoMode(in SemanticLocationParameters params, IStatusCallback callback, boolean mode) = 4;
}