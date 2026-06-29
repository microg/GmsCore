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
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

/**
 * A request to clear the registries stored for your app.
 */
@SafeParcelable.Class
public class ClearCreationOptionsRequest extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getDeleteAll", defaultValue = "true")
    private final boolean deleteAll;
    @Field(value = 2, getterName = "getClearTypedRegistryOption")
    @Nullable
    private final ClearTypedCreationOption clearTypedRegistryOption;

    /**
     * Cosntructs a request to clear all registries for your app that was registered with the IdentityCredentials.registerCredentials API.
     */
    public ClearCreationOptionsRequest() {
        this(true, null);
    }

    /**
     * constructs an instance of {@link ClearCreationOptionsRequest}
     *
     * @param deleteAll                whether to delete all registries for your app
     * @param clearTypedRegistryOption an option to clear the registries for a given type that matches the {@code RegisterCreationOptionsRequest.type} provided during registration
     */
    @Constructor
    public ClearCreationOptionsRequest(@Param(1) boolean deleteAll, @Param(2) @Nullable ClearTypedCreationOption clearTypedRegistryOption) {
        this.deleteAll = deleteAll;
        this.clearTypedRegistryOption = clearTypedRegistryOption;
    }

    /**
     * whether to delete all registries for your app
     */
    public boolean getDeleteAll() {
        return deleteAll;
    }

    /**
     * an option to clear the registries for a given type that matches the RegisterCreationOptionsRequest.type provided during registration
     */
    @Nullable
    public ClearTypedCreationOption getClearTypedRegistryOption() {
        return clearTypedRegistryOption;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ClearCreationOptionsRequest> CREATOR = findCreator(ClearCreationOptionsRequest.class);

    /**
     * A request to configure how to clear the registries for a given type.
     * <p>
     * The order of the conditions are important. If {@code deleteAllForType} is true, then the other conditions are ignored and all the
     * registries for the given type are deleted. Otherwise, the registries with the IDs provided in {@code registryIds} will be deleted.
     */
    @Class
    public static class ClearTypedCreationOption extends AbstractSafeParcelable {
        @Field(value = 1, getterName = "getDeleteAllForType")
        private final boolean deleteAllForType;

        @Field(value = 2, getterName = "getType")
        @NonNull
        private final String type;

        @Field(value = 3, getterName = "getRegistryIds")
        @NonNull
        private final List<String> registryIds;

        /**
         * constructs an instance of {@link ClearTypedCreationOption}
         *
         * @param deleteAllForType whether to delete all registries for the given type
         * @param type             the type of registry to clear, matching the RegistrationRequest.type provided during registration
         * @param registryIds      the IDs of the registries for the given type to delete
         */
        @Constructor
        public ClearTypedCreationOption(@Param(1) boolean deleteAllForType, @NonNull @Param(2) String type, @NonNull @Param(3) List<String> registryIds) {
            this.deleteAllForType = deleteAllForType;
            this.type = type;
            this.registryIds = registryIds;
        }

        /**
         * whether to delete all registries for the given type
         */
        public final boolean getDeleteAllForType() {
            return this.deleteAllForType;
        }

        /**
         * the IDs of the registries for the given type to delete
         */
        @NonNull
        public final List<String> getRegistryIds() {
            return this.registryIds;
        }

        /**
         * the type of registry to clear, matching the RegistrationRequest.type provided during registration
         */
        @NonNull
        public final String getType() {
            return this.type;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<ClearTypedCreationOption> CREATOR = findCreator(ClearTypedCreationOption.class);
    }
}
