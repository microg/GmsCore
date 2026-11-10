/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.SparseArray;

import com.google.android.gms.vision.text.internal.client.LineBoxParcel;

import org.microg.gms.common.PublicApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PublicApi
public class TextBlock implements Text {
    private final List<Line> lines = new ArrayList<>();

    TextBlock(SparseArray<LineBoxParcel> parcels) {
        for (int i = 0; i < parcels.size(); i++) {
            LineBoxParcel line = parcels.valueAt(i);
            if (line != null) lines.add(new Line(line));
        }
    }

    @Override
    public Rect getBoundingBox() {
        return TextGeometry.bounds(getCornerPoints());
    }

    @Override
    public List<? extends Text> getComponents() {
        return new ArrayList<>(lines);
    }

    @Override
    public Point[] getCornerPoints() {
        if (lines.isEmpty()) return new Point[0];
        if (lines.size() == 1) return lines.get(0).getCornerPoints();
        double radians = Math.toRadians(lines.get(0).getAngle());
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (Line line : lines) {
            for (Point point : line.getCornerPoints()) {
                double x = point.x * cos + point.y * sin;
                double y = -point.x * sin + point.y * cos;
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
        }
        if (Double.isInfinite(left) || Double.isNaN(left)) return new Point[0];
        return new Point[]{TextGeometry.point(left * cos - top * sin, left * sin + top * cos), TextGeometry.point(right * cos - top * sin, right * sin + top * cos), TextGeometry.point(right * cos - bottom * sin, right * sin + bottom * cos), TextGeometry.point(left * cos - bottom * sin, left * sin + bottom * cos)};
    }

    @Override
    public String getLanguage() {
        Map<String, Integer> counts = new HashMap<>();
        String best = "und";
        int bestCount = 0;
        for (Line line : lines) {
            String language = line.getLanguage();
            Integer previous = counts.get(language);
            int count = previous == null ? 1 : previous + 1;
            counts.put(language, count);
            if (count > bestCount) {
                best = language;
                bestCount = count;
            }
        }
        return best;
    }

    @Override
    public String getValue() {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) value.append('\n');
            value.append(lines.get(i).getValue());
        }
        return value.toString();
    }
}
