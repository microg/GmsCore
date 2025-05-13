/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.internal;

import com.google.android.gms.fitness.request.ListSubscriptionsRequest;
import com.google.android.gms.fitness.request.SubscribeRequest;
import com.google.android.gms.fitness.request.UnsubscribeRequest;

interface IGoogleFitRecordingApi {
    void subscribe(in SubscribeRequest request) = 0;
    void unsubscribe(in UnsubscribeRequest request) = 1;
    void listSubscriptions(in ListSubscriptionsRequest request) = 2;
}
