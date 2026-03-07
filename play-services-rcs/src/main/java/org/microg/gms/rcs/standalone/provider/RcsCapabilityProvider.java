/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs.standalone.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class RcsCapabilityProvider extends ContentProvider {
    private static final String TAG = "RcsCapabilityProvider";

    @Override
    public boolean onCreate() {
        Log.d(TAG, "STANDALONE RCS Capability Provider created");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "STANDALONE RCS Capability Provider query: " + uri);
        return null;
    }

    @Override
    public String getType(Uri uri) {
        Log.d(TAG, "STANDALONE RCS Capability Provider getType: " + uri);
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "STANDALONE RCS Capability Provider insert: " + uri);
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "STANDALONE RCS Capability Provider delete: " + uri);
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.d(TAG, "STANDALONE RCS Capability Provider update: " + uri);
        return 0;
    }
}
