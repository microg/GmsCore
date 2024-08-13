/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Scope;

import java.util.ArrayList;
import java.util.Collections;

public class Games {

    public static final String TAG = "GamesAPI";
    public static final String EXTRA_PLAYER_IDS = "players";
    public static final String EXTRA_STATUS = "status";
    public static final String SERVICE_GAMES = "oauth2:https://www.googleapis.com/auth/games";
    public static final String SERVICE_GAMES_LITE = "oauth2:https://www.googleapis.com/auth/games_lite";
    public static final String SERVICE_GAMES_SNAPSHOTS = "oauth2:https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/games_lite";
    public static final Scope SCOPE_GAMES = new Scope("https://www.googleapis.com/auth/games");
    public static final Scope SCOPE_GAMES_LITE = new Scope("https://www.googleapis.com/auth/games_lite");
    public static final Scope SCOPE_GAMES_SNAPSHOTS = new Scope("https://www.googleapis.com/auth/drive.appdata");
    public static final Scope SCOPE_GAMES_FIRST_PARTY = new Scope("https://www.googleapis.com/auth/games.firstparty");

    public static final Api<GamesOptions> API = new Api<>(new GamesApiClientBuilder(Collections.singletonList(Games.SCOPE_GAMES)));
    public static final Api<GamesOptions> API_1P = new Api<>(new GamesApiClientBuilder(Collections.singletonList(Games.SCOPE_GAMES_FIRST_PARTY)));

    public static final class GamesOptions implements Api.ApiOptions.HasGoogleSignInAccountOptions {

        public boolean isHeadless;
        public boolean showConnectingPopup;
        public int connectingPopupGravity;
        public int sdkVariant;
        public String forceResolveAccountKey;
        public ArrayList<String> proxyApis;
        public boolean unauthenticated;
        public boolean skipPgaCheck;
        public boolean skipWelcomePopup;
        public GoogleSignInAccount googleSignInAccount;
        public String realClientPackageName;
        public int unknownIntValue;
        public int API_VERSION;

        public static final class Builder {
            private boolean isHeadless;
            private boolean showConnectingPopup;
            private int connectingPopupGravity;
            private int sdkVariant;
            private String forceResolveAccountKey;
            private ArrayList<String> proxyApis;
            private boolean unauthenticated;
            private boolean skipPgaCheck;
            private boolean skipWelcomePopup;
            private GoogleSignInAccount googleSignInAccount;
            private String realClientPackageName;
            private int unknownIntValue;
            private int API_VERSION;

            public Builder() {
                this.isHeadless = false;
                this.showConnectingPopup = true;
                this.connectingPopupGravity = 17;
                this.sdkVariant = 4368;
                this.forceResolveAccountKey = null;
                this.proxyApis = new ArrayList<>();
                this.unauthenticated = false;
                this.skipPgaCheck = false;
                this.skipWelcomePopup = false;
                this.googleSignInAccount = null;
                this.realClientPackageName = null;
                this.unknownIntValue = 0;
                this.API_VERSION = 9;
            }

            public Builder setRealClientPackageName(String packageName) {
                this.realClientPackageName = packageName;
                return this;
            }

            public void isHeadless() {
                this.isHeadless = true;
            }

            public void skipWelcomePopup(Boolean bool) {
                this.skipWelcomePopup = bool;
            }

            public GamesOptions build() {
                return new GamesOptions(this.isHeadless, this.showConnectingPopup, this.connectingPopupGravity, this.sdkVariant, this.forceResolveAccountKey, this.proxyApis, this.unauthenticated, this.skipPgaCheck, this.skipWelcomePopup, this.googleSignInAccount, this.realClientPackageName, this.unknownIntValue, this.API_VERSION);
            }

            public void unauthenticated() {
                this.unauthenticated = true;
            }

            public Builder setSdkVariant(int sdkVariant) {
                this.sdkVariant = sdkVariant;
                return this;
            }

            public Builder setShowConnectingPopup(boolean showConnectingPopup) {
                this.showConnectingPopup = showConnectingPopup;
                this.connectingPopupGravity = Gravity.CENTER;
                return this;
            }

            public Builder(GamesOptions gamesOptions) {
                this.isHeadless = false;
                this.showConnectingPopup = true;
                this.connectingPopupGravity = Gravity.CENTER;
                this.sdkVariant = 4368;
                this.forceResolveAccountKey = null;
                this.proxyApis = new ArrayList<>();
                this.unauthenticated = false;
                this.skipPgaCheck = false;
                this.skipWelcomePopup = false;
                this.googleSignInAccount = null;
                this.realClientPackageName = null;
                this.unknownIntValue = 0;
                this.API_VERSION = 9;
                if (gamesOptions != null) {
                    this.isHeadless = gamesOptions.isHeadless;
                    this.showConnectingPopup = gamesOptions.showConnectingPopup;
                    this.connectingPopupGravity = gamesOptions.connectingPopupGravity;
                    this.sdkVariant = gamesOptions.sdkVariant;
                    this.forceResolveAccountKey = gamesOptions.forceResolveAccountKey;
                    this.proxyApis = gamesOptions.proxyApis;
                    this.unauthenticated = gamesOptions.unauthenticated;
                    this.skipPgaCheck = gamesOptions.skipPgaCheck;
                    this.skipWelcomePopup = gamesOptions.skipWelcomePopup;
                    this.googleSignInAccount = gamesOptions.googleSignInAccount;
                    this.realClientPackageName = gamesOptions.realClientPackageName;
                    this.unknownIntValue = gamesOptions.unknownIntValue;
                    this.API_VERSION = gamesOptions.API_VERSION;
                }
            }

            public Builder setShowConnectingPopup(boolean showConnectingPopup, int connectingPopupGravity) {
                this.showConnectingPopup = showConnectingPopup;
                this.connectingPopupGravity = connectingPopupGravity;
                return this;
            }
        }

        public GamesOptions(boolean isHeadless, boolean showConnectingPopup, int connectingPopupGravity, int sdkVariant, String forceResolveAccountKey, ArrayList<String> proxyApis, boolean unauthenticated, boolean skipPgaCheck, boolean skipWelcomePopup, GoogleSignInAccount googleSignInAccount, String realClientPackageName, int unknownIntValue, int API_VERSION) {
            this.isHeadless = isHeadless;
            this.showConnectingPopup = showConnectingPopup;
            this.connectingPopupGravity = connectingPopupGravity;
            this.sdkVariant = sdkVariant;
            this.forceResolveAccountKey = forceResolveAccountKey;
            this.proxyApis = proxyApis;
            this.unauthenticated = unauthenticated;
            this.skipPgaCheck = skipPgaCheck;
            this.skipWelcomePopup = skipWelcomePopup;
            this.googleSignInAccount = googleSignInAccount;
            this.realClientPackageName = realClientPackageName;
            this.unknownIntValue = unknownIntValue;
            this.API_VERSION = API_VERSION;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static Builder builder(GoogleSignInAccount googleSignInAccount) {
            Builder builder = new Builder(null);
            builder.googleSignInAccount = googleSignInAccount;
            return builder;
        }

        @Override
        public GoogleSignInAccount getGoogleSignInAccount() {
            return googleSignInAccount;
        }

        public Bundle getGamesOptions() {
            Bundle bundle = new Bundle();
            bundle.putBoolean("com.google.android.gms.games.key.isHeadless", this.isHeadless);
            bundle.putBoolean("com.google.android.gms.games.key.showConnectingPopup", this.showConnectingPopup);
            bundle.putInt("com.google.android.gms.games.key.connectingPopupGravity", this.connectingPopupGravity);
            bundle.putBoolean("com.google.android.gms.games.key.retryingSignIn", false);
            bundle.putInt("com.google.android.gms.games.key.sdkVariant", this.sdkVariant);
            bundle.putString("com.google.android.gms.games.key.forceResolveAccountKey", this.forceResolveAccountKey);
            bundle.putStringArrayList("com.google.android.gms.games.key.proxyApis", this.proxyApis);
            bundle.putBoolean("com.google.android.gms.games.key.unauthenticated", this.unauthenticated);
            bundle.putBoolean("com.google.android.gms.games.key.skipPgaCheck", this.skipPgaCheck);
            bundle.putBoolean("com.google.android.gms.games.key.skipWelcomePopup", this.skipWelcomePopup);
            bundle.putParcelable("com.google.android.gms.games.key.googleSignInAccount", this.googleSignInAccount);
            bundle.putString("com.google.android.gms.games.key.realClientPackageName", this.realClientPackageName);
            bundle.putInt("com.google.android.gms.games.key.API_VERSION", this.API_VERSION);
            bundle.putString("com.google.android.gms.games.key.gameRunToken", null);
            return bundle;
        }

        @Override
        public String toString() {
            return "GamesOptions{" + "isHeadless=" + isHeadless + ", showConnectingPopup=" + showConnectingPopup + ", connectingPopupGravity=" + connectingPopupGravity + ", sdkVariant=" + sdkVariant + ", forceResolveAccountKey='" + forceResolveAccountKey + '\'' + ", proxyApis=" + proxyApis + ", unauthenticated=" + unauthenticated + ", skipPgaCheck=" + skipPgaCheck + ", skipWelcomePopup=" + skipWelcomePopup + ", googleSignInAccount=" + googleSignInAccount + ", realClientPackageName='" + realClientPackageName + '\'' + ", unknownIntValue=" + unknownIntValue + ", API_VERSION=" + API_VERSION + '}';
        }
    }

    public static AchievementsClient getAchievementsClient(Context context, GoogleSignInAccount googleSignInAccount) {
        Log.d(Games.TAG, "getAchievementsClient: start");
        if (googleSignInAccount == null) {
            Log.d(Games.TAG, "getAchievementsClient: googleSignInAccount must not be null");
            return null;
        }
        return new AchievementsClientImpl(context, GamesOptions.builder(googleSignInAccount).build());
    }

    public static LeaderboardsClient getLeaderboardsClient(Context context, GoogleSignInAccount googleSignInAccount) {
        Log.d(Games.TAG, "getLeaderboardsClient: start");
        if (googleSignInAccount == null) {
            Log.d(Games.TAG, "getLeaderboardsClient: googleSignInAccount must not be null");
            return null;
        }
        return new LeaderboardsClientImpl(context, GamesOptions.builder(googleSignInAccount).build());
    }

    public static SnapshotsClient getSnapshotsClient(Context context, GoogleSignInAccount googleSignInAccount) {
        Log.d(Games.TAG, "getSnapshotsClient: start");
        if (googleSignInAccount == null) {
            Log.d(Games.TAG, "getSnapshotsClient: googleSignInAccount must not be null");
            return null;
        }
        return new SnapshotsClientImpl(context, GamesOptions.builder(googleSignInAccount).build());
    }

}
