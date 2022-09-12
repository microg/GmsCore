/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.images;

import org.microg.gms.common.PublicApi;

/**
 * Immutable class for describing width and height dimensions in pixels.
 */
@PublicApi
public class Size {
    private int width;
    private int height;

    /**
     * Create a new immutable Size instance.
     *
     * @param width  The width of the size, in pixels
     * @param height The height of the size, in pixels
     */
    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Check if this size is equal to another size.
     * <p>
     * Two sizes are equal if and only if both their widths and heights are equal.
     * <p>
     * A size object is never equal to any other type of object.
     *
     * @return {@code true} if the objects were equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Size)) return false;

        Size size = (Size) o;

        if (width != size.width) return false;
        return height == size.height;
    }

    /**
     * Get the height of the size (in pixels).
     *
     * @return height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Get the width of the size (in pixels).
     *
     * @return width
     */
    public int getWidth() {
        return width;
    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        return result;
    }

    /**
     * Parses the specified string as a size value.
     * <p>
     * The ASCII characters {@code \}{@code u002a} ('*') and {@code \}{@code u0078} ('x') are recognized as separators between the width and height.
     * <p>
     * For any {@code Size s}: {@code Size.parseSize(s.toString()).equals(s)}. However, the method also handles sizes expressed in the following forms:
     * <p>
     * "width{@code x}height" or "width{@code *}height" => new Size(width, height), where width and height are string integers potentially containing a sign, such as "-10", "+7" or "5".
     *
     * @param string the string representation of a size value.
     * @return the size value represented by {@code string}.
     * @throws NumberFormatException if {@code string} cannot be parsed as a size value.
     * @throws NullPointerException  if {@code string} was null
     */
    public static Size parseSize(String string) {
        if (string == null) throw new NullPointerException("string must not be null");
        int split = string.indexOf('*');
        if (split < 0) split = string.indexOf('x');
        if (split < 0) throw new NumberFormatException("Invalid Size: \"" + string + "\"");
        return new Size(Integer.parseInt(string.substring(0, split)), Integer.parseInt(string.substring(split + 1)));
    }

    /**
     * Return the size represented as a string with the format {@code "WxH"}
     * @return string representation of the size
     */
    @Override
    public String toString() {
        return width + "x" + height;
    }
}
