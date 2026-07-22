/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps;

import android.graphics.Point;
import android.os.RemoteException;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.internal.*;
import com.google.android.gms.maps.model.*;
import org.microg.gms.common.Hide;

/**
 * This is the main class of the Street View feature in the Google Maps SDK for Android and is the entry point for all methods related to Street
 * View panoramas. You cannot instantiate a {@link StreetViewPanorama} object directly, rather, you must obtain one from the
 * {@code #getStreetViewPanoramaAsync()} method on a {@link StreetViewPanoramaFragment} or {@link StreetViewPanoramaView} that you have added to
 * your application.
 * <p>
 * Note: Similar to a {@link View} object, a {@link StreetViewPanorama} can only be read and modified from the main thread. Calling
 * {@link StreetViewPanorama} methods from another thread will result in an exception.
 */
public class StreetViewPanorama {
    private final IStreetViewPanoramaDelegate delegate;

    @Hide
    public StreetViewPanorama(@NonNull IStreetViewPanoramaDelegate delegate) {
        this.delegate = delegate;
    }

    private IStreetViewPanoramaDelegate getDelegate() {
        return delegate;
    }

    /**
     * Changes the current camera position, orientation and zoom, to a given position over a specified duration
     *
     * @param camera   The camera position to animate to. Must not be {@code null}.
     * @param duration The length of time, in milliseconds, it takes to transition from the current camera position to the given one
     */
    public void animateTo(@NonNull StreetViewPanoramaCamera camera, long duration) {
        try {
            getDelegate().animateTo(camera, duration);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Returns the current location of the user and information regarding the current panorama's adjacent panoramas
     *
     * @return The current location of the user
     */
    @NonNull
    public StreetViewPanoramaLocation getLocation() {
        try {
            return getDelegate().getStreetViewPanoramaLocation();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Returns the current orientation and zoom
     *
     * @return The current camera
     */
    @NonNull
    public StreetViewPanoramaCamera getPanoramaCamera() {
        try {
            return getDelegate().getPanoramaCamera();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Returns whether or not the panning gestures are enabled for the user
     *
     * @return {@code true} if panning gestures are enabled
     */
    public boolean isPanningGesturesEnabled() {
        try {
            return getDelegate().isPanningGesturesEnabled();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Returns whether or not the street names appear on the panorama
     *
     * @return {@code true} if street names are shown
     */
    public boolean isStreetNamesEnabled() {
        try {
            return getDelegate().isStreetNamesEnabled();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Returns whether or not the zoom gestures are enabled for the user
     *
     * @return {@code true} if zoom gestures are enabled
     */
    public boolean isUserNavigationEnabled() {
        try {
            return getDelegate().isUserNavigationEnabled();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Returns whether or not the zoom gestures are enabled for the user
     *
     * @return {@code true} if zoom gestures are enabled
     */
    public boolean isZoomGesturesEnabled() {
        try {
            return getDelegate().isZoomGesturesEnabled();
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Returns a screen location that corresponds to an orientation ({@link StreetViewPanoramaOrientation}). The screen location is in screen pixels
     * (not display pixels) relative to the top left of the Street View panorama (not of the whole screen).
     *
     * @param orientation A {@link StreetViewPanoramaOrientation} on the Street View panorama to convert to a screen location.
     * @return A {@link Point} representing the screen location in screen pixels. Returns {@code null} if the orientation is unable to be projected on the screen
     * (e.g. behind the user's field of view)
     */
    public Point orientationToPoint(StreetViewPanoramaOrientation orientation) {
        try {
            IObjectWrapper orientationToPoint = getDelegate().orientationToPoint(orientation);
            if (orientationToPoint == null) {
                return null;
            }
            return (Point) ObjectWrapper.unwrap(orientationToPoint);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Returns the orientation that corresponds to a screen location. The screen location is specified in screen pixels (not display pixels) relative to
     * the top left of the Street View panorama (not the top left of the whole screen).
     *
     * @param point A {@link Point} on the screen in screen pixels.
     * @return The {@link StreetViewPanoramaOrientation} corresponding to the point on the screen, or {@code null} if the Street View panorama has not
     * been initialized or if the given point is not a valid point on the screen
     */
    public StreetViewPanoramaOrientation pointToOrientation(Point point) {
        try {
            return getDelegate().pointToOrientation(ObjectWrapper.wrap(point));
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets a callback that's invoked when the camera changes
     *
     * @param listener The callback that's invoked when the camera changes. To unset the callback, use {@code null}.
     */
    public final void setOnStreetViewPanoramaCameraChangeListener(OnStreetViewPanoramaCameraChangeListener listener) {
        try {
            getDelegate().setOnStreetViewPanoramaCameraChangeListener(new IOnStreetViewPanoramaCameraChangeListener.Stub() {
                @Override
                public void onStreetViewPanoramaCameraChange(StreetViewPanoramaCamera camera) throws RemoteException {
                    listener.onStreetViewPanoramaCameraChange(camera);
                }
            });
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets a callback that's invoked when the panorama changes
     *
     * @param listener The callback that's invoked when the panorama changes. To unset the callback, use {@code null}.
     */
    public final void setOnStreetViewPanoramaChangeListener(OnStreetViewPanoramaChangeListener listener) {
        try {
            getDelegate().setOnStreetViewPanoramaChangeListener(new IOnStreetViewPanoramaChangeListener.Stub() {
                @Override
                public void onStreetViewPanoramaChange(StreetViewPanoramaLocation location) throws RemoteException {
                    listener.onStreetViewPanoramaChange(location);
                }
            });
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets a callback that's invoked when the panorama is tapped.
     *
     * @param listener The callback that's invoked when the panorama is tapped. To unset the callback, use {@code null}.
     */
    public final void setOnStreetViewPanoramaClickListener(OnStreetViewPanoramaClickListener listener) {
        try {
            getDelegate().setOnStreetViewPanoramaClickListener(new IOnStreetViewPanoramaClickListener.Stub() {
                @Override
                public void onStreetViewPanoramaClick(StreetViewPanoramaOrientation orientation) throws RemoteException {
                    listener.onStreetViewPanoramaClick(orientation);
                }
            });
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets a callback that's invoked when the panorama is long-pressed.
     *
     * @param listener The callback that's invoked when the panorama is long-pressed. To unset the callback, use {@code null}.
     */
    public final void setOnStreetViewPanoramaLongClickListener(OnStreetViewPanoramaLongClickListener listener) {
        try {
            getDelegate().setOnStreetViewPanoramaLongClickListener(new IOnStreetViewPanoramaLongClickListener.Stub() {
                @Override
                public void onStreetViewPanoramaLongClick(StreetViewPanoramaOrientation orientation) throws RemoteException {
                    listener.onStreetViewPanoramaLongClick(orientation);
                }
            });
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets whether the user is able to use panning gestures
     *
     * @param enablePanning {@code true} if users are allowed to use panning gestures
     */
    public void setPanningGesturesEnabled(boolean enablePanning) {
        try {
            getDelegate().enablePanning(enablePanning);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets the StreetViewPanorama to a given location
     *
     * @param position Latitude and longitude of the desired location. Must not be {@code null}.
     * @param source   StreetViewSource specifies the source of panoramas to search. If source is {@code null}, use the default.
     */
    public void setPosition(LatLng position, StreetViewSource source) {
        try {
            getDelegate().setPositionWithSource(position, source);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets the StreetViewPanorama to a given location
     *
     * @param position Latitude and longitude of the desired location. Should not be {@code null}.
     */
    public void setPosition(LatLng position) {
        try {
            getDelegate().setPosition(position);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets the StreetViewPanorama to a given location
     *
     * @param position Latitude and longitude of the desired location. Must not be {@code null}.
     * @param radius   Radius, specified in meters, that defines the area in which to search for a panorama, centered on the given latitude and longitude
     */
    public void setPosition(LatLng position, int radius) {
        try {
            getDelegate().setPositionWithRadius(position, radius);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets the StreetViewPanorama to a given location
     *
     * @param position Latitude and longitude of the desired location. Must not be {@code null}.
     * @param radius   Radius, specified in meters, that defines the area in which to search for a panorama, centered on the given latitude and longitude
     * @param source   StreetViewSource specifies the source of panoramas to search. If source is {@code null}, use the default.
     */
    public void setPosition(LatLng position, int radius, StreetViewSource source) {
        try {
            getDelegate().setPositionWithRadiusAndSource(position, radius, source);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets the StreetViewPanorama to a given location
     *
     * @param panoId Panorama ID of the desired location. Must not be {@code null}.
     */
    public void setPosition(String panoId) {
        try {
            getDelegate().setPositionWithID(panoId);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets whether the user is able to see street names on panoramas
     *
     * @param enableStreetNames {@code true} if users are able to see street names on panoramas
     */
    public void setStreetNamesEnabled(boolean enableStreetNames) {
        try {
            getDelegate().enableStreetNames(enableStreetNames);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets whether the user is able to move to another panorama
     *
     * @param enableUserNavigation {@code true} if users are allowed to move to another panorama
     */
    public void setUserNavigationEnabled(boolean enableUserNavigation) {
        try {
            getDelegate().enableUserNavigation(enableUserNavigation);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * Sets whether the user is able to use zoom gestures
     *
     * @param enableZoom {@code true} if users are allowed to use zoom gestures
     */
    public void setZoomGesturesEnabled(boolean enableZoom) {
        try {
            getDelegate().enableZoom(enableZoom);
        } catch (RemoteException e) {
            throw new RuntimeRemoteException(e);
        }
    }

    /**
     * A listener for when the StreetViewPanoramaCamera changes
     */
    public interface OnStreetViewPanoramaCameraChangeListener {
        /**
         * Called when the user makes changes to the camera on the panorama or if the camera is changed programmatically. Implementations of this
         * method are always invoked on the main thread.
         *
         * @param camera The position the camera has changed to
         */
        void onStreetViewPanoramaCameraChange(StreetViewPanoramaCamera camera);
    }

    /**
     * A listener for when the Street View panorama loads a new panorama
     */
    public interface OnStreetViewPanoramaChangeListener {
        /**
         * The StreetViewPanorama performs an animated transition from one location to another when the user performs a manual navigation action. This callback is called when the transition animation has occurred and the rendering of the first panorama has occurred. This callback also occurs when the developer sets a position and the rendering of the first panorama has occurred. It is possible that not all the panoramas have loaded when this callback is activated. Implementations of this method are always invoked on the main thread.
         *
         * @param location Location the StreetViewPanorama is changed to. null if it is an invalid panorama
         */
        void onStreetViewPanoramaChange(StreetViewPanoramaLocation location);
    }

    /**
     * Callback interface for when the user taps on the panorama.
     * <p>
     * Listeners will be invoked on the main thread
     */
    public interface OnStreetViewPanoramaClickListener {
        /**
         * Called when the user makes a tap gesture on the panorama, but only if none of the overlays of the panorama handled the gesture.
         * Implementations of this method are always invoked on the main thread.
         *
         * @param orientation The tilt and bearing values corresponding to the point on the screen where the user tapped. These values have an absolute value within a
         *                    specific panorama, and are independent of the current orientation of the camera.
         */
        void onStreetViewPanoramaClick(StreetViewPanoramaOrientation orientation);
    }

    /**
     * Callback interface for when the user long presses on the panorama.
     * <p>
     * Listeners will be invoked on the main thread
     */
    public interface OnStreetViewPanoramaLongClickListener {
        /**
         * Called when the user makes a long-press gesture on the panorama, but only if none of the overlays of the panorama handled the gesture.
         * Implementations of this method are always invoked on the main thread.
         *
         * @param orientation The tilt and bearing values corresponding to the point on the screen where the user long-pressed. These values have an absolute value within a
         *                    specific panorama, and are independent of the current orientation of the camera.
         */
        void onStreetViewPanoramaLongClick(StreetViewPanoramaOrientation orientation);
    }
}
