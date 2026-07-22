package com.google.android.gms.googlehelp.internal.common;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;

import com.google.android.gms.googlehelp.GoogleHelp;
import com.google.android.gms.googlehelp.InProductHelp;

interface IGoogleHelpCallbacks {
    void onProcessGoogleHelpFinished(in GoogleHelp googleHelp) = 0;
    oneway void onSaveAsyncPsdFinished() = 6;
    oneway void onSaveAsyncPsbdFinished() = 7;
    void onRequestChatSupportSuccess(int chatQueuePosition) = 8;
    void onRequestChatSupportFailed() = 9;
    void onRequestC2cSupportSuccess() = 10;
    void onRequestC2cSupportFailed() = 11;
    void onSuggestions(in byte[] suggestions) = 12;
    void onNoSuggestions() = 13;
    void onEscalationOptions(in byte[] options) = 14;
    void onNoEscalationOptions() = 15;
    void onProcessInProductHelpFinished(in InProductHelp inProductHelp) = 16;
    void onRealtimeSupportStatus(in byte[] status) = 17;
    void onNoRealtimeSupportStatus() = 18;
}
