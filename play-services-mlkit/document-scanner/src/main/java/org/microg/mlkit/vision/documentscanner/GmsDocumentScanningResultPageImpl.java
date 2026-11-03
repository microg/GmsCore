/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.mlkit.vision.documentscanner;

import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ParcelCompat;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

public class GmsDocumentScanningResultPageImpl extends GmsDocumentScanningResult.Page {
    @NonNull
    private final Uri imageUri;
    @Nullable
    private final String hash;

    public GmsDocumentScanningResultPageImpl(@NonNull Uri imageUri, @Nullable String hash) {
        this.imageUri = imageUri;
        this.hash = hash;
    }

    @NonNull
    @Override
    public Uri getImageUri() {
        return imageUri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(getImageUri(), flags);
        if (hash == null) {
            dest.writeInt(1);
        } else {
            dest.writeInt(0);
            dest.writeString(hash);
        }
    }

    public static final Creator<GmsDocumentScanningResultPageImpl> CREATOR = new Creator<GmsDocumentScanningResultPageImpl>() {
        @Override
        public GmsDocumentScanningResultPageImpl createFromParcel(Parcel source) {
            return new GmsDocumentScanningResultPageImpl(
                    ParcelCompat.readParcelable(source, Uri.class.getClassLoader(), Uri.class),
                    source.readInt() == 0 ? source.readString() : null
            );
        }

        @Override
        public GmsDocumentScanningResultPageImpl[] newArray(int size) {
            return new GmsDocumentScanningResultPageImpl[size];
        }
    };
}
