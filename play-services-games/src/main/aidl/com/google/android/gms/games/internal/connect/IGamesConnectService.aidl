package com.google.android.gms.games.internal.connect;

import com.google.android.gms.games.internal.connect.GamesSignInRequest;
import com.google.android.gms.games.internal.connect.IGamesConnectCallbacks;

interface IGamesConnectService {
    void signIn(IGamesConnectCallbacks callback, in GamesSignInRequest request) = 1;
}