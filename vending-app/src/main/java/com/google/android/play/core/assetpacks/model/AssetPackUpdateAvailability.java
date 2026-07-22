/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks.model;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.CLASS)
@IntDef({AssetPackUpdateAvailability.UNKNOWN, AssetPackUpdateAvailability.UPDATE_NOT_AVAILABLE, AssetPackUpdateAvailability.UPDATE_AVAILABLE})
public @interface AssetPackUpdateAvailability {
    int UNKNOWN = 0;
    int UPDATE_NOT_AVAILABLE = 1;
    int UPDATE_AVAILABLE = 2;
}
