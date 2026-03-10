/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.games;

import android.app.Activity;
import android.content.Intent;
import android.os.RemoteException;
import com.google.android.gms.tasks.Task;

/**
 * A client to interact with Players.
 */
public interface PlayersClient {
    /**
     * Used by the Player Search UI to return a list of parceled Player objects. Retrieve with {@link Intent#getParcelableArrayListExtra(String)}.
     *
     * @see #getPlayerSearchIntent()
     */
    String EXTRA_PLAYER_SEARCH_RESULTS = "player_search_results";

    /**
     * Returns a {@link Task} which asynchronously loads the current signed-in {@link Player}, if available.
     * <p>
     * The returned {@code Task} can fail with a {@link RemoteException}.
     *
     * @param forceReload If {@code true}, this call will clear any locally-cached data and attempt to fetch the latest data from the server. This would
     *                    commonly be used for something like a user-initiated refresh. Normally, this should be set to {@code false} to gain advantages
     *                    of data caching.
     */
    Task<AnnotatedData<Player>> getCurrentPlayer(boolean forceReload);

    /**
     * Returns a {@link Task} which asynchronously loads the current signed-in {@link Player}, if available.
     * <p>
     * The returned {@code Task} can fail with a {@link RemoteException}.
     */
    Task<Player> getCurrentPlayer();

    /**
     * Returns a {@link Task} which asynchronously loads the current signed-in player ID, if available.
     * <p>
     * The returned {@code Task} can fail with a {@link RemoteException}.
     */
    Task<String> getCurrentPlayerId();

    /**
     * Returns a {@link Task} which asynchronously loads an {@link Intent} that will display a screen where the user can search for players.
     * <p>
     * Note that this must be invoked with {@link Activity#startActivityForResult(Intent, int)}, so that the identity of the
     * calling package can be established.
     * <p>
     * If the user canceled, the result will be {@link Activity#RESULT_CANCELED}. If the user selected any players from the search
     * results list, the result will be {@link Activity#RESULT_OK}, and the data intent will contain a list of parceled Player objects in
     * {@link #EXTRA_PLAYER_SEARCH_RESULTS}.
     * <p>
     * Note that the current Player Search UI only allows a single selection, so the returned list of parceled Player objects will
     * currently contain at most one Player.
     * <p>
     * The returned {@code Task} can fail with a {@link RemoteException}.
     */
    Task<Intent> getPlayerSearchIntent();
}
