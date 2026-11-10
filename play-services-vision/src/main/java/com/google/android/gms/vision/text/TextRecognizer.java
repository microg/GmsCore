/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.internal.client.INativeTextRecognizer;
import com.google.android.gms.vision.text.internal.client.INativeTextRecognizerCreator;
import com.google.android.gms.vision.text.internal.client.LineBoxParcel;
import com.google.android.gms.vision.text.internal.client.RecognitionOptions;
import com.google.android.gms.vision.text.internal.client.TextRecognizerOptions;

import org.microg.gms.common.PublicApi;

@PublicApi
public final class TextRecognizer extends Detector<TextBlock> {
    private static final String TAG = "GmsTextRecognizer";
    private INativeTextRecognizer remote;

    private TextRecognizer(INativeTextRecognizer remote) {
        this.remote = remote;
    }

    @Override
    public final synchronized SparseArray<TextBlock> detect(Frame frame) {
        if (frame == null) throw new IllegalArgumentException("No frame supplied");
        Bitmap bitmap = frame.getBitmap();
        if (bitmap == null && frame.getMetadata().getFormat() != ImageFormat.NV21) {
            throw new IllegalArgumentException("Text recognition requires a Bitmap or NV21 frame");
        }
        SparseArray<TextBlock> result = new SparseArray<>();
        if (remote == null) return result;
        try {
            Object image = bitmap != null ? bitmap : frame.getGrayscaleImageData();
            LineBoxParcel[] lines = remote.recognizeWithOptions(ObjectWrapper.wrap(image), frame.getMetadata().createParcel(), new RecognitionOptions());
            if (lines == null) return result;
            SparseArray<SparseArray<LineBoxParcel>> blocks = new SparseArray<>();
            for (LineBoxParcel line : lines) {
                if (line == null) continue;
                SparseArray<LineBoxParcel> block = blocks.get(line.blockId);
                if (block == null) {
                    block = new SparseArray<>();
                    blocks.put(line.blockId, block);
                }
                block.put(line.lineId, line);
            }
            for (int i = 0; i < blocks.size(); i++) {
                result.put(blocks.keyAt(i), new TextBlock(blocks.valueAt(i)));
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Text recognition failed", e);
        }
        return result;
    }

    @Override
    public final synchronized boolean isOperational() {
        return remote != null;
    }

    @Override
    public final void release() {
        try {
            super.release();
        } finally {
            synchronized (this) {
                if (remote != null) {
                    try {
                        remote.close();
                    } catch (RemoteException e) {
                        Log.w(TAG, "Unable to release text recognizer", e);
                    } finally {
                        remote = null;
                    }
                }
            }
        }
    }

    @PublicApi
    public static class Builder {
        private final Context context;

        public Builder(Context context) {
            if (context == null) throw new IllegalArgumentException("Context must not be null");
            this.context = context;
        }

        public TextRecognizer build() {
            INativeTextRecognizer remote = null;
            try {
                Class<?> creatorClass = Class.forName("com.google.android.gms.vision.text.ChimeraNativeTextRecognizerCreator");
                IBinder binder = (IBinder) creatorClass.getConstructor().newInstance();
                INativeTextRecognizerCreator creator = INativeTextRecognizerCreator.Stub.asInterface(binder);
                remote = creator.create(ObjectWrapper.wrap(context), new TextRecognizerOptions());
            } catch (Exception | LinkageError e) {
                Log.w(TAG, "Text recognition is unavailable", e);
            }
            return new TextRecognizer(remote);
        }
    }
}
