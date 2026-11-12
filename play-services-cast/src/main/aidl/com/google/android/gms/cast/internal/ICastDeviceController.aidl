package com.google.android.gms.cast.internal;

import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.JoinOptions;
import com.google.android.gms.cast.internal.ICastDeviceControllerListener;

/**
 * Transaction ids verified against play-services-cast 23.0.0. Newer clients append an ApiMetadata parcelable to every
 * call, which is safe to leave unread.
 */
interface ICastDeviceController {
  oneway void disconnect() = 0;
  oneway void leaveApplication() = 3;
  oneway void stopApplication(String sessionId) = 4;
  oneway void requestStatus() = 5;
  oneway void setVolume(double volume, double expectedVolume, boolean muted) = 6;
  oneway void setMute(boolean mute, double volume, boolean expectedMute) = 7;
  oneway void sendMessage(String namespace, String message, long requestId) = 8;
  oneway void registerNamespace(String namespace) = 10;
  oneway void unregisterNamespace(String namespace) = 11;
  oneway void launchApplication(String applicationId, in LaunchOptions launchOptions) = 12;
  oneway void joinApplication(String applicationId, String sessionId, in JoinOptions joinOptions) = 13;
  // The following are used by the GoogleApi based client (play-services-cast 19+), which does not pass the listener
  // as part of the service request extras anymore.
  oneway void connect() = 16;
  oneway void registerListener(ICastDeviceControllerListener listener) = 17;
  oneway void unregisterListener() = 18;
}
