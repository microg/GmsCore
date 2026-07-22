/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification.ui

import android.view.View
import androidx.annotation.AttrRes
import androidx.databinding.BindingAdapter
import org.microg.gms.ui.resolveColor

@BindingAdapter("app:backgroundColorAttr")
fun View.setBackgroundColorAttribute(@AttrRes resId: Int) = context.resolveColor(resId)?.let { setBackgroundColor(it) }
