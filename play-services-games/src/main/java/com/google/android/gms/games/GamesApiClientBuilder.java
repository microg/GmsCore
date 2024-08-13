/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Scope;

import org.microg.gms.common.api.ApiClientBuilder;
import org.microg.gms.common.api.ApiClientSettings;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

import java.util.ArrayList;
import java.util.List;

public class GamesApiClientBuilder implements ApiClientBuilder<Games.GamesOptions> {

    private final List<Scope> scopeList = new ArrayList<>();

    public GamesApiClientBuilder(List<Scope> scopes) {
        scopeList.addAll(scopes);
    }

    @Override
    public Api.Client build(Games.GamesOptions options, Context context, Looper looper, ApiClientSettings clientSettings, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        Log.d(Games.TAG, "GamesGmsClientImpl build options: " + options.toString());
        return new GamesGmsClientImpl(context, options, callbacks, connectionFailedListener);
    }

    public List<Scope> getScopes(Games.GamesOptions options) {
        return scopeList;
    }
}
