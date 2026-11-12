package com.google.android.gms.cast.internal;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.ApplicationStatus;
import com.google.android.gms.cast.CastDeviceStatus;

oneway interface ICastDeviceControllerListener {
  void onDisconnected(int reason) = 0;
  void onApplicationConnectionSuccess(in ApplicationMetadata applicationMetadata, String applicationStatus, String sessionId, boolean wasLaunched) = 1;
  void onApplicationConnectionFailure(int statusCode) = 2;
  // Deprecated: void onStatusReceived(String string1, double double1, boolean boolean1) = 3;
  void onTextMessageReceived(String namespace, String message) = 4;
  void onBinaryMessageReceived(String namespace, in byte[] data) = 5;
  // Both complete the client's pending leaveApplication / stopApplication result
  void onLeaveApplicationResult(int statusCode) = 6;
  void onStopApplicationResult(int statusCode) = 7;
  void onApplicationDisconnected(int statusCode) = 8;
  void onSendMessageFailure(String namespace, long requestId, int statusCode) = 9;
  void onSendMessageSuccess(String namespace, long requestId) = 10;
  void onApplicationStatusChanged(in ApplicationStatus applicationStatus) = 11;
  void onDeviceStatusChanged(in CastDeviceStatus deviceStatus) = 12;
  // Answer to connect()
  void onConnectedWithResult(int statusCode) = 13;
  void onConnectionSuspended(int reason) = 14;
}
