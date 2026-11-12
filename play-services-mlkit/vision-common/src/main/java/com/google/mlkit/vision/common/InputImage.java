/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.net.Uri;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.Objects;

@PublicApi
public class InputImage {
    public static final int IMAGE_FORMAT_NV21 = 17;
    public static final int IMAGE_FORMAT_YUV_420_888 = 35;
    public static final int IMAGE_FORMAT_YV12 = 842094169;

    @IntDef({IMAGE_FORMAT_NV21, IMAGE_FORMAT_YUV_420_888, IMAGE_FORMAT_YV12})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImageFormat {
    }

    private final Object source;
    private final int format;
    private final int width;
    private final int height;
    private final int rotationDegrees;

    private InputImage(Object source, int format, int width, int height, int rotationDegrees) {
        this.source = source;
        this.format = format;
        this.width = width;
        this.height = height;
        this.rotationDegrees = rotationDegrees;
    }

    @NonNull
    public static InputImage fromBitmap(@NonNull Bitmap bitmap, int rotationDegrees) {
        Objects.requireNonNull(bitmap, "Bitmap must not be null");
        checkRotation(rotationDegrees);
        if (bitmap.isRecycled()) throw new IllegalArgumentException("Bitmap has been recycled");
        return new InputImage(bitmap, -1, bitmap.getWidth(), bitmap.getHeight(), rotationDegrees);
    }

    @NonNull
    public static InputImage fromByteArray(@NonNull byte[] byteArray, int width, int height, int rotationDegrees, @ImageFormat int format) {
        Objects.requireNonNull(byteArray, "Image data must not be null");
        return fromByteBuffer(ByteBuffer.wrap(byteArray), width, height, rotationDegrees, format);
    }

    @NonNull
    public static InputImage fromByteBuffer(@NonNull ByteBuffer byteBuffer, int width, int height, int rotationDegrees, @ImageFormat int format) {
        Objects.requireNonNull(byteBuffer, "Image buffer must not be null");
        checkRotation(rotationDegrees);
        if (width <= 0 || height <= 0 || width % 2 != 0 || height % 2 != 0) {
            throw new IllegalArgumentException("Packed YUV requires positive, even image dimensions");
        }
        if (format != IMAGE_FORMAT_NV21 && format != IMAGE_FORMAT_YV12) {
            throw new IllegalArgumentException("ByteBuffer images require NV21 or YV12 format");
        }
        long pixels = (long) width * height;
        long requiredBytes = pixels + pixels / 2;
        // A YV12 buffer is tightly packed at this exact length. Camera preview rows can include padding.
        if (format == IMAGE_FORMAT_YV12 && byteBuffer.limit() != requiredBytes) {
            long yStride = (width + 15L) / 16 * 16;
            long chromaStride = (yStride / 2 + 15) / 16 * 16;
            requiredBytes = yStride * height + chromaStride * height;
        }
        if (byteBuffer.limit() < requiredBytes) {
            throw new IllegalArgumentException("Image buffer is truncated");
        }
        ByteBuffer imageBuffer = byteBuffer.duplicate();
        imageBuffer.rewind();
        return new InputImage(imageBuffer, format, width, height, rotationDegrees);
    }

    @NonNull
    public static InputImage fromFilePath(@NonNull Context context, @NonNull Uri imageUri) throws IOException {
        Objects.requireNonNull(context, "Context must not be null");
        Objects.requireNonNull(imageUri, "Image Uri must not be null");
        ExifInterface exif;
        try (InputStream stream = openStream(context, imageUri)) {
            exif = new ExifInterface(stream);
        }
        Bitmap bitmap;
        try (InputStream stream = openStream(context, imageUri)) {
            bitmap = BitmapFactory.decodeStream(stream);
        }
        if (bitmap == null) throw new IOException("Unable to decode image");
        try {
            Matrix orientation = new Matrix();
            if (exif.isFlipped()) orientation.setScale(-1f, 1f);
            orientation.postRotate(exif.getRotationDegrees());
            if (!orientation.isIdentity()) {
                Bitmap upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), orientation, true);
                if (upright != bitmap) bitmap.recycle();
                bitmap = upright;
            }
            return fromBitmap(bitmap, 0);
        } catch (RuntimeException | Error e) {
            bitmap.recycle();
            throw e;
        }
    }

    @NonNull
    public static InputImage fromMediaImage(@NonNull Image image, int rotationDegrees) {
        return fromMediaImage(image, rotationDegrees, Long.MAX_VALUE);
    }

    @Hide
    @NonNull
    public static InputImage fromMediaImage(@NonNull Image image, int rotationDegrees, long maxJpegPixels) {
        Objects.requireNonNull(image, "Image must not be null");
        checkRotation(rotationDegrees);
        if (maxJpegPixels <= 0) throw new IllegalArgumentException("JPEG pixel limit must be positive");
        int format = image.getFormat();
        if (format == IMAGE_FORMAT_YUV_420_888) {
            return new InputImage(image, IMAGE_FORMAT_YUV_420_888, image.getWidth(), image.getHeight(), rotationDegrees);
        }
        if (format != android.graphics.ImageFormat.JPEG) {
            throw new IllegalArgumentException("Media images require JPEG or YUV_420_888 format");
        }
        Image.Plane[] planes = image.getPlanes();
        if (planes.length != 1) throw new IllegalArgumentException("JPEG images require one plane");
        ByteBuffer jpeg = planes[0].getBuffer().duplicate();
        byte[] encoded = new byte[jpeg.remaining()];
        jpeg.get(encoded);
        if (maxJpegPixels != Long.MAX_VALUE) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(encoded, 0, encoded.length, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw new IllegalArgumentException("Unable to decode JPEG image");
            }
            if ((long) bounds.outWidth * bounds.outHeight > maxJpegPixels) {
                throw new IllegalArgumentException("Oversized JPEG image");
            }
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.length);
        if (bitmap == null) throw new IllegalArgumentException("Unable to decode JPEG image");
        try {
            return fromBitmap(bitmap, rotationDegrees);
        } catch (RuntimeException | Error e) {
            bitmap.recycle();
            throw e;
        }
    }

    @Hide
    @NonNull
    public Object getSource() {
        return source instanceof ByteBuffer ? getByteBuffer() : source;
    }

    @Hide
    @Nullable
    public Bitmap getBitmapInternal() {
        return source instanceof Bitmap ? (Bitmap) source : null;
    }

    @Hide
    @Nullable
    public ByteBuffer getByteBuffer() {
        return source instanceof ByteBuffer ? ((ByteBuffer) source).duplicate() : null;
    }

    @Hide
    @Nullable
    public Image getMediaImage() {
        return source instanceof Image ? (Image) source : null;
    }

    @Hide
    public int getFormat() {
        return format;
    }

    @Hide
    public int getWidth() {
        return width;
    }

    @Hide
    public int getHeight() {
        return height;
    }

    @Hide
    public int getRotationDegrees() {
        return rotationDegrees;
    }

    private static void checkRotation(int rotationDegrees) {
        if (rotationDegrees != 0 && rotationDegrees != 90 && rotationDegrees != 180 && rotationDegrees != 270) {
            throw new IllegalArgumentException("Rotation must be 0, 90, 180 or 270 degrees");
        }
    }

    private static InputStream openStream(Context context, Uri uri) throws IOException {
        InputStream stream = context.getContentResolver().openInputStream(uri);
        if (stream == null) throw new IOException("Unable to open image");
        return stream;
    }
}
