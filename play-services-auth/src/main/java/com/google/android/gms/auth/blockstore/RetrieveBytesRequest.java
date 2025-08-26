/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.blockstore;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SafeParcelable.Class
public class RetrieveBytesRequest extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getKeys")
    private final List<String> keys;

    @Field(value = 2)
    public final boolean retrieveAll;

    @Constructor
    public RetrieveBytesRequest(@Param(1) List<String> keys, @Param(2) boolean retrieveAll) {
        if (retrieveAll && keys != null && !keys.isEmpty()) {
            throw new IllegalArgumentException("retrieveAll was set to true but other constraint(s) was also provided: keys");
        }
        this.retrieveAll = retrieveAll;

        List<String> tmp = new ArrayList<>();
        if (keys != null) {
            for (String k : keys) {
                if (k == null || k.isEmpty()) {
                    throw new IllegalArgumentException("Element in keys cannot be null or empty");
                }
                tmp.add(k);
            }
        }
        this.keys = Collections.unmodifiableList(tmp);
    }

    public List<String> getKeys() {
        return keys;
    }

    public boolean getRetrieveAll() {
        return retrieveAll;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Override
    public String toString() {
        return ToStringHelper.name("RetrieveBytesRequest").field("keys", keys).field("retrieveAll", retrieveAll).end();
    }

    public static final SafeParcelableCreatorAndWriter<RetrieveBytesRequest> CREATOR = findCreator(RetrieveBytesRequest.class);

    public static class Builder {
        private final List<String> keys = new ArrayList<>();
        private boolean retrieveAll = false;

        public Builder setKeys(List<String> ks) {
            keys.clear();
            keys.addAll(ks);
            return this;
        }

        public Builder retrieveAll(boolean retrieveAll) {
            this.retrieveAll = retrieveAll;
            return this;
        }

        @PublicApi
        @NonNull
        public RetrieveBytesRequest build() {
            return new RetrieveBytesRequest(new ArrayList<>(keys), retrieveAll);
        }
    }
}