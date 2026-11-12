/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.text;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.text.aidls.TextBlockParcel;
import com.google.mlkit.vision.text.aidls.TextElementParcel;
import com.google.mlkit.vision.text.aidls.TextLineParcel;
import com.google.mlkit.vision.text.aidls.TextParcel;
import com.google.mlkit.vision.text.aidls.TextSymbolParcel;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@PublicApi
public class Text {
    private final String text;
    private final List<TextBlock> textBlocks;

    @Hide
    public Text(@NonNull TextParcel result) {
        this(result, null);
    }

    @Hide
    public Text(@NonNull TextParcel result, @Nullable Matrix matrix) {
        Matrix transform = matrix == null ? null : new Matrix(matrix);
        if (transform != null && transform.isIdentity()) transform = null;
        text = nonNullText(result.text);
        List<TextBlock> blocks = new ArrayList<>();
        if (result.blocks != null) {
            for (TextBlockParcel block : result.blocks) {
                if (block != null) blocks.add(new TextBlock(block, transform));
            }
        }
        textBlocks = Collections.unmodifiableList(blocks);
    }

    @NonNull
    public String getText() {
        return text;
    }

    @NonNull
    public List<TextBlock> getTextBlocks() {
        return textBlocks;
    }

    @PublicApi
    public static class TextBlock {
        private final String text;
        private final Rect boundingBox;
        private final Point[] cornerPoints;
        private final String language;
        private final List<Line> lines;

        private TextBlock(TextBlockParcel parcel, Matrix transform) {
            text = nonNullText(parcel.text);
            boundingBox = copyRect(parcel.boundingBox, transform);
            cornerPoints = copyPoints(parcel.cornerPoints, transform);
            language = nonNullLanguage(parcel.language);
            List<Line> children = new ArrayList<>();
            if (parcel.lines != null) {
                for (TextLineParcel line : parcel.lines) {
                    if (line != null) children.add(new Line(line, transform));
                }
            }
            lines = Collections.unmodifiableList(children);
        }

        @Nullable
        public Rect getBoundingBox() {
            return copyRect(boundingBox);
        }

        @Nullable
        public Point[] getCornerPoints() {
            return copyPoints(cornerPoints);
        }

        @NonNull
        public synchronized List<Line> getLines() {
            return lines;
        }

        @NonNull
        public String getRecognizedLanguage() {
            return language;
        }

        @NonNull
        public String getText() {
            return text;
        }
    }

    @PublicApi
    public static class Line {
        private final String text;
        private final Rect boundingBox;
        private final Point[] cornerPoints;
        private final String language;
        private final float confidence;
        private final float angle;
        private final List<Element> elements;

        private Line(TextLineParcel parcel, Matrix transform) {
            text = nonNullText(parcel.text);
            boundingBox = copyRect(parcel.boundingBox, transform);
            cornerPoints = copyPoints(parcel.cornerPoints, transform);
            language = nonNullLanguage(parcel.language);
            confidence = parcel.confidence;
            angle = transformedAngle(parcel.angle, cornerPoints, transform);
            List<Element> children = new ArrayList<>();
            if (parcel.elements != null) {
                for (TextElementParcel element : parcel.elements) {
                    if (element != null) children.add(new Element(element, transform));
                }
            }
            elements = Collections.unmodifiableList(children);
        }

        public float getAngle() {
            return angle;
        }

        @Nullable
        public Rect getBoundingBox() {
            return copyRect(boundingBox);
        }

        public float getConfidence() {
            return confidence;
        }

        @Nullable
        public Point[] getCornerPoints() {
            return copyPoints(cornerPoints);
        }

        @NonNull
        public synchronized List<Element> getElements() {
            return elements;
        }

        @NonNull
        public String getRecognizedLanguage() {
            return language;
        }

        @NonNull
        public String getText() {
            return text;
        }
    }

    @PublicApi
    public static class Element {
        private final String text;
        private final Rect boundingBox;
        private final Point[] cornerPoints;
        private final String language;
        private final float confidence;
        private final float angle;
        private final List<Symbol> symbols;

        private Element(TextElementParcel parcel, Matrix transform) {
            text = nonNullText(parcel.text);
            boundingBox = copyRect(parcel.boundingBox, transform);
            cornerPoints = copyPoints(parcel.cornerPoints, transform);
            language = nonNullLanguage(parcel.language);
            confidence = parcel.confidence;
            angle = transformedAngle(parcel.angle, cornerPoints, transform);
            List<Symbol> children = new ArrayList<>();
            if (parcel.symbols != null) {
                for (TextSymbolParcel symbol : parcel.symbols) {
                    if (symbol != null) children.add(new Symbol(symbol, language, transform));
                }
            }
            symbols = Collections.unmodifiableList(children);
        }

        public float getAngle() {
            return angle;
        }

        @Nullable
        public Rect getBoundingBox() {
            return copyRect(boundingBox);
        }

        public float getConfidence() {
            return confidence;
        }

        @Nullable
        public Point[] getCornerPoints() {
            return copyPoints(cornerPoints);
        }

        @NonNull
        public String getRecognizedLanguage() {
            return language;
        }

        @NonNull
        public synchronized List<Symbol> getSymbols() {
            return symbols;
        }

        @NonNull
        public String getText() {
            return text;
        }
    }

    @PublicApi
    public static class Symbol {
        private final String text;
        private final Rect boundingBox;
        private final Point[] cornerPoints;
        private final String language;
        private final float confidence;
        private final float angle;

        private Symbol(TextSymbolParcel parcel, String language, Matrix transform) {
            text = nonNullText(parcel.text);
            boundingBox = copyRect(parcel.boundingBox, transform);
            cornerPoints = copyPoints(parcel.cornerPoints, transform);
            this.language = language;
            confidence = parcel.confidence;
            angle = transformedAngle(parcel.angle, cornerPoints, transform);
        }

        public float getAngle() {
            return angle;
        }

        @Nullable
        public Rect getBoundingBox() {
            return copyRect(boundingBox);
        }

        public float getConfidence() {
            return confidence;
        }

        @Nullable
        public Point[] getCornerPoints() {
            return copyPoints(cornerPoints);
        }

        @NonNull
        public String getRecognizedLanguage() {
            return language;
        }

        @NonNull
        public String getText() {
            return text;
        }
    }

    private static String nonNullText(String value) {
        return value == null ? "" : value;
    }

    private static String nonNullLanguage(String value) {
        return value == null || value.isEmpty() ? "und" : value;
    }

    private static Rect copyRect(Rect value) {
        return copyRect(value, null);
    }

    private static Rect copyRect(Rect value, Matrix transform) {
        if (value == null) return null;
        if (transform == null) return new Rect(value);
        RectF mapped = new RectF(value);
        transform.mapRect(mapped);
        return new Rect((int) Math.floor(mapped.left), (int) Math.floor(mapped.top), (int) Math.ceil(mapped.right), (int) Math.ceil(mapped.bottom));
    }

    private static Point[] copyPoints(List<Point> values, Matrix transform) {
        if (values == null || values.isEmpty()) return null;
        Point[] copy = copyPoints(values.toArray(new Point[0]));
        if (copy == null || transform == null) return copy;
        float[] mapped = new float[copy.length * 2];
        for (int i = 0; i < copy.length; i++) {
            mapped[i * 2] = copy[i].x;
            mapped[i * 2 + 1] = copy[i].y;
        }
        transform.mapPoints(mapped);
        for (int i = 0; i < copy.length; i++) {
            copy[i].set(Math.round(mapped[i * 2]), Math.round(mapped[i * 2 + 1]));
        }
        return copy;
    }

    private static Point[] copyPoints(Point[] values) {
        if (values == null) return null;
        Point[] copy = new Point[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) return null;
            copy[i] = new Point(values[i]);
        }
        return copy;
    }

    private static float transformedAngle(float original, Point[] points, Matrix transform) {
        if (transform == null || points == null || points.length < 2 || points[0].equals(points[1])) return original;
        return (float) Math.toDegrees(Math.atan2((double) points[1].y - points[0].y, (double) points[1].x - points[0].x));
    }
}
