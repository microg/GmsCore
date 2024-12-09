/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks.protocol;

import com.google.android.play.core.assetpacks.protocol.IAssetModuleServiceCallback;

interface IAssetModuleService {
    oneway void startDownload(String packageName, in List<Bundle> list, in Bundle bundle, in IAssetModuleServiceCallback callback) = 1;
    oneway void getSessionStates(String packageName, in Bundle bundle, in IAssetModuleServiceCallback callback) = 4;
    oneway void notifyChunkTransferred(String packageName, in Bundle bundle, in Bundle bundle2, in IAssetModuleServiceCallback callback) = 5;
    oneway void notifyModuleCompleted(String packageName, in Bundle bundle, in Bundle bundle2, in IAssetModuleServiceCallback callback) = 6;
    oneway void notifySessionFailed(String packageName, in Bundle bundle, in Bundle bundle2, in IAssetModuleServiceCallback callback) = 8;
    oneway void keepAlive(String packageName, in Bundle bundle, in IAssetModuleServiceCallback callback) = 9;
    oneway void getChunkFileDescriptor(String packageName, in Bundle bundle, in Bundle bundle2, in IAssetModuleServiceCallback callback) = 10;
    oneway void requestDownloadInfo(String packageName, in List<Bundle> list, in Bundle bundle, in IAssetModuleServiceCallback callback) = 11;
    oneway void removeModule(String packageName, in Bundle bundle, in Bundle bundle2, in IAssetModuleServiceCallback callback) = 12;
    oneway void cancelDownloads(String packageName, in List<Bundle> list, in Bundle bundle, in IAssetModuleServiceCallback callback) = 13;
}