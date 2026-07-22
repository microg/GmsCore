/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.games;

import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Data object representing the current level information of a player in the metagame.
 * <p>
 * A {@code PlayerLevelInfo} has four components: the player's current XP, the timestamp of the player's last level-up, the
 * player's current level, and the player's next level.
 */
public class PlayerLevelInfo extends AutoSafeParcelable {
    @Field(1)
    private long currentXpTotal;
    @Field(2)
    private long lastLevelUpTimestamp;
    @Field(3)
    private PlayerLevel currentLevel;
    @Field(4)
    private PlayerLevel nextLevel;

    @Hide
    public PlayerLevelInfo() {
    }

    @Hide
    public PlayerLevelInfo(long currentXpTotal, long lastLevelUpTimestamp, PlayerLevel currentLevel, PlayerLevel nextLevel) {
        this.currentXpTotal = currentXpTotal;
        this.lastLevelUpTimestamp = lastLevelUpTimestamp;
        this.currentLevel = currentLevel;
        this.nextLevel = nextLevel;
    }

    /**
     * Getter for the player's current level object. This object will be the same as the one returned from {@link #getNextLevel()} if the
     * player reached the maximum level.
     *
     * @return The player's current level object.
     * @see #isMaxLevel()
     */
    public PlayerLevel getCurrentLevel() {
        return currentLevel;
    }

    /**
     * @return The player's current XP value.
     */
    public long getCurrentXpTotal() {
        return currentXpTotal;
    }

    /**
     * @return The timestamp of the player's last level-up.
     */
    public long getLastLevelUpTimestamp() {
        return lastLevelUpTimestamp;
    }

    /**
     * Getter for the player's next level object. This object will be the same as the one returned from {@link #getCurrentLevel()} if the
     * player reached the maximum level.
     *
     * @return The player's next level object.
     * @see #isMaxLevel()
     */
    public PlayerLevel getNextLevel() {
        return nextLevel;
    }

    /**
     * @return {@code true} if the player reached the maximum level ({@link #getCurrentLevel()} is the same as {@link #getNextLevel()}.
     */
    public boolean isMaxLevel() {
        return currentLevel.equals(nextLevel);
    }

    public static final SafeParcelableCreatorAndWriter<PlayerLevelInfo> CREATOR = findCreator(PlayerLevelInfo.class);
}
