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
 * Data interface for managing Thread Network Credentials.
 */
@SafeParcelable.Class
public class ThreadNetworkCredentials extends AbstractSafeParcelable {
    /**
     * The minimum 2.4GHz channel.
     */
    public static final int CHANNEL_MIN_2P4GHZ = 11;
    /**
     * The maximum 2.4GHz channel.
     */
    public static final int CHANNEL_MAX_2P4GHZ = 26;
    /**
     * The 2.4 GHz channel page.
     */
    public static final int CHANNEL_PAGE_2P4GHZ = 0;

    /**
     * The length of Extended PAN ID that can be set by {@link ThreadNetworkCredentials.Builder#setExtendedPanId(byte[])}.
     */
    public static final int LENGTH_EXTENDED_PANID = 8;
    /**
     * The maximum length of NetworkName that can be set by {@link ThreadNetworkCredentials.Builder#setNetworkName(String)}.
     */
    public static final int LENGTH_MAX_NETWORK_NAME = 16;
    /**
     * The maximum length of Operational Dataset that can be set by {@link #fromActiveOperationalDataset(byte[])}.
     */
    public static final int LENGTH_MAX_OPERATIONAL_DATASET = 256;
    /**
     * The maximum length of Security Policy Flags that can be set by {@link ThreadNetworkCredentials.SecurityPolicy#SecurityPolicy(int, byte[])}.
     */
    public static final int LENGTH_MAX_SECURITY_POLICY_FLAGS = 2;
    /**
     * The length of Mesh-Local Prefix that can be set by {@link ThreadNetworkCredentials.Builder#setMeshLocalPrefix(byte[])}.
     */
    public static final int LENGTH_MESH_LOCAL_PREFIX = 8;
    /**
     * The minimum length of Network Name that can be set by {@link ThreadNetworkCredentials.Builder#setNetworkName(String)}.
     */
    public static final int LENGTH_MIN_NETWORK_NAME = 1;
    /**
     * The minimum length of Security Policy Flags that can be set by {@link ThreadNetworkCredentials.SecurityPolicy#SecurityPolicy(int, byte[])}.
     */
    public static final int LENGTH_MIN_SECURITY_POLICY_FLAGS = 1;
    /**
     * The length of Network Key that can be set by {@link ThreadNetworkCredentials.Builder#setNetworkKey(byte[])}.
     */
    public static final int LENGTH_NETWORK_KEY = 16;
    /**
     * The length of PSKc that can be set by {@link ThreadNetworkCredentials.Builder#setPskc(byte[])}.
     */
    public static final int LENGTH_PSKC = 16;
    /**
     * The fisrt byte of Mesh-Local Prefix that can be set by {@link ThreadNetworkCredentials.Builder#setMeshLocalPrefix(byte[])}.
     */
    public static final byte MESH_LOCAL_PREFIX_FIRST_BYTE = -3;

    /**
     * The default channel mask which enables all 2.4GHz channels.
     */
    public static final ThreadNetworkCredentials.ChannelMaskEntry DEFAULT_CHANNEL_MASK = new ChannelMaskEntry(0, new byte[]{0, 31, -1, -32});
    /**
     * The default Thread 1.2 Security Policy.
     */
    public static final ThreadNetworkCredentials.SecurityPolicy DEFAULT_SECURITY_POLICY = new SecurityPolicy(672, new byte[]{-1, -8});

    @Field(value = 1, getterName = "getActiveOperationalDataset")
    private final byte[] activeOperationalDataset;
    @Field(value = 2, getterName = "getCreatedAtMillis")
    private final long createdAtMillis;
    @Field(value = 3, getterName = "getUpdatedAtMillis")
    private final long updatedAtMillis;

    @Constructor
    public ThreadNetworkCredentials(@Param(1) byte[] activeOperationalDataset, @Param(2) long createdAtMillis, @Param(3) long updatedAtMillis) {
        this.activeOperationalDataset = activeOperationalDataset;
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }

    /**
     * Returns the Thread active operational dataset as encoded Thread TLV list.
     */
    public byte[] getActiveOperationalDataset() {
        return activeOperationalDataset;
    }

    /**
     * Returns the Unix epoch in Milliseconds the {@link ThreadNetworkCredentials} instance is created at. Note that this is the
     * time when the Thread network credentials is first added to the Thread Network service via
     * {@link ThreadNetworkClient#addCredentials(ThreadBorderAgent, ThreadNetworkCredentials)}. Zero will be returned
     * if this instance is not returned by ThreadNetworkClient methods (e.g. created with
     * {@link #fromActiveOperationalDataset(byte[])}).
     */
    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    /**
     * Returns the Unix epoch in Milliseconds the {@link ThreadNetworkCredentials} instance is updated at. Note that this is the
     * time when the Thread network credentials was last added/updated to the Thread Network service via
     * {@link ThreadNetworkClient#addCredentials(ThreadBorderAgent, ThreadNetworkCredentials). Zero will be returned
     * if this instance is not returned by ThreadNetworkClient methods (e.g. created with
     * {@link #fromActiveOperationalDataset(byte[])}).
     */
    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    /**
     * The Channel Mask Entry of Thread Operational Dataset.
     */
    public static class ChannelMaskEntry {
        private final int page;
        private final byte[] mask;

        /**
         * Creates a new {@link ChannelMaskEntry} object.
         *
         * @throws IllegalArgumentException if page exceeds range [0, 255].
         */
        public ChannelMaskEntry(int page, byte[] mask) {
            if (page < 0 || page > 255) throw new IllegalArgumentException("page exceeds range [0, 255].");
            this.page = page;
            this.mask = mask;
        }

        /**
         * Returns the Channel Mask.
         */
        public byte[] getMask() {
            return mask;
        }

        /**
         * Returns the Channel Page.
         */
        public int getPage() {
            return page;
        }
    }

    /**
     * The class represents Thread Security Policy.
     */
    public static class SecurityPolicy {
        private final int rotationTimeHours;
        private final byte[] flags;

        /**
         * Creates a new {@link SecurityPolicy} object.
         *
         * @param rotationTimeHours the value for Thread key rotation in hours. Must be in range of 0x1-0xffff.
         * @param flags             security policy flags with length of either 1 byte for Thread 1.1 or 2 bytes for Thread 1.2 or higher.
         * @throws IllegalArgumentException if {@code rotationTimeHours} is not in range of 0x1-0xffff or
         *                                  length of flags is smaller than {@link ThreadNetworkCredentials#LENGTH_MIN_SECURITY_POLICY_FLAGS}.
         */
        public SecurityPolicy(int rotationTimeHours, byte[] flags) {
            if (rotationTimeHours < 1 || rotationTimeHours > 0xffff) throw new IllegalArgumentException("rotationTimeHours is not in range of 0x1-0xffff");
            if (flags.length < LENGTH_MIN_SECURITY_POLICY_FLAGS) throw new IllegalArgumentException("length of flags is smaller than LENGTH_MIN_SECURITY_POLICY_FLAGS");
            this.rotationTimeHours = rotationTimeHours;
            this.flags = flags;
        }

        /**
         * Returns 1 byte flags for Thread 1.1 or 2 bytes flags for Thread 1.2.
         */
        public byte[] getFlags() {
            return flags;
        }

        /**
         * Returns the Security Policy Rotation Time in hours.
         */
        public int getRotationTimeHours() {
            return rotationTimeHours;
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ThreadNetworkCredentials> CREATOR = findCreator(ThreadNetworkCredentials.class);
}
