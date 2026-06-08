/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.formats.client;

import com.google.android.gms.dynamic.IObjectWrapper;

interface IOnPublisherAdViewLoadedListener {
    void onPublisherAdViewLoaded(IObjectWrapper view) = 0;
}
