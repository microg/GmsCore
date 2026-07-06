package com.google.android.gms.common.rcs;

interface IRcsMessageCallback {
    void onMessageSent(in String messageId);
    void onMessageFailed(int errorCode);
}