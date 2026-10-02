/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast.framework.media.internal;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Downloads images (e.g. album art for the media notification and the expanded controller) for the client library.
 */
public class FetchBitmapTaskImpl extends IFetchBitmapTask.Stub {
    private static final String TAG = FetchBitmapTaskImpl.class.getSimpleName();
    // Bound for images requested without size hints, so a large image can not exhaust the app's heap.
    private static final int MAX_UNHINTED_DIMENSION = 2048;

    private final IFetchBitmapTaskProgressPublisher progressPublisher;
    private final int targetWidth;
    private final int targetHeight;
    private final long maxBytes;
    private final int maxRedirects;
    private final int connectTimeout;
    private final int readTimeout;

    public FetchBitmapTaskImpl(IFetchBitmapTaskProgressPublisher progressPublisher, int targetWidth, int targetHeight, long maxBytes, int maxRedirects, int connectTimeout, int readTimeout) {
        this.progressPublisher = progressPublisher;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.maxBytes = maxBytes > 0 ? maxBytes : 2 * 1024 * 1024;
        this.maxRedirects = Math.max(maxRedirects, 0);
        this.connectTimeout = connectTimeout > 0 ? connectTimeout : 10000;
        this.readTimeout = readTimeout > 0 ? readTimeout : 10000;
    }

    @Override
    public Bitmap fetchBitmap(Uri uri) throws RemoteException {
        if (uri == null) return null;
        try {
            byte[] data = download(uri);
            if (data == null) return null;
            return decode(data);
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            Log.w(TAG, "Failed to fetch " + uri + ": " + e.getMessage());
            return null;
        }
    }

    private byte[] download(Uri uri) throws IOException {
        URL url = new URL(uri.toString());
        for (int redirects = 0; ; redirects++) {
            String protocol = url.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                Log.w(TAG, "Unsupported protocol: " + protocol);
                return null;
            }
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setConnectTimeout(connectTimeout);
                connection.setReadTimeout(readTimeout);
                // Follow redirects manually, HttpURLConnection does not follow redirects across protocols.
                connection.setInstanceFollowRedirects(false);
                int code = connection.getResponseCode();
                if (code >= 300 && code < 400 && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || redirects >= maxRedirects) return null;
                    url = new URL(url, location);
                    continue;
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "Unexpected response " + code + " for " + url);
                    return null;
                }
                long total = connection.getContentLength();
                if (total > maxBytes) {
                    Log.w(TAG, "Image too large: " + total);
                    return null;
                }
                try (InputStream in = connection.getInputStream()) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    long read = 0;
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        read += count;
                        if (read > maxBytes) {
                            Log.w(TAG, "Image too large, aborting after " + read + " bytes");
                            return null;
                        }
                        out.write(buffer, 0, count);
                        publishProgress(read, total);
                    }
                    return out.toByteArray();
                }
            } finally {
                connection.disconnect();
            }
        }
    }

    private void publishProgress(long read, long total) {
        if (progressPublisher == null) return;
        try {
            progressPublisher.publishProgress(read, total);
        } catch (RemoteException e) {
            // Ignore, progress is informational only
        }
    }

    private Bitmap decode(byte[] data) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        int sampleSize = 1;
        if (targetWidth > 0 && targetHeight > 0) {
            while (options.outWidth / (sampleSize * 2) >= targetWidth && options.outHeight / (sampleSize * 2) >= targetHeight) {
                sampleSize *= 2;
            }
        } else {
            while (options.outWidth / sampleSize > MAX_UNHINTED_DIMENSION || options.outHeight / sampleSize > MAX_UNHINTED_DIMENSION) {
                sampleSize *= 2;
            }
        }
        options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        if (bitmap == null || targetWidth <= 0 || targetHeight <= 0) return bitmap;
        if (bitmap.getWidth() <= targetWidth && bitmap.getHeight() <= targetHeight) return bitmap;
        float scale = Math.min((float) targetWidth / bitmap.getWidth(), (float) targetHeight / bitmap.getHeight());
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(bitmap.getWidth() * scale)), Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
        if (scaled != bitmap) bitmap.recycle();
        return scaled;
    }
}
