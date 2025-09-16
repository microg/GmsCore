/*
 * SPDX-FileCopyrightText: 2024 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.account;

import android.accounts.Account;
import com.google.android.gms.auth.account.IWorkAccountCallback;

interface IWorkAccountService {

    void setWorkAuthenticatorEnabled(boolean enabled) = 0;

    void addWorkAccount(IWorkAccountCallback callback, String token) = 1;

    void removeWorkAccount(IWorkAccountCallback callback, in Account account) = 2;
}