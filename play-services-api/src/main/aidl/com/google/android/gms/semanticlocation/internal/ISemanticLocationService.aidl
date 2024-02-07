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
// Declare any non-default types here with import statements

interface ISemanticLocationService {
    void registerSemanticLocationEventsOperation(in SemanticLocationParameters semanticLocationParameters, IStatusCallback callback, in SemanticLocationEventRequest semanticLocationEventRequest, in PendingIntent pendingIntent);

    void setIncognitoModeOperation(in SemanticLocationParameters semanticLocationParameters, IStatusCallback callback, boolean mode);

    void unregisterSemanticLocationEventsOperation(in SemanticLocationParameters semanticLocationParameters, IStatusCallback callback, in PendingIntent pendingIntent);

}