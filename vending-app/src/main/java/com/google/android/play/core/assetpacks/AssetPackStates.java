/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks;

import java.util.Map;

/**
 * Contains the state for all requested packs.
 */
public abstract class AssetPackStates {
    /**
     * Returns a map from a pack's name to its state.
     */
    public abstract Map<String, AssetPackState> packStates();

    /**
     * Returns total size of all requested packs in bytes.
     */
    public abstract long totalBytes();
}
