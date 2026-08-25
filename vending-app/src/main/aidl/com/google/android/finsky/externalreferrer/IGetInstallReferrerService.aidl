/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.externalreferrer;

interface IGetInstallReferrerService {
    Bundle getInstallReferrer(in Bundle request);
}