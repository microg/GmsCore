/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.libraries.entitlement.utils;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Http constants using for entitlement flow of TS.43. */
public final class HttpConstants {
    /** HTTP content is unknown. */
    public static final int UNKNOWN = -1;

    /** HTTP content is JSON. */
    public static final int JSON = 0;

    /** HTTP content is XML. */
    public static final int XML = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({UNKNOWN, JSON, XML})
    public @interface ContentType {}

    private HttpConstants() {}
}