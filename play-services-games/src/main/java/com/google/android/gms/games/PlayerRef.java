/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.database.CharArrayBuffer;
import android.net.Uri;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.data.DataBufferRef;
import com.google.android.gms.common.data.DataHolder;
import org.microg.gms.common.Hide;

@Hide
public class PlayerRef extends DataBufferRef implements Player {
    public PlayerRef(DataHolder dataHolder, int position) {
        super(dataHolder, position);
    }

    private Uri toUri(String str) {
        if (str == null) return null;
        return Uri.parse(str);
    }

    @Override
    public Uri getBannerImageLandscapeUri() {
        return toUri(getString(PlayerColumns.bannerImageLandscapeUri));
    }

    @Override
    public Uri getBannerImagePortraitUri() {
        return toUri(getString(PlayerColumns.bannerImagePortraitUri));
    }

    @Override
    public CurrentPlayerInfo getCurrentPlayerInfo() {
        return null;
    }

    @Override
    public void getDisplayName(CharArrayBuffer dataOut) {
        copyToBuffer(PlayerColumns.profileName, dataOut);
    }

    @Override
    public String getDisplayName() {
        return getString(PlayerColumns.profileName);
    }

    @Override
    public Uri getHiResImageUri() {
        return toUri(getString(PlayerColumns.profileHiResImageUri));
    }

    @Override
    public Uri getIconImageUri() {
        return toUri(getString(PlayerColumns.profileIconImageUri));
    }

    @Override
    public long getLastPlayedWithTimestamp() {
        return getLong(PlayerColumns.playedWithTimestamp);
    }

    @Override
    public PlayerLevelInfo getLevelInfo() {
        return null;
    }

    @Override
    public String getPlayerId() {
        return getString(PlayerColumns.externalPlayerId);
    }

    @Override
    public PlayerRelationshipInfo getRelationshipInfo() {
        return null;
    }

    @Override
    public long getRetrievedTimestamp() {
        return getLong(PlayerColumns.lastUpdated);
    }

    @Override
    public String getTitle() {
        return getString(PlayerColumns.playerTitle);
    }

    @Override
    public void getTitle(CharArrayBuffer dataOut) {
        copyToBuffer(PlayerColumns.playerTitle, dataOut);
    }

    @Override
    public boolean hasHiResImage() {
        return hasColumn(PlayerColumns.profileHiResImageUri);
    }

    @Override
    public boolean hasIconImage() {
        return hasColumn(PlayerColumns.profileIconImageUri);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        new PlayerEntity(this).writeToParcel(dest, flags);
    }

    @Override
    public Player freeze() {
        return null;
    }

    public static final Creator<Player> CREATOR = new Creator<Player>() {
        @Override
        public Player createFromParcel(Parcel source) {
            return PlayerEntity.CREATOR.createFromParcel(source);
        }

        @Override
        public Player[] newArray(int size) {
            return new Player[size];
        }
    };
}
