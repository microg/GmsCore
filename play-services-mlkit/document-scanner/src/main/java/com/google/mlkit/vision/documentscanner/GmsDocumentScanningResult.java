/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.mlkit.vision.documentscanner;

import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_SCANNING_RESULT;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Result for document scanning.
 */
public abstract class GmsDocumentScanningResult implements Parcelable {

    /**
     * Returns the scanned pages or {@code null} if {@link GmsDocumentScannerOptions#RESULT_FORMAT_JPEG} was not specified when
     * creating the scanner options.
     */
    @Nullable
    public abstract List<Page> getPages();

    /**
     * Returns the PDF result or {@code null} if {@link GmsDocumentScannerOptions#RESULT_FORMAT_PDF} was not specified when creating the
     * scanner options.
     */
    @Nullable
    public abstract Pdf getPdf();

    /**
     * Gets the {@link GmsDocumentScanningResult} from the scanning Activity result data.
     * <p>
     * Returns {@code null} if fails to extract or construct the result.
     */
    @Nullable
    public static GmsDocumentScanningResult fromActivityResultIntent(@Nullable Intent data) {
        if (data == null) {
            return null;
        }
        //noinspection deprecation
        return data.getParcelableExtra(EXTRA_SCANNING_RESULT);
    }

    /**
     * Represents a page in the scanning result.
     */
    public static abstract class Page implements Parcelable {
        /**
         * Returns the image file Uri.
         * <p>
         * This Uri points to a file in the client app's private cache storage.
         */
        @NonNull
        public abstract Uri getImageUri();
    }

    /**
     * Represents the PDF in the scanning result.
     */
    public static abstract class Pdf implements Parcelable {
        /**
         * Returns the PDF file Uri.
         * <p>
         * This Uri points to a file in the client app's private cache storage.
         */
        @NonNull
        public abstract Uri getUri();

        /**
         * Returns the number of page being scanned.
         */
        public abstract int getPageCount();
    }

}
