/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.games;

import android.os.Parcelable;
import com.google.android.gms.common.data.Freezable;

/**
 * Data object representing the relationship information of a player.
 */
public interface PlayerRelationshipInfo extends Freezable<PlayerRelationshipInfo>, Parcelable {
    /**
     * Retrieves this player's friend status relative to the currently signed-in player. The possible output can be found in
     * {@link Player.PlayerFriendStatus}.
     */
    @Player.PlayerFriendStatus
    int getFriendStatus();
}
