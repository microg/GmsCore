/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.threadnetwork;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Data interface for Thread Border Agent.
 */
@SafeParcelable.Class
public class ThreadBorderAgent extends AbstractSafeParcelable {
    @Field(value = 2, getterName = "getId")
    private final byte[] id;

    @Constructor
    public ThreadBorderAgent(@Param(2) byte[] id) {
        this.id = id;
    }

    /**
     * Returns the id which uniquely identifies a Thread Border Agent device.
     */
    public byte[] getId() {
        return id;
    }

    /**
     * Creates a new {@link ThreadBorderAgent.Builder} for constructing a {@link ThreadBorderAgent}.
     *
     * @param id the id which uniquely identifies a Border Agent. The length must be 16 bytes.
     * @throws IllegalArgumentException if the id is not of length 16 bytes.
     */
    public static Builder newBuilder(byte[] id) {
        if (id.length != 16) throw new IllegalArgumentException("the id is not of length 16 bytes");
        return new Builder(id);
    }

    /**
     * Builder for constructing {@link ThreadBorderAgent} instances.
     */
    public static class Builder {
        private byte[] id;

        private Builder(byte[] id) {
            this.id = id;
        }

        /**
         * Constructs a {@link ThreadBorderAgent} as configured by this builder.
         */
        public ThreadBorderAgent build() {
            return new ThreadBorderAgent(id);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return "ThreadBorderAgent{" + byteArrayToHex(id) + "}";
    }

    public static final SafeParcelableCreatorAndWriter<ThreadBorderAgent> CREATOR = findCreator(ThreadBorderAgent.class);
}
