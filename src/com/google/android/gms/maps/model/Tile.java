package com.google.android.gms.maps.model;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * Contains information about a Tile that is returned by a {@link TileProvider}.
 * TODO SafeParceled
 */
@PublicApi
public class Tile extends AutoSafeParcelable {
    @SafeParceled(1)
    private final int versionCode;
    /**
     * The width of the image encoded by {@link #data} in pixels.
     */
    @SafeParceled(2)
    public final int width;
    /**
     * The height of the image encoded by {@link #data} in pixels.
     */
    @SafeParceled(3)
    public final int height;
    /**
     * A byte array containing the image data. The image will be created from this data by calling
     * {@link android.graphics.BitmapFactory#decodeByteArray(byte[], int, int)}.
     */
    @SafeParceled(4)
    public final byte[] data;

    private Tile() {
        versionCode = 1;
        width = height = 0;
        data = null;
    }

    /**
     * Constructs a {@link Tile}.
     *
     * @param width  the width of the image in pixels
     * @param height the height of the image in pixels
     * @param data   A byte array containing the image data. The image will be created from this
     *               data by calling
     *               {@link android.graphics.BitmapFactory#decodeByteArray(byte[], int, int)}.
     */
    public Tile(int width, int height, byte[] data) {
        this.versionCode = 1;
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public static Creator<Tile> CREATOR = new AutoCreator<>(Tile.class);
}
