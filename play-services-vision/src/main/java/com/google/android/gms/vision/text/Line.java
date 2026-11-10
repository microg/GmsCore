/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text;

import android.graphics.Point;
import android.graphics.Rect;

import com.google.android.gms.vision.text.internal.client.LineBoxParcel;
import com.google.android.gms.vision.text.internal.client.WordBoxParcel;

import org.microg.gms.common.PublicApi;

import java.util.ArrayList;
import java.util.List;

@PublicApi
public class Line implements Text {
    private final LineBoxParcel line;

    Line(LineBoxParcel line) {
        this.line = line;
    }

    public float getAngle() {
        return line.boundingBox == null ? 0f : line.boundingBox.angle;
    }

    @Override
    public Rect getBoundingBox() {
        return TextGeometry.bounds(getCornerPoints());
    }

    @Override
    public List<? extends Text> getComponents() {
        List<Element> elements = new ArrayList<>();
        if (line.words != null) {
            for (WordBoxParcel word : line.words) {
                if (word != null) elements.add(new Element(word));
            }
        }
        return elements;
    }

    @Override
    public Point[] getCornerPoints() {
        return TextGeometry.corners(line.boundingBox);
    }

    @Override
    public String getLanguage() {
        return TextGeometry.language(line.language);
    }

    @Override
    public String getValue() {
        return line.value == null ? "" : line.value;
    }

    public boolean isVertical() {
        return line.isVertical;
    }
}
