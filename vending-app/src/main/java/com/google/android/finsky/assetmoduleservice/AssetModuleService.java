/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.assetmoduleservice;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.play.core.assetpacks.protocol.IAssetModuleService;
import com.google.android.play.core.assetpacks.protocol.IAssetModuleServiceCallback;

import java.util.ArrayList;
import java.util.List;

public class AssetModuleService extends Service {
    private static final String TAG = "AssetModuleService";

    private final List<String> requested = new ArrayList<>();

    private final IAssetModuleService.Stub service = new IAssetModuleService.Stub() {

        @Override
        public void startDownload(String packageName, List<Bundle> list, Bundle bundle, IAssetModuleServiceCallback callback) throws RemoteException {
            Log.d(TAG, "Method (startDownload) called by packageName -> " + packageName);
            Bundle result = new Bundle();
            result.putStringArrayList("pack_names", new ArrayList<>());
            callback.onStartDownload(-1, result);
        }

        @Override
        public void getSessionStates(String packageName, Bundle bundle, IAssetModuleServiceCallback callback) {
            Log.d(TAG, "Method (getSessionStates) called but not implement by packageName -> " + packageName);
        }

        @Override
        public void notifyChunkTransferred(String packageName, Bundle bundle, Bundle bundle2, IAssetModuleServiceCallback callback) {
            Log.d(TAG, "Method (notifyChunkTransferred) called but not implement by packageName -> " + packageName);
        }

        @Override
        public void notifyModuleCompleted(String packageName, Bundle bundle, Bundle bundle2, IAssetModuleServiceCallback callback) {
            Log.d(TAG, "Method (notifyModuleCompleted) called but not implement by packageName -> " + packageName);
        }

        @Override
        public void notifySessionFailed(String packageName, Bundle bundle, Bundle bundle2, IAssetModuleServiceCallback callback) {
            Log.d(TAG, "Method (notifySessionFailed) called but not implement by packageName -> " + packageName);
        }

        @Override
        public void keepAlive(String packageName, Bundle bundle, IAssetModuleServiceCallback callback) {
            Log.d(TAG, "Method (keepAlive) called but not implement by packageName -> " + packageName);
        }

        @Override
        public void getChunkFileDescriptor(String packageName, Bundle bundle, Bundle bundle2, IAssetModuleServiceCallback callback) {
            Log.d(TAG, "Method (getChunkFileDescriptor) called but not implement by packageName -> " + packageName);
        }

        @Override
        public void requestDownloadInfo(String packageName, List<Bundle> list, Bundle bundle, IAssetModuleServiceCallback callback) throws RemoteException {
            Log.d(TAG, "Method (requestDownloadInfo) called by packageName -> " + packageName);
            Bundle result = new Bundle();
            if (requested.contains(packageName)) {
                result.putInt("error_code", -5);
                callback.onError(result);
                return;
            }
            requested.add(packageName);
            result.putStringArrayList("pack_names", new ArrayList<>());
            callback.onRequestDownloadInfo(result, result);
        }

        @Override
        public void removeModule(String packageName, Bundle bundle, Bundle bundle2, IAssetModuleServiceCallback callback) {
            Log.d(TAG, "Method (removeModule) called but not implement by packageName -> " + packageName);
        }

        @Override
        public void cancelDownloads(String packageName, List<Bundle> list, Bundle bundle, IAssetModuleServiceCallback callback) {
            Log.d(TAG, "Method (cancelDownloads) called but not implement by packageName -> " + packageName);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return service.asBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        requested.clear();
        return super.onUnbind(intent);
    }
}