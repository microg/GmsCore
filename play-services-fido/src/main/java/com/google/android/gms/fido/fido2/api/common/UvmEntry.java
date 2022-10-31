/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

/**
 * Represents a single User Verification Method Entry
 */
@PublicApi
public class UvmEntry extends AutoSafeParcelable {
    @Field(1)
    private int userVerificationMethod;
    @Field(2)
    private short keyProtectionType;
    @Field(3)
    private short matcherProtectionType;

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
            UvmEntry entry = new UvmEntry();
            entry.userVerificationMethod = userVerificationMethod;
            entry.keyProtectionType = keyProtectionType;
            entry.matcherProtectionType = matcherProtectionType;
            return entry;
        }
    }

    @PublicApi(exclude = true)
    public static final Creator<UvmEntry> CREATOR = new AutoCreator<>(UvmEntry.class);
}
