package com.google.android.gms.common.rcs;

interface IRcsService {
    boolean isRcsEnabled();
    void sendMessage(String destination, String text, IRcsMessageCallback callback);
    void startSession(String sessionId);
    void stopSession(String sessionId);
}