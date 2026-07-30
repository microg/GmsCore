/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Information about a phone number in the Constellation/RCS system.
 * Contains verification status, capabilities, and registration details.
 */
public class PhoneNumberInfo implements Parcelable {

    @NonNull
    private final String phoneNumber;
    private final boolean isVerified;
    private final boolean isRcsEnabled;
    private final int verificationStatus;
    private final long lastVerifiedTimestamp;
    private final long registrationExpiryTimestamp;
    @Nullable
    private final String carrierName;
    @Nullable
    private final String formattedNumber;
    private final int countryCode;
    private final boolean isRoaming;
    private final int supportedFeatures;
    @Nullable
    private final String configurationServer;

    public PhoneNumberInfo(@NonNull String phoneNumber, boolean isVerified,
                            boolean isRcsEnabled, int verificationStatus,
                            long lastVerifiedTimestamp, long registrationExpiryTimestamp,
                            @Nullable String carrierName, @Nullable String formattedNumber,
                            int countryCode, boolean isRoaming, int supportedFeatures,
                            @Nullable String configurationServer) {
        this.phoneNumber = phoneNumber;
        this.isVerified = isVerified;
        this.isRcsEnabled = isRcsEnabled;
        this.verificationStatus = verificationStatus;
        this.lastVerifiedTimestamp = lastVerifiedTimestamp;
        this.registrationExpiryTimestamp = registrationExpiryTimestamp;
        this.carrierName = carrierName;
        this.formattedNumber = formattedNumber;
        this.countryCode = countryCode;
        this.isRoaming = isRoaming;
        this.supportedFeatures = supportedFeatures;
        this.configurationServer = configurationServer;
    }

    protected PhoneNumberInfo(Parcel in) {
        phoneNumber = in.readString();
        isVerified = in.readByte() != 0;
        isRcsEnabled = in.readByte() != 0;
        verificationStatus = in.readInt();
        lastVerifiedTimestamp = in.readLong();
        registrationExpiryTimestamp = in.readLong();
        carrierName = in.readString();
        formattedNumber = in.readString();
        countryCode = in.readInt();
        isRoaming = in.readByte() != 0;
        supportedFeatures = in.readInt();
        configurationServer = in.readString();
    }

    @NonNull
    public String getPhoneNumber() { return phoneNumber; }
    public boolean isVerified() { return isVerified; }
    public boolean isRcsEnabled() { return isRcsEnabled; }
    public int getVerificationStatus() { return verificationStatus; }
    public long getLastVerifiedTimestamp() { return lastVerifiedTimestamp; }
    public long getRegistrationExpiryTimestamp() { return registrationExpiryTimestamp; }
    @Nullable
    public String getCarrierName() { return carrierName; }
    @Nullable
    public String getFormattedNumber() { return formattedNumber; }
    public int getCountryCode() { return countryCode; }
    public boolean isRoaming() { return isRoaming; }
    public int getSupportedFeatures() { return supportedFeatures; }
    @Nullable
    public String getConfigurationServer() { return configurationServer; }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(phoneNumber);
        dest.writeByte((byte) (isVerified ? 1 : 0));
        dest.writeByte((byte) (isRcsEnabled ? 1 : 0));
        dest.writeInt(verificationStatus);
        dest.writeLong(lastVerifiedTimestamp);
        dest.writeLong(registrationExpiryTimestamp);
        dest.writeString(carrierName);
        dest.writeString(formattedNumber);
        dest.writeInt(countryCode);
        dest.writeByte((byte) (isRoaming ? 1 : 0));
        dest.writeInt(supportedFeatures);
        dest.writeString(configurationServer);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<PhoneNumberInfo> CREATOR = new Creator<PhoneNumberInfo>() {
        @Override
        public PhoneNumberInfo createFromParcel(Parcel in) { return new PhoneNumberInfo(in); }
        @Override
        public PhoneNumberInfo[] newArray(int size) { return new PhoneNumberInfo[size]; }
    };

    @NonNull
    @Override
    public String toString() {
        return "PhoneNumberInfo{phoneNumber='" + phoneNumber + '\'' + ", isVerified=" + isVerified
                + ", isRcsEnabled=" + isRcsEnabled + ", carrier='" + carrierName + '\'' + '}';
    }
}
