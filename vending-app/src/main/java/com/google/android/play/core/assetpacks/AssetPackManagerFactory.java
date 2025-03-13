/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks;

import android.content.Context;
import androidx.annotation.NonNull;

/**
 * Creates instances of {@link AssetPackManager}.
 */
public final class AssetPackManagerFactory {
    private AssetPackManagerFactory() {
    }

    /**
     * Creates an instance of {@link AssetPackManager}.
     *
     * @param applicationContext a fully initialized application context
     */
    @NonNull
    public static AssetPackManager getInstance(Context applicationContext) {
        return new AssetPackManagerImpl();
    }
}
