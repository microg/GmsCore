/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.SOURCE)
@IntDef({ThrottleBehavior.THROTTLE_BACKGROUND, ThrottleBehavior.THROTTLE_ALWAYS, ThrottleBehavior.THROTTLE_NEVER})
public @interface ThrottleBehavior {
    int THROTTLE_BACKGROUND = 0;
    int THROTTLE_ALWAYS = 1;
    int THROTTLE_NEVER = 2;
}
