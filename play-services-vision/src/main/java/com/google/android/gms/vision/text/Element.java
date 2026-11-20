/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text;

import android.graphics.Point;
import android.graphics.Rect;

import com.google.android.gms.vision.text.internal.client.WordBoxParcel;

import org.microg.gms.common.PublicApi;

import java.util.Collections;
import java.util.List;

@PublicApi
public class Element implements Text {
    private final WordBoxParcel word;

    Element(WordBoxParcel word) {
        this.word = word;
    }

    @Override
    public Rect getBoundingBox() {
        return TextGeometry.bounds(getCornerPoints());
    }

    @Override
    public List<? extends Text> getComponents() {
        return Collections.emptyList();
    }

    @Override
    public Point[] getCornerPoints() {
        return TextGeometry.corners(word.boundingBox);
    }

    @Override
    public String getLanguage() {
        return TextGeometry.language(word.language);
    }

    @Override
    public String getValue() {
        return word.value == null ? "" : word.value;
    }
}
