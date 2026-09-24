/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.agesignals.protocol;

import android.os.Bundle;

interface IAgeSignalsServiceCallback {
    oneway void onCompleteCheckAgeSignals(in Bundle bundle) = 0;
    oneway void onError(in Bundle bundle) = 2;
}
