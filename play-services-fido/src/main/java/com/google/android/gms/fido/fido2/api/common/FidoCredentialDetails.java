/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Objects;

/**
 * Contains the attributes of a single FIDO credential that are returned to the caller in response to a
 * {@link Fido2PrivilegedApiClient#getCredentialList(String)} call.
 */
public class FidoCredentialDetails extends AutoSafeParcelable {
    @Field(1)
    @Nullable
    private String userName;
    @Field(2)
    @Nullable
    private String userDisplayName;
    @Field(3)
    @Nullable
    private byte[] userId;
    @Field(4)
    @NonNull
    private byte[] credentialId;
    @Field(5)
    private boolean discoverable;
    @Field(6)
    private boolean paymentCredential;

    private FidoCredentialDetails() {
    }

    @Constructor
    FidoCredentialDetails(@Nullable @Param(1) String userName, @Nullable @Param(2) String userDisplayName, @Nullable @Param(3) byte[] userId, @NonNull @Param(4) byte[] credentialId, @Param(5) boolean discoverable, @Param(6) boolean paymentCredential) {
        this.userName = userName;
        this.userDisplayName = userDisplayName;
        this.userId = userId;
        this.credentialId = credentialId;
        this.discoverable = discoverable;
        this.paymentCredential = paymentCredential;
    }

    /**
     * De-serializes the {@link FidoCredentialDetails} from bytes, reversing {@link #serializeToBytes()}.
     *
     * @return The deserialized {@link FidoCredentialDetails}.
     */
    @NonNull
    public static FidoCredentialDetails deserializeFromBytes(@NonNull byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    /**
     * Returns the credential's credential ID.
     */
    @NonNull
    public byte[] getCredentialId() {
        return credentialId;
    }

    /**
     * Returns true if the credential is discoverable.
     */
    public boolean getIsDiscoverable() {
        return discoverable;
    }

    /**
     * Returns true if the credential is for payments.
     */
    public boolean getIsPaymentCredential() {
        return paymentCredential;
    }

    /**
     * Returns the last used time in Unix Epoch Millis.
     */
    public long getLastUsedTime() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the credential's user display name.
     */
    @Nullable
    public String getUserDisplayName() {
        return userDisplayName;
    }

    /**
     * Returns the credential's user ID.
     */
    @Nullable
    public byte[] getUserId() {
        return userId;
    }

    /**
     * Returns the credential's user name.
     */
    @Nullable
    public String getUserName() {
        return this.userName;
    }

    public int hashCode() {
        return Objects.hashCode(new Object[]{this.userName, this.userDisplayName, this.userId, this.credentialId, this.discoverable, this.paymentCredential});
    }

    /**
     * Serializes the {@link FidoCredentialDetails} to bytes. Use {@link #deserializeFromBytes(byte[])} to deserialize.
     *
     * @return the serialized byte array.
     */
    @NonNull
    public byte[] serializeToBytes() {
        return SafeParcelableSerializer.serializeToBytes(this);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FidoCredentialDetails> CREATOR = findCreator(FidoCredentialDetails.class);
}
