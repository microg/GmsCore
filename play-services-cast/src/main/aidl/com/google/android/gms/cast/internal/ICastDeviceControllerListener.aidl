package com.google.android.gms.cast.internal;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.ApplicationStatus;
import com.google.android.gms.cast.CastDeviceStatus;

interface ICastDeviceControllerListener {
  void onConnected();
  void onDisconnected(int reason);
  void onApplicationConnectionSuccess(ApplicationMetadata applicationMetadata, String applicationStatus, String sessionId, boolean wasLaunched);
  void onApplicationConnectionFailure(int statusCode);
  void onApplicationDisconnected(int errorCode);
  void onTextMessageReceived(String namespace, String message);
  void onBinaryMessageReceived(String namespace, byte[] data);
  void onApplicationStatusChanged(in ApplicationStatus applicationStatus);
  void onDeviceStatusChanged(in CastDeviceStatus deviceStatus);
  void onSendMessageSuccess(String response, long requestId);
  void onSendMessageFailure(String response, long requestId, int statusCode);
  // Connectionless readiness signal: the cxless client stays "not connected" until the service
  // calls this with statusCode 0 (SUCCESS) after connect(). Without it the session never starts.
  void onConnectedWithResult(int statusCode) = 14;
}