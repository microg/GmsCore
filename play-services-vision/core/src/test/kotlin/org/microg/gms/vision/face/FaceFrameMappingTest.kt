/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rotated analysis frame maps onto the input frame. Only the plain float entry points are
 * exercised here, so the test does not need any Android graphics classes.
 */
class FaceFrameMappingTest {

    private val width = 100
    private val height = 50

    /** Input frame -> rotated analysis frame, the inverse of what is under test. */
    private fun toAnalysis(x: Float, y: Float, rotation: Int): Pair<Float, Float> = when (rotation.mod(4)) {
        1 -> (height - 1f - y) to x
        2 -> (width - 1f - x) to (height - 1f - y)
        3 -> y to (width - 1f - x)
        else -> x to y
    }

    private fun map(x: Float, y: Float, rotation: Int): Pair<Float, Float> =
        FaceFrameMapping.mapX(x, y, rotation, width, height) to FaceFrameMapping.mapY(x, y, rotation, width, height)

    @Test
    fun unrotatedFrameIsUnchanged() {
        assertEquals(7f to 11f, map(7f, 11f, 0))
    }

    @Test
    fun cornersOfTheAnalysisFrameMapOntoCornersOfTheInputFrame() {
        // top-left of the analysis frame is the input top-left rotated into place
        assertEquals(0f to (height - 1f), map(0f, 0f, 1))
        assertEquals((width - 1f) to 0f, map(0f, 0f, 3))
        assertEquals((width - 1f) to (height - 1f), map(0f, 0f, 2))
    }

    @Test
    fun mappedPointsRoundTripThroughTheAnalysisFrame() {
        for (rotation in 0..3) {
            for (inputX in listOf(0f, 1f, width / 3f, width - 1f)) {
                for (inputY in listOf(0f, 2f, height / 2f, height - 1f)) {
                    val (analysisX, analysisY) = toAnalysis(inputX, inputY, rotation)
                    val (x, y) = map(analysisX, analysisY, rotation)
                    assertEquals("rotation=$rotation x", inputX, x, 0.001f)
                    assertEquals("rotation=$rotation y", inputY, y, 0.001f)
                }
            }
        }
    }

    @Test
    fun mappedPointsStayInsideTheInputFrame() {
        for (rotation in 0..3) {
            // the analysis frame is transposed for quarter rotations
            val analysisWidth = if (rotation.mod(2) == 0) width else height
            val analysisHeight = if (rotation.mod(2) == 0) height else width
            for (analysisX in listOf(0f, 1f, analysisWidth / 2f, analysisWidth - 1f)) {
                for (analysisY in listOf(0f, 1f, analysisHeight / 2f, analysisHeight - 1f)) {
                    val (x, y) = map(analysisX, analysisY, rotation)
                    assertTrue("rotation=$rotation x=$x", x in 0f..(width - 1f))
                    assertTrue("rotation=$rotation y=$y", y in 0f..(height - 1f))
                }
            }
        }
    }
}
