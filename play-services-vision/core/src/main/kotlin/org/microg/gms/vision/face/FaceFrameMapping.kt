/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.face

import android.graphics.PointF
import android.graphics.Rect
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.face.aidls.ContourParcel
import com.google.mlkit.vision.face.aidls.FaceParcel
import com.google.mlkit.vision.face.aidls.LandmarkParcel
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * [FaceDetectorHelper] rotates the incoming frame so that the face detector always sees an upright
 * image. Every coordinate it produces therefore lives in the *rotated* frame, while ML Kit - and
 * every consumer of it - expects results in the coordinate space of the image that was handed to
 * [com.google.mlkit.vision.face.FaceDetector.process].
 *
 * This maps such results back. [rotation] is the rotation of the input image in 90 degree steps,
 * clockwise (the same value as `FrameMetadataParcel.rotation`), [inputWidth] and [inputHeight] are
 * the dimensions of that unrotated image.
 */
object FaceFrameMapping {

    /** Maps a point from the rotated analysis frame back into the input frame. */
    fun mapPoint(point: PointF, rotation: Int, inputWidth: Int, inputHeight: Int): PointF =
        PointF(mapX(point.x, point.y, rotation, inputWidth, inputHeight), mapY(point.x, point.y, rotation, inputWidth, inputHeight))

    /** X component of [mapPoint], kept on plain floats so it can be unit tested. */
    fun mapX(x: Float, y: Float, rotation: Int, inputWidth: Int, inputHeight: Int): Float = when (rotation.mod(4)) {
        // 90 degrees clockwise: analysis(x', y') == input(y', inputHeight - 1 - x')
        1 -> y
        // 180 degrees
        2 -> inputWidth - 1f - x
        // 270 degrees clockwise
        3 -> inputWidth - 1f - y
        else -> x
    }

    /** Y component of [mapPoint], kept on plain floats so it can be unit tested. */
    fun mapY(x: Float, y: Float, rotation: Int, inputWidth: Int, inputHeight: Int): Float = when (rotation.mod(4)) {
        1 -> inputHeight - 1f - x
        2 -> inputHeight - 1f - y
        3 -> x
        else -> y
    }

    /**
     * Maps a rectangle. A quarter rotation both swaps and mirrors the corners, so all four are
     * mapped and re-combined, then clamped: ML Kit guarantees an in-bounds bounding box.
     */
    fun mapRect(rect: Rect, rotation: Int, inputWidth: Int, inputHeight: Int): Rect {
        if (rotation.mod(4) == 0) return Rect(rect).apply { clamp(inputWidth, inputHeight) }
        val corners = listOf(
            PointF(rect.left.toFloat(), rect.top.toFloat()),
            PointF(rect.right.toFloat(), rect.top.toFloat()),
            PointF(rect.left.toFloat(), rect.bottom.toFloat()),
            PointF(rect.right.toFloat(), rect.bottom.toFloat()),
        ).map { mapPoint(it, rotation, inputWidth, inputHeight) }
        return Rect(
            corners.minOf { it.x }.toInt(),
            corners.minOf { it.y }.toInt(),
            corners.maxOf { it.x }.toInt(),
            corners.maxOf { it.y }.toInt(),
        ).apply { clamp(inputWidth, inputHeight) }
    }

    private fun Rect.clamp(width: Int, height: Int) {
        if (left > right) {
            val tmp = left; left = right; right = tmp
        }
        if (top > bottom) {
            val tmp = top; top = bottom; bottom = tmp
        }
        left = left.coerceIn(0, width)
        right = right.coerceIn(0, width)
        top = top.coerceIn(0, height)
        bottom = bottom.coerceIn(0, height)
    }

    /**
     * Maps a whole detection: bounding box, landmarks and contours share the analysis frame.
     * Head euler angles are recomputed afterwards, because the rotation of the analysis frame
     * rotated the face inside it too.
     */
    fun mapDetection(face: FaceParcel, rotation: Int, inputWidth: Int, inputHeight: Int): FaceParcel {
        val landmarks = face.landmarkParcelList.map {
            LandmarkParcel(it.type, mapPoint(it.position, rotation, inputWidth, inputHeight))
        }
        val contours = face.contourParcelList.map { contour ->
            ContourParcel(contour.type, ArrayList(contour.pointsList.map { mapPoint(it, rotation, inputWidth, inputHeight) }))
        }
        val (roll, pan) = estimateEulerAngles(landmarks)
        return FaceParcel(
            face.id,
            mapRect(face.boundingBox, rotation, inputWidth, inputHeight),
            roll,
            pan,
            face.tiltAngle,
            face.leftEyeOpenProbability,
            face.rightEyeOpenProbability,
            face.smileProbability,
            face.confidenceScore,
            ArrayList(landmarks),
            ArrayList(contours),
        )
    }

    /**
     * Very rough head pose from the eyes and the nose base: good enough for consumers that expect
     * the angle to follow the head, without a pose model.
     *
     * Returns roll (euler Z, positive is a counter-clockwise rotation within the image plane) and
     * pan (euler Y, positive is the face turning towards the right side of the image).
     */
    private fun estimateEulerAngles(landmarks: List<LandmarkParcel>): Pair<Float, Float> {
        val left = landmarks.firstOrNull { it.type == FaceLandmark.LEFT_EYE } ?: return 0f to 0f
        val right = landmarks.firstOrNull { it.type == FaceLandmark.RIGHT_EYE } ?: return 0f to 0f
        val nose = landmarks.firstOrNull { it.type == FaceLandmark.NOSE_BASE } ?: return 0f to 0f
        val eyeDistance = hypot((right.position.x - left.position.x).toDouble(), (right.position.y - left.position.y).toDouble()).toFloat()
        if (eyeDistance <= 1f) return 0f to 0f
        // Screen coordinates grow downwards, so a positive heading here is a clockwise rotation on
        // screen, which is a negative euler Z.
        val roll = -Math.toDegrees(atan2((right.position.y - left.position.y).toDouble(), (right.position.x - left.position.x).toDouble())).toFloat()
        val noseOffset = (nose.position.x - (left.position.x + right.position.x) / 2f) / eyeDistance
        val pan = (noseOffset * 150f).coerceIn(-60f, 60f)
        return (if (abs(roll) < 0.5f) 0f else roll) to (if (abs(pan) < 0.5f) 0f else pan)
    }
}
