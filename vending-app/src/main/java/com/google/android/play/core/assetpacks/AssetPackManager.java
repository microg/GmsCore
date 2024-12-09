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

import java.util.List;
import java.util.Map;

/**
 * Manages downloads of asset packs.
 */
public interface AssetPackManager {

    /**
     * Requests to cancel the download of the specified asset packs.
     * <p>
     * Note: Only active downloads can be canceled.
     *
     * @return The new state for all specified packs.
     */
    AssetPackStates cancel(@NonNull List<String> packNames);

    /**
     * Unregisters all listeners previously added using {@link #registerListener}.
     */
    void clearListeners();

    /**
     * Requests to download the specified asset packs.
     * <p>
     * This method will fail if the app is not in the foreground.
     *
     * @return the state of all specified pack names
     */
    Task<AssetPackStates> fetch(List<String> packNames);

    /**
     * [advanced API] Returns the location of an asset in a pack, or {@code null} if the asset is not present in the given pack.
     * <p>
     * You don't need to use this API for common use-cases: you can use the standard File API for accessing assets from
     * asset packs that were extracted into the filesystem; and you can use Android's AssetManager API to access assets
     * from packs that were installed as APKs.
     * <p>
     * This API is useful for game engines that don't use Asset Manager and for developers that want a unified method to
     * access assets, independently from the delivery mode.
     */
    @Nullable
    AssetLocation getAssetLocation(@NonNull String packName, @NonNull String assetPath);

    /**
     * Returns the location of the specified asset pack on the device or {@code null} if this pack is not downloaded.
     * <p>
     * The files found at this path should not be modified.
     */
    @Nullable
    AssetPackLocation getPackLocation(@NonNull String packName);

    /**
     * Returns the location of all installed asset packs as a mapping from the asset pack name to an {@link AssetPackLocation}.
     * <p>
     * The files found at these paths should not be modified.
     */
    Map<String, AssetPackLocation> getPackLocations();

    /**
     * Requests download state or details for the specified asset packs.
     * <p>
     * Do not use this method to determine whether an asset pack is downloaded. Instead use {@link #getPackLocation}.
     */
    Task<AssetPackStates> getPackStates(List<String> packNames);

    /**
     * Registers a listener that will be notified of changes to the state of pack downloads for this app. Listeners should be
     * subsequently unregistered using {@link #unregisterListener}.
     */
    void registerListener(@NonNull AssetPackStateUpdateListener listener);

    /**
     * Deletes the specified asset pack from the internal storage of the app.
     * <p>
     * Use this method to delete asset packs instead of deleting files manually. This ensures that the Asset Pack will not be
     * re-downloaded during an app update.
     * <p>
     * If the asset pack is currently being downloaded or installed, this method does not cancel the process. For this case,
     * use {@link #cancel} instead.
     *
     * @return A task that will be successful only if files were successfully deleted.
     */

    Task<Void> removePack(@NonNull String packName);

    /**
     * Shows a confirmation dialog to resume all pack downloads that are currently in the
     * {@link AssetPackStatus#WAITING_FOR_WIFI} state. If the user accepts the dialog, packs are downloaded over cellular data.
     * <p>
     * The status of an asset pack is set to {@link AssetPackStatus#WAITING_FOR_WIFI} if the user is currently not on a Wi-Fi
     * connection and the asset pack is large or the user has set their download preference in the Play Store to only
     * download apps over Wi-Fi. By showing this dialog, your app can ask the user if they accept downloading the asset
     * pack over cellular data instead of waiting for Wi-Fi.
     * <p>
     * The confirmation activity returns one of the following values:
     * <ul>
     *   <li>{@link Activity#RESULT_OK Activity#RESULT_OK} if the user accepted.
     *   <li>{@link Activity#RESULT_CANCELED Activity#RESULT_CANCELED} if the user denied or the dialog has been closed in any other way (e.g.
     *   backpress).
     * </ul>
     *
     * @param activityResultLauncher an activityResultLauncher to launch the confirmation dialog.
     * @return whether the confirmation dialog has been started.
     * @deprecated This API has been deprecated in favor of {@link #showConfirmationDialog(ActivityResultLauncher)}.
     */
    @Deprecated
    boolean showCellularDataConfirmation(@NonNull ActivityResultLauncher<IntentSenderRequest> activityResultLauncher);

    /**
     * Shows a confirmation dialog to resume all pack downloads that are currently in the
     * {@link AssetPackStatus#WAITING_FOR_WIFI} state. If the user accepts the dialog, packs are downloaded over cellular data.
     * <p>
     * The status of an asset pack is set to {@link AssetPackStatus#WAITING_FOR_WIFI} if the user is currently not on a Wi-Fi
     * connection and the asset pack is large or the user has set their download preference in the Play Store to only
     * download apps over Wi-Fi. By showing this dialog, your app can ask the user if they accept downloading the asset
     * pack over cellular data instead of waiting for Wi-Fi.
     *
     * @param activity the activity on top of which the confirmation dialog is displayed. Use your current
     *                 activity for this.
     * @return A {@link Task} that completes once the dialog has been accepted, denied or closed. A successful task
     * result contains one of the following values:
     * <ul>
     *   <li>{@link Activity#RESULT_OK Activity#RESULT_OK} if the user accepted.
     *   <li>{@link Activity#RESULT_CANCELED Activity#RESULT_CANCELED} if the user denied or the dialog has been closed in any other way (e.g.
     *   backpress).
     * </ul>
     * @deprecated This API has been deprecated in favor of {@link #showConfirmationDialog(Activity)}.
     */
    @Deprecated
    Task<Integer> showCellularDataConfirmation(@NonNull Activity activity);

    /**
     * Shows a dialog that asks the user for consent to download packs that are currently in either the
     * {@link AssetPackStatus#REQUIRES_USER_CONFIRMATION} state or the {@link AssetPackStatus#WAITING_FOR_WIFI} state.
     * <p>
     * If the app has not been installed by Play, an update may be triggered to ensure that a valid version is installed. This
     * will cause the app to restart and all asset requests to be cancelled. These assets should be requested again after the
     * app restarts.
     * <p>
     * The confirmation activity returns one of the following values:
     * <ul>
     *   <li>{@link Activity#RESULT_OK Activity#RESULT_OK} if the user accepted.
     *   <li>{@link Activity#RESULT_CANCELED Activity#RESULT_CANCELED} if the user denied or the dialog has been closed in any other way (e.g.
     *   backpress).
     * </ul>
     *
     * @param activityResultLauncher an activityResultLauncher to launch the confirmation dialog.
     * @return whether the confirmation dialog has been started.
     */
    boolean showConfirmationDialog(@NonNull ActivityResultLauncher<IntentSenderRequest> activityResultLauncher);

    /**
     * Shows a dialog that asks the user for consent to download packs that are currently in either the
     * {@link AssetPackStatus#REQUIRES_USER_CONFIRMATION} state or the {@link AssetPackStatus#WAITING_FOR_WIFI} state.
     * <p>
     * If the app has not been installed by Play, an update may be triggered to ensure that a valid version is installed. This
     * will cause the app to restart and all asset requests to be cancelled. These assets should be requested again after the
     * app restarts.
     *
     * @param activity the activity on top of which the confirmation dialog is displayed. Use your current
     *                 activity for this.
     * @return A {@link Task} that completes once the dialog has been accepted, denied or closed. A successful task
     * result contains one of the following values:
     * <ul>
     *   <li>{@link Activity#RESULT_OK Activity#RESULT_OK} if the user accepted.
     *   <li>{@link Activity#RESULT_CANCELED Activity#RESULT_CANCELED} if the user denied or the dialog has been closed in any other way (e.g.
     *   backpress).
     * </ul>
     */
    Task<Integer> showConfirmationDialog(@NonNull Activity activity);

    /**
     * Unregisters a listener previously added using {@link #registerListener}.
     */
    void unregisterListener(@NonNull AssetPackStateUpdateListener listener);
}
