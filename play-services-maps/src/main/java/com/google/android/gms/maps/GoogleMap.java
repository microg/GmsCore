/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps;

import android.os.RemoteException;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.internal.ICancelableCallback;
import com.google.android.gms.maps.internal.IGoogleMapDelegate;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.RuntimeRemoteException;
import org.microg.gms.common.Hide;

/**
 * This is the main class of the Google Maps SDK for Android and is the entry point for all methods related to the map. You cannot instantiate a
 * {@link GoogleMap} object directly, rather, you must obtain one from the {@code getMapAsync()} method on a {@link MapFragment} or {@link MapView} that you
 * have added to your application.
 * <p>
 * Note: Similar to a {@link View} object, a {@link GoogleMap} can only be read and modified from the Android UI thread. Calling {@link GoogleMap} methods from
 * another thread will result in an exception.
 * <p>
 * You can adjust the viewpoint of a map by changing the position of the camera (as opposed to moving the map). You can use the map's camera to set parameters
 * such as location, zoom level, tilt angle, and bearing.
 */
public class GoogleMap {
    /**
     * No base map tiles.
     */
    public static final int MAP_TYPE_NONE = 0;
    /**
     * Basic maps.
     */
    public static final int MAP_TYPE_NORMAL = 1;
    /**
     * Satellite maps with no labels.
     */
    public static final int MAP_TYPE_SATELLITE = 2;
    /**
     * Terrain maps.
     */
    public static final int MAP_TYPE_TERRAIN = 3;
    /**
     * Satellite maps with a transparent layer of major streets.
     */
    public static final int MAP_TYPE_HYBRID = 4;

    private final IGoogleMapDelegate delegate;

    @Hide
    public GoogleMap(IGoogleMapDelegate delegate) {
        this.delegate = delegate;
    }

    private IGoogleMapDelegate getDelegate() {
        return delegate;
    }

    public Circle addCircle(CircleOptions options) {
        try {
            return new Circle(getDelegate().addCircle(options));
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Animates the movement of the camera from the current position to the position defined in the update. During the animation, a call to
     * {@link #getCameraPosition()} returns an intermediate location of the camera.
     * <p>
     * See CameraUpdateFactory for a set of updates.
     *
     * @param update The change that should be applied to the camera.
     */
    public void animateCamera(CameraUpdate update) {
        try {
            getDelegate().animateCamera(update.wrapped);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Animates the movement of the camera from the current position to the position defined in the update and calls an optional callback on completion. See
     * {@link CameraUpdateFactory} for a set of updates.
     * <p>
     * During the animation, a call to {@link #getCameraPosition()} returns an intermediate location of the camera.
     *
     * @param update   The change that should be applied to the camera.
     * @param callback The callback to invoke from the Android UI thread when the animation stops. If the animation completes normally,
     *                 {@link GoogleMap.CancelableCallback#onFinish()} is called; otherwise, {@link GoogleMap.CancelableCallback#onCancel()} is called. Do not
     *                 update or animate the camera from within {@link GoogleMap.CancelableCallback#onCancel()}.
     */
    public void animateCamera(CameraUpdate update, @Nullable GoogleMap.CancelableCallback callback) {
        try {
            getDelegate().animateCameraWithCallback(update.wrapped, new ICancelableCallback.Stub() {
                @Override
                public void onFinish() throws RemoteException {
                    callback.onFinish();
                }

                @Override
                public void onCancel() throws RemoteException {
                    callback.onCancel();
                }
            });
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Gets the current position of the camera.
     * <p>
     * The {@link CameraPosition} returned is a snapshot of the current position, and will not automatically update when the camera moves.
     *
     * @return The current position of the Camera.
     */
    public CameraPosition getCameraPosition() {
        try {
            return getDelegate().getCameraPosition();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Repositions the camera according to the instructions defined in the update. The move is instantaneous, and a subsequent {@link #getCameraPosition()} will
     * reflect the new position. See {@link CameraUpdateFactory} for a set of updates.
     *
     * @param update The change that should be applied to the camera.
     */
    public void moveCamera(CameraUpdate update) {
        try {
            getDelegate().moveCamera(update.wrapped);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * A callback interface for reporting when a task is complete or canceled.
     */
    public interface CancelableCallback {
        /**
         * Invoked when a task is canceled.
         */
        void onCancel();

        /**
         * Invoked when a task is complete.
         */
        void onFinish();
    }

    /**
     * Callback interface for when the camera motion starts.
     */
    public interface OnCameraMoveStartedListener {
        /**
         * Camera motion initiated in response to user gestures on the map. For example: pan, tilt, pinch to zoom, or rotate.
         */
        int REASON_GESTURE = 1;
        /**
         * Non-gesture animation initiated in response to user actions. For example: zoom buttons, my location button, or marker clicks.
         */
        int REASON_API_ANIMATION = 2;
        /**
         * Developer initiated animation.
         */
        int REASON_DEVELOPER_ANIMATION = 3;

        /**
         * Called when the camera starts moving after it has been idle or when the reason for camera motion has changed.
         * Do not update or animate the camera from within this method.
         * <p>
         * This is called on the Android UI thread.
         *
         * @param reason The reason for the camera change. Possible values:
         *               <ul>
         *                   <li>{@link #REASON_GESTURE}: User gestures on the map.</li>
         *                   <li>{@link #REASON_API_ANIMATION}: Default animations resulting from user interaction.</li>
         *                   <li>{@link #REASON_DEVELOPER_ANIMATION}: Developer animations.</li>
         *               </ul>
         */
        void onCameraMoveStarted(int reason);
    }
}
