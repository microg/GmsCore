package com.google.android.gms.cast.internal;

import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.JoinOptions;

interface ICastDeviceController {
  oneway void disconnect() = 0;
  oneway void stopApplication(String sessionId) = 4;
  oneway void sendMessage(String namespace, String message, long requestId) = 8;
  oneway void registerNamespace(String namespace) = 10;
  oneway void unregisterNamespace(String namespace) = 11;
  oneway void launchApplication(String applicationId, in LaunchOptions launchOptions) = 12;
  oneway void joinApplication(String applicationId, String sessionId, in JoinOptions joinOptions) = 13;
}
