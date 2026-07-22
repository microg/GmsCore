/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.appupdate.protocol;

import com.google.android.play.core.appupdate.protocol.IAppUpdateServiceCallback;

interface IAppUpdateService {
    oneway void requestUpdateInfo(String packageName, in Bundle bundle, in IAppUpdateServiceCallback callback) = 1;
    oneway void completeUpdate(String packageName, in Bundle bundle, in IAppUpdateServiceCallback callback) = 2;
    oneway void updateProgress(in Bundle bundle) = 3;
}