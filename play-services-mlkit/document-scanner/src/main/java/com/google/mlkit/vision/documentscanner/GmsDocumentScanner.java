/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.mlkit.vision.documentscanner;

import android.app.Activity;
import android.content.IntentSender;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.tasks.Task;

/**
 * A client to open a document scanner powered by Google Play services.
 */
public interface GmsDocumentScanner extends OptionalModuleApi {
    /**
     * Returns an {@link IntentSender} for starting the document scanner UI flow.
     */
    @NonNull
    Task<IntentSender> getStartScanIntent(@NonNull Activity activity);
}
