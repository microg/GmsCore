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
 * <p>
 * The order of the conditions are important. If {@code deleteAll} is true, then the other conditions are ignored, and all the registries for your app that
 * was registered with the {@link IdentityCredentialClient#registerCredentials} API are deleted.
 */
@SafeParcelable.Class
public class ClearRegistryRequest extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getDeleteAll", defaultValue = "true")
    private final boolean deleteAll;

    @Field(value = 2, getterName = "getClearTypedRegistryOption")
    @Nullable
    private final ClearTypedRegistryOption clearTypedRegistryOption;

    /**
     * Cosntructs a request to clear all registries for your app that was registered with the IdentityCredentials.registerCredentials API.
     */
    public ClearRegistryRequest() {
        this(true, null);
    }

    /**
     * constructs an instance of {@link ClearRegistryRequest}
     *
     * @param deleteAll                whether to delete all registries for your app
     * @param clearTypedRegistryOption an option to clear the registries for a given type that matches the RegistrationRequest.type provided during registration
     */
    @Constructor
    public ClearRegistryRequest(@Param(1) boolean deleteAll, @Param(2) @Nullable ClearTypedRegistryOption clearTypedRegistryOption) {
        this.deleteAll = deleteAll;
        this.clearTypedRegistryOption = clearTypedRegistryOption;
    }

    /**
     * whether to delete all registries for your app
     */
    public final boolean getDeleteAll() {
        return this.deleteAll;
    }

    /**
     * an option to clear the registries for a given type that matches the RegistrationRequest.type provided during registration
     */
    @Nullable
    public final ClearTypedRegistryOption getClearTypedRegistryOption() {
        return this.clearTypedRegistryOption;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ClearRegistryRequest> CREATOR = findCreator(ClearRegistryRequest.class);

    /**
     * A request to configure how to clear the registries for a given type.
     * <p>
     * The order of the conditions are important. If {@code deleteAllForType} is true, then the other conditions are ignored and all the registries for the
     * given type are deleted. Otherwise, if {@code deleteIdlessRegistry} is true, then the registry with an empty ID is deleted; at the same time, the
     * registries with the IDs provided in {@code registryIds} will also be deleted.
     */
    public static class ClearTypedRegistryOption extends AbstractSafeParcelable {
        @Field(value = 1, getterName = "getDeleteAllForType")
        private final boolean deleteAllForType;

        @Field(value = 2, getterName = "getType")
        @NonNull
        private final String type;

        @Field(value = 3, getterName = "getDeleteIdlessRegistry")
        private final boolean deleteIdlessRegistry;

        @Field(value = 4, getterName = "getRegistryIds")
        @NonNull
        private final List<String> registryIds;

        /**
         * constructs an instance of {@link ClearTypedRegistryOption}
         *
         * @param deleteAllForType     whether to delete all registries for the given type
         * @param type                 the type of registry to clear, matching the RegistrationRequest.type provided during registration
         * @param deleteIdlessRegistry whether to delete the registry for the given type that was registered without an ID provided; in other words, the registry that was registered without providing RegistrationRequest.id
         * @param registryIds          the IDs of the registries for the given type to delete
         */
        @Constructor
        public ClearTypedRegistryOption(@Param(1) boolean deleteAllForType, @NonNull @Param(2) String type, @Param(3) boolean deleteIdlessRegistry, @NonNull @Param(4) List<String> registryIds) {
            this.deleteAllForType = deleteAllForType;
            this.type = type;
            this.deleteIdlessRegistry = deleteIdlessRegistry;
            this.registryIds = registryIds;
        }

        /**
         * whether to delete all registries for the given type
         */
        public final boolean getDeleteAllForType() {
            return this.deleteAllForType;
        }

        /**
         * whether to delete the registry for the given type that was registered without an ID provided; in other words, the registry that was registered without providing RegistrationRequest.id
         */
        public final boolean getDeleteIdlessRegistry() {
            return this.deleteIdlessRegistry;
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

        public static final SafeParcelableCreatorAndWriter<ClearTypedRegistryOption> CREATOR = findCreator(ClearTypedRegistryOption.class);
    }
}
