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

import java.util.ArrayList;
import java.util.List;

public class GmsDocumentScanningResultImpl extends GmsDocumentScanningResult {
    @Nullable
    private final List<Page> pages;
    @Nullable
    private final Pdf pdf;

    public GmsDocumentScanningResultImpl(@Nullable List<Page> pages, @Nullable Pdf pdf) {
        this.pages = pages;
        this.pdf = pdf;
    }

    @Nullable
    @Override
    public List<Page> getPages() {
        return pages;
    }

    @Nullable
    @Override
    public Pdf getPdf() {
        return pdf;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeList(getPages());
        dest.writeParcelable(getPdf(), flags);
    }

    public static final Creator<GmsDocumentScanningResultImpl> CREATOR = new Creator<GmsDocumentScanningResultImpl>() {

        @Override
        public GmsDocumentScanningResultImpl createFromParcel(Parcel source) {
            return new GmsDocumentScanningResultImpl(
                    ParcelCompat.readArrayList(source, GmsDocumentScanningResultPageImpl.class.getClassLoader(), GmsDocumentScanningResult.Page.class),
                    ParcelCompat.readParcelable(source, GmsDocumentScanningResultPdfImpl.class.getClassLoader(), GmsDocumentScanningResult.Pdf.class)
            );
        }

        @Override
        public GmsDocumentScanningResultImpl[] newArray(int size) {
            return new GmsDocumentScanningResultImpl[size];
        }
    };

    @NonNull
    public static GmsDocumentScanningResult create(@Nullable List<Uri> imageUris, @Nullable List<String> imageHashes, @Nullable Uri pdfUri, int pageCount) {
        ArrayList<GmsDocumentScanningResult.Page> pages = new ArrayList<>();
        if (imageUris != null) {
            if (imageHashes != null) {
                if (imageHashes.size() != imageUris.size())
                    throw new IllegalArgumentException("imageHashes and imageUris size mismatch.");
                for (int i = 0; i < imageHashes.size(); i++) {
                    pages.add(new GmsDocumentScanningResultPageImpl((Uri) imageUris.get(i), (String) imageHashes.get(i)));
                }
            } else {
                for (Uri uri : imageUris) {
                    pages.add(new GmsDocumentScanningResultPageImpl(uri, null));
                }
            }
        }
        GmsDocumentScanningResult.Pdf pdf = null;
        if (pdfUri != null) {
            pdf = new GmsDocumentScanningResultPdfImpl(pdfUri, pageCount);
        }
        return new GmsDocumentScanningResultImpl(pages, pdf);
    }
}
