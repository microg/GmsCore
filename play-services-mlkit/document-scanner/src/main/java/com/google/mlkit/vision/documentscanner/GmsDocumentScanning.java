/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.mlkit.vision.documentscanner;

import androidx.annotation.NonNull;

import org.microg.mlkit.vision.documentscanner.GmsDocumentScannerImpl;

/**
 * Entry point to get a {@link GmsDocumentScanner} for opening a document scanner powered by Google Play services.
 */
public final class GmsDocumentScanning {
    /**
     * Creates a new instance of {@link GmsDocumentScanner} with the specified options.
     */
    @NonNull
    public static GmsDocumentScanner getClient(@NonNull GmsDocumentScannerOptions options) {
        return new GmsDocumentScannerImpl(options);
    }

    private GmsDocumentScanning() {
    }
}
