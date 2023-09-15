/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Represents up to three user verification methods used by the authenticator.
 */
@PublicApi
public class UvmEntries extends AutoSafeParcelable {
    @Field(1)
    @Nullable
    private List<UvmEntry> uvmEntryList;

    @Nullable
    public List<UvmEntry> getUvmEntryList() {
        return uvmEntryList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UvmEntries)) return false;

        UvmEntries that = (UvmEntries) o;

        if (uvmEntryList == null && that.uvmEntryList == null) return true;
        if (uvmEntryList == null || that.uvmEntryList == null) return false;
        return uvmEntryList.containsAll(that.uvmEntryList) && that.uvmEntryList.containsAll(uvmEntryList);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{new HashSet<>(uvmEntryList)});
    }

    /**
     * Builder for {@link UvmEntries}
     */
    public static class Builder {
        private List<UvmEntry> uvmEntryList = new ArrayList<>();

        /**
         * The constructor of {@link UvmEntries.Builder}.
         */
        public Builder() {
        }

        public Builder addAll(@NonNull List<UvmEntry> uvmEntryList) {
            if (this.uvmEntryList.size() + uvmEntryList.size() > 3) throw new IllegalStateException();
            this.uvmEntryList.addAll(uvmEntryList);
            return this;
        }

        public Builder addUvmEntry(@Nullable UvmEntry uvmEntry) {
            if (uvmEntryList.size() >= 3) throw new IllegalStateException();
            uvmEntryList.add(uvmEntry);
            return this;
        }

        @NonNull
        public UvmEntries build() {
            UvmEntries uvmEntries = new UvmEntries();
            uvmEntries.uvmEntryList = new ArrayList<>(uvmEntryList);
            return uvmEntries;
        }
    }

    @PublicApi(exclude = true)
    public static Creator<UvmEntries> CREATOR = new AutoCreator<>(UvmEntries.class);
}
