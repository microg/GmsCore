/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.internal.ICreator;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.RuntimeRemoteException;
import org.microg.gms.maps.MapsContextLoader;

/**
 * Use this class to initialize the Maps SDK for Android if features need to be used before obtaining a map.
 * It must be called because some classes such as BitmapDescriptorFactory and CameraUpdateFactory need to be initialized.
 * <p>
 * If you are using {@link MapFragment} or {@link MapView} and have already obtained a (non-null) {@link GoogleMap} by calling {@code getMapAsync()} on either
 * of these classes and waiting for the {@code onMapReady(GoogleMap map)} callback, then you do not need to worry about this class.
 */
public class MapsInitializer {
    private static final String TAG = "MapsInitializer";
    private static boolean initialized = false;
    private static Renderer renderer = Renderer.LEGACY;

    /**
     * Initializes the Maps SDK for Android so that its classes are ready for use. If you are using {@link MapFragment} or {@link MapView} and have
     * already obtained a (non-null) {@link GoogleMap} by calling {@code getMapAsync()} on either of these classes, then it is not necessary to call this.
     *
     * @param context Required to fetch the necessary SDK resources and code. Must not be {@code null}.
     * @return A ConnectionResult error code.
     */
    public static synchronized int initialize(@NonNull Context context) {
        return initialize(context, null, null);
    }

    /**
     * Specifies which {@link MapsInitializer.Renderer} type you prefer to use to initialize the Maps SDK for Android, and provides a callback to receive the
     * actual {@link MapsInitializer.Renderer} type. This call will initialize the Maps SDK for Android, so that its classes are ready for use. The callback
     * will be triggered when the Maps SDK is initialized.
     * <p>
     * The Maps SDK only initializes once per Application lifecycle. Only the first call of this method or {@link #initialize(Context)} takes effect.
     * If you are using {@link MapFragment} or {@link MapView} and have already obtained a (non-null) {@link GoogleMap} by calling {@code getMapAsync()} on
     * either of these classes, then this call will have no effect other than triggering the callback for the initialized {@link MapsInitializer.Renderer}.
     * To make renderer preference meaningful, you must call this method before {@link #initialize(Context)}, and before {@link MapFragment#onCreate(Bundle)}
     * and {@link MapView#onCreate(Bundle)}. It's recommended to do this in {@link Application#onCreate()}.
     * <p>
     * Note the following:
     * <ul>
     * <li>Use {@code LATEST} to request the new renderer. No action is necessary if you prefer to use the legacy renderer.</li>
     * <li>The latest renderer may not always be returned due to various reasons, including not enough memory, unsupported Android version, or routine downtime.</li>
     * <li>The new renderer will eventually become the default renderer through a progressive rollout. At that time, you will need to request {@code LEGACY} in
     * order to continue using the legacy renderer.</li>
     * </ul>
     *
     * @param context           Required to fetch the necessary SDK resources and code. Must not be {@code null}.
     * @param preferredRenderer Which {@link MapsInitializer.Renderer} type you prefer to use for your application.
     *                          If {@code null} is provided, the default preference is taken.
     * @param callback          The callback that the Maps SDK triggers when it informs you about which renderer type was actually loaded.
     *                          You can define what you want to do differently according to the maps renderer that is loaded.
     * @return A ConnectionResult error code.
     */
    public static synchronized int initialize(@NonNull Context context, @Nullable MapsInitializer.Renderer preferredRenderer, @Nullable OnMapsSdkInitializedCallback callback) {
        Log.d(TAG, "preferredRenderer: " + preferredRenderer);
        if (initialized) {
            if (callback != null) {
                callback.onMapsSdkInitialized(renderer);
            }
            return CommonStatusCodes.SUCCESS;
        }
        try {
            ICreator creator = MapsContextLoader.getCreator(context, preferredRenderer);
            try {
                CameraUpdateFactory.setDelegate(creator.newCameraUpdateFactoryDelegate());
                BitmapDescriptorFactory.setDelegate(creator.newBitmapDescriptorFactoryDelegate());
                int preferredRendererInt = 0;
                if (preferredRenderer != null) {
                    if (preferredRenderer == Renderer.LEGACY) preferredRendererInt = 1;
                    else if (preferredRenderer == Renderer.LATEST) preferredRendererInt = 2;
                }
                try {
                    if (creator.getRendererType() == 2) {
                        renderer = Renderer.LATEST;
                    }
                    creator.logInitialization(ObjectWrapper.wrap(context), preferredRendererInt);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to retrieve renderer type or log initialization.", e);
                }
                Log.d(TAG, "loadedRenderer: " + renderer);
                if (callback != null) {
                    callback.onMapsSdkInitialized(renderer);
                }
                return CommonStatusCodes.SUCCESS;
            } catch (RemoteException e) {
                throw new RuntimeRemoteException(e);
            }
        } catch (Exception e) {
            return CommonStatusCodes.INTERNAL_ERROR;
        }
    }

    /**
     * Enables you to specify which {@link MapsInitializer.Renderer} you prefer to use for your application {@code LATEST} or {@code LEGACY}.
     * It also informs you which maps {@link MapsInitializer.Renderer} is actually used for your application.
     */
    public enum Renderer {
        LEGACY, LATEST
    }
}
