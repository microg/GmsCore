package com.google.android.gms.maps.model;

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * Contains information about a Tile that is returned by a {@link TileProvider}.
 * TODO SafeParceled
 */
public class Tile implements SafeParcelable {
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
        versionCode = -1;
        width = height = 0;
        data = null;
    }

    private Tile(Parcel in) {
        this();
        SafeParcelUtil.readObject(this, in);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        SafeParcelUtil.writeObject(this, out, flags);
    }

    public static Creator<Tile> CREATOR = new Creator<Tile>() {
        public Tile createFromParcel(Parcel source) {
            return new Tile(source);
        }

        public Tile[] newArray(int size) {
            return new Tile[size];
        }
    };
}
