/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.moduleinstall;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

/**
 * Response returned from {@link ModuleInstallClient#installModules(ModuleInstallRequest)} which includes an integer
 * session id that is corresponding to a unique install request.
 */
@SafeParcelable.Class
public class ModuleInstallResponse extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getSessionId")
    private int sessionId;
    @Field(2)
    @Hide
    public boolean shouldUnregisterListener;

    @Constructor
    @Hide
    public ModuleInstallResponse(@Param(1) int sessionId, @Param(2) boolean shouldUnregisterListener) {
        this.sessionId = sessionId;
        this.shouldUnregisterListener = shouldUnregisterListener;
    }

    /**
     * Returns {@code true} if the requested modules are already installed, {@code false} otherwise.
     */
    public boolean areModulesAlreadyInstalled() {
        return sessionId == 0;
    }

    /**
     * Returns the session id corresponding to the {@link ModuleInstallRequest} sent in
     * {@link ModuleInstallClient#installModules(ModuleInstallRequest)}. A session id of 0 indicates that the optional
     * modules are already installed.
     */
    public int getSessionId() {
        return sessionId;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ModuleInstallResponse> CREATOR = findCreator(ModuleInstallResponse.class);
}
