package com.google.android.gms.wearable.internal;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.AncsNotificationParcelable;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;
import com.google.android.gms.wearable.internal.IChannelStreamCallbacks;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.IWearableService;

import com.google.android.gms.wearable.internal.AddAccountToConsentRequest;

import com.google.android.gms.wearable.internal.LogCounterRequest;
import com.google.android.gms.wearable.internal.LogEventRequest;
import com.google.android.gms.wearable.internal.LogTimerRequest;

import com.google.android.gms.wearable.MessageOptions;

interface IWearableService {
    // Configs
    void putConfig(IWearableCallbacks callbacks, in ConnectionConfiguration config) = 19;
    void deleteConfig(IWearableCallbacks callbacks, String name) = 20;
    void getConfigs(IWearableCallbacks callbacks) = 21;
    void enableConfig(IWearableCallbacks callbacks, String name) = 22; // aka enableConnection
    void disableConfig(IWearableCallbacks callbacks, String name) = 23;

    void getRelatedConfigs(IWearableCallbacks callbacks) = 72;
    void updateConfig(IWearableCallbacks iWearableCallbacks, in ConnectionConfiguration config) = 73;

    // DataItems
    void putData(IWearableCallbacks callbacks, in PutDataRequest request) = 5;
    void getDataItem(IWearableCallbacks callbacks, in Uri uri) = 6;
    void getDataItems(IWearableCallbacks callbacks) = 7;
    void getDataItemsByUri(IWearableCallbacks callbacks, in Uri uri) = 8;
    void getDataItemsByUriWithFilter(IWearableCallbacks callbacks, in Uri uri, int typeFilter) = 39;
    void deleteDataItems(IWearableCallbacks callbacks, in Uri uri) = 10;
    void deleteDataItemsWithFilter(IWearableCallbacks callbacks, in Uri uri, int typeFilter) = 40;

    void sendMessage(IWearableCallbacks callbacks, String targetNodeId, String path, in byte[] data) = 11;
    void sendRequest(IWearableCallbacks callbacks, String targetNodeId, String path, in byte[] data) = 57;
    void sendMessageWithOptions(IWearableCallbacks callbacks, String targetNodeId, String path, in byte[] data, in MessageOptions options) = 58;
    void sendRequestWithOptions(IWearableCallbacks callbacks, String targetNodeId, String path, in byte[] data, in MessageOptions options) = 59;

    void getFdForAsset(IWearableCallbacks callbacks, in Asset asset) = 12;

    void getLocalNode(IWearableCallbacks callbacks) = 13;
    void getNodeId(IWearableCallbacks callbacks, String address) = 66;
    void getConnectedNodes(IWearableCallbacks callbacks) = 14;

    // Capabilties
    void getConnectedCapability(IWearableCallbacks callbacks, String capability, int nodeFilter) = 41;
    void getAllCapabilities(IWearableCallbacks callbacks, int nodeFilter) = 42;
    void addLocalCapability(IWearableCallbacks callbacks, String capability) = 45;
    void removeLocalCapability(IWearableCallbacks callbacks, String capability) = 46;

    void addListener(IWearableCallbacks callbacks, in AddListenerRequest request) = 15;
    void removeListener(IWearableCallbacks callbacks, in RemoveListenerRequest request) = 16;

    void getStorageInformation(IWearableCallbacks callbacks) = 17;
    void clearStorage(IWearableCallbacks callbacks) = 18;

    void endCall(IWearableCallbacks callbacks) = 24;
    void acceptRingingCall(IWearableCallbacks callbacks) = 25;
    void silenceRinger(IWearableCallbacks callbacks) = 29;

    // Apple Notification Center Service
    void injectAncsNotificationForTesting(IWearableCallbacks callbacks, in AncsNotificationParcelable notification) = 26;
    void doAncsPositiveAction(IWearableCallbacks callbacks, int i) = 27;
    void doAncsNegativeAction(IWearableCallbacks callbacks, int i) = 28;

    // Channels
    void openChannel(IWearableCallbacks callbacks, String s1, String s2) = 30;
    void closeChannel(IWearableCallbacks callbacks, String s) = 31;
    void closeChannelWithError(IWearableCallbacks callbacks, String s, int errorCode) = 32;
    void getChannelInputStream(IWearableCallbacks callbacks, IChannelStreamCallbacks channelCallbacks, String s) = 33;
    void getChannelOutputStream(IWearableCallbacks callbacks, IChannelStreamCallbacks channelCallbacks, String s) = 34;
    void writeChannelInputToFd(IWearableCallbacks callbacks, String s, in ParcelFileDescriptor fd) = 37;
    void readChannelOutputFromFd(IWearableCallbacks callbacks, String s, in ParcelFileDescriptor fd, long l1, long l2) = 38;

    void syncWifiCredentials(IWearableCallbacks callbacks) = 36;

    // Cloud Sync
    void optInCloudSync(IWearableCallbacks callbacks, boolean enable) = 47;
    void getCloudSyncOptInDone(IWearableCallbacks callbacks) = 48; // deprecated
    void setCloudSyncSetting(IWearableCallbacks callbacks, boolean enable) = 49;
    void getCloudSyncSetting(IWearableCallbacks callbacks) = 50;
    void getCloudSyncOptInStatus(IWearableCallbacks callbacks) = 51;

    void sendAmsRemoteCommand(IWearableCallbacks callbacks, byte command) = 52;

    void getConsentStatus(IWearableCallbacks callbacks) = 64;
    void addAccountToConsent(IWearableCallbacks callbacks, in AddAccountToConsentRequest request) = 65;

//    void privacyRecordOptinRequest(IWearableCallbacks callbacks, in PrivacyRecordOptinRequest request) = 70;

    void someBoolUnknown(IWearableCallbacks callbacks) = 84; // cannot figure out name

    void getCompanionPackageForNode(IWearableCallbacks callbacks, String nodeId) = 62;

    void setCloudSyncSettingByNode(IWearableCallbacks callbacks, String s, boolean b) = 74;

    void logCounter(IWearableCallbacks callbacks, in LogCounterRequest request) = 105;
    void logEvent(IWearableCallbacks callbacks, in LogEventRequest request) = 106;
    void logTimer(IWearableCallbacks callbacks, in LogTimerRequest request) = 107;
    void clearLogs(IWearableCallbacks callbacks) = 108; // just assuming this is clearLogs

    // deprecated Connection
    void putConnection(IWearableCallbacks callbacks, in ConnectionConfiguration config) = 1;
    void getConnection(IWearableCallbacks callbacks) = 2;
    void enableConnection(IWearableCallbacks callbacks) = 3;
    void disableConnection(IWearableCallbacks callbacks) = 4;
}
