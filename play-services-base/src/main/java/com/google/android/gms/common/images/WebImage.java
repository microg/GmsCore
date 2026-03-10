/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.images;

import java.util.Locale;

import android.net.Uri;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * A class that represents an image that is located on a web server.
 */
public class WebImage extends AutoSafeParcelable {
    public static final Creator<WebImage> CREATOR = new AutoCreator<WebImage>(WebImage.class);

    @Field(1)
    private int versionCode = 1;

    @Field(2)
    private final Uri url;

    @Field(3)
    private final int width;

    @Field(4)
    private final int height;

    @Hide
    private WebImage() {
        this.url = null;
        this.width = 0;
        this.height = 0;
    }

    /**
     * Constructs a new {@link WebImage} with the given URL.
     *
     * @param url The URL of the image.
     * @throws IllegalArgumentException If the URL is null or empty.
     */
    public WebImage(Uri url) {
        this(url, 0, 0);
    }

    /**
     * Constructs a new {@link WebImage} with the given URL and dimensions.
     *
     * @param url    The URL of the image.
     * @param width  The width of the image, in pixels.
     * @param height The height of the image, in pixels.
     * @throws IllegalArgumentException If the URL is null or empty, or the dimensions are invalid.
     */
    public WebImage(Uri url, int width, int height) {
        if (url == null) throw new IllegalArgumentException("url cannot be null");
        if (width < 0 || height < 0) throw new IllegalArgumentException("width and height must not be negative");
        this.url = url;
        this.width = width;
        this.height = height;
    }

    /**
     * Gets the image height, in pixels.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the image URL.
     */
    public Uri getUrl() {
        return url;
    }

    /**
     * Gets the image width, in pixels.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns a string representation of this object.
     */
    public String toString() {
        return String.format(Locale.getDefault(), "Image %dx%d %s", Integer.valueOf(width), Integer.valueOf(height), url.toString());
    }
}
