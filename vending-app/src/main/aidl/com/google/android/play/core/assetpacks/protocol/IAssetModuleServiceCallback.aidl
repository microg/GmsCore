/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks.protocol;

interface IAssetModuleServiceCallback {
    oneway void onStartDownload(int status, in Bundle bundle) = 1;
    oneway void onCancelDownload(int status) = 2;
    oneway void onGetSession(int status) = 3;
    oneway void onGetSessionStates(in List<Bundle> list) = 4;
    oneway void onNotifyChunkTransferred(in Bundle bundle,in Bundle bundle2) = 5;
    oneway void onError(in Bundle bundle) = 6;
    oneway void onNotifyModuleCompleted(in Bundle bundle,in Bundle bundle2) = 7;
    oneway void onNotifySessionFailed(in Bundle bundle) = 9;
    oneway void onKeepAlive(in Bundle bundle, in Bundle bundle2) = 10;
    oneway void onGetChunkFileDescriptor(in Bundle bundle, in Bundle bundle2) = 11;
    oneway void onRequestDownloadInfo(in Bundle bundle, in Bundle bundle2) = 12;
    oneway void onRemoveModule() = 13;
    oneway void onCancelDownloads() = 14;
}