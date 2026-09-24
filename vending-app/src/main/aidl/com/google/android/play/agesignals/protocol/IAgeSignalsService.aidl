/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.agesignals.protocol;

import android.os.Bundle;
import com.google.android.play.agesignals.protocol.IAgeSignalsAccessCallback;
import com.google.android.play.agesignals.protocol.IAgeSignalsServiceCallback;

interface IAgeSignalsService {
    oneway void checkAgeSignals(String packageName, in Bundle bundle, in IAgeSignalsServiceCallback callback) = 0;
    oneway void requestAgeSignalsAccess(String packageName, in Bundle bundle, in IAgeSignalsAccessCallback callback) = 1;
}
