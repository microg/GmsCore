/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.face;

import android.graphics.PointF;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import org.microg.gms.utils.ToStringHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represent a face landmark. A landmark is a point on a detected face, such as an eye, nose, or mouth.
 * <p>
 * When 'left' and 'right' are used, they are relative to the subject in the image. For example, the {@link #LEFT_EYE} landmark is the subject's left eye,
 * not the eye that is on the left when viewing the image.
 */
public class FaceLandmark {
    /**
     * The center of the subject's bottom lip.
     */
    public static final int MOUTH_BOTTOM = 0;
    /**
     * The midpoint between the subject's left mouth corner and the outer corner of the subject's left eye. For full profile faces, this becomes the
     * centroid of the nose base, nose tip, left ear lobe and left ear tip.
     */
    public static final int LEFT_CHEEK = 1;
    /**
     * The midpoint of the subject's left ear tip and left ear lobe.
     */
    public static final int LEFT_EAR = 3;
    /**
     * The center of the subject's left eye cavity.
     */
    public static final int LEFT_EYE = 4;
    /**
     * The subject's left mouth corner where the lips meet.
     */
    public static final int MOUTH_LEFT = 5;
    /**
     * The midpoint between the subject's nostrils where the nose meets the face.
     */
    public static final int NOSE_BASE = 6;
    /**
     * The midpoint between the subject's right mouth corner and the outer corner of the subject's right eye. For full profile faces, this becomes the
     * centroid of the nose base, nose tip, right ear lobe and right ear tip.
     */
    public static final int RIGHT_CHEEK = 7;
    /**
     * The midpoint of the subject's right ear tip and right ear lobe.
     */
    public static final int RIGHT_EAR = 9;
    /**
     * The center of the subject's right eye cavity.
     */
    public static final int RIGHT_EYE = 10;
    /**
     * The subject's right mouth corner where the lips meet.
     */
    public static final int MOUTH_RIGHT = 11;

    @Retention(RetentionPolicy.CLASS)
    @IntDef(value = {MOUTH_BOTTOM, LEFT_CHEEK, LEFT_EAR, LEFT_EYE, MOUTH_LEFT, NOSE_BASE, RIGHT_CHEEK, RIGHT_EAR, RIGHT_EYE, MOUTH_RIGHT})
    public @interface LandmarkType {
    }

    private final @LandmarkType int type;
    @NonNull
    private final PointF position;

    FaceLandmark(@LandmarkType int type, @NonNull PointF position) {
        this.type = type;
        this.position = position;
    }

    /**
     * Gets the {@link FaceLandmark.LandmarkType} type.
     */
    @LandmarkType
    public int getLandmarkType() {
        return type;
    }

    /**
     * Gets a 2D point for landmark position, where (0, 0) is the upper-left corner of the image. The point is guaranteed to be within the bounds of
     * the image.
     */
    @NonNull
    public PointF getPosition() {
        return position;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("FaceLandmark").field("type", type).field("position", position).toString();
    }
}
