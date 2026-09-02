/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wallet.firstparty;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class ExitResult extends AbstractSafeParcelable {

    @Field(1)
    public int paymentsExitCode;
    @Field(2)
    public String debugMessage;
    @Field(3)
    public int playBillingExitCode;
    @Field(4)
    public int apiErrorReason;

    @Constructor
    public ExitResult() {
        this(402, "", 0, 0);
    }

    @Constructor
    public ExitResult(@Param(1) int paymentsExitCode, @Param(2) String debugMessage, @Param(3) int playBillingExitCode, @Param(4) int apiErrorReason) {
        this.paymentsExitCode = paymentsExitCode;
        this.debugMessage = debugMessage;
        this.playBillingExitCode = playBillingExitCode;
        this.apiErrorReason = apiErrorReason;
    }

    public void writeToIntent(Intent intent) {
        Bundle bundle = new Bundle();
        bundle.putInt("paymentsExitCode", paymentsExitCode);
        bundle.putString("debugMessage", debugMessage);
        bundle.putInt("playBillingExitCode", playBillingExitCode);
        bundle.putInt("apiErrorReason", apiErrorReason);
        intent.putExtra("com.google.android.gms.wallet.firstparty.EXTRA_EXIT_RESULT_BUNDLE", bundle);
    }


    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ExitResult> CREATOR = findCreator(ExitResult.class);
}
