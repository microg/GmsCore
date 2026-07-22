/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.internal.player;

import android.net.Uri;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

public class MostRecentGameInfoEntity extends AutoSafeParcelable implements MostRecentGameInfo {
    @Field(1)
    private String gameId;
    @Field(2)
    private String gameName;
    @Field(3)
    private long activityTimestampMillis;
    @Field(4)
    private Uri gameIconImageUri;
    @Field(5)
    private Uri gameHiResImageUri;
    @Field(6)
    private Uri gameFeaturedImageUri;

    @Hide
    public MostRecentGameInfoEntity() {
    }

    @Hide
    public MostRecentGameInfoEntity(String gameId, String gameName, long activityTimestampMillis, Uri gameIconImageUri, Uri gameHiResImageUri, Uri gameFeaturedImageUri) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.activityTimestampMillis = activityTimestampMillis;
        this.gameIconImageUri = gameIconImageUri;
        this.gameHiResImageUri = gameHiResImageUri;
        this.gameFeaturedImageUri = gameFeaturedImageUri;
    }

    @Override
    public long getActivityTimestampMillis() {
        return activityTimestampMillis;
    }

    @Override
    public Uri getGameFeaturedImageUri() {
        return gameFeaturedImageUri;
    }

    @Override
    public Uri getGameHiResImageUri() {
        return gameHiResImageUri;
    }

    @Override
    public Uri getGameIconImageUri() {
        return gameIconImageUri;
    }

    @Override
    public String getGameId() {
        return gameId;
    }

    @Override
    public String getGameName() {
        return gameName;
    }

    @Override
    public boolean isDataValid() {
        return true;
    }

    @Override
    public MostRecentGameInfo freeze() {
        return this;
    }

    public static final SafeParcelableCreatorAndWriter<MostRecentGameInfoEntity> CREATOR = findCreator(MostRecentGameInfoEntity.class);
}
