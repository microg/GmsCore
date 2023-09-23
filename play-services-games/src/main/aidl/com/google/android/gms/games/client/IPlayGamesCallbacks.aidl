package com.google.android.gms.games.client;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;

interface IPlayGamesCallbacks {
    void onData(in DataHolder dataHolder) = 1000;
    void onStatus5028(in Status status) = 5027;
}