/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.text;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

@PublicApi
public interface TextRecognizerOptionsInterface {
    int LATIN = 1;
    int LATIN_AND_CHINESE = 2;
    int LATIN_AND_DEVANAGARI = 3;
    int LATIN_AND_JAPANESE = 4;
    int LATIN_AND_KOREAN = 5;

    @Hide
    @Nullable
    Executor getExecutor();

    @Hide
    String getLoggingLibraryName();

    @Hide
    String getLoggingLibraryNameForOptionalModule();

    @Hide
    String getConfigLabel();

    @Hide
    @LanguageOption
    int getLoggingLanguageOption();

    @Hide
    String getLanguageHint();

    @Hide
    boolean getIsThickClient();

    @Hide
    String getModuleId();

    @Hide
    int getLoggingEventId();

    @PublicApi
    @IntDef({LATIN, LATIN_AND_CHINESE, LATIN_AND_DEVANAGARI, LATIN_AND_JAPANESE, LATIN_AND_KOREAN})
    @Retention(RetentionPolicy.SOURCE)
    @interface LanguageOption {
    }
}
