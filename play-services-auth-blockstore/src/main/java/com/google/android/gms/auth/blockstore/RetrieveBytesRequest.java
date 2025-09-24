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

import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A request to retrieve app data from BlockStore.
 */
@SafeParcelable.Class
public class RetrieveBytesRequest extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getKeys")
    private final List<String> keys;

    @Field(value = 2, getterName = "getRetrieveAll")
    private final boolean retrieveAll;

    @Constructor
    RetrieveBytesRequest(@Param(1) List<String> keys, @Param(2) boolean retrieveAll) {
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

    /**
     * Returns the list of keys whose associated data, if any, should be retrieved.
     * <p>
     * An empty list means that no key-based filtering will be performed. In other words, no data will be returned if the key list is empty and no
     * other criterion is provided.
     * <p>
     * Note that the app data that was stored without an explicit key can be requested with the default key
     * {@link BlockstoreClient#DEFAULT_BYTES_DATA_KEY}.
     */
    public List<String> getKeys() {
        return keys;
    }

    /**
     * Returns whether or not all app's Block Store data should be retrieved.
     */
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

    /**
     * A builder for {@link RetrieveBytesRequest} objects.
     */
    public static class Builder {
        private final List<String> keys = new ArrayList<>();
        private boolean retrieveAll = false;

        /**
         * Builds and returns the {@link RetrieveBytesRequest} object.
         */
        @NonNull
        public RetrieveBytesRequest build() {
            return new RetrieveBytesRequest(new ArrayList<>(keys), retrieveAll);
        }

        /**
         * Sets the list of keys whose associated data, if any, should be retrieved.
         * <p>
         * The default value is an empty list, which means that no key-based filtering will be performed. In other words, no data will be returned if the
         * key list is empty and no other criterion is provided.
         * <p>
         * Note that the app data that was stored without an explicit key can be requested with the default key
         * {@link BlockstoreClient#DEFAULT_BYTES_DATA_KEY}.
         */
        public Builder setKeys(List<String> keys) {
            this.keys.clear();
            this.keys.addAll(keys);
            return this;
        }

        /**
         * Sets whether or not all app's Block Store data should be retrieved.
         * <p>
         * The default is {@code false}.
         * <p>
         * Note that if {@code retrieveAll} is set to true, then you should NOT set any other retrieval criterion, e.g. {@code keys} should be empty. Otherwise, an
         * IllegalStateException will be thrown.
         */
        public Builder retrieveAll(boolean retrieveAll) {
            this.retrieveAll = retrieveAll;
            return this;
        }
    }
}