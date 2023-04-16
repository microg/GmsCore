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

interface IWearableService {
    // Configs
    void putConfig(IWearableCallbacks callbacks, in ConnectionConfiguration config) = 19;
    void deleteConfig(IWearableCallbacks callbacks, String name) = 20;
    void getConfigs(IWearableCallbacks callbacks) = 21;
    void enableConfig(IWearableCallbacks callbacks, String name) = 22;
    void disableConfig(IWearableCallbacks callbacks, String name) = 23;

    // DataItems
    void putData(IWearableCallbacks callbacks, in PutDataRequest request) = 5;
    void getDataItem(IWearableCallbacks callbacks, in Uri uri) = 6;
    void getDataItems(IWearableCallbacks callbacks) = 7;
    void getDataItemsByUri(IWearableCallbacks callbacks, in Uri uri) = 8;
    void getDataItemsByUriWithFilter(IWearableCallbacks callbacks, in Uri uri, int typeFilter) = 39;
    void deleteDataItems(IWearableCallbacks callbacks, in Uri uri) = 10;
    void deleteDataItemsWithFilter(IWearableCallbacks callbacks, in Uri uri, int typeFilter) = 40;

    void sendMessage(IWearableCallbacks callbacks, String targetNodeId, String path, in byte[] data) = 11;
    void getFdForAsset(IWearableCallbacks callbacks, in Asset asset) = 12;

    void getLocalNode(IWearableCallbacks callbacks) = 13;
    void getConnectedNodes(IWearableCallbacks callbacks) = 14;

    // Capabilties
    void getConnectedCapability(IWearableCallbacks callbacks, String capability, int nodeFilter) = 41;
    void getConnectedCapaibilties(IWearableCallbacks callbacks, int nodeFilter) = 42;
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

    void sendRemoteCommand(IWearableCallbacks callbacks, byte b) = 52;

    // deprecated Connection
    void putConnection(IWearableCallbacks callbacks, in ConnectionConfiguration config) = 1;
    void getConnection(IWearableCallbacks callbacks) = 2;
    void enableConnection(IWearableCallbacks callbacks) = 3;
    void disableConnection(IWearableCallbacks callbacks) = 4;
}
