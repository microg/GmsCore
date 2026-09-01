/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

import java.util.Arrays;

/**
 * Represents a single User Verification Method Entry
 */
@PublicApi
@SafeParcelable.Class
public class UvmEntry extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getUserVerificationMethod")
    private int userVerificationMethod;
    @Field(value = 2, getterName = "getKeyProtectionType")
    private short keyProtectionType;
    @Field(value = 3, getterName = "getMatcherProtectionType")
    private short matcherProtectionType;

    @Constructor
    UvmEntry(@Param(1) int userVerificationMethod, @Param(2) short keyProtectionType, @Param(3) short matcherProtectionType) {
        this.userVerificationMethod = userVerificationMethod;
        this.keyProtectionType = keyProtectionType;
        this.matcherProtectionType = matcherProtectionType;
    }

    public int getUserVerificationMethod() {
        return userVerificationMethod;
    }

    public short getKeyProtectionType() {
        return keyProtectionType;
    }

    public short getMatcherProtectionType() {
        return matcherProtectionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UvmEntry)) return false;

        UvmEntry uvmEntry = (UvmEntry) o;

        if (userVerificationMethod != uvmEntry.userVerificationMethod) return false;
        if (keyProtectionType != uvmEntry.keyProtectionType) return false;
        return matcherProtectionType == uvmEntry.matcherProtectionType;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{userVerificationMethod, keyProtectionType, matcherProtectionType});
    }

    /**
     * Builder for {@link UvmEntry}.
     */
    public static class Builder {
        private int userVerificationMethod;
        private short keyProtectionType;
        private short matcherProtectionType;

        public Builder setUserVerificationMethod(int userVerificationMethod) {
            this.userVerificationMethod = userVerificationMethod;
            return this;
        }

        public Builder setKeyProtectionType(short keyProtectionType) {
            this.keyProtectionType = keyProtectionType;
            return this;
        }

        public Builder setMatcherProtectionType(short matcherProtectionType) {
            this.matcherProtectionType = matcherProtectionType;
            return this;
        }

        public UvmEntry build() {
            return new UvmEntry(userVerificationMethod, keyProtectionType, matcherProtectionType);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Hide
    public static final SafeParcelableCreatorAndWriter<UvmEntry> CREATOR = findCreator(UvmEntry.class);
}
