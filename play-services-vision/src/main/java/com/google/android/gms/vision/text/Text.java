/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text;

import android.graphics.Point;
import android.graphics.Rect;

import com.google.android.gms.vision.text.internal.client.BoundingBoxParcel;

import org.microg.gms.common.PublicApi;

import java.util.List;

@PublicApi
public interface Text {
    Rect getBoundingBox();

    List<? extends Text> getComponents();

    Point[] getCornerPoints();

    String getLanguage();

    String getValue();
}

final class TextGeometry {
    private TextGeometry() {
    }

    static Point[] corners(BoundingBoxParcel box) {
        if (box == null) return new Point[0];
        double radians = Math.toRadians(box.angle);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Point[]{new Point(box.left, box.top), point(box.left + box.width * cos, box.top + box.width * sin), point(box.left + box.width * cos - box.height * sin, box.top + box.width * sin + box.height * cos), point(box.left - box.height * sin, box.top + box.height * cos)};
    }

    static Point point(double x, double y) {
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    static Rect bounds(Point[] corners) {
        if (corners.length == 0) return new Rect();
        int left = corners[0].x;
        int top = corners[0].y;
        int right = left;
        int bottom = top;
        for (Point point : corners) {
            left = Math.min(left, point.x);
            top = Math.min(top, point.y);
            right = Math.max(right, point.x);
            bottom = Math.max(bottom, point.y);
        }
        return new Rect(left, top, right, bottom);
    }

    static String language(String language) {
        return language == null || language.isEmpty() ? "und" : language;
    }
}
