/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.moduleinstall;

import android.os.Parcel;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Response returned from {@link ModuleInstallClient#areModulesAvailable(OptionalModuleApi...)} indicating whether the
 * requested modules are already present on device.
 */
@SafeParcelable.Class
public class ModuleAvailabilityResponse extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "areModulesAvailable")
    private final boolean modulesAvailable;
    @Field(value = 2, getterName = "getAvailabilityStatus", type = "int")
    private final @AvailabilityStatus int availabilityStatus;

    @Constructor
    @Hide
    public ModuleAvailabilityResponse(@Param(1) boolean modulesAvailable, @Param(2) @AvailabilityStatus int availabilityStatus) {
        this.modulesAvailable = modulesAvailable;
        this.availabilityStatus = availabilityStatus;
    }

    /**
     * Returns {@code true} if the requested modules are already present, {@code false} otherwise.
     */
    public boolean areModulesAvailable() {
        return modulesAvailable;
    }

    /**
     * Returns the {@link ModuleAvailabilityResponse.AvailabilityStatus} for the requested modules.
     */
    public @AvailabilityStatus int getAvailabilityStatus() {
        return availabilityStatus;
    }

    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AvailabilityStatus.STATUS_ALREADY_AVAILABLE, AvailabilityStatus.STATUS_READY_TO_DOWNLOAD, AvailabilityStatus.STATUS_UNKNOWN_MODULE})
    public @interface AvailabilityStatus {
        /**
         * All the modules requested are already present on device.
         */
        int STATUS_ALREADY_AVAILABLE = 0;
        /**
         * There are modules requested not present on device, but they can be downloaded via an install request.
         */
        int STATUS_READY_TO_DOWNLOAD = 1;
        /**
         * There are modules requested that cannot be recognized. You can still still try to download the modules via an install
         * request, but it's not guaranteed the modules can be downloaded.
         */
        int STATUS_UNKNOWN_MODULE = 2;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ModuleAvailabilityResponse> CREATOR = findCreator(ModuleAvailabilityResponse.class);
}
