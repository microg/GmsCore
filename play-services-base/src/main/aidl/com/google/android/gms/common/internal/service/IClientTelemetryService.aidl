/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal.service;

import com.google.android.gms.common.internal.TelemetryData;

interface IClientTelemetryService {
    oneway void log(in TelemetryData data) = 0;
}