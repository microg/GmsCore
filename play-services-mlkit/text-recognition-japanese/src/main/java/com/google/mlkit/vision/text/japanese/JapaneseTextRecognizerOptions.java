/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.text.japanese;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

import java.util.Objects;
import java.util.concurrent.Executor;

@PublicApi
public final class JapaneseTextRecognizerOptions implements TextRecognizerOptionsInterface {
    @Nullable
    private final Executor executor;

    private JapaneseTextRecognizerOptions(@Nullable Executor executor) {
        this.executor = executor;
    }

    @Hide
    @Override
    @LanguageOption
    public int getLoggingLanguageOption() {
        return LATIN_AND_JAPANESE;
    }

    @Hide
    @Override
    @Nullable
    public Executor getExecutor() {
        return executor;
    }

    @Hide
    @Override
    public String getLoggingLibraryName() {
        return "play-services-mlkit-text-recognition-japanese";
    }

    @Hide
    @Override
    public String getLoggingLibraryNameForOptionalModule() {
        return "optional-module-text-japanese";
    }

    @Hide
    @Override
    public String getConfigLabel() {
        return "taser_tflite_gocrjapanese_and_latin_mbv2_aksara_layout_gcn_mobile";
    }

    @Hide
    @Override
    public String getLanguageHint() {
        return "ja";
    }

    @Hide
    @Override
    public boolean getIsThickClient() {
        return false;
    }

    @Hide
    @Override
    public String getModuleId() {
        return "com.google.android.gms.mlkit_ocr_japanese";
    }

    @Hide
    @Override
    public int getLoggingEventId() {
        return 24332;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof JapaneseTextRecognizerOptions && Objects.equals(executor, ((JapaneseTextRecognizerOptions) other).executor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executor);
    }

    @PublicApi
    public static class Builder {
        @Nullable
        private Executor executor;

        public Builder() {
        }

        @NonNull
        public Builder setExecutor(@NonNull Executor executor) {
            if (executor == null) throw new NullPointerException("Executor must not be null");
            this.executor = executor;
            return this;
        }

        @NonNull
        public JapaneseTextRecognizerOptions build() {
            return new JapaneseTextRecognizerOptions(executor);
        }
    }
}
