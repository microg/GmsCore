/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.inappreview.protocol;

import android.os.Bundle;

interface IInAppReviewServiceCallback {
    oneway void onRequestInstallCallback(in Bundle bundle) = 1;
}