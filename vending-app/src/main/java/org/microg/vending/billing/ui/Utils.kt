/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing.ui

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.microg.vending.billing.TAG
import org.microg.vending.billing.core.ui.BGravity
import org.microg.vending.billing.core.ui.BImageInfo
import org.microg.vending.billing.core.ui.BTextInfo
import org.microg.vending.billing.core.ui.ColorType
import org.microg.vending.billing.core.ui.TextAlignmentType

fun getWindowWidth(context: Context): Int {
    val resources = context.resources
    return when (resources.configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> resources.displayMetrics.widthPixels
        Configuration.ORIENTATION_LANDSCAPE -> (resources.displayMetrics.widthPixels * 0.6).toInt()
        else -> resources.displayMetrics.widthPixels
    }
}

@Composable
fun DayNightColor(day: Long, night: Long): Color = if (isSystemInDarkTheme()) Color(night) else Color(day)

// TODO: Manage colors in theme
@Composable
fun getColorByType(t: ColorType?): Color? {
    if (t == null)
        return null
    return when (t) {
        ColorType.BACKGROUND_PRIMARY -> MaterialTheme.colorScheme.background
        ColorType.APPS_PRIMARY -> MaterialTheme.colorScheme.primary
        ColorType.APPS_2 -> DayNightColor(0xff5f6368, 0xffcae2ff)
        ColorType.APPS_3 -> DayNightColor(0xff5f6368, 0xff003b92)
        ColorType.MUSIC_3 -> Color.Transparent
        ColorType.TEXT_PRIMARY -> DayNightColor(0xff202124, 0xffe8eaed)
        ColorType.TEXT_SECONDARY -> DayNightColor(0xff5f6368, 0xff9aa0a6)
        ColorType.PRIMARY_BUTTON_LABEL_DISABLED -> DayNightColor(0xff5f6368, 0xff5b5e64)
        ColorType.ERROR_COLOR_PRIMARY -> MaterialTheme.colorScheme.error
        else -> {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "invalid color type $t")
            Color.Transparent
        }
    }
}

@Composable
fun getColorFilter(imageInfo: BImageInfo?): ColorFilter? {
    if (imageInfo?.colorFilterValue != null)
        return ColorFilter.tint(Color(imageInfo.colorFilterValue))
    if (imageInfo?.colorFilterType != null) {
        return when (imageInfo.colorFilterType) {
            21, 3 -> ColorFilter.tint(
                if (isSystemInDarkTheme()) Color(0xffe8eaed) else Color(
                    0xff202124
                )
            )

            else -> {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Unknown color filter type: ${imageInfo.colorFilterType}")
                null
            }
        }
    }
    return null
}

fun getContentScale(imageInfo: BImageInfo?): ContentScale? {
    return null
}

fun getFontSizeByType(t: Int?): TextUnit {
    return when (t) {
        10 -> 18.sp
        12 -> 16.sp
        14 -> 14.sp
        20 -> 14.sp
        21 -> 12.sp
        null -> 14.sp
        else -> {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "invalid font type $t")
            14.sp
        }
    }
}

fun getTextAlignment(textInfo: BTextInfo?): TextAlign {
    val gravitys = textInfo?.gravityList
    if (gravitys?.containsAll(listOf(BGravity.LEFT, BGravity.START)) == true)
        return TextAlign.Left
    if (gravitys?.containsAll(listOf(BGravity.CENTER, BGravity.CENTER_HORIZONTAL)) == true)
        return TextAlign.Center
    if (gravitys?.containsAll(listOf(BGravity.RIGHT, BGravity.END)) == true)
        return TextAlign.Right
    return when (val t = textInfo?.textAlignmentType) {
        TextAlignmentType.TEXT_ALIGNMENT_TEXT_END -> TextAlign.Right
        TextAlignmentType.TEXT_ALIGNMENT_TEXT_START -> TextAlign.Left
        TextAlignmentType.TEXT_ALIGNMENT_CENTER -> TextAlign.Center
        TextAlignmentType.TEXT_ALIGNMENT_VIEW_END -> TextAlign.Right
        null -> TextAlign.Left
        else -> {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "invalid text alignment type $t")
            TextAlign.Left
        }
    }
}