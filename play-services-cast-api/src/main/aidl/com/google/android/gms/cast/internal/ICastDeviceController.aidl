package com.google.android.gms.cast.internal;

import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.JoinOptions;

interface ICastDeviceController {
  void disconnect() = 0;
  void stopApplication(String sessionId) = 4;
  void sendMessage(String namespace, String message, long requestId) = 8;
  void registerNamespace(String namespace) = 10;
  void unregisterNamespace(String namespace) = 11;
  void launchApplication(String applicationId, in LaunchOptions launchOptions) = 12;
  void joinApplication(String applicationId, String sessionId, in JoinOptions joinOptions) = 13;
}
