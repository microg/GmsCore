/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.games;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

/**
 * Data object representing a level a player can obtain in the metagame.
 * <p>
 * A {@code PlayerLevel} has three components: a numeric value, and a range of XP totals it represents. A player is considered a
 * given level if they have <b>at least</b> {@link #getMinXp()} and <b>less than</b> {@link #getMaxXp()}.
 */
@SafeParcelable.Class
public class PlayerLevel extends AbstractSafeParcelable {
    @Field(1)
    private final int levelNumber;
    @Field(2)
    private final long minXp;
    @Field(3)
    private final long maxXp;

    @Constructor
    @Hide
    public PlayerLevel(@Param(1) int levelNumber, @Param(2) long minXp, @Param(3) long maxXp) {
        this.levelNumber = levelNumber;
        this.minXp = minXp;
        this.maxXp = maxXp;
    }

    /**
     * Returns the number for this level, e.g. "level 10".
     * <p>
     * This is the level that this object represents. For a player to be considered as being of this level, the value given by
     * {@link PlayerLevelInfo#getCurrentXpTotal()} must fall in the range [{@link #getMinXp()}, {@link #getMaxXp()}).
     *
     * @return The level number for this level.
     */
    public int getLevelNumber() {
        return levelNumber;
    }

    /**
     * @return The maximum XP value represented by this level, exclusive.
     */
    public long getMaxXp() {
        return maxXp;
    }

    /**
     * @return The minimum XP value needed to attain this level, inclusive.
     */
    public long getMinXp() {
        return minXp;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PlayerLevel> CREATOR = findCreator(PlayerLevel.class);
}
