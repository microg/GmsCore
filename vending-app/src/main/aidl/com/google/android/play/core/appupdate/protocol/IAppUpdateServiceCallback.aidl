/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.appupdate.protocol;

interface IAppUpdateServiceCallback {
    oneway void onUpdateResult(in Bundle bundle) = 1;
    oneway void onCompleteResult(in Bundle bundle) = 2;
}