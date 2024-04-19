/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.microg.vending.billing.containsAny
import org.microg.vending.billing.core.ui.BGravity
import org.microg.vending.billing.core.ui.BViewInfo
import org.microg.vending.billing.core.ui.UIType

fun RowScope.applyAlignment(modifier: Modifier, viewInfo: BViewInfo?): Modifier {
    val gravitys = viewInfo?.gravityList ?: return modifier
    var newModifier = modifier
    if (gravitys.containsAny(listOf(BGravity.TOP))) {
        newModifier = newModifier.align(Alignment.Top)
    }
    if (gravitys.containsAny(listOf(BGravity.CENTER, BGravity.CENTER_VERTICAL))) {
        newModifier = newModifier.align(Alignment.CenterVertically)
    }
    if (gravitys.containsAny(listOf(BGravity.BOTTOM))) {
        newModifier = newModifier.align(Alignment.Bottom)
    }

    return newModifier
}

fun ColumnScope.applyAlignment(modifier: Modifier, viewInfo: BViewInfo?): Modifier {
    val gravitys = viewInfo?.gravityList ?: return modifier
    var newModifier = modifier
    if (gravitys.containsAny(listOf(BGravity.START, BGravity.LEFT))) {
        newModifier = newModifier.align(Alignment.Start)
    }
    if (gravitys.containsAny(listOf(BGravity.CENTER, BGravity.CENTER_HORIZONTAL))) {
        newModifier = newModifier.align(Alignment.CenterHorizontally)
    }
    if (gravitys.containsAny(listOf(BGravity.RIGHT, BGravity.END))) {
        newModifier = newModifier.align(Alignment.End)
    }

    return newModifier
}

fun Modifier.visibility(visibleType: Int): Modifier {
    val visible = when (visibleType) {
        1 -> false
        else -> true
    }
    return this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        if (visible) {
            layout(placeable.width, placeable.height) {
                // place this item in the original position
                placeable.placeRelative(0, 0)
            }
        } else {
            layout(0, 0) {}
        }
    }
}

fun Modifier.applySize(size: DpSize?): Modifier = composed {
    this.let { mit ->
        var newModifier = mit
        size?.let {
            newModifier = newModifier.size(size)
        }
        newModifier
    }
}

@Composable
fun Modifier.applyViewInfo(viewInfo: BViewInfo?): Modifier {
    return this.let { mit ->
        var newModifier = mit
        viewInfo?.visibilityType?.let {
            newModifier = newModifier.visibility(it)
        }
        viewInfo?.startMargin?.let {
            newModifier = newModifier.padding(start = Dp(it.takeIf { it > 0 } ?: 0f))
        }
        viewInfo?.topMargin?.let {
            newModifier = newModifier.padding(top = Dp(it.takeIf { it > 0 } ?: 0f))
        }
        viewInfo?.endMargin?.let {
            newModifier = newModifier.padding(end = Dp(it.takeIf { it > 0 } ?: 0f))
        }
        viewInfo?.bottomMargin?.let {
            newModifier = newModifier.padding(bottom = Dp(it.takeIf { it > 0 } ?: 0f))
        }
        viewInfo?.action?.let { action ->
            when (action.uiInfo?.uiType) {
                UIType.BILLING_PROFILE_SCREEN_ABANDON,
                UIType.PURCHASE_CART_PAYMENT_OPTIONS_LINK,
                UIType.BILLING_PROFILE_OPTION_REDEEM_CODE,
                UIType.BILLING_PROFILE_OPTION_ADD_PLAY_CREDIT,
                UIType.BILLING_PROFILE_OPTION_CREATE_INSTRUMENT -> {
                    val viewState = LocalBillingUiViewState.current
                    newModifier = newModifier.clickable {
                        viewState.onClickAction(action)
                    }
                }

                else -> {}
            }
        }
        viewInfo?.width?.let {
            newModifier = newModifier.width(Dp(it))
        }
        viewInfo?.height?.let {
            newModifier = newModifier.height(Dp(it))
        }
        getColorByType(viewInfo?.borderColorType)?.let {
            newModifier = newModifier.border(BorderStroke(1.dp, it), shape = RoundedCornerShape(5))
        }
        getColorByType(viewInfo?.backgroundColorType)?.let {
            newModifier = newModifier.background(it)
        }
        viewInfo?.startPadding?.let {
            newModifier = newModifier.padding(start = Dp(it.takeIf { it > 0 } ?: 0f))
        }
        viewInfo?.topPadding?.let {
            newModifier = newModifier.padding(top = Dp(it.takeIf { it > 0 } ?: 0f))
        }
        viewInfo?.endPadding?.let {
            newModifier = newModifier.padding(end = Dp(it.takeIf { it > 0 } ?: 0f))
        }
        viewInfo?.bottomPadding?.let {
            newModifier = newModifier.padding(bottom = Dp(it.takeIf { it > 0 } ?: 0f))
        }

        newModifier
    }
}

fun Modifier.applyUITypePadding(uiType: UIType?): Modifier {
    if (uiType == UIType.PURCHASE_ERROR_SCREEN) {
        return this.padding(top = 15.dp, start = 30.dp, end = 30.dp)
    }
    return this
}