/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks;

import android.app.Activity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.assetpacks.model.AssetPackStatus;
import org.microg.gms.common.Hide;

import java.util.List;
import java.util.Map;

@Hide
public class AssetPackManagerImpl implements AssetPackManager {
    @Override
    public AssetPackStates cancel(@NonNull List<String> packNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearListeners() {
    }

    @Override
    public Task<AssetPackStates> fetch(List<String> packNames) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public AssetLocation getAssetLocation(@NonNull String packName, @NonNull String assetPath) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public AssetPackLocation getPackLocation(@NonNull String packName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, AssetPackLocation> getPackLocations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Task<AssetPackStates> getPackStates(List<String> packNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerListener(@NonNull AssetPackStateUpdateListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Task<Void> removePack(@NonNull String packName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean showCellularDataConfirmation(@NonNull ActivityResultLauncher<IntentSenderRequest> activityResultLauncher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Task<Integer> showCellularDataConfirmation(@NonNull Activity activity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean showConfirmationDialog(@NonNull ActivityResultLauncher<IntentSenderRequest> activityResultLauncher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Task<Integer> showConfirmationDialog(@NonNull Activity activity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterListener(@NonNull AssetPackStateUpdateListener listener) {

    }

    public @AssetPackStatus int getLocalStatus(String packName, int remoteStatus) {
        throw new UnsupportedOperationException();
    }

    public int getTransferProgressPercentage(String packName) {
        throw new UnsupportedOperationException();
    }

    public String getInstalledVersionTag(String packName) {
        throw new UnsupportedOperationException();
    }
}
