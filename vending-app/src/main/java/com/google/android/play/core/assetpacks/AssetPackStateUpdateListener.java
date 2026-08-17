/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks;

import com.google.android.play.core.listener.StateUpdateListener;

/**
 * Listener that may be registered for updates on the state of the download of asset packs.
 */
public interface AssetPackStateUpdateListener extends StateUpdateListener<AssetPackState> {
}
