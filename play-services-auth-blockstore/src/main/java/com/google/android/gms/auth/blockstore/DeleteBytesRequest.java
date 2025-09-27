/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.blockstore;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A request to delete app data from BlockStore.
 */
@SafeParcelable.Class
public class DeleteBytesRequest extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getKeys")
    private final List<String> keys;

    @Field(value = 2, getterName = "getDeleteAll")
    private final boolean deleteAll;

    @Constructor
    DeleteBytesRequest(@Param(1) List<String> keys, @Param(2) boolean deleteAll) {
        this.keys = keys;
        this.deleteAll = deleteAll;
        if (deleteAll && keys != null && !keys.isEmpty()) {
            throw new IllegalArgumentException("deleteAll was set to true but keys were also provided");
        }
        for (String key : keys) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Element in keys cannot be null or empty");
            }
        }
    }

    /**
     * Returns the list of keys whose associated data, if any, should be deleted.
     * <p>
     * An empty list means that no key-based filtering will be performed. In other words, no data will be deleted if the key list is empty and no other
     * criterion is provided.
     * <p>
     * Note that the app data that was stored without an explicit key can be deleted with the default key
     * {@link BlockstoreClient#DEFAULT_BYTES_DATA_KEY}.
     */
    @NonNull
    public List<String> getKeys() {
        return keys;
    }

    /**
     * Returns whether or not all app's Block Store data should be deleted.
     */
    public boolean getDeleteAll() {
        return deleteAll;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @NonNull
    @Override
    @Hide
    public String toString() {
        return ToStringHelper.name("DeleteBytesRequest").field("deleteAll", deleteAll).field("keys", keys).end();
    }

    public static final SafeParcelableCreatorAndWriter<DeleteBytesRequest> CREATOR = findCreator(DeleteBytesRequest.class);

    /**
     * A builder for {@link DeleteBytesRequest} objects.
     */
    public static class Builder {
        private final List<String> keyList = new ArrayList<>();
        private boolean deleteAll = false;

        /**
         * Constructor for the {@link DeleteBytesRequest.Builder} class.
         */
        public Builder() {
        }

        /**
         * Builds and returns the {@link DeleteBytesRequest} object.
         */
        public DeleteBytesRequest build() {
            if (deleteAll && !keyList.isEmpty()) {
                throw new IllegalStateException("deleteAll=true but keys are provided");
            }
            return new DeleteBytesRequest(new ArrayList<>(keyList), deleteAll);
        }

        /**
         * Sets whether or not all app's Block Store data should be deleted.
         * <p>
         * The default is {@code false}.
         * <p>
         * Note that if {@code deleteAll} is set to true, then you should NOT set any other deletion criterion, e.g. {@code keys} should be empty. Otherwise, an
         * IllegalStateException will be thrown.
         */
        public Builder setDeleteAll(boolean deleteAll) {
            this.deleteAll = deleteAll;
            return this;
        }

        /**
         * Sets the list of keys whose associated data, if any, should be deleted.
         * <p>
         * The default value is an empty list, which means that no key-based filtering will be performed. In other words, no data will be deleted if the
         * key list is empty and no other criterion is provided.
         * <p>
         * Note that the app data that was stored without an explicit key can be deleted with the default key
         * {@link BlockstoreClient#DEFAULT_BYTES_DATA_KEY}.
         */
        public Builder setKeys(List<String> keys) {
            keyList.clear();
            keyList.addAll(keys);
            return this;
        }
    }
}