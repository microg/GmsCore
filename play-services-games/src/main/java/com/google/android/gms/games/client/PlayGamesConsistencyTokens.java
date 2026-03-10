/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.client;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import org.microg.gms.utils.ToStringHelper;

public class PlayGamesConsistencyTokens implements Parcelable {
    public final String oneupConsistencyToken;
    public final String superGlueConsistencyToken;

    public PlayGamesConsistencyTokens(String oneupConsistencyToken, String superGlueConsistencyToken) {
        this.oneupConsistencyToken = oneupConsistencyToken;
        this.superGlueConsistencyToken = superGlueConsistencyToken;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("PlayGamesConsistencyTokens")
                .field("oneupConsistencyToken", oneupConsistencyToken)
                .field("superGlueConsistencyToken", superGlueConsistencyToken)
                .end();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(oneupConsistencyToken);
        dest.writeString(superGlueConsistencyToken);
    }

    public static final Creator<PlayGamesConsistencyTokens> CREATOR = new Creator<PlayGamesConsistencyTokens>() {
        @Override
        public PlayGamesConsistencyTokens createFromParcel(Parcel source) {
            return new PlayGamesConsistencyTokens(source.readString(), source.readString());
        }

        @Override
        public PlayGamesConsistencyTokens[] newArray(int size) {
            return new PlayGamesConsistencyTokens[size];
        }
    };
}
