package com.google.android.gms.reminders.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;

import com.google.android.gms.reminders.AccountState;

interface IRemindersCallbacks {
    void onDataHolder(in DataHolder data, in Status status) = 0;
    void onStatus(in Status status) = 1;
    void onNoStatus() = 2;
    void onDataHolderNoStatus(in DataHolder data, in Status status) = 3;
    void onBool(boolean b, in Status status) = 4;
    void onString(in String s, in Status status) = 5;
    void onAccountState(in AccountState accountState, in Status status) = 6;
    void onAsyncDataHolder(in DataHolder data) = 7;
}