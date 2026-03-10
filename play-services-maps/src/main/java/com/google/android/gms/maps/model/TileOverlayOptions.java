/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps.model;

import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import com.google.android.gms.maps.model.internal.ITileProviderDelegate;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Defines options for a TileOverlay.
 */
@PublicApi
public class TileOverlayOptions extends AutoSafeParcelable {

    @Field(1)
    private int versionCode = 1;
    /**
     * This is a IBinder to the {@link #tileProvider}, built using {@link ITileProviderDelegate}.
     */
    @Field(2)
    private IBinder tileProviderBinder;
    private TileProvider tileProvider;
    @Field(3)
    private boolean visible = true;
    @Field(4)
    private float zIndex;
    @Field(5)
    private boolean fadeIn = true;
    @Field(6)
    private float transparency = 0.0f;

    /**
     * Creates a new set of tile overlay options.
     */
    public TileOverlayOptions() {
    }

    /**
     * Specifies whether the tiles should fade in. The default is {@code true}.
     *
     * @return this {@link TileOverlayOptions} object with a new fadeIn setting.
     */
    public TileOverlayOptions fadeIn(boolean fadeIn) {
        this.fadeIn = fadeIn;
        return this;
    }

    /**
     * Gets whether the tiles should fade in.
     *
     * @return {@code true} if the tiles are to fade in; {@code false} if it is not.
     */
    public boolean getFadeIn() {
        return fadeIn;
    }

    /**
     * Gets the tile provider set for this {@link TileOverlayOptions} object.
     *
     * @return the {@link TileProvider} of the tile overlay.
     */
    public TileProvider getTileProvider() {
        if (tileProvider == null && tileProviderBinder != null) {
            ITileProviderDelegate delegate = ITileProviderDelegate.Stub.asInterface(tileProviderBinder);
            this.tileProvider = new TileProvider() {
                @Override
                public Tile getTile(int x, int y, int zoom) {
                    try {
                        return delegate.getTile(x, y, zoom);
                    } catch (RemoteException e) {
                        return null;
                    }
                }
            };
        }
        return tileProvider;
    }

    /**
     * Gets the transparency set for this {@link TileOverlayOptions} object.
     *
     * @return the transparency of the tile overlay.
     */
    public float getTransparency() {
        return transparency;
    }

    /**
     * Gets the zIndex set for this {@link TileOverlayOptions} object.
     *
     * @return the zIndex of the tile overlay.
     */
    public float getZIndex() {
        return zIndex;
    }

    /**
     * Gets the visibility setting for this {@link TileOverlayOptions} object.
     *
     * @return {@code true} if the tile overlay is to be visible; {@code false} if it is not.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Specifies the tile provider to use for this tile overlay.
     *
     * @param tileProvider the {@link TileProvider} to use for this tile overlay.
     * @return the object for which the method was called, with the new tile provider set.
     */
    public TileOverlayOptions tileProvider(@NonNull final TileProvider tileProvider) {
        this.tileProvider = tileProvider;
        this.tileProviderBinder = new ITileProviderDelegate.Stub() {
            @Override
            public Tile getTile(int x, int y, int zoom) throws RemoteException {
                return tileProvider.getTile(x, y, zoom);
            }
        };
        return this;
    }

    /**
     * Specifies the transparency of the tile overlay. The default transparency is {@code 0} (opaque).
     *
     * @param transparency a float in the range {@code [0..1]} where {@code 0} means that the tile overlay is opaque and {@code 1} means that the tile overlay is transparent.
     * @return this {@link TileOverlayOptions} object with a new transparency setting.
     * @throws IllegalArgumentException if the transparency is outside the range [0..1].
     */
    public TileOverlayOptions transparency(float transparency) {
        if (transparency < 0.0f || transparency > 1.0f) throw new IllegalArgumentException("Transparency must be in the range [0..1]");
        this.transparency = transparency;
        return this;
    }

    /**
     * Specifies the visibility for the tile overlay. The default visibility is {@code true}.
     *
     * @return this {@link TileOverlayOptions} object with a new visibility setting.
     */
    public TileOverlayOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Specifies the tile overlay's zIndex, i.e., the order in which it will be drawn where
     * overlays with larger values are drawn above those with lower values. See the documentation
     * at the top of this class for more information about zIndex.
     *
     * @return this {@link TileOverlayOptions} object with a new zIndex set.
     */
    public TileOverlayOptions zIndex(float zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    public static Creator<TileOverlayOptions> CREATOR = new AutoCreator<TileOverlayOptions>(TileOverlayOptions.class);
}
