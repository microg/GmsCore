/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.mlkit.vision.documentscanner;

import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.core.os.ParcelCompat;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

public class GmsDocumentScanningResultPdfImpl extends GmsDocumentScanningResult.Pdf {
    @NonNull
    private final Uri uri;
    private final int pageCount;

    public GmsDocumentScanningResultPdfImpl(@NonNull Uri uri, int pageCount) {
        this.uri = uri;
        this.pageCount = pageCount;
    }

    @NonNull
    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public int getPageCount() {
        return pageCount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(getUri(), flags);
        dest.writeInt(getPageCount());
    }

    public static final Creator<GmsDocumentScanningResultPdfImpl> CREATOR = new Creator<GmsDocumentScanningResultPdfImpl>() {
        @Override
        public GmsDocumentScanningResultPdfImpl createFromParcel(Parcel source) {
            return new GmsDocumentScanningResultPdfImpl(
                    ParcelCompat.readParcelable(source, Uri.class.getClassLoader(), Uri.class),
                    source.readInt()
            );
        }

        @Override
        public GmsDocumentScanningResultPdfImpl[] newArray(int size) {
            return new GmsDocumentScanningResultPdfImpl[size];
        }
    };
}
