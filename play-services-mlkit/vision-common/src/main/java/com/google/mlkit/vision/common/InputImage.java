/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.common;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

@PublicApi
public class InputImage {
    public static final int IMAGE_FORMAT_NV21 = 17;
    public static final int IMAGE_FORMAT_YUV_420_888 = 35;
    public static final int IMAGE_FORMAT_YV12 = 842094169;
    private static final long MAX_DECODE_PIXELS = 16_000_000L;
    private static final int MAX_REPLAYED_HEADER_BYTES = 4 * 1024 * 1024;
    private static final int MAX_CACHED_HEADER_BYTES = 128 * 1024 * 1024;

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
        try (InputStream stream = openStream(context, imageUri)) {
            if (stream instanceof FileInputStream && !(stream instanceof AssetFileDescriptor.AutoCloseInputStream)) {
                FileInputStream fileStream = (FileInputStream) stream;
                long start = streamPosition(fileStream);
                if (start >= 0L) {
                    InputImage image = decodeWithHeaderReplay(fileStream);
                    if (image != null) return image;
                    fileStream.getChannel().position(start);
                }
            }
            return decodeWithCachedHeader(context.getCacheDir(), stream);
        }
    }

    private static long streamPosition(FileInputStream stream) {
        try {
            return stream.getChannel().position();
        } catch (IOException e) {
            return -1L;
        }
    }

    private static InputImage decodeWithHeaderReplay(InputStream input) throws IOException {
        // Keep the checked header in memory so a provider cannot change it between reads.
        BufferedInputStream stream = new BufferedInputStream(input);
        stream.mark(MAX_REPLAYED_HEADER_BYTES);
        checkImageBounds(stream);
        if (!resetHeader(stream)) return null;
        ExifInterface exif = new ExifInterface(stream);
        if (!resetHeader(stream)) return null;
        return decodeBitmap(stream, exif);
    }

    private static boolean resetHeader(BufferedInputStream stream) {
        try {
            stream.reset();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static InputImage decodeWithCachedHeader(File cacheDir, InputStream input) throws IOException {
        // Replay only the bytes consumed by the bounds and EXIF readers.
        File header = File.createTempFile("mlkit-image-", ".tmp", cacheDir);
        try {
            ExifInterface exif;
            try (FileOutputStream output = new FileOutputStream(header)) {
                ImageHeaderStream recording = new ImageHeaderStream(input, output);
                checkImageBounds(recording);
                try (FileInputStream prefix = new FileInputStream(header)) {
                    FileChannel channel = prefix.getChannel();
                    exif = new ExifInterface(new SequenceInputStream(prefix, recording) {
                        @Override
                        public int available() throws IOException {
                            long available = super.available();
                            // EXIF uses availability for forward seeks, including past the cached prefix.
                            if (channel.isOpen()) available += recording.available();
                            return (int) Math.min(available, Integer.MAX_VALUE);
                        }
                    });
                }
                // EXIF may swallow an I/O failure while reading metadata.
                if (recording.failure != null) throw recording.failure;
            }
            try (FileInputStream prefix = new FileInputStream(header)) {
                return decodeBitmap(new SequenceInputStream(prefix, input), exif);
            }
        } finally {
            header.delete();
        }
    }

    private static final class ImageHeaderStream extends InputStream {
        private final InputStream input;
        private final FileOutputStream output;
        private final byte[] singleByte = new byte[1];
        private int written;
        private IOException failure;

        private ImageHeaderStream(InputStream input, FileOutputStream output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public int read() throws IOException {
            return read(singleByte, 0, 1) == -1 ? -1 : singleByte[0] & 0xff;
        }

        @Override
        public int read(byte[] data, int offset, int length) throws IOException {
            if (failure != null) throw failure;
            try {
                int count = input.read(data, offset, length);
                if (count > 0) {
                    if (count > MAX_CACHED_HEADER_BYTES - written) throw new IOException("Image header is too large");
                    output.write(data, offset, count);
                    written += count;
                }
                return count;
            } catch (IOException e) {
                failure = e;
                throw e;
            }
        }

        @Override
        public int available() throws IOException {
            return input.available();
        }
    }

    private static void checkImageBounds(InputStream stream) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, bounds);
        try {
            checkDecodeSize(bounds.outWidth, bounds.outHeight, MAX_DECODE_PIXELS);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static InputImage decodeBitmap(InputStream stream, ExifInterface exif) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeStream(stream);
        if (bitmap == null) throw new IOException("Unable to decode image");
        try {
            checkDecodeSize(bitmap.getWidth(), bitmap.getHeight(), MAX_DECODE_PIXELS);
            Matrix orientation = new Matrix();
            if (exif.isFlipped()) orientation.setScale(-1f, 1f);
            orientation.postRotate(exif.getRotationDegrees());
            if (!orientation.isIdentity()) {
                Bitmap upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), orientation, true);
                if (upright != bitmap) bitmap.recycle();
                bitmap = upright;
            }
            return fromBitmap(bitmap, 0);
        } catch (IllegalArgumentException e) {
            bitmap.recycle();
            throw new IOException(e.getMessage(), e);
        } catch (RuntimeException | Error e) {
            bitmap.recycle();
            throw e;
        }
    }

    @NonNull
    public static InputImage fromMediaImage(@NonNull Image image, int rotationDegrees) {
        return fromMediaImage(image, rotationDegrees, MAX_DECODE_PIXELS);
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
        checkDecodeSize(image.getWidth(), image.getHeight(), maxJpegPixels);
        Image.Plane[] planes = image.getPlanes();
        if (planes.length != 1) throw new IllegalArgumentException("JPEG images require one plane");
        ByteBuffer jpeg = planes[0].getBuffer().duplicate();
        byte[] encoded = new byte[jpeg.remaining()];
        jpeg.get(encoded);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(encoded, 0, encoded.length, bounds);
        checkDecodeSize(bounds.outWidth, bounds.outHeight, maxJpegPixels);
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

    private static void checkDecodeSize(int width, int height, long maxPixels) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Unable to decode image dimensions");
        if ((long) width * height > maxPixels) throw new IllegalArgumentException("Oversized image");
    }

    private static InputStream openStream(Context context, Uri uri) throws IOException {
        InputStream stream = context.getContentResolver().openInputStream(uri);
        if (stream == null) throw new IOException("Unable to open image");
        return stream;
    }
}
