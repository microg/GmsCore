/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.barcode;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.SparseArray;

import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.internal.client.BarcodeDetectorOptions;
import com.google.android.gms.vision.barcode.internal.client.INativeBarcodeDetector;
import com.google.android.gms.vision.barcode.internal.client.INativeBarcodeDetectorCreator;
import com.google.android.gms.vision.internal.FrameMetadataParcel;

import org.microg.gms.common.PublicApi;

/**
 * Recognizes barcodes (in a variety of 1D and 2D formats) in a supplied {@link Frame}.
 * <p>
 * Build new BarcodeDetector instances using {@link BarcodeDetector.Builder}. By default, BarcodeDetector searches for barcodes in every supported format. For the best performance it is highly recommended that you specify a narrower set of barcode formats to detect.
 * <p>
 * Recognition results are returned by {@link #detect(Frame)} as Barcode instances.
 */
@PublicApi
public class BarcodeDetector extends Detector<Barcode> {
    private INativeBarcodeDetector remote;

    private BarcodeDetector(INativeBarcodeDetector remote) {
        this.remote = remote;
    }

    /**
     * Recognizes barcodes in the supplied {@link Frame}.
     *
     * @return mapping of int to {@link Barcode}, where the int domain represents an opaque ID for the barcode. Identical barcodes (as determined by their raw value) will have the same ID across frames.
     */
    @Override
    public SparseArray<Barcode> detect(Frame frame) {
        if (frame == null) throw new IllegalArgumentException("No frame supplied.");
        SparseArray<Barcode> result = new SparseArray<>();
        if (remote != null) {
            FrameMetadataParcel metadataParcel = frame.getMetadata().createParcel();
            Barcode[] barcodes = null;
            if (frame.getBitmap() != null) {
                try {
                    barcodes = remote.detectBitmap(ObjectWrapper.wrap(frame.getBitmap()), metadataParcel);
                } catch (RemoteException e) {
                    // Ignore
                }
            } else {
                try {
                    barcodes = remote.detectBytes(ObjectWrapper.wrap(frame.getGrayscaleImageData()), metadataParcel);
                } catch (RemoteException e) {
                    // Ignore
                }
            }
            if (barcodes != null) {
                for (Barcode barcode : barcodes) {
                    result.append(barcode.rawValue.hashCode(), barcode);
                }
            }
        }

        return result;
    }

    @Override
    public boolean isOperational() {
        return remote != null && super.isOperational();
    }

    @Override
    public void release() {
        super.release();
        try {
            remote.close();
        } catch (RemoteException e) {
            // Ignore
        }
        remote = null;
    }

    /**
     * Barcode detector builder.
     */
    public static class Builder {
        private Context context;
        private BarcodeDetectorOptions options = new BarcodeDetectorOptions();

        /**
         * Builder for BarcodeDetector.
         */
        public Builder(Context context) {
            this.context = context;
        }

        /**
         * Bit mask (containing values like {@link Barcode#QR_CODE} and so on) that selects which formats this barcode detector should recognize.
         * <p>
         * By default, the detector will recognize all supported formats. This corresponds to the special {@link Barcode#ALL_FORMATS} constant.
         */
        public Builder setBarcodeFormats(int formats) {
            options.formats = formats;
            return this;
        }

        /**
         * Builds a barcode detector instance using the provided settings. If the underlying native implementation is unavailable (e.g. hasn't been downloaded yet), the detector will always return an empty result set. In this case, it will report that it is non-operational via {@link BarcodeDetector#isOperational()}.
         * <p>
         * Note that this method may cause blocking disk reads and should not be called on an application's main thread. To avoid blocking the main thread, consider moving Detector construction to a background thread using {@link android.os.AsyncTask}. Enable {@link android.os.StrictMode} to automatically detect blocking operations on the main thread.
         *
         * @return new {@link BarcodeDetector} instance
         */
        public BarcodeDetector build() {
            // TODO: Actually implement dynamite or load from remote
            INativeBarcodeDetector remote = null;
            try {
                Class<?> clazz = Class.forName("com.google.android.gms.vision.barcode.ChimeraNativeBarcodeDetectorCreator");
                Object instance = clazz.getConstructor().newInstance();
                INativeBarcodeDetectorCreator creator = INativeBarcodeDetectorCreator.Stub.asInterface(((IInterface) instance).asBinder());
                remote = creator.create(ObjectWrapper.wrap(context), options);
            } catch (Exception e) {
                // Ignore
            }
            return new BarcodeDetector(remote);
        }
    }
}
