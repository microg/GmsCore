/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.moduleinstall;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

/**
 * Response returned from {@link ModuleInstallClient#getInstallModulesIntent(OptionalModuleApi...)} that includes a
 * {@link PendingIntent} that can be used to launch the module installation flow.
 */
@SafeParcelable.Class
public class ModuleInstallIntentResponse extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getPendingIntent")
    private final @Nullable PendingIntent pendingIntent;

    @Constructor
    @Hide
    public ModuleInstallIntentResponse(@Param(1) @Nullable PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    /**
     * Returns the {@link PendingIntent} to launch the module installation flow. This intent can be started with
     * {@link Activity#startIntentSenderForResult(IntentSender, int, Intent, int, int, int)} to present the UI. A null
     * {@link PendingIntent} indicates the requested optional modules are already present on device.
     */
    public @Nullable PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ModuleInstallIntentResponse> CREATOR = findCreator(ModuleInstallIntentResponse.class);
}
