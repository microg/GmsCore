/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.play.core.splitinstall.protocol;
import com.google.android.play.core.splitinstall.protocol.ISplitInstallServiceCallback;

interface ISplitInstallService {
    void startInstall(String pkg,in List<Bundle> splits,in Bundle bundle, ISplitInstallServiceCallback callback) = 1;
    void completeInstalls(String pkg, int sessionId,in Bundle bundle, ISplitInstallServiceCallback callback) = 2;
    void cancelInstall(String pkg, int sessionId, ISplitInstallServiceCallback callback) = 3;
    void getSessionState(String pkg, int sessionId, ISplitInstallServiceCallback callback) = 4;
    void getSessionStates(String pkg, ISplitInstallServiceCallback callback) = 5;
    void splitRemoval(String pkg,in List<Bundle> splits, ISplitInstallServiceCallback callback) = 6;
    void splitDeferred(String pkg,in List<Bundle> splits,in Bundle bundle, ISplitInstallServiceCallback callback) = 7;
    void getSessionState2(String pkg, int sessionId, ISplitInstallServiceCallback callback) = 8;
    void getSessionStates2(String pkg, ISplitInstallServiceCallback callback) = 9;
    void getSplitsAppUpdate(String pkg, ISplitInstallServiceCallback callback) = 10;
    void completeInstallAppUpdate(String pkg, ISplitInstallServiceCallback callback) = 11;
    void languageSplitInstall(String pkg,in List<Bundle> splits,in Bundle bundle, ISplitInstallServiceCallback callback) = 12;
    void languageSplitUninstall(String pkg,in List<Bundle> splits, ISplitInstallServiceCallback callback) =13;
}