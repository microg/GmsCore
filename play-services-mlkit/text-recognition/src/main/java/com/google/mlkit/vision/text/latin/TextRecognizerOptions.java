/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.text.latin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

import java.util.Objects;
import java.util.concurrent.Executor;

@PublicApi
public class TextRecognizerOptions implements TextRecognizerOptionsInterface {
    private static final String DEFAULT_CONFIG_LABEL = "taser_tflite_gocrlatin_mbv2_scriptid_aksara_layout_gcn_mobile";

    @NonNull
    public static final TextRecognizerOptions DEFAULT_OPTIONS = new Builder().build();

    @Nullable
    private final Executor executor;
    private final String configLabel;

    @Hide
    public TextRecognizerOptions(@Nullable Executor executor, String configLabel) {
        this.executor = executor;
        this.configLabel = configLabel;
    }

    @Hide
    @Override
    @LanguageOption
    public int getLoggingLanguageOption() {
        return LATIN;
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
        return "play-services-mlkit-text-recognition";
    }

    @Hide
    @Override
    public String getLoggingLibraryNameForOptionalModule() {
        return "optional-module-text-latin";
    }

    @Hide
    @Override
    public String getConfigLabel() {
        return configLabel;
    }

    @Hide
    @Override
    public String getLanguageHint() {
        return "en";
    }

    @Hide
    @Override
    public boolean getIsThickClient() {
        return false;
    }

    @Hide
    @Override
    public String getModuleId() {
        return "com.google.android.gms.vision.ocr";
    }

    @Hide
    @Override
    public int getLoggingEventId() {
        return 24306;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof TextRecognizerOptions && Objects.equals(executor, ((TextRecognizerOptions) other).executor);
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
        public TextRecognizerOptions build() {
            return new TextRecognizerOptions(executor, DEFAULT_CONFIG_LABEL);
        }
    }
}
