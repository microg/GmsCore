/*
 * Copyright (C) 2018 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.phenotype;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ConfigurationProvider extends ContentProvider {
    private static final String TAG = "GmsPhenotypeCfgProvider";

    // RCS-related flags to allow Google Messages RCS provisioning over microG
    private static final String FLAG_ALLOW_MANUAL_MSISDN = "RcsFlags__allow_manual_phone_number_input";
    private static final String FLAG_UPI_NO_ACS_FALLBACK = "RcsProvisioning__min_gmscore_version_for_upi_without_acs_fallback_met";
    private static final String FLAG_ENABLE_UPI = "RcsProvisioning__enable_upi";
    private static final String FLAG_ENABLE_UPI_MVP = "RcsProvisioning__enable_upi_mvp";
    private static final String FLAG_ACS_URL = "RcsFlags__acs_url";
    private static final String FLAG_MCC_URL_FORMAT = "RcsFlags__mcc_url_format";
    private static final String JIBE_MCC_URL_FORMAT = "rcs-acs-mcc%s.jibe.google.com";
    private static final String FLAG_ALLOW_OVERRIDES = "RcsFlags__allow_overrides";
    private static final String FLAG_TRUE = "true";
    private static final String FLAG_FALSE = "false";

    @Override
    public boolean onCreate() {
        Log.d(TAG, "ConfigurationProvider created");
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        String packageName = Uri.decode(uri.getLastPathSegment());
        if (packageName == null) return null;

        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});

        // Serve RCS flags for Google Messages and IMS library so RCS can be enabled
        if (packageName.startsWith("com.google.android.ims.library") ||
            packageName.startsWith("com.google.android.apps.messaging")) {
            cursor.addRow(new Object[]{FLAG_ALLOW_MANUAL_MSISDN, FLAG_TRUE});
            cursor.addRow(new Object[]{FLAG_UPI_NO_ACS_FALLBACK, FLAG_TRUE});
            cursor.addRow(new Object[]{FLAG_ENABLE_UPI, FLAG_TRUE});
            cursor.addRow(new Object[]{FLAG_ENABLE_UPI_MVP, FLAG_TRUE});
            cursor.addRow(new Object[]{FLAG_ACS_URL, ""});
            cursor.addRow(new Object[]{FLAG_MCC_URL_FORMAT, JIBE_MCC_URL_FORMAT});
            cursor.addRow(new Object[]{"RcsProvisioning__enable_client_attestation_check", FLAG_FALSE});
            cursor.addRow(new Object[]{"RcsProvisioning__enable_client_attestation_check_v2", FLAG_FALSE});
            cursor.addRow(new Object[]{FLAG_ALLOW_OVERRIDES, FLAG_TRUE});
            Log.d(TAG, "Serving RCS phenotype flags for " + packageName);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        throw new UnsupportedOperationException();
    }
}
