/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.collection.LruCache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is used to load images from the network and handles local caching for you.
 */
public class ImageManager {
    /**
     * Returns a new ImageManager for loading images from the network.
     *
     * @param context The context used by the ImageManager.
     * @return A new ImageManager.
     */
    public static ImageManager create(Context context) {
        if (INSTANCE == null) {
            synchronized (ImageManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ImageManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public static final String TAG = "ImageManager";
    private static volatile ImageManager INSTANCE;
    private final LruCache<String, Bitmap> memoryCache;
    private final ExecutorService executorService;
    private final Handler handler;
    private final Context context;

    private ImageManager(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newFixedThreadPool(4);

        final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
        this.memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    /**
     * Compress Bitmap
     */
    public byte[] compressBitmap(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        Log.d(TAG, "compressBitmap width: " + bitmap.getWidth() + " height:" + bitmap.getHeight());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(format, quality, byteArrayOutputStream);
        byte[] bitmapBytes = byteArrayOutputStream.toByteArray();
        bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        Log.d(TAG, "compressBitmap compress width: " + bitmap.getWidth() + " height:" + bitmap.getHeight());
        return bitmapBytes;
    }

    public byte[] compressBitmap(Bitmap original, int newWidth, int newHeight) {
        Log.d(TAG, "compressBitmap width: " + original.getWidth() + " height:" + original.getHeight());
        Bitmap target = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
        Log.d(TAG, "compressBitmap target width: " + target.getWidth() + " height:" + target.getHeight());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        target.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public void loadImage(final String url, final ImageView imageView) {
        if (imageView == null) {
            Log.d(TAG, "loadImage: imageView is null");
            return;
        }
        final Bitmap cachedBitmap = getBitmapFromCache(url);
        if (cachedBitmap != null) {
            Log.d(TAG, "loadImage from cached");
            imageView.setImageBitmap(cachedBitmap);
        } else {
            Log.d(TAG, "loadImage from net");
            imageView.setTag(url);
            executorService.submit(() -> {
                final Bitmap bitmap = downloadBitmap(url);
                if (bitmap != null) {
                    addBitmapToCache(url, bitmap);
                    if (imageView.getTag().equals(url)) {
                        handler.post(() -> imageView.setImageBitmap(bitmap));
                    }
                }
            });
        }
    }

    private Bitmap getBitmapFromCache(String key) {
        Bitmap bitmap = memoryCache.get(key);
        if (bitmap == null) {
            bitmap = getBitmapFromDiskCache(key);
        }
        return bitmap;
    }

    private void addBitmapToCache(String key, Bitmap bitmap) {
        if (getBitmapFromCache(key) == null) {
            memoryCache.put(key, bitmap);
            addBitmapToDiskCache(key, bitmap);
        }
    }

    private Bitmap getBitmapFromDiskCache(String key) {
        File file = getDiskCacheFile(key);
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return null;
    }

    private void addBitmapToDiskCache(String key, Bitmap bitmap) {
        File file = getDiskCacheFile(key);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException e) {
            Log.e(TAG, "addBitmapToDiskCache: ", e);
        }
    }

    private File getDiskCacheFile(String key) {
        File cacheDir = context.getCacheDir();
        return new File(cacheDir, md5(key));
    }

    private String md5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                StringBuilder h = new StringBuilder(Integer.toHexString(0xFF & b));
                while (h.length() < 2) h.insert(0, "0");
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "md5: ", e);
        }
        return "";
    }

    @WorkerThread
    private Bitmap downloadBitmap(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (IOException e) {
            Log.d(TAG, "downloadBitmap: ", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }


}
