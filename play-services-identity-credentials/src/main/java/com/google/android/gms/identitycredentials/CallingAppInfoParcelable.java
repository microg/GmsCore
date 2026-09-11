/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.identitycredentials;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Information pertaining to the calling application.
 * <p>
 * The {@code packageCertificates} will either return a single byte-array corresponding to the oldest available signature for pre-P devices; for P+
 * devices, it will return the full rotation history (including the signature used to sign the package) or an empty list. The empty list means the
 * package was not found or the package does not have any trust-worthy signatures.
 */
public class CallingAppInfoParcelable implements Parcelable {
    @NonNull
    private final String packageName;
    @NonNull
    private final List<byte[]> packageCertificates;
    @NonNull
    private final String origin;

    public CallingAppInfoParcelable(@NonNull String packageName, @NonNull List<byte[]> packageCertificates, @NonNull String origin) {
        this.packageName = packageName;
        this.packageCertificates = packageCertificates;
        this.origin = origin;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * the calling origin
     */
    @NonNull
    public String getOrigin() {
        return origin;
    }

    /**
     * a list of byte arrays, one for each rotated signature in raw bytes
     */
    @NonNull
    public List<byte[]> getPackageCertificates() {
        return packageCertificates;
    }

    /**
     * the calling app package name
     */
    @NonNull
    public String getPackageName() {
        return packageName;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeInt(packageCertificates.size());
        for (byte[] packageCertificate : packageCertificates) {
            dest.writeInt(packageCertificate.length);
            dest.writeByteArray(packageCertificate);
        }
        dest.writeString(origin);
    }

    public static final Creator<CallingAppInfoParcelable> CREATOR = new Creator<CallingAppInfoParcelable>() {
        @Override
        public CallingAppInfoParcelable createFromParcel(Parcel source) {
            String packageName = source.readString();
            int numPackageCertificates = source.readInt();
            if (packageName == null || numPackageCertificates < 0) {
                return null;
            }
            List<byte[]> packageCertificates = new ArrayList<>(numPackageCertificates);
            for (int i = 0; i < numPackageCertificates; i++) {
                byte[] packageCertificate = new byte[source.readInt()];
                source.readByteArray(packageCertificate);
                packageCertificates.add(packageCertificate);
            }
            String origin = source.readString();
            return new CallingAppInfoParcelable(packageName, packageCertificates, origin);
        }

        @Override
        public CallingAppInfoParcelable[] newArray(int size) {
            return new CallingAppInfoParcelable[size];
        }
    };
}
