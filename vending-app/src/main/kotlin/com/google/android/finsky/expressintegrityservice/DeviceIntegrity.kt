/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.expressintegrityservice

import com.android.vending.Timestamp
import com.google.android.finsky.ClientKey
import okio.ByteString

data class DeviceIntegrity(
    var clientKey: ClientKey?, var deviceIntegrityToken: ByteString?, var creationTime: Timestamp?, var lastManualSoftRefreshTime: Timestamp?
)