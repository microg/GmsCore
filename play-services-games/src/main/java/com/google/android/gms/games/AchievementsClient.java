/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.app.Activity;
import android.content.Intent;
import android.os.RemoteException;

import com.google.android.gms.common.data.AbstractDataBuffer;
import com.google.android.gms.games.achievement.AchievementBuffer;
import com.google.android.gms.tasks.Task;

/**
 * A client to interact with achievements functionality.
 */
public interface AchievementsClient {

    /**
     * Returns a {@link Task} which asynchronously loads an {@link Intent} to show the list of achievements for a game.
     * Note that the Intent returned from the {@code Task} must be invoked with {@link Activity#startActivityForResult(Intent, int)},
     * so that the identity of the calling package can be established.
     * <p>
     * The returned {@code Task} can fail with a {@link RemoteException}.
     */
    Task<Intent> getAchievementsIntent();

    /**
     * Increments an achievement by the given number of steps.
     * The achievement must be an incremental achievement. Once an achievement reaches at least the maximum number of steps,
     * it will be unlocked automatically. Any further increments will be ignored.
     * <p>
     * This is the fire-and-forget form of the API.
     * Use this form if you don't need to know the status of the operation immediately.
     * For most applications, this will be the preferred API to use, though note that the update may not be sent to the server until the next sync.
     * See {@link AchievementsClient#incrementImmediate(String, int)} if you need the operation to attempt to communicate to the server immediately or need to have the status code delivered to your application.
     *
     * @param id       The achievement ID to increment.
     * @param numSteps The number of steps to increment by. Must be greater than 0.
     */
    void increment(String id, int numSteps);

    /**
     * Returns a {@link Task} which asynchronously increments an achievement by the given number of steps.
     * The achievement must be an incremental achievement. Once an achievement reaches at least the maximum number of steps,
     * it will be unlocked automatically. Any further increments will be ignored.
     * <p>
     * This form of the API will attempt to update the user's achievement on the server immediately.
     * The {@link Boolean} in a successful response indicates whether the achievement is now unlocked.
     *
     * @param id       The ID of the achievement to increment.
     * @param numSteps The number of steps to increment by. Must be greater than 0.
     */
    Task<Boolean> incrementImmediate(String id, int numSteps);

    /**
     * Returns a {@link Task} which asynchronously loads an annotated {@link AchievementBuffer} that represents the achievement data for the currently signed-in player.
     * <p>
     * {@link AbstractDataBuffer#release()} should be called to release resources after usage.
     *
     * @param forceReload If true, this call will clear any locally cached data and attempt to fetch the latest data from the server.
     *                    This would commonly be used for something like a user-initiated refresh.
     *                    Normally, this should be set to false to gain advantages of data caching.
     */
    Task<AnnotatedData<AchievementBuffer>> load(boolean forceReload);

    /**
     * Reveals a hidden achievement to the currently signed-in player. If the achievement has already been unlocked, this will have no effect.
     * <p>
     * This is the fire-and-forget form of the API. Use this form if you don't need to know the status of the operation immediately.
     * For most applications, this will be the preferred API to use, though note that the update may not be sent to the server until the next sync.
     * See {@link #revealImmediate(String)} if you need the operation to attempt to communicate to the server immediately or need to have the status code delivered to your application.
     *
     * @param id The achievement ID to reveal.
     */
    void reveal(String id);

    /**
     * Returns a {@link Task} which asynchronously reveals a hidden achievement to the currently signed in player.
     * If the achievement is already visible, this will have no effect.
     * <p>
     * This form of the API will attempt to update the user's achievement on the server immediately.
     * The Task will complete successfully when the server has been updated.
     *
     * @param id The ID of the achievement to reveal.
     */
    Task<Void> revealImmediate(String id);

    /**
     * Sets an achievement to have at least the given number of steps completed.
     * Calling this method while the achievement already has more steps than the provided value is a no-op.
     * Once the achievement reaches the maximum number of steps, the achievement will automatically be unlocked, and any further mutation operations will be ignored.
     * <p>
     * This is the fire-and-forget form of the API. Use this form if you don't need to know the status of the operation immediately.
     * For most applications, this will be the preferred API to use, though note that the update may not be sent to the server until the next sync.
     * See {@link #setStepsImmediate(String, int)} if you need the operation to attempt to communicate to the server immediately or need to have the status code delivered to your application.
     *
     * @param id       The ID of the achievement to modify.
     * @param numSteps The number of steps to set the achievement to. Must be greater than 0.
     */
    void setSteps(String id, int numSteps);

    /**
     * Returns a {@link Task} which asynchronously sets an achievement to have at least the given number of steps completed.
     * Calling this method while the achievement already has more steps than the provided value is a no-op.
     * Once the achievement reaches the maximum number of steps, the achievement will automatically be unlocked, and any further mutation operations will be ignored.
     * <p>
     * This form of the API will attempt to update the user's achievement on the server immediately.
     * The {@link Boolean} in a successful response indicates whether the achievement is now unlocked.
     *
     * @param id       The ID of the achievement to modify.
     * @param numSteps The number of steps to set the achievement to. Must be greater than 0.
     */
    Task<Boolean> setStepsImmediate(String id, int numSteps);

    /**
     * Unlocks an achievement for the currently signed in player. If the achievement is hidden this will reveal it to the player.
     * <p>
     * This is the fire-and-forget form of the API. Use this form if you don't need to know the status of the operation immediately.
     * For most applications, this will be the preferred API to use, though note that the update may not be sent to the server until the next sync.
     * See {@link #unlockImmediate(String)} if you need the operation to attempt to communicate to the server immediately or need to have the status code delivered to your application.
     *
     * @param id The achievement ID to unlock.
     */
    void unlock(String id);

    /**
     * Returns a {@link Task} which asynchronously unlocks an achievement for the currently signed in player.
     * If the achievement is hidden this will reveal it to the player.
     * <p>
     * This form of the API will attempt to update the user's achievement on the server immediately.
     * The {@link Task} will complete successfully when the server has been updated.
     *
     * @param id The ID of the achievement to unlock.
     */
    Task<Void> unlockImmediate(String id);
}
