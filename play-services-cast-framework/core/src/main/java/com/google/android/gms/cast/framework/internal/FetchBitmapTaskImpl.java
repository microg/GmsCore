/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast.framework.internal;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.cast.framework.media.internal.IFetchBitmapTask;
import com.google.android.gms.cast.framework.media.internal.IFetchBitmapTaskProgressPublisher;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Downloads images (e.g. album art shown in the mini controller and media notification) on behalf of the client
 * library. Invoked on a background thread of the client.
 */
public class FetchBitmapTaskImpl extends IFetchBitmapTask.Stub {
    private static final String TAG = "GmsCastFetchBitmap";
    private static final long DEFAULT_MAX_BYTES = 2 * 1024 * 1024;
    private static final int DEFAULT_TIMEOUT_MILLIS = 10000;

    private final IFetchBitmapTaskProgressPublisher progressPublisher;
    private final int preferredWidth;
    private final int preferredHeight;
    private final boolean allowLargerThanPreferred;
    private final long maxBytes;
    private final int progressIntervalMillis;
    private final int timeoutMillis;

    public FetchBitmapTaskImpl(IFetchBitmapTaskProgressPublisher progressPublisher, int preferredWidth, int preferredHeight, boolean allowLargerThanPreferred, long maxBytes, int progressIntervalMillis, int timeoutMillis) {
        this.progressPublisher = progressPublisher;
        this.preferredWidth = preferredWidth;
        this.preferredHeight = preferredHeight;
        this.allowLargerThanPreferred = allowLargerThanPreferred;
        this.maxBytes = maxBytes > 0 ? maxBytes : DEFAULT_MAX_BYTES;
        this.progressIntervalMillis = progressIntervalMillis;
        this.timeoutMillis = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MILLIS;
    }

    @Override
    public Bitmap doFetch(Uri uri) {
        if (uri == null) return null;
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            Log.d(TAG, "Not fetching unsupported uri: " + uri);
            return null;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            connection.setInstanceFollowRedirects(true);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Unexpected response " + connection.getResponseCode() + " for " + uri);
                return null;
            }
            long total = connection.getContentLength();
            if (total > maxBytes) {
                Log.d(TAG, "Image too large: " + total + " bytes");
                return null;
            }
            byte[] data = readFully(connection.getInputStream(), total);
            if (data == null) return null;
            return scale(BitmapFactory.decodeByteArray(data, 0, data.length));
        } catch (Exception e) {
            Log.w(TAG, "Failed to fetch " + uri, e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private byte[] readFully(InputStream inputStream, long total) throws Exception {
        try {
            // The size the server claims is only a hint, don't reserve whatever it says
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) Math.max(16 * 1024, Math.min(total, 1024 * 1024)));
            byte[] buffer = new byte[8 * 1024];
            long lastProgress = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                if (outputStream.size() > maxBytes) {
                    Log.d(TAG, "Image too large, aborting");
                    return null;
                }
                long now = SystemClock.elapsedRealtime();
                if (progressPublisher != null && progressIntervalMillis > 0 && now - lastProgress >= progressIntervalMillis) {
                    lastProgress = now;
                    try {
                        progressPublisher.publishProgress(outputStream.size(), total);
                    } catch (Exception e) {
                        // Progress is informational only
                    }
                }
            }
            return outputStream.toByteArray();
        } finally {
            inputStream.close();
        }
    }

    private Bitmap scale(Bitmap bitmap) {
        if (bitmap == null || allowLargerThanPreferred || preferredWidth <= 0 || preferredHeight <= 0) return bitmap;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= preferredWidth && height <= preferredHeight) return bitmap;
        float factor = Math.min((float) preferredWidth / width, (float) preferredHeight / height);
        int scaledWidth = Math.max(1, Math.round(width * factor));
        int scaledHeight = Math.max(1, Math.round(height * factor));
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
    }
}
