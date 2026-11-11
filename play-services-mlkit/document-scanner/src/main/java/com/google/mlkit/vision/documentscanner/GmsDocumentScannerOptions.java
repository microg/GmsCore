/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.mlkit.vision.documentscanner;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import org.microg.gms.common.Hide;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

/**
 * Options for {@link GmsDocumentScanner}.
 */
public class GmsDocumentScannerOptions {
    /**
     * {@link GmsDocumentScannerOptions.ScannerMode} with the full feature set, for example, document image cleaning.
     * <p>
     * In the future, major new features delivered along with Google Play services updates will be automatically enabled.
     */
    public static final int SCANNER_MODE_FULL = 1;
    /**
     * {@link GmsDocumentScannerOptions.ScannerMode} that only enables the basic features (see {@link #SCANNER_MODE_BASE}) plus filters.
     * <p>
     * In the future, major new features delivered along with Google Play services updates will be disabled by default.
     */
    public static final int SCANNER_MODE_BASE_WITH_FILTER = 2;
    /**
     * {@link GmsDocumentScannerOptions.ScannerMode} that only enables the basic features, for example, scanning with camera,
     * document auto rotation, crop and rotation, adding or removing or reordering pages, saving as JPEGs or PDF.
     * <p>
     * In the future, major new features delivered along with Google Play services updates will be disabled by default.
     */
    public static final int SCANNER_MODE_BASE = 3;
    /**
     * {@link GmsDocumentScannerOptions.CaptureMode} for automatically triggering capture in scanner camera viewfinder.
     */
    public static final int CAPTURE_MODE_AUTO = 1;
    /**
     * {@link GmsDocumentScannerOptions.CaptureMode} for manually triggering capture in scanner camera viewfinder.
     */
    public static final int CAPTURE_MODE_MANUAL = 2;
    public static final int RESULT_FORMAT_JPEG = 101;
    public static final int RESULT_FORMAT_PDF = 102;

    /**
     * The default {@link GmsDocumentScannerOptions}.
     * <p>
     * This is equivalent to {@code new GmsDocumentScannerOptions.Builder().build()}.
     */
    @NonNull
    public static final GmsDocumentScannerOptions DEFAULT_OPTIONS = new Builder().build();

    private final String cameraId;
    @CaptureMode
    private final int captureMode;
    private final boolean galleryImportAllowed;
    private final int pageLimit;
    @ResultFormat
    private final int[] resultFormats;
    private final boolean enableAllNewFeaturesByDefault;
    private final boolean filterAllowed;
    private final boolean shadowRemovalAllowed;
    private final boolean stainRemovalAllowed;
    private final boolean enableAutoEnhancements;

    public GmsDocumentScannerOptions(String cameraId, @CaptureMode int captureMode, boolean galleryImportAllowed, int pageLimit, @ResultFormat int[] resultFormats, boolean enableAllNewFeaturesByDefault, boolean filterAllowed, boolean shadowRemovalAllowed, boolean stainRemovalAllowed, boolean enableAutoEnhancements) {
        this.cameraId = cameraId;
        this.captureMode = captureMode;
        this.galleryImportAllowed = galleryImportAllowed;
        this.pageLimit = pageLimit;
        this.resultFormats = resultFormats;
        this.enableAllNewFeaturesByDefault = enableAllNewFeaturesByDefault;
        this.filterAllowed = filterAllowed;
        this.shadowRemovalAllowed = shadowRemovalAllowed;
        this.stainRemovalAllowed = stainRemovalAllowed;
        this.enableAutoEnhancements = enableAutoEnhancements;
    }

    @Hide
    public String getCameraId() {
        return cameraId;
    }

    @Hide
    @CaptureMode
    public int getCaptureMode() {
        return captureMode;
    }

    @Hide
    public int getPageLimit() {
        return pageLimit;
    }

    @Hide
    @ResultFormat
    public int[] getResultFormats() {
        return resultFormats;
    }

    @Hide
    public boolean isAllNewFeaturesEnabledByDefault() {
        return enableAllNewFeaturesByDefault;
    }

    @Hide
    public boolean isAutoEnhancementsEnabled() {
        return enableAutoEnhancements;
    }

    @Hide
    public boolean isFilterAllowed() {
        return filterAllowed;
    }

    @Hide
    public boolean isShadowRemovalAllowed() {
        return shadowRemovalAllowed;
    }

    @Hide
    public boolean isStainRemovalAllowed() {
        return stainRemovalAllowed;
    }

    @Hide
    public boolean isGalleryImportAllowed() {
        return galleryImportAllowed;
    }

    /**
     * Builder for {@link GmsDocumentScannerOptions}.
     */
    public static class Builder {

        private boolean galleryImportAllowed = true;
        private int pageLimit = -1;
        private int scannerMode = SCANNER_MODE_FULL;
        @ResultFormat
        private int[] resultFormats = {GmsDocumentScannerOptions.RESULT_FORMAT_JPEG};

        /**
         * Sets whether to show a UI button to allow the user to import images from photo gallery.
         * <p>
         * The default value is {@code true}.
         */
        @NonNull
        public Builder setGalleryImportAllowed(boolean galleryImportAllowed) {
            this.galleryImportAllowed = galleryImportAllowed;
            return this;
        }

        /**
         * Sets a page limit for the maximum number of pages that can be scanned in a single scanning session.
         * <p>
         * The provided value should be be greater than or equal to 1.
         * <p>
         * If no page limit is set, the restriction on the number of pages that can be scanned is determined by hardware resources, for
         * example, memory, disk storage.
         */
        @NonNull
        public Builder setPageLimit(int pageLimit) {
            this.pageLimit = pageLimit;
            return this;
        }

        /**
         * Sets scanner {@link GmsDocumentScannerOptions.ResultFormat}s.
         * <p>
         * Available formats: {@link GmsDocumentScannerOptions#RESULT_FORMAT_JPEG}, {@link GmsDocumentScannerOptions#RESULT_FORMAT_PDF}.
         * <p>
         * The default value is {@link GmsDocumentScannerOptions#RESULT_FORMAT_JPEG}.
         */
        @NonNull
        public Builder setResultFormats(@ResultFormat int format, @NonNull @ResultFormat int... moreFormats) {
            int length = moreFormats.length;
            this.resultFormats = Arrays.copyOf(moreFormats, length + 1);
            this.resultFormats[length] = format;
            return this;
        }

        /**
         * Sets the scanner mode which determines what features are enabled.
         * <p>
         * Available modes: {@link GmsDocumentScannerOptions#SCANNER_MODE_FULL}, {@link GmsDocumentScannerOptions#SCANNER_MODE_BASE_WITH_FILTER}, {@link GmsDocumentScannerOptions#SCANNER_MODE_BASE}.
         * <p>
         * The default value is {@link GmsDocumentScannerOptions#SCANNER_MODE_FULL}.
         */
        @NonNull
        public Builder setScannerMode(@ScannerMode int scannerMode) {
            this.scannerMode = scannerMode;
            return this;
        }

        /**
         * Builds a {@link GmsDocumentScannerOptions} instance.
         */
        public GmsDocumentScannerOptions build() {
            return new GmsDocumentScannerOptions(
                    "",
                    CAPTURE_MODE_AUTO,
                    galleryImportAllowed,
                    pageLimit,
                    resultFormats,
                    scannerMode == SCANNER_MODE_FULL,
                    scannerMode != SCANNER_MODE_BASE,
                    scannerMode == SCANNER_MODE_FULL,
                    scannerMode == SCANNER_MODE_FULL,
                    scannerMode == SCANNER_MODE_FULL
            );
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CAPTURE_MODE_AUTO, CAPTURE_MODE_MANUAL})
    public @interface CaptureMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF})
    public @interface ResultFormat {
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SCANNER_MODE_FULL, SCANNER_MODE_BASE_WITH_FILTER, SCANNER_MODE_BASE})
    public @interface ScannerMode {
    }
}
