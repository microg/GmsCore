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
 * Data object representing the information related only to the signed in user.
 */
public interface CurrentPlayerInfo extends Freezable<CurrentPlayerInfo>, Parcelable {
    /**
     * Retrieves if the user has shared the friends list with the game. The possible output can be found in {@link Player.FriendsListVisibilityStatus}.
     */
    @Player.FriendsListVisibilityStatus
    int getFriendsListVisibilityStatus();
}
