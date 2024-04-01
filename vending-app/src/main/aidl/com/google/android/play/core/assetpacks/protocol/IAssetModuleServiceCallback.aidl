/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks.protocol;

interface IAssetModuleServiceCallback {
    void onStartDownload(int status, in Bundle bundle) = 1;
    void onCancelDownload(int status) = 2;
    void onGetSession(int status) = 3;
    void onGetSessionStates(in List<Bundle> list) = 4;
    void onNotifyChunkTransferred(in Bundle bundle) = 5;
    void onError(in Bundle bundle) = 6;
    void onNotifyModuleCompleted(in Bundle bundle) = 7;
    void onNotifySessionFailed(in Bundle bundle) = 9;
    void onKeepAlive(in Bundle bundle, in Bundle bundle2) = 10;
    void onGetChunkFileDescriptor(in Bundle bundle, in Bundle bundle2) = 11;
    void onRequestDownloadInfo(in Bundle bundle, in Bundle bundle2) = 12;
    void onRemoveModule() = 13;
    void onCancelDownloads() = 14;
}