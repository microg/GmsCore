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

import org.microg.gms.utils.ToStringHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SafeParcelable.Class
public class DeleteBytesRequest extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getKeys")
    private final List<String> keys;

    @Field(value = 2)
    public final boolean deleteAll;

    @Constructor
    public DeleteBytesRequest(@Param(1) List<String> keys, @Param(2) boolean deleteAll) {
        if (deleteAll) {
            boolean ok = (keys == null || keys.isEmpty());
            if (!ok) {
                throw new IllegalArgumentException("deleteAll was set to true but keys were also provided");
            }
        }
        this.deleteAll = deleteAll;
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

    public boolean getDeleteAll() {
        return deleteAll;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Override
    public String toString() {
        return ToStringHelper.name("DeleteBytesRequest").field("deleteAll", deleteAll).field("keys", keys).end();
    }

    public static final SafeParcelableCreatorAndWriter<DeleteBytesRequest> CREATOR = findCreator(DeleteBytesRequest.class);

    public static class Builder {
        private final List<String> keyList = new ArrayList<>();
        private boolean deleteAll = false;

        public Builder setKeys(List<String> keys) {
            keyList.clear();
            keyList.addAll(keys);
            return this;
        }

        public Builder setDeleteAll(boolean deleteAll) {
            this.deleteAll = deleteAll;
            return this;
        }

        public DeleteBytesRequest build() {
            if (deleteAll && !keyList.isEmpty()) {
                throw new IllegalStateException("deleteAll=true but keys are provided");
            }
            return new DeleteBytesRequest(new ArrayList<>(keyList), deleteAll);
        }
    }
}