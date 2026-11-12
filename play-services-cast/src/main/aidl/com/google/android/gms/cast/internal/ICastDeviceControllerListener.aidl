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
  // Both complete the pending leaveApplication() / stopApplication() request on the client
  void onStatusRequestResult(int statusCode) = 6;
  void onStatusRequestResult2(int statusCode) = 7;
  void onApplicationDisconnected(int paramInt) = 8;
  void onSendMessageFailure(String response, long requestId, int statusCode) = 9;
  void onSendMessageSuccess(String response, long requestId) = 10;
  void onApplicationStatusChanged(in ApplicationStatus applicationStatus) = 11;
  void onDeviceStatusChanged(in CastDeviceStatus deviceStatus) = 12;
  // Result of ICastDeviceController.connect(), 0 on success
  void onConnected(int statusCode) = 13;
  void onConnectionSuspended(int reason) = 14;
}
