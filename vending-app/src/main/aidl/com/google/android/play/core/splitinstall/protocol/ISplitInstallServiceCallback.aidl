/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.play.core.splitinstall.protocol;


interface ISplitInstallServiceCallback {
    oneway void onStartInstall(int status, in Bundle bundle) = 1;
    oneway void onInstallCompleted(int status, in Bundle bundle) = 2;
    oneway void onCancelInstall(int status, in Bundle bundle) = 3;
    oneway void onGetSessionState(int status, in Bundle bundle) = 4;
    oneway void onError(in Bundle bundle) = 5;
    oneway void onGetSessionStates(in List<Bundle> list) = 6;
    oneway void onDeferredUninstall(in Bundle bundle) = 7;
    oneway void onDeferredInstall(in Bundle bundle) = 8;
    oneway void onDeferredLanguageInstall(in Bundle bundle) = 11;
    oneway void onDeferredLanguageUninstall(in Bundle bundle) = 12;
}