package com.google.android.gms.cast.internal;

import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.JoinOptions;
import com.google.android.gms.cast.internal.ICastDeviceControllerListener;

// Recent clients append an ApiMetadata parcelable to every call; it is ignored.
interface ICastDeviceController {
  oneway void disconnect() = 0;
  // Deprecated: void launchApplicationOld(String applicationId, boolean relaunchIfRunning) = 1;
  // Deprecated: void joinApplicationOld(String applicationId, String sessionId) = 2;
  oneway void leaveApplication() = 3;
  oneway void stopApplication(String sessionId) = 4;
  oneway void requestStatus() = 5;
  oneway void setVolume(double level, double expectedLevel, boolean expectedMute) = 6;
  oneway void setMute(boolean mute, double expectedLevel, boolean expectedMute) = 7;
  oneway void sendMessage(String namespace, String message, long requestId) = 8;
  oneway void sendBinaryMessage(String namespace, in byte[] message, long requestId) = 9;
  oneway void registerNamespace(String namespace) = 10;
  oneway void unregisterNamespace(String namespace) = 11;
  oneway void launchApplication(String applicationId, in LaunchOptions launchOptions) = 12;
  oneway void joinApplication(String applicationId, String sessionId, in JoinOptions joinOptions) = 13;
  // Used by clients that bind without a listener in the service request extras
  oneway void connect() = 16;
  oneway void addListener(ICastDeviceControllerListener listener) = 17;
  oneway void removeListener() = 18;
}
