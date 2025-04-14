/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.face;

import android.graphics.Rect;

import android.util.SparseArray;
import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.mlkit.vision.face.aidls.ContourParcel;
import com.google.mlkit.vision.face.aidls.FaceParcel;
import com.google.mlkit.vision.face.aidls.LandmarkParcel;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

import java.util.ArrayList;
import java.util.List;

public class Face {
    @NonNull
    private final Rect boundingBox;
    private final int trackingId;
    private final float rightEyeOpenProbability;
    private final float leftEyeOpenProbability;
    private final float smileProbability;
    private final float eulerX;
    private final float eulerY;
    private final float eulerZ;
    @NonNull
    private final SparseArray<FaceLandmark> landmarks = new SparseArray<>();
    @NonNull
    private final SparseArray<FaceContour> contours = new SparseArray<>();

    private static boolean isValidLandmarkType(int landmarkType) {
        return landmarkType == 0 || landmarkType == 1 || (landmarkType >= 3 && landmarkType <= 7) || (landmarkType >= 9 && landmarkType <= 11);
    }

    private static boolean isValidContourType(int contourType) {
        return contourType >= 1 && contourType <= 15;
    }

    @Hide
    public Face(FaceParcel faceParcel) {
        boundingBox = faceParcel.boundingBox;
        trackingId = faceParcel.id;
        for (LandmarkParcel landmarkParcel : faceParcel.landmarkParcelList) {
            if (isValidLandmarkType(landmarkParcel.type)) {
                landmarks.put(landmarkParcel.type, new FaceLandmark(landmarkParcel.type, landmarkParcel.position));
            }
        }
        for (ContourParcel contourParcel : faceParcel.contourParcelList) {
            if (isValidContourType(contourParcel.type)) {
                contours.put(contourParcel.type, new FaceContour(contourParcel.type, contourParcel.pointsList));
            }
        }
        eulerX = faceParcel.tiltAngle;
        eulerY = faceParcel.panAngle;
        eulerZ = faceParcel.rollAngle;
        smileProbability = faceParcel.smileProbability;
        leftEyeOpenProbability = faceParcel.leftEyeOpenProbability;
        rightEyeOpenProbability = faceParcel.rightEyeOpenProbability;
    }

    /**
     * Gets a list of all available {@link FaceContour}s. All {@link FaceContour}s are defined in {@link FaceContour.ContourType}. If no contours are available, an
     * empty list is returned.
     */
    @NonNull
    public List<FaceContour> getAllContours() {
        List<FaceContour> list = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            list.add(contours.valueAt(i));
        }
        return list;
    }

    /**
     * Gets a list of all available {@link FaceLandmark}s. All possible {@link FaceLandmark}s are defined in {@link FaceLandmark.LandmarkType}. If no landmarks are
     * available, an empty list is returned.
     */
    @NonNull
    public List<FaceLandmark> getAllLandmarks() {
        List<FaceLandmark> list = new ArrayList<>();
        for (int i = 0; i < landmarks.size(); i++) {
            list.add(landmarks.valueAt(i));
        }
        return list;
    }

    /**
     * Returns the {@code NonNull} axis-aligned bounding rectangle of the detected face.
     */
    @NonNull
    public Rect getBoundingBox() {
        return boundingBox;
    }

    /**
     * Gets contour based on the provided {@link FaceContour.ContourType}. It returns {@code null} if the contour is not available.
     */
    @Nullable
    public FaceContour getContour(@FaceContour.ContourType int contourType) {
        return contours.get(contourType);
    }

    /**
     * Returns the rotation of the face about the horizontal axis of the image, in degrees. Positive euler X is the face is looking up.
     *
     * @return the rotation of the face about the horizontal axis of the image
     */
    public Float getHeadEulerAngleX() {
        return eulerX;
    }

    /**
     * Returns the rotation of the face about the vertical axis of the image, in degrees. Positive euler y is when the face turns toward the right side
     * of the image that is being processed.
     *
     * @return the rotation of the face about the vertical axis of the image
     */
    public Float getHeadEulerAngleY() {
        return eulerY;
    }

    /**
     * Returns the rotation of the face about the axis pointing out of the image, in degrees. Positive euler z is a counter-clockwise rotation within the image plane.
     */
    public Float getHeadEulerAngleZ() {
        return eulerZ;
    }

    /**
     * Gets a {@link FaceLandmark} based on the provided {@link FaceLandmark.LandmarkType}. It returns {@code null} if the landmark type is not available.
     */
    public FaceLandmark getLandmark(@FaceLandmark.LandmarkType int landmarkType) {
        return landmarks.get(landmarkType);
    }

    /**
     * Returns a value between 0.0 and 1.0 giving a probability that the face's left eye is open. This returns {@code null} if the probability was not
     * computed. The probability is not computed if classification is not enabled via
     * {@link FaceDetectorOptions.Builder#setClassificationMode(int)} or the feature is not available.
     */
    public Float getLeftEyeOpenProbability() {
        if (leftEyeOpenProbability < 0.0f || leftEyeOpenProbability > 1.0f) {
            return null;
        }
        return leftEyeOpenProbability;
    }

    /**
     * Returns a value between 0.0 and 1.0 giving a probability that the face's right eye is open. This returns {@code null} if the probability was not
     * computed. The probability is not computed if classification is not enabled via
     * {@link FaceDetectorOptions.Builder#setClassificationMode(int)} or the feature is not available.
     */
    public Float getRightEyeOpenProbability() {
        if (rightEyeOpenProbability < 0.0f || rightEyeOpenProbability > 1.0f) {
            return null;
        }
        return rightEyeOpenProbability;
    }

    /**
     * Returns a value between 0.0 and 1.0 giving a probability that the face is smiling. This returns {@code null} if the probability was not computed.
     * The probability is not computed if classification is not enabled via {@link FaceDetectorOptions.Builder#setClassificationMode(int)} or the
     * required landmarks are not found.
     */
    public Float getSmilingProbability() {
        if (smileProbability < 0.0f || smileProbability > 1.0f) {
            return null;
        }
        return smileProbability;
    }

    /**
     * Returns the tracking ID if the tracking is enabled. Otherwise, returns {@code null}.
     */
    public Integer getTrackingId() {
        if (trackingId == -1) {
            return null;
        }
        return trackingId;
    }

    @Override
    public String toString() {
        return ToStringHelper.name("Face")
                .field("boundingBox", boundingBox)
                .field("trackingId", trackingId)
                .field("rightEyeOpenProbability", rightEyeOpenProbability)
                .field("leftEyeOpenProbability", leftEyeOpenProbability)
                .field("smileProbability", smileProbability)
                .field("eulerX", eulerX)
                .field("eulerY", eulerY)
                .field("eulerZ", eulerZ)
                .field("landmarks", landmarks)
                .field("contours", contours)
                .toString();
    }
}
