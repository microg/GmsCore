/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay;

import android.app.Activity;
import android.app.PendingIntent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tapandpay.issuer.CreatePushProvisionSessionRequest;
import com.google.android.gms.tapandpay.issuer.IsTokenizedRequest;
import com.google.android.gms.tapandpay.issuer.PushProvisionSessionContext;
import com.google.android.gms.tapandpay.issuer.PushTokenizeRequest;
import com.google.android.gms.tapandpay.issuer.ServerPushProvisionRequest;
import com.google.android.gms.tapandpay.issuer.TokenInfo;
import com.google.android.gms.tapandpay.issuer.TokenStatus;
import com.google.android.gms.tapandpay.issuer.ViewTokenRequest;
import com.google.android.gms.tasks.Task;

import java.util.List;

public interface TapAndPayClient extends HasApiKey<Api.ApiOptions.NotRequiredOptions> {

    @NonNull
    String DATA_CHANGED_LISTENER_KEY = "tapAndPayDataChangedListener";

    @NonNull
    Task<PushProvisionSessionContext> createPushProvisionSession(@NonNull CreatePushProvisionSessionRequest request);

    void createWallet(@NonNull Activity activity, int i);

    @NonNull
    Task<String> getActiveWalletId();

    @NonNull
    Task<String> getEnvironment();

    @NonNull
    Task<String> getLinkingToken(@NonNull String str);

    @NonNull
    Task<String> getStableHardwareId();

    @NonNull
    Task<TokenStatus> getTokenStatus(int i, @NonNull String str);

    @NonNull
    Task<Boolean> isTokenized(@NonNull IsTokenizedRequest request);

    @NonNull
    Task<List<TokenInfo>> listTokens();

    void pushTokenize(@NonNull Activity activity, @NonNull PushTokenizeRequest request, int i);

    @NonNull
    Task<Void> registerDataChangedListener(@NonNull TapAndPay.DataChangedListener listener);

    @NonNull
    Task<Void> removeDataChangedListener(@NonNull TapAndPay.DataChangedListener listener);

    void requestDeleteToken(@NonNull Activity activity, @NonNull String str, int i, int i2);

    void requestSelectToken(@NonNull Activity activity, @NonNull String str, int i, int i2);

    void serverPushProvision(@NonNull Activity activity, @NonNull ServerPushProvisionRequest request, int i);

    void tokenize(@NonNull Activity activity, @Nullable String str, int i, @NonNull String str2, int i2, int i3);

    @NonNull
    Task<PendingIntent> viewToken(@NonNull ViewTokenRequest request);
}
