/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.mlkit.vision.documentscanner;

import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_CALLBACKS;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_CAMERA_ID;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_DEFAULT_CAPTURE_MODE;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_ENABLE_ALL_NEW_FEATURES_BY_DEFAULT;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_ENABLE_AUTO_ENHANCEMENTS;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_ENABLE_COMPUTE_HASH_FOR_GALLERY_IMAGE;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_ENABLE_GALLERY_IMPORT_AUTO_TRANSFORM;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_FILTER_ALLOWED;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_FLASH_MODE_CHANGE_ALLOWED;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_GALLERY_IMPORT_ALLOWED;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_INITIAL_IMAGE_URIS;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_PAGE_EDIT_LISTENER_ENABLED;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_PAGE_LIMIT_MAX;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_REQUEST_URIS_IN_RESULT_INTENT;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_RESULT_FORMATS;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_SHADOW_REMOVAL_ALLOWED;
import static org.microg.mlkit.vision.documentscanner.GmsDocumentScannerConstants.EXTRA_STAIN_REMOVAL_ALLOWED;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;

import androidx.annotation.NonNull;

import com.google.android.gms.common.Feature;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.internal.GmsDocumentScanningDelegateActivity;

public class GmsDocumentScannerImpl implements GmsDocumentScanner {
    private static int PENDING_INTENT_COUNTER = 0;

    @NonNull
    private final GmsDocumentScannerOptions options;

    public GmsDocumentScannerImpl(@NonNull GmsDocumentScannerOptions options) {
        this.options = options;
    }

    @NonNull
    @Override
    public Task<IntentSender> getStartScanIntent(@NonNull Activity activity) {
        Intent intent = new Intent(activity, GmsDocumentScanningDelegateActivity.class)
                .putExtra(EXTRA_REQUEST_URIS_IN_RESULT_INTENT, true)
                .putParcelableArrayListExtra(EXTRA_INITIAL_IMAGE_URIS, null)
                .putExtra(EXTRA_DEFAULT_CAPTURE_MODE, options.getCaptureMode())
                .putExtra(EXTRA_FLASH_MODE_CHANGE_ALLOWED, true)
                .putExtra(EXTRA_GALLERY_IMPORT_ALLOWED, options.isGalleryImportAllowed())
                .putExtra(EXTRA_ENABLE_GALLERY_IMPORT_AUTO_TRANSFORM, true)
                .putExtra(EXTRA_PAGE_LIMIT_MAX, options.getPageLimit())
                .putExtra(EXTRA_PAGE_EDIT_LISTENER_ENABLED, false)
                .putExtra(EXTRA_RESULT_FORMATS, options.getResultFormats())
                .putExtra(EXTRA_ENABLE_ALL_NEW_FEATURES_BY_DEFAULT, options.isAllNewFeaturesEnabledByDefault())
                .putExtra(EXTRA_FILTER_ALLOWED, options.isFilterAllowed())
                .putExtra(EXTRA_SHADOW_REMOVAL_ALLOWED, options.isShadowRemovalAllowed())
                .putExtra(EXTRA_STAIN_REMOVAL_ALLOWED, options.isStainRemovalAllowed())
                .putExtra(EXTRA_ENABLE_COMPUTE_HASH_FOR_GALLERY_IMAGE, false)
                .putExtra(EXTRA_ENABLE_AUTO_ENHANCEMENTS, options.isAutoEnhancementsEnabled())
                .putExtra(EXTRA_CAMERA_ID, options.getCameraId());
        PendingIntent pendingIntent = PendingIntent.getActivity(activity, PENDING_INTENT_COUNTER++, intent, PendingIntent.FLAG_IMMUTABLE);
        return Tasks.forResult(pendingIntent.getIntentSender());
    }

    @NonNull
    @Override
    public Feature[] getOptionalFeatures() {
        return new Feature[0];
    }
}
