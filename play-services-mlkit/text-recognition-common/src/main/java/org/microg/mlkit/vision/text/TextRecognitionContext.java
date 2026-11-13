/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.mlkit.vision.text;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public final class TextRecognitionContext extends ContentProvider {
    private static volatile Context applicationContext;

    static Context getApplicationContext() {
        Context context = applicationContext;
        if (context == null) throw new IllegalStateException("ML Kit text initialization provider is unavailable");
        return context;
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) return false;
        applicationContext = context.getApplicationContext();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Override
    public String getType(Uri uri) { return null; }
    @Override
    public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
