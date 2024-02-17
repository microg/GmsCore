/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.account.data;

import com.google.android.gms.auth.account.data.IDeviceManagementInfoCallback;
import android.accounts.Account;
import com.google.android.gms.common.api.internal.IStatusCallback;

interface IAccountDataService {
    void requestDeviceManagementInfo(in IDeviceManagementInfoCallback callback, in Account account) = 0;
    void requestAccountInfo(in IStatusCallback callback, in Account account, boolean isPrimary) = 1;
    void requestProfileInfo(in IStatusCallback callback, String profile) = 2;
}
