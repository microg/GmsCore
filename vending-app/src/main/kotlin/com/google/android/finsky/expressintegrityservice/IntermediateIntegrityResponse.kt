/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.expressintegrityservice

data class IntermediateIntegrityResponseData(
    var intermediateIntegrity: IntermediateIntegrity, var callerKeyMd5: String, var appVersionCode: Int, var deviceIntegrityResponse: DeviceIntegrityResponse, var appAccessRiskVerdictEnabled: Boolean?
)
