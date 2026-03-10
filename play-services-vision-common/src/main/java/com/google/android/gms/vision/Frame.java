/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.vision;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.view.Display;

import com.google.android.gms.vision.internal.FrameMetadataParcel;

import org.microg.gms.common.PublicApi;

import java.nio.ByteBuffer;

/**
 * Image data with associated {@link Metadata}.
 * <p>
 * A frame is constructed via the {@link Builder} class, specifying the image data, dimensions, and sequencing information (frame ID, timestamp).
 */
public class Frame {
    public static final int ROTATION_0 = 0;
    public static final int ROTATION_90 = 1;
    public static final int ROTATION_180 = 2;
    public static final int ROTATION_270 = 3;

    private Bitmap bitmap;
    private ByteBuffer imageData;
    private final Metadata metadata = new Metadata();

    /**
     * Returns the bitmap which was specified in creating this frame, or null if no bitmap was used to create this frame. If the bitmap is not available, then {@link #getGrayscaleImageData()} should be called instead.
     */
    public Bitmap getBitmap() {
        return bitmap;
    }

    /**
     * Returns the grayscale version of the frame data, with one byte per pixel. Note that the returned byte buffer will be prefixed by the Y channel (i.e., the grayscale image data), but may optionally include additional image data beyond the Y channel (this can be ignored).
     * <p>
     * If a bitmap was specified when creating this frame, the bitmap is first converted to a grayscale byte[] (allocation / copy required). It is recommended that you use the bitmap directly through {@link #getBitmap()} if the associated native detection code supports it, since this would move the grayscale conversion into native code where it will be faster.
     */
    public ByteBuffer getGrayscaleImageData() {
        if (bitmap == null) {
            return imageData;
        }
        int width = metadata.width;
        int height = metadata.height;
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        byte[] values = new byte[width * height];

        for (int i = 0; i < pixels.length; i++) {
            values[i] = (byte)((int)((float) Color.red(pixels[i]) * 0.299F + (float)Color.green(pixels[i]) * 0.587F + (float)Color.blue(pixels[i]) * 0.114F));
        }

        return ByteBuffer.wrap(values);
    }

    /**
     * Returns the metadata associated with the frame.
     */
    public Frame.Metadata getMetadata() {
        return metadata;
    }

    /**
     * Builder for creating a frame instance.
     * At a minimum, image information must be specified either through {@link #setBitmap(Bitmap)} or {@link #setImageData(ByteBuffer, int, int, int)}.
     */
    public static class Builder {
        private final Frame frame = new Frame();

        /**
         * Creates the frame instance.
         *
         * @throws IllegalStateException if the image data has not been set via {@link #setBitmap(Bitmap)} or {@link #setImageData(ByteBuffer, int, int, int)}.
         */
        public Frame build() {
            if (this.frame.bitmap == null && this.frame.imageData == null) {
                throw new IllegalStateException("Missing image data.  Call either setBitmap or setImageData to specify the image");
            }
            return frame;
        }

        /**
         * Sets the image data for the frame from a supplied bitmap.
         * <p>
         * While this is a convenient way to specify certain images (e.g., images read from a file), note that a copy is required to extract pixel information for use in detectors -- this could mean extra GC overhead. Using {@link #setImageData(ByteBuffer, int, int, int)} is the preferred way to specify image data if you can handle the data in a supported byte format and reuse the byte buffer, since it does not require a making a copy.
         */
        public Builder setBitmap(Bitmap bitmap) {
            this.frame.bitmap = bitmap;
            this.frame.metadata.width = bitmap.getWidth();
            this.frame.metadata.height = bitmap.getHeight();
            return this;
        }

        /**
         * Sets the frame ID. A frame source such as a live video camera or a video player is expected to assign IDs in monotonically increasing order, to indicate the sequence that the frame appeared relative to other frames.
         * <p>
         * A {@link Detector.Processor} implementation may rely upon this sequence ID to detect frame sequence gaps, to compute velocity, etc.
         */
        public Builder setId(int id) {
            this.frame.metadata.id = id;
            return this;
        }

        /**
         * Sets the image data from the supplied byte buffer, size, and format.
         *
         * @param data   contains image byte data according to the associated format.
         * @param width
         * @param height
         * @param format one of {@link ImageFormat#NV16}, {@link ImageFormat#NV21}, or {@link ImageFormat#YV12}.
         * @throws IllegalArgumentException if the supplied data is null, or an invalid image format was supplied.
         */
        public Builder setImageData(ByteBuffer data, int width, int height, int format) {
            if (data == null) throw new IllegalArgumentException("Null image data supplied");
            if (data.capacity() < width * height) throw new IllegalArgumentException("Invalid image data size");
            if (format != ImageFormat.NV16 && format != ImageFormat.NV21 && format != ImageFormat.YV12)
                throw new IllegalArgumentException("Unsupported image format: " + format);
            this.frame.imageData = data;
            this.frame.metadata.width = width;
            this.frame.metadata.height = height;
            this.frame.metadata.format = format;
            return this;
        }

        /**
         * Sets the image rotation, indicating the rotation from the upright orientation.
         * <p>
         * Since the camera may deliver images that are rotated (e.g., if the user holds the device upside down), specifying the rotation with the image indicates how to make the image be upright, if necessary. Some detectors may rely upon rotating the image before attempting detection, whereas others may not. In preserving the original image from the camera along with this value, the detector may choose whether to make this correction (and to assume the associated cost).
         * <p>
         * However, note that the detector is expected to report detection position coordinates that are relative to the upright version of the image (whether or not the image was actually rotated by the detector). The {@link Detector} will always deliver frame metadata to the {@link Detector.Processor} that indicates the dimensions and orientation of an unrotated, upright frame.
         *
         * @param rotation one of {@link Frame#ROTATION_0}, {@link Frame#ROTATION_90}, {@link Frame#ROTATION_180}, {@link Frame#ROTATION_270}. Has the same meaning as {@link Display#getRotation()}.
         */
        public Builder setRotation(int rotation) {
            this.frame.metadata.rotation = rotation;
            return this;
        }

        /**
         * Sets the frame timestamp, in milliseconds. A frame source such as a live video camera or a video player is expected to assign timestamps in a way that makes sense for the medium. For example, live video may use the capture time of each frame, whereas a video player may use the elapsed time to the frame within the video. Timestamps should be in monotonically increasing order, to indicate the passage of time.
         * <p>
         * A {@link Detector.Processor} implementation may rely upon this sequence ID to detect frame sequence gaps, to compute velocity, etc.
         */
        public Builder setTimestampMillis(long timestampMillis) {
            this.frame.metadata.timestampMillis = timestampMillis;
            return this;
        }
    }

    /**
     * Frame metadata, describing the image dimensions, rotation, and sequencing information.
     */
    public static class Metadata {
        private int format = -1;
        private int height;
        private int id;
        private int rotation;
        private long timestampMillis;
        private int width;

        public Metadata() {
        }

        private Metadata(Metadata metadata) {
            this.format = metadata.format;
            this.height = metadata.height;
            this.id = metadata.id;
            this.rotation = metadata.rotation;
            this.timestampMillis = metadata.timestampMillis;
            this.width = metadata.width;
        }

        /**
         * Returns the format of this image if image data is set.
         *
         * @return one of {@link ImageFormat#NV16}, {@link ImageFormat#NV21, {@link ImageFormat#YV12} or {@link ImageFormat#YUV_420_888}.
         */
        public int getFormat() {
            return format;
        }

        /**
         * Returns the frame height.
         */
        public int getHeight() {
            return height;
        }

        /**
         * Returns the frame ID. A frame source such as a live video camera or a video player is expected to assign IDs in monotonically increasing order, to indicate the sequence that the frame appeared relative to other frames.
         */
        public int getId() {
            return id;
        }

        /**
         * Returns the image rotation, indicating the counter-clockwise rotation from the upright orientation. Has the same meaning as in {@link Display#getRotation()}.
         * <p>
         * Since the camera may deliver images that are rotated (e.g., if the user holds the device upside down), specifying the rotation with the image indicates how to make the image be upright, if necessary. Some detectors may rely upon rotating the image before attempting detection, whereas others may not. In preserving the original image from the camera along with this value, the detector may choose whether to make this correction (and to assume the associated cost).
         * <p>
         * However, note that the detector is expected to report detection position coordinates that are relative to the upright version of the image (whether or not the image was actually rotated by the detector). The {@link Detector} will always deliver frame metadata to the {@link Detector.Processor} that indicates the dimensions and orientation of an unrotated, upright frame.
         *
         * @return one of {@link Frame#ROTATION_0}, {@link Frame#ROTATION_90}, {@link Frame#ROTATION_180}, {@link Frame#ROTATION_270}.
         */
        public int getRotation() {
            return rotation;
        }

        /**
         * Returns the timestamp, in milliseconds.
         * <p>
         * A frame source such as a live video camera or a video player is expected to assign timestamps in a way that makes sense for the medium. For example, live video may use the capture time of each frame, whereas a video player may use the elapsed time to the frame within the video. Timestamps should be in monotonically increasing order, to indicate the passage of time.
         */
        public long getTimestampMillis() {
            return timestampMillis;
        }

        /**
         * Returns the frame width.
         */
        public int getWidth() {
            return width;
        }

        @PublicApi(exclude = true)
        public FrameMetadataParcel createParcel() {
            FrameMetadataParcel parcel = new FrameMetadataParcel();
            parcel.width = width;
            parcel.height = height;
            parcel.id = id;
            parcel.timestampMillis = timestampMillis;
            parcel.rotation = rotation;
            return parcel;
        }

        @PublicApi(exclude = true)
        public Metadata withRotationAppliedToSize() {
            Metadata metadata = new Metadata(this);
            if (metadata.rotation % 2 != 0) {
                metadata.width = height;
                metadata.height = width;
            }
            metadata.rotation = 0;
            return metadata;
        }
    }
}
