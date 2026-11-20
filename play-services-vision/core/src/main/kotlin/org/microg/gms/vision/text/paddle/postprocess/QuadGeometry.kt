/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.postprocess

import org.opencv.core.Point

internal object QuadGeometry {
    fun orderMinAreaRectPoints(points: Array<Point>): List<Point> {
        val sorted = points.sortedBy { it.x }
        val topLeft: Point
        val bottomLeft: Point
        if (sorted[1].y > sorted[0].y) {
            topLeft = sorted[0]
            bottomLeft = sorted[1]
        } else {
            topLeft = sorted[1]
            bottomLeft = sorted[0]
        }

        val topRight: Point
        val bottomRight: Point
        if (sorted[3].y > sorted[2].y) {
            topRight = sorted[2]
            bottomRight = sorted[3]
        } else {
            topRight = sorted[3]
            bottomRight = sorted[2]
        }
        return listOf(topLeft, topRight, bottomRight, bottomLeft)
    }
}
