/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.content.Context;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.games.internal.IGamesService;

import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

public class GamesGmsClientImpl extends GmsClient<IGamesService> {
    private final Context context;
    private final Games.GamesOptions mGamesOptions;

    public GamesGmsClientImpl(Context context, Games.GamesOptions gamesOptions, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.GAMES.ACTION);
        Log.d(Games.TAG, "GamesGmsClientImpl: connect");
        this.context = context;
        packageName = gamesOptions.realClientPackageName;
        account = gamesOptions.googleSignInAccount.getAccount();
        scopes = gamesOptions.googleSignInAccount.getGrantedScopes().toArray(new Scope[0]);
        serviceId = GmsService.GAMES.SERVICE_ID;

        mGamesOptions = gamesOptions;
    }

    @Override
    protected IGamesService interfaceFromBinder(IBinder binder) {
        return IGamesService.Stub.asInterface(binder);
    }

}
