package com.google.android.gms.games.internal.connect;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.internal.connect.GamesSignInResponse;

interface IGamesConnectCallbacks {
    void onSignIn(in Status status, in GamesSignInResponse response) = 1;
}