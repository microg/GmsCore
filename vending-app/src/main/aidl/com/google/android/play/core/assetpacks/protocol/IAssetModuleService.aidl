/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks.protocol;

import com.google.android.play.core.assetpacks.protocol.IAssetModuleServiceCallback;

interface IAssetModuleService {
    void startDownload(String packageName, in List<Bundle> list, in Bundle bundle, in IAssetModuleServiceCallback callback) = 1;
    void getSessionStates(String packageName, in Bundle bundle, in IAssetModuleServiceCallback callback) = 4;
    void notifyChunkTransferred(String packageName, in Bundle bundle, in Bundle bundle2, in IAssetModuleServiceCallback callback) = 5;
    void notifyModuleCompleted(String packageName, in Bundle bundle, in Bundle bundle2, in IAssetModuleServiceCallback callback) = 6;
    void notifySessionFailed(String packageName, in Bundle bundle, in Bundle bundle2, in IAssetModuleServiceCallback callback) = 8;
    void keepAlive(String packageName, in Bundle bundle, in IAssetModuleServiceCallback callback) = 9;
    void getChunkFileDescriptor(String packageName, in Bundle bundle, in Bundle bundle2, in IAssetModuleServiceCallback callback) = 10;
    void requestDownloadInfo(String packageName, in List<Bundle> list, in Bundle bundle, in IAssetModuleServiceCallback callback) = 11;
    void removeModule(String packageName, in Bundle bundle, in Bundle bundle2, in IAssetModuleServiceCallback callback) = 12;
    void cancelDownloads(String packageName, in List<Bundle> list, in Bundle bundle, in IAssetModuleServiceCallback callback) = 13;
}