/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.postprocess

import org.microg.gms.vision.text.paddle.model.OCRBox

object BoxSorter {
    private const val ROW_THRESHOLD_Y = 10f

    fun sortInReadingOrder(boxes: List<OCRBox>): List<OCRBox> {
        if (boxes.size <= 1) {
            return boxes
        }

        val list = boxes.toMutableList()
        list.sortWith(compareBy({ it.points[0].y }, { it.points[0].x }))

        for (i in 0 until list.size - 1) {
            var j = i
            while (j >= 0) {
                val next = list[j + 1]
                val curr = list[j]
                if (kotlin.math.abs(next.points[0].y - curr.points[0].y) < ROW_THRESHOLD_Y &&
                    next.points[0].x < curr.points[0].x
                ) {
                    list[j] = next
                    list[j + 1] = curr
                    j--
                } else {
                    break
                }
            }
        }
        return list
    }
}
