/*
 * Copyright (c) 2014 μg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.maps.model;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;

/**
 * Defines options for a TileOverlay.
 */
public class TileOverlayOptions implements SafeParcelable {

    private TileProvider tileProvider;
    private boolean visible = true;
    private boolean fadeIn = true;
    private float zIndex;

    /**
     * Creates a new set of tile overlay options.
     */
    public TileOverlayOptions() {
    }

    private TileOverlayOptions(Parcel in) {
        SafeParcelUtil.readObject(this, in);
    }

    @Override
    public int describeContents() {
        return 0;
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
        return tileProvider;
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
    public TileOverlayOptions tileProvider(TileProvider tileProvider) {
        this.tileProvider = tileProvider;
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        SafeParcelUtil.writeObject(this, dest, flags);
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

    public static Creator<TileOverlayOptions> CREATOR = new Creator<TileOverlayOptions>() {
        public TileOverlayOptions createFromParcel(Parcel source) {
            return new TileOverlayOptions(source);
        }

        public TileOverlayOptions[] newArray(int size) {
            return new TileOverlayOptions[size];
        }
    };
}
