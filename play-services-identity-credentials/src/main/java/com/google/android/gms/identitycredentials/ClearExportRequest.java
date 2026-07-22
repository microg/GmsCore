/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.identitycredentials;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

/**
 * A request to clear the registries stored for your app.
 * <p>
 * The order of the conditions are important. If {@code deleteAll} is true, then the other conditions are ignored, and all the registries for your app that
 * was registered with the {@link IdentityCredentialClient#registerExport} API are deleted.
 */
@SafeParcelable.Class
public class ClearExportRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getDeleteAll")
    private final boolean deleteAll;

    @Field(value = 2, getterName = "getRegistryIds")
    @NonNull
    private final List<String> registryIds;

    /**
     * constructs an instance of {@link ClearExportRequest}
     *
     * @param deleteAll   whether to delete all export registries for your app
     * @param registryIds the IDs of the registries for the given type to delete
     */
    @Constructor
    public ClearExportRequest(@Param(1) boolean deleteAll, @NonNull @Param(2) List<String> registryIds) {
        this.deleteAll = deleteAll;
        this.registryIds = registryIds;
    }

    /**
     * whether to delete all export registries for your app
     */
    public final boolean getDeleteAll() {
        return this.deleteAll;
    }

    /**
     * the IDs of the registries for the given type to delete
     */
    @NonNull
    public final List<String> getRegistryIds() {
        return this.registryIds;
    }

    @NonNull
    public final ClearRegistryRequest.ClearTypedRegistryOption getClearRegistryOption() {
        return new ClearRegistryRequest.ClearTypedRegistryOption(this.deleteAll, "androidx.identitycredentials.TYPE_CREDENTIALS_SYNC", false, this.registryIds);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ClearExportRequest> CREATOR = findCreator(ClearExportRequest.class);
}
