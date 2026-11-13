/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.mlkit.vision.text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.os.RemoteException;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.android.gms.common.Feature;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.odml.image.BitmapExtractor;
import com.google.android.odml.image.ByteBufferExtractor;
import com.google.android.odml.image.ImageProperties;
import com.google.android.odml.image.MediaImageExtractor;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;
import com.google.mlkit.vision.common.aidls.ImageMetadataParcel;
import com.google.mlkit.vision.text.aidls.ITextRecognizer;
import com.google.mlkit.vision.text.aidls.ITextRecognizerCreator;
import com.google.mlkit.vision.text.aidls.TextRecognizerOptions;

import org.microg.gms.common.Constants;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TextRecognizerImpl implements TextRecognizer {
    private static final String TAG = "GmsTextRecognition";
    private static final long MAX_IMAGE_PIXELS = 16_000_000L;
    private static final String[] FEATURES = {"vision.ocr", "mlkit.ocr.chinese", "mlkit.ocr.devanagari", "mlkit.ocr.japanese", "mlkit.ocr.korean"};
    private final TextRecognizerOptionsInterface options;
    private final Executor executor;
    private final ExecutorService ownedExecutor;
    private final Object remoteLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean();
    private ITextRecognizer remote;

    public TextRecognizerImpl(TextRecognizerOptionsInterface options) {
        int script = options.getLoggingLanguageOption();
        if (script < 1 || script > 5 || options.getIsThickClient()) {
            throw new IllegalArgumentException("Unsupported text recognition options");
        }
        this.options = options;
        if (options.getExecutor() == null) {
            ownedExecutor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "mlkit-text"));
            executor = ownedExecutor;
        } else {
            ownedExecutor = null;
            executor = options.getExecutor();
        }
    }

    @Override
    public Feature[] getOptionalFeatures() {
        return new Feature[]{new Feature(FEATURES[options.getLoggingLanguageOption() - 1], 1L)};
    }

    @Override
    public int getDetectorType() {
        return TYPE_TEXT_RECOGNITION;
    }

    @Override
    public Task<Text> process(InputImage image) {
        if (image == null) throw new NullPointerException("Input image must not be null");
        return process(owned -> image, null, null);
    }

    @Override
    public Task<Text> process(MlImage image) {
        if (image == null) throw new NullPointerException("Input image must not be null");
        if (closed.get()) return Tasks.forException(new MlKitException("Text recognizer is closed", MlKitException.UNAVAILABLE));
        AtomicBoolean released = new AtomicBoolean();
        ImageLoader input = owned -> convertImage(image, owned);
        Runnable release = () -> {
            if (released.compareAndSet(false, true)) image.close();
        };
        try {
            image.getInternal().acquire();
        } catch (IllegalStateException e) {
            return Tasks.forException(new MlKitException("Invalid OCR image", MlKitException.INVALID_ARGUMENT, e));
        }
        try {
            return process(input, null, release);
        } catch (Error e) {
            release.run();
            throw e;
        }
    }

    private static InputImage convertImage(MlImage image, OwnedBitmap owned) throws MlKitException {
        try {
            ImageProperties properties = image.getContainedImageProperties().get(0);
            switch (properties.getStorageType()) {
                case MlImage.STORAGE_TYPE_BITMAP:
                    return InputImage.fromBitmap(BitmapExtractor.extract(image), image.getRotation());
                case MlImage.STORAGE_TYPE_MEDIA_IMAGE:
                    return convertMediaImage(MediaImageExtractor.extract(image), image.getRotation(), owned);
                case MlImage.STORAGE_TYPE_BYTEBUFFER:
                    if (properties.getImageFormat() == MlImage.IMAGE_FORMAT_RGB || properties.getImageFormat() == MlImage.IMAGE_FORMAT_RGBA) {
                        owned.bitmap = decodeRgbBuffer(image, properties.getImageFormat());
                        return InputImage.fromBitmap(owned.bitmap, image.getRotation());
                    }
                    int format;
                    if (properties.getImageFormat() == MlImage.IMAGE_FORMAT_NV21) format = InputImage.IMAGE_FORMAT_NV21;
                    else if (properties.getImageFormat() == MlImage.IMAGE_FORMAT_YV12) format = InputImage.IMAGE_FORMAT_YV12;
                    else throw new IllegalArgumentException("OCR byte buffers require NV21 or YV12 format");
                    return InputImage.fromByteBuffer(ByteBufferExtractor.extract(image), image.getWidth(), image.getHeight(), image.getRotation(), format);
                default:
                    throw new IllegalArgumentException("Unsupported OCR image storage");
            }
        } catch (RuntimeException e) {
            throw new MlKitException("Invalid OCR image", MlKitException.INVALID_ARGUMENT, e);
        }
    }

    private static InputImage convertMediaImage(Image image, int rotationDegrees, OwnedBitmap owned) {
        InputImage input = InputImage.fromMediaImage(image, rotationDegrees, MAX_IMAGE_PIXELS);
        owned.bitmap = input.getBitmapInternal();
        return input;
    }

    private static Bitmap decodeRgbBuffer(MlImage image, int format) {
        long count = (long) image.getWidth() * image.getHeight();
        if (count > MAX_IMAGE_PIXELS) throw new IllegalArgumentException("Oversized OCR image");
        ByteBuffer buffer = ByteBufferExtractor.extract(image);
        buffer.rewind();
        int channels = format == MlImage.IMAGE_FORMAT_RGBA ? 4 : 3;
        if (buffer.remaining() < count * channels) throw new IllegalArgumentException("Image buffer is truncated");
        int[] colors = new int[(int) count];
        for (int i = 0; i < colors.length; i++) {
            int red = buffer.get() & 255;
            int green = buffer.get() & 255;
            int blue = buffer.get() & 255;
            int alpha = channels == 4 ? buffer.get() & 255 : 255;
            colors[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        return Bitmap.createBitmap(colors, image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
    }

    @Override
    public Task<Text> process(Bitmap bitmap, int rotationDegrees) {
        return process(InputImage.fromBitmap(bitmap, rotationDegrees));
    }

    @Override
    public Task<Text> process(Image image, int rotationDegrees) {
        return process(image, rotationDegrees, null);
    }

    @Override
    public Task<Text> process(Image image, int rotationDegrees, Matrix coordinatesMatrix) {
        if (image == null) throw new NullPointerException("Image must not be null");
        if (rotationDegrees != 0 && rotationDegrees != 90 && rotationDegrees != 180 && rotationDegrees != 270) {
            throw new IllegalArgumentException("Rotation must be 0, 90, 180 or 270 degrees");
        }
        return process(owned -> convertMediaImage(image, rotationDegrees, owned),
                coordinatesMatrix == null ? null : new Matrix(coordinatesMatrix), null);
    }

    @Override
    public Task<Text> process(ByteBuffer buffer, int width, int height, int rotationDegrees, int format) {
        return process(InputImage.fromByteBuffer(buffer, width, height, rotationDegrees, format));
    }

    private Task<Text> process(ImageLoader input, Matrix coordinatesMatrix, Runnable release) {
        TaskCompletionSource<Text> completion = new TaskCompletionSource<>();
        if (closed.get()) {
            if (release != null) release.run();
            completion.setException(new MlKitException("Text recognizer is closed", MlKitException.UNAVAILABLE));
            return completion.getTask();
        }
        try {
            executor.execute(() -> {
                try {
                    Text result;
                    synchronized (remoteLock) {
                        if (closed.get()) throw new MlKitException("Text recognizer is closed", MlKitException.UNAVAILABLE);
                        try (OwnedBitmap owned = new OwnedBitmap()) {
                            InputImage image = input.load(owned);
                            if (closed.get()) throw new MlKitException("Text recognizer is closed", MlKitException.UNAVAILABLE);
                            ImageMetadataParcel metadata = new ImageMetadataParcel();
                            metadata.format = image.getFormat();
                            metadata.width = image.getWidth();
                            metadata.height = image.getHeight();
                            metadata.rotation = image.getRotationDegrees() / 90;
                            metadata.timestampMillis = SystemClock.elapsedRealtime();
                            result = new Text(getRemote().process(ObjectWrapper.wrap(image.getSource()), metadata), coordinatesMatrix);
                        }
                    }
                    completion.setResult(result);
                } catch (MlKitException e) {
                    completion.trySetException(e);
                } catch (IllegalArgumentException e) {
                    completion.trySetException(new MlKitException("Invalid OCR image", MlKitException.INVALID_ARGUMENT, e));
                } catch (OutOfMemoryError e) {
                    completion.trySetException(new MlKitException("Insufficient memory for text recognition", MlKitException.RESOURCE_EXHAUSTED, e));
                } catch (Exception | LinkageError e) {
                    completion.trySetException(new MlKitException("Text recognition failed", MlKitException.INTERNAL, e));
                } finally {
                    if (release != null) release.run();
                }
            });
        } catch (RuntimeException e) {
            if (release != null) release.run();
            completion.trySetException(new MlKitException("OCR executor rejected the request", MlKitException.INTERNAL, e));
        }
        return completion.getTask();
    }

    private interface ImageLoader {
        InputImage load(OwnedBitmap owned) throws MlKitException;
    }

    private static final class OwnedBitmap implements AutoCloseable {
        private Bitmap bitmap;

        @Override
        public void close() {
            if (bitmap != null) bitmap.recycle();
        }
    }

    private ITextRecognizer getRemote() throws MlKitException {
        if (remote != null) return remote;

        Context context;
        ITextRecognizerCreator creator;
        try {
            context = TextRecognitionContext.getApplicationContext();
            creator = loadCreator(context);
        } catch (Exception | LinkageError e) {
            throw new MlKitException("Text recognition module is unavailable", MlKitException.UNAVAILABLE, e);
        }

        ITextRecognizer created = null;
        try {
            IObjectWrapper wrappedContext = ObjectWrapper.wrap(context);
            int script = options.getLoggingLanguageOption();
            if (script == TextRecognizerOptionsInterface.LATIN) {
                created = creator.create(wrappedContext);
            } else {
                TextRecognizerOptions parcel = new TextRecognizerOptions();
                parcel.languageOption = script;
                parcel.configLabel = options.getConfigLabel();
                parcel.libraryName = options.getLoggingLibraryNameForOptionalModule();
                parcel.optionFlag = true;
                parcel.languageHint = options.getLanguageHint();
                created = creator.createWithOptions(wrappedContext, parcel);
            }
            if (created == null) throw new IllegalStateException("OCR module returned no recognizer");
            created.init();
        } catch (Exception | LinkageError e) {
            if (created != null) {
                try {
                    created.close();
                } catch (Exception | LinkageError closeError) {
                    if (closeError != e) e.addSuppressed(closeError);
                }
            }
            throw new MlKitException("Failed to create or initialize text recognizer", MlKitException.INTERNAL, e);
        }
        remote = created;
        return remote;
    }

    private ITextRecognizerCreator loadCreator(Context context) throws Exception {
        Context gms = context.createPackageContext(Constants.GMS_PACKAGE_NAME,
                Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        Class<?> factory = gms.getClassLoader().loadClass("com.google.android.gms.chimera.DynamiteContextFactory");
        Context module = (Context) factory.getMethod("createDynamiteContext", String.class, Context.class)
                .invoke(null, options.getModuleId(), context);
        if (module == null) throw new IllegalStateException("OCR module is unavailable");
        ClassLoader loader = module.getClassLoader();
        Class<?> descriptor = loader.loadClass("com.google.android.gms.dynamite.descriptors." + options.getModuleId() + ".ModuleDescriptor");
        if (descriptor.getField("MODULE_VERSION").getInt(null) <= 0) {
            throw new IllegalStateException("OCR module is not supported on this Android version");
        }
        IBinder binder = (IBinder) loader.loadClass("com.google.android.gms.vision.text.mlkit.TextRecognizerCreator")
                .getConstructor().newInstance();
        return ITextRecognizerCreator.Stub.asInterface(binder);
    }

    @Override
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            executor.execute(this::closeRemote);
        } catch (RuntimeException e) {
            closeRemote();
        } finally {
            if (ownedExecutor != null) ownedExecutor.shutdown();
        }
    }

    private void closeRemote() {
        synchronized (remoteLock) {
            if (remote == null) return;
            try {
                remote.close();
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to close text recognizer", e);
            } finally {
                remote = null;
            }
        }
    }
}
