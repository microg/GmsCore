package com.google.android.gms.cast.internal;

import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.JoinOptions;
import com.google.android.gms.cast.internal.ICastDeviceControllerListener;

interface ICastDeviceController {
  oneway void disconnect() = 0;
  oneway void setVolumeLevel(double volume) = 1;
  oneway void setMuted(boolean muted) = 2;
  oneway void stopApplication(String sessionId) = 3;
  oneway void registerNamespace(String namespace) = 10;
  oneway void unregisterNamespace(String namespace) = 11;
  oneway void launchApplication(String applicationId, in LaunchOptions launchOptions) = 12;
  oneway void joinApplication(String applicationId, String sessionId, in JoinOptions joinOptions) = 13;
  // Connectionless (Cast.API_CXLESS) path used by the modern Cast SDK: the client delivers its
  // listener out-of-band via setListener (txn 18) then calls connect (txn 17), and waits for the
  // service to reply ICastDeviceControllerListener.onConnectedWithResult before launching.
  oneway void connect() = 17;
  oneway void setListener(ICastDeviceControllerListener listener) = 18;
  oneway void unregisterListener() = 19;
}