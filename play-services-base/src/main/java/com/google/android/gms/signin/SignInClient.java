/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.signin;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.internal.IAccountAccessor;
import com.google.android.gms.signin.internal.ISignInCallbacks;

public interface SignInClient extends Api.Client {
    void clearAccountFromSessionStore();
    void saveDefaultAccount(@NonNull IAccountAccessor accountAccessor, boolean crossClient);
    void signIn(@NonNull ISignInCallbacks callbacks);
}
