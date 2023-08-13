/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.credential.manager.common;

import com.google.android.gms.common.api.Status;

interface ISettingsCallback {
    void onSetting(in Status status, in byte[] value);
}